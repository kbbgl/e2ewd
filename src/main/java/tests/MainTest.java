package tests;

import cmd_ops.CmdOperations;
import cmd_ops.ElastiCubeRESTAPIClient;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import integrations.SlackClient;
import integrations.WebAppDBConnection;
import logging.TestLog;
import logging.TestResultToJSONConverter;
import models.ElastiCube;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run_strategy.*;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

public class MainTest {

    private final Logger logger = LoggerFactory.getLogger(MainTest.class);
    private boolean testSuccess;
    private int attamptNumber;
    private final int maxNumberAttempts = 5;
    private List<ElastiCube> elastiCubes;
    private ResultFile resultFile = ResultFile.getInstance();
    private ConfigFile configFile = ConfigFile.getInstance();
    private TestLog testLog = TestLog.getInstance();
    private StrategyContext strategyContext = new StrategyContext();

    public MainTest(){
    }

    public void init() throws JSONException {
        logger.debug("Initiating test...");
        preRun();

        attamptNumber = 1;
        run(attamptNumber);
    }

    private void preRun(){
        logger.debug("Executing pre-run test validations...");
        resultFile.delete();

        resultFile.create();
    }

    private void run(int attempt) throws JSONException {

        // Set test success initially
        setTestSuccess(true);

        // Check number of attempts
        if (attempt > maxNumberAttempts){
            String message = "Max number of attempts " + maxNumberAttempts + " exceeded.";
            logger.warn(message);
            setTestSuccess(false);
            setNumberOfElastiCubes(0);
            terminate(message);
        }

        logger.info("Attempt number " + attempt);


        // Run Broker health test
        if (configFile.isRunBrokerHealthCheck()){
            try {
                BrokerHealthClient brokerHealthClient = BrokerHealthClient.getInstance();
                brokerHealthClient.executeQuery();
            } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                logger.error("Error initializing broker health client: " + e.getMessage());
            }
        }


        // Run microservices health test
        if (configFile.isRunMicroservicesHealthCheck()){
            try {
                MicroservicesHealthClient microservicesHealthClient = MicroservicesHealthClient.getInstance();
                microservicesHealthClient.executeCall();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
                logger.error("Error running microservices health test: " + e.getMessage());
            }
        }

        // Retrieve list of RUNNING ElastiCubes
        logger.info("Retrieving list of ElastiCubes...");

        // requirement https://github.com/kbbgl/e2ewd/issues/39 to use API instead of PSM to retrieve EC names
        //        elastiCubes = CmdOperations.getInstance().getListElastiCubes();

