import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import file_ops.VersionFile;
import integrations.SlackClient;
import integrations.WebAppDBConnection;
import logging.Logger;
import logging.TestLog;
import logging.TestResultToJSONConverter;
import models.ElastiCube;
import org.json.JSONException;
import run_strategy.*;
import tests.MonetDBTest;
import tests.SisenseRESTAPI;
import tests.TelnetTest;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class App {

    private static String runningLocation;
    private static final ConfigFile configFile = ConfigFile.getInstance();
    private static ResultFile resultFile = ResultFile.getInstance();
    private static final CmdOperations operations = CmdOperations.getInstance();
    private static Logger logger = Logger.getInstance();
    private static StrategyContext strategyContext = new StrategyContext();
    private static TestLog testLog = TestLog.getInstance();
    private static String host = System.getenv("COMPUTERNAME");

    public static void main(String[] args) throws JSONException {

        setRunningLocation();
        testLog.setHost(host);
        testLog.setTestStartTime(new Date());
        logger.write("[App.main] STARTING...");

        VersionFile versionFile = new VersionFile(runningLocation);
        logger.write("[App.main] Application version: " + versionFile.getVersion());


        if (!operations.getSisenseVersion().equals("CANNOT DETECT")) {
            logger.write("[App.main] Sisense version: " + operations.getSisenseVersion());
        }
        preRun();

        int attempt = 1;
        run(attempt);

    }

    private static void preRun(){

        resultFile.delete();
        if (!configFile.isConfigFileValid()){
            logger.write("Exiting...");
            testLog.setReasonForFailure("Invalid config.properties file");
            System.exit(1);
        }
        else {
            logger.write(configFile.toString());
            resultFile.create();
        }
    }

    private static void run(int attempt) throws JSONException {

        if (attempt > 5) {
            logger.write("5 run attempts exceeded. Exiting...");

            if (!ConfigFile.getInstance().getSlackWebhookURL().isEmpty()){
                SlackClient.getInstance().sendMessage();
            }
            resultFile.write(false);
            testLog.setNumberOfElastiCubes(0);
            testLog.setReasonForFailure("5 run attempts exceeded");
            testLog.setHealthy(false);
            testLog.setTestEndTime(new Date());
            try {
                WebAppDBConnection.sendOperation(testLog.toJSON());
            } catch (IOException | ParseException e) {
                logger.write("[App.run] WARNING - Error sending test log:" + e.getMessage());
            }
            System.exit(0);
        }

        logger.write("[App.run] Attempt number " + attempt);
        logger.write("[App.run] Retrieving list of ElastiCubes...");
        List<ElastiCube> elastiCubeList = operations.getListElastiCubes();

        // Check if ECS returned 0 ElastiCubes with error
        if (elastiCubeList == null){
            setAndExecuteStrategy();
            runECSTelnetTests();
            run(++attempt);
        } else {


            // Check if ECS returned 0 ElastiCubes without error
            if (elastiCubeList.size() == 0){
                logger.write("[App.run] No ElastiCubes found, no errors from ECS. Exiting...");
                resultFile.write(true);
                testLog.setTestEndTime(new Date());
                testLog.setHealthy(true);
                testLog.setNumberOfElastiCubes(0);
                try {
                    WebAppDBConnection.sendOperation(testLog.toJSON());
                } catch (IOException | ParseException e) {
                    logger.write("[App.run] WARNING - Error sending test log:" + e.getMessage());
                }
                System.exit(1);
            }
            else {

                logger.write("[App.run] Found " + elastiCubeList.size() + " running ElastiCubes: \n" + Arrays.toString(elastiCubeList.toArray()));
                testLog.setNumberOfElastiCubes(elastiCubeList.size());
                // Get list of ElastiCubes
                Map<String, Boolean> tests = new HashMap<>(elastiCubeList.size());
                logger.write("[App.run] Running REST API tests... ");
                for (ElastiCube elasticube : elastiCubeList){
                    tests.put(elasticube.getName(), SisenseRESTAPI.queryTableIsSuccessful(elasticube.getName()));
                }

                boolean testResult = true;
                logger.write("[App.run] REST API test results:");
                logger.write("[App.run] " + TestResultToJSONConverter.toJSON(tests));
                for (Map.Entry<String, Boolean> entry : tests.entrySet()){
                    if (!entry.getValue()){
                        testResult = false;
                        testLog.setHealthy(false);
                        testLog.setReasonForFailure(entry.getKey() + " REST API query failed");
                    }
                    // Remove ElastiCubes which API query was successful for
                    else {
                        elastiCubeList.removeIf(elastiCube -> elastiCube.getName().equals(entry.getKey()));
                    }
                }

                // Run MonetDB tests on ElastiCubes that failed REST API call
                MonetDBTest monetDBTest = new MonetDBTest(elastiCubeList);
                if (elastiCubeList.size() > 0){
                    logger.write("[App.run] Running MonetDB tests... ");

                    try {
                        Map<String, Boolean> monetDBTestSet = monetDBTest.resultSet();
                        logger.write("[App.run] MonetDB test results: ");
                        logger.write("[App.run] " + TestResultToJSONConverter.toJSON(monetDBTestSet));
                        for (Map.Entry<String, Boolean> entry : monetDBTestSet.entrySet()){
                            if (!entry.getValue()){
                                testLog.appendReasonForFailure(entry.getKey() + " MonetDB query failed.");
                            }
                        }

                    } catch (IOException | InterruptedException e) {
                        logger.write("[App.run] ERROR - " + e.getMessage());
                    }
                }
                // Check whether the test failed and Slack webhook is set
                if (!testResult && !ConfigFile.getInstance().getSlackWebhookURL().isEmpty()){
                    SlackClient.getInstance().sendMessage();
                }

                resultFile.write(testResult);
                testLog.setHealthy(testResult);
                testLog.setTestEndTime(new Date());
                try {
                    WebAppDBConnection.sendOperation(testLog.toJSON());
                } catch (IOException | ParseException e) {
                    logger.write("[App.run] WARNING - Test not sent to web application: " + e.getMessage());
                }
                logger.write("[App.run] EXITING...");

                System.exit(1);
            }
        }
    }

    private static void runECSTelnetTests(){
        TelnetTest.isConnected(logger, "localhost", 811);
        TelnetTest.isConnected(logger, "localhost", 812);
    }

    private static void setAndExecuteStrategy(){

        if (configFile.isEcsDump() && configFile.isIisDump() && !configFile.restartECS() && !configFile.restartIIS()){
            strategyContext.setStrategy(new CompleteDumpStrategy());
        } else if (configFile.isEcsDump() && configFile.isIisDump() && configFile.restartECS() && configFile.restartIIS()){
            strategyContext.setStrategy(new CompleteResetAndDumpStrategy());
        } else if (!configFile.isEcsDump() && !configFile.isIisDump() && configFile.restartECS() && configFile.restartIIS()){
            strategyContext.setStrategy(new CompleteResetStrategy());
        } else if (configFile.isEcsDump() && !configFile.isIisDump() && !configFile.restartECS() && !configFile.restartIIS()){
            strategyContext.setStrategy(new ECSDumpStrategy());
        } else if (!configFile.isEcsDump() && !configFile.isIisDump() && configFile.restartECS() && !configFile.restartIIS()){
            strategyContext.setStrategy(new ECSResetStrategy());
        } else if (!configFile.isEcsDump() && configFile.isIisDump() && !configFile.restartECS() && !configFile.restartIIS()){
            strategyContext.setStrategy(new IISDumpStrategy());
        } else if (!configFile.isEcsDump() && !configFile.isIisDump() && !configFile.restartECS() && configFile.restartIIS()){
            strategyContext.setStrategy(new IISResetStrategy());
        } else
            strategyContext.setStrategy(new NoResetNoDumpStrategy());

        strategyContext.runStrategy();
    }

//    private static String getVersion() throws URISyntaxException, IOException {
//
//        Properties properties = new Properties();
//        String versionFile = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + File.separator + "version.properties";
//        try (InputStream inputStream = new FileInputStream(versionFile )){
//            properties.load(inputStream);
//            return properties.getProperty("version");
//        }
//    }

//    private static String getVersion() {
//
//        Enumeration enumeration;
//        try {
//            enumeration = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
//            while (enumeration.hasMoreElements()){
//                try {
//                    URL url = (URL) enumeration.nextElement();
//                    try (InputStream inputStream = url.openStream()){
//                        Manifest manifest = new Manifest(inputStream);
//                        Attributes attributes = manifest.getMainAttributes();
//                        String version = attributes.getValue("Implementation-Version");
//                        if (version != null){
//                            return version;
//                        }
//                    }
//                } catch (Exception ignored){
//
//                }
//            }
//        } catch (IOException ignored){
//
//        }
//        return null;
//    }

    private static void setRunningLocation(){
        try {
            runningLocation = new File(App.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getParent();
        } catch (URISyntaxException e) {
            logger.write("[App.runningLocation] ERROR : " + e.getMessage());
        }
    }

}