package tests;

import file_ops.ConfigFile;
import file_ops.ResultFile;
import integrations.SlackClient;
import integrations.WebAppDBConnection;
import logging.Logger;
import logging.TestLog;
import logging.TestResultToJSONConverter;
import models.ElastiCube;
import org.json.JSONException;
import run_strategy.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class MainTest {

    private int attamptNumber;
    private final int maxNumberAttempts = 5;
    private List<ElastiCube> elastiCubes;
    private Logger logger = Logger.getInstance();
    private ResultFile resultFile = ResultFile.getInstance();
    private ConfigFile configFile = ConfigFile.getInstance();
    private boolean isSlackEnabled = !configFile.getSlackWebhookURL().isEmpty();
    private TestLog testLog = TestLog.getInstance();
    private StrategyContext strategyContext = new StrategyContext();

    public MainTest(List<ElastiCube> elastiCubes){
        this.elastiCubes = elastiCubes;
    }

    public void init() throws JSONException {
        preRun();

        attamptNumber = 1;
        run(attamptNumber);
    }

    private void preRun(){

        resultFile.delete();
        if (!configFile.isConfigFileValid()){
            quitWithPreRunFailure("Invalid config.properties file");
        } else {
            logger.write(configFile.toString());
            resultFile.create();
        }
    }

    private void run(int attempt) throws JSONException {

        String methodName = "[MainTest.Run] ";

        // Check number of attempts
        if (attempt > maxNumberAttempts){
            String message = "Max number of attempts " + maxNumberAttempts + " exceeded.";
            logger.write(methodName + message);
            testLog.setReasonForFailure(message);
            quitWithRunFailure();
        }

        logger.write(methodName + "Attempt number " + attempt);

        // check to see if 0 elasticubes returned with error
        if (elastiCubes == null){
            setAndExecuteStrategy();
            runECSTelnetTests();
            retry();
        }
        // if no ECS error
        else {

            // end test if 0 running cubes
            if (elastiCubes.size() == 0){

                quitWithSuccessNoRunningCubes();
            }
            // if more than 0 running cubes
            else {
                logger.write(methodName + "Found " +elastiCubes.size() + " running ElastiCubes: \n" + Arrays.toString(elastiCubes.toArray()));
                testLog.setNumberOfElastiCubes(elastiCubes.size());

                // execute REST API tests and remove ElastiCubes that their REST API tests succeeded
                try {
                    executeRESTAPITests();

                } catch (IOException e) {
                    logger.write(methodName + "failed to execute test, error: " + e.getMessage());
                }
            }
        }
    }

    private void retry() throws JSONException {

        run(++attamptNumber);

    }

    private void quitWithSuccess(int numberOfElastiCubesTested){

        String methodName = "[MainTest.quitWithSuccess] ";
        testLog.setNumberOfElastiCubes(numberOfElastiCubesTested);
        testLog.setHealthy(true);
        testLog.setTestEndTime(new Date());

        try {
            WebAppDBConnection.sendOperation(testLog.toJSON());
        } catch (ParseException | IOException | JSONException e) {
            logger.write(methodName + "WARNING - Error sending test log:" + e.getMessage());
        }
        resultFile.write(true);
        logger.write("[MainTest.quitWithSuccess] EXITING...");
        System.exit(0);

    }

    private void quitWithElastiCubeFailures(int numberOfElastiCubesTested){
        String methodName = "[MainTest.quitWithElastiCubeFailures] ";
        testLog.setNumberOfElastiCubes(numberOfElastiCubesTested);
        testLog.setHealthy(false);
        testLog.setTestEndTime(new Date());

        try {
            WebAppDBConnection.sendOperation(testLog.toJSON());
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed");
        } catch (ParseException | IOException | JSONException e) {
            logger.write(methodName + "WARNING - Error sending test log:" + e.getMessage());
        }
        resultFile.write(false);
        logger.write("[MainTest.quitWithElastiCubeFailures] EXITING...");
        System.exit(0);
    }

    private void quitWithSuccessNoRunningCubes(){
        String methodName = "[MainTest.quitWithSuccessNoRunningCubes] ";
        testLog.setNumberOfElastiCubes(0);
        testLog.setHealthy(true);
        testLog.setTestEndTime(new Date());
        logger.write(methodName + "No ElastiCubes found, no errors from ECS.");
        resultFile.write(true);
        logger.write(methodName + " EXITING...");
        System.exit(0);
    }

    private void quitWithRunFailure(){

        // set test log
        testLog.setNumberOfElastiCubes(0);
        testLog.setHealthy(false);
        testLog.setTestEndTime(new Date());
        resultFile.write(false);
        logger.write("[MainTest.quitWithRunFailure] EXITING...");

        // notify slack
        if (isSlackEnabled){
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog tests exceeded" + maxNumberAttempts + " - test failed");
        }

        // notify webdb
        try {
            WebAppDBConnection.sendOperation(testLog.toJSON());
        } catch (ParseException | IOException | JSONException e) {
            logger.write("[App.run] WARNING - Error sending test log:" + e.getMessage());
        }

        // exit
        System.exit(1);
    }

    private void quitWithPreRunFailure(String reasonForQuit){

        logger.write("[MainTest.quitWithPreRunFailure] " + reasonForQuit);
        logger.write("[MainTest.quitWithPreRunFailure] EXITING...");
        System.exit(1);
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
        TelnetTest.isConnected("localhost", 811);
        TelnetTest.isConnected("localhost", 812);
    }

    private void executeRESTAPITests() throws JSONException, IOException {
        String methodName = "[MainTest.restAPITestResultSet] ";
        Map<String, Boolean> restAPITests = new HashMap<>(elastiCubes.size());
        logger.write(methodName + "Running REST API tests...");

        for (ElastiCube elastiCube : elastiCubes){

            SisenseRESTAPIClient client = new SisenseRESTAPIClient(elastiCube.getName());
            client.exeecuteQuery();
            restAPITests.put(elastiCube.getName(), client.isCallSuccessful());

            // check if test failed and send warning and execute MonetDB query
            if (!client.isCallSuccessful()){
                SlackClient.getInstance()
                        .sendMessage(":warning: WARNING! REST API test failed for ElastiCube " +
                                elastiCube.getName() + " ");
                try {
                    executeMonetDBTest(elastiCube);
                } catch (InterruptedException e) {
                    logger.write(methodName + "ERROR running MonetDB test: " + e.getMessage());
                }
            }
        }

        logger.write(methodName + "REST API test results:");
        logger.write(TestResultToJSONConverter.toJSON(restAPITests).toString(3));

        for (Map.Entry<String, Boolean> entry : restAPITests.entrySet()){

            // exit with success if REST API tests failed
            if (!entry.getValue()){
                quitWithElastiCubeFailures(elastiCubes.size());
            }
            // exit with success if REST API tests succeeded
            else {
                quitWithSuccess(elastiCubes.size());
            }
        }
    }

    private void executeMonetDBTest(ElastiCube elastiCube) throws IOException, InterruptedException {

        logger.write("[MainTest.executeMonetDBTests] Running MonetDB test... ");

        MonetDBTest monetDBTest = new MonetDBTest(elastiCube);
        monetDBTest.executeQuery();

        logger.write("[MainTest.executeMonetDBTests] MonetDB query result for ElastiCube " + elastiCube.getName() + ":" + monetDBTest.isQuerySuccessful());
        testLog.addElastiCubeToFailedElastiCubes(elastiCube.getName(), monetDBTest.isQuerySuccessful());

    }

}