        // Create EC client and retrieve list of ElastiCubes
        try {
            ElastiCubeRESTAPIClient ecClient = new ElastiCubeRESTAPIClient();
            elastiCubes = ecClient.getListOfElastiSucbes();

            // Case when API call to get ElastiCubes succeeded but 0 returned
            if (ecClient.isCallSuccessful() && elastiCubes.size() == 0){
                String defaultEC = ecClient.getDefaultElastiCube();
                logger.info("No ElastiCubes in RUNNING mode.");
                logger.info("Chosen default ElastiCube to start: " + defaultEC);
                CmdOperations.getInstance().runDefaultElastiCube(defaultEC);
                retry();
            } else if (!ecClient.isCallSuccessful()){
                logger.error("API call to retrieve ElastiCubes was not successful");
                setAndExecuteStrategy();
                runECSTelnetTests();
                retry();
            } else if(elastiCubes.size() > 0){
                setNumberOfElastiCubes(elastiCubes.size());

                JSONArray elastiCubesJSONArray = new JSONArray(elastiCubes.toArray());
                logger.info("Found " +elastiCubes.size() + " running ElastiCubes: \n" + elastiCubesJSONArray.toString(3));

                // execute REST API tests and remove ElastiCubes that their REST API tests succeeded
                try {
                    executeRESTAPITests();

                } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                    logger.error("Failed to execute JAQL REST API call: " + e.getMessage());
                    logger.debug(Arrays.toString(e.getStackTrace()));
                    testSuccess = false;
                    terminate("Error running REST API test: " + e.getMessage());
                }
            }

        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException | InterruptedException e) {
            logger.error("Error running API call to retrieve ElastiCubes. Exception: \n" + Arrays.toString(e.getStackTrace()));
            terminate("Could not get ElastiCubes from API: " + e.getMessage());
        }
    }

    private void retry() throws JSONException {

        logger.info("Retrying...");
        run(++attamptNumber);

    }

    private void setAndExecuteStrategy(){
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

    private static void runECSTelnetTests(){
        TelnetTest.isConnected(811);
        TelnetTest.isConnected(812);
    }

    private void executeRESTAPITests() throws JSONException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Map<String, Boolean> restAPITests = new HashMap<>(elastiCubes.size());
        logger.info("Running REST API tests...");

        for (ElastiCube elastiCube : elastiCubes){

            // close client after
            SisenseRESTAPIClient client = new SisenseRESTAPIClient(elastiCube.getName());
            client.executeQuery();
            restAPITests.put(elastiCube.getName(), client.isCallSuccessful());

            // check if test failed and send warning and execute MonetDB query
            if (!client.isCallSuccessful()){
                logger.warn("REST API test failed for ElastiCube " +
                            elastiCube.getName() +
                            " with response code " +
                            client.getResponseCode() +
                            ", response body: \n" +
                            client.getCallResponse());

                SlackClient.getInstance()
                        .sendMessage(":warning: WARNING! REST API test failed for ElastiCube *" +
                                elastiCube.getName() + "* with response code `" + client.getResponseCode() + "`, response body: \n```" + client.getCallResponse() + "```");

                setTestSuccess(false);

                try {
                    executeMonetDBTest(elastiCube);

                } catch (InterruptedException e) {
                    logger.error("Error running MonetDB test: " +e.getMessage());
                    logger.debug(Arrays.toString(e.getStackTrace()));
                    testSuccess = false;
                    terminate("Error running MonetDB test: " + e.getMessage());
                }

                // If response code is 404 or 401, do not run any more tests
                if (client.getResponseCode() == 404 || client.getResponseCode() == 401){
                    break;
                }

            }
        }

        logger.info("REST API test results:");
        logger.info(TestResultToJSONConverter.toJSON(restAPITests).toString(3));

        terminate();

    }

    private void terminate(){

        testLog.setTestEndTime(new Date());
        testLog.setHealthy(testSuccess);

        // send Slack notification if enabled and test failed
        if (!isTestSuccess() && SlackClient.getInstance() != null){
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed ");
        }

        // send test to web app db
        // quota exceeded so disabling
//        try {
//            WebAppDBConnection.sendOperation(testLog.toJSON());
//        } catch (IOException | ParseException | JSONException e) {
//            logger.warn("Failed sending test log to mongo: " + e.getMessage());
//        }

        ResultFile.getInstance().write(testSuccess);
        logger.info("Test result: " + testSuccess);
        logger.info("EXITING...");
        System.exit(0);

    }

    public void terminate(String reasonForFailure){

        testLog.setTestEndTime(new Date());
        testLog.setReasonForFailure(reasonForFailure);
        testLog.setHealthy(testSuccess);

        // send Slack notification if enabled and test failed
        if (!isTestSuccess() && SlackClient.getInstance() != null){
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed ");
        }

        // send test to web app db
        // quota exceeded so disabling
//        try {
//            WebAppDBConnection.sendOperation(testLog.toJSON());
//        } catch (IOException | ParseException | JSONException e) {
//            logger.warn("Failed sending test log to mongo: " + e.getMessage());
//        }

        ResultFile.getInstance().write(testSuccess);
        logger.info("Test result: " + testSuccess);
        logger.info("EXITING...");
        System.exit(0);

    }

    private void executeMonetDBTest(ElastiCube elastiCube) throws IOException, InterruptedException {

        logger.info("Running MonetDB test...");

        MonetDBTest monetDBTest = new MonetDBTest(elastiCube);
        monetDBTest.executeQuery();

        logger.info("MonetDB query result for ElastiCube " + elastiCube.getName() + ":" + monetDBTest.isQuerySuccessful());
        if (!monetDBTest.isQuerySuccessful() && configFile.isEcDump()){
            CmdOperations.getInstance().ecDump(elastiCube);
        }
        logger.info("Number of concurrent connection to ElastiCube " + elastiCube.getName() +  " : " + CmdOperations.getInstance().getMonetDBConcurrentConnections(elastiCube));
        testLog.addElastiCubeToFailedElastiCubes(elastiCube.getName(), monetDBTest.isQuerySuccessful());

    }

    private boolean isTestSuccess() {
        return testSuccess;
    }

    private void setTestSuccess(boolean testSuccess) {
        this.testSuccess = testSuccess;
        testLog.setHealthy(testSuccess);
    }

    private void setNumberOfElastiCubes(int numberOfElastiCubes){
        testLog.setNumberOfElastiCubes(numberOfElastiCubes);
    }

}
