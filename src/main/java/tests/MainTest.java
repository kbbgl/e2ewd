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

    private boolean testSuccess;
    private int attamptNumber;
    private final int maxNumberAttempts = 5;
    private int numberOfElastiCubes;
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
            terminate("Invalid config.properties file");
        } else {
            logger.write(configFile.toString());
            resultFile.create();
        }
    }

    private void run(int attempt) throws JSONException {

        String methodName = "[MainTest.Run] ";

        // Set test success initially
        setTestSuccess(true);

        // Check number of attempts
        if (attempt > maxNumberAttempts){
            String message = "Max number of attempts " + maxNumberAttempts + " exceeded.";
            logger.write(methodName + message);
            setTestSuccess(false);
            setNumberOfElastiCubes(0);
            terminate(message);
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

            setNumberOfElastiCubes(elastiCubes.size());

            // end test if 0 running cubes
            if (elastiCubes.size() == 0){
                setTestSuccess(true);
                terminate();
            }
            // if more than 0 running cubes
            else {
                logger.write(methodName + "Found " +elastiCubes.size() + " running ElastiCubes: \n" + Arrays.toString(elastiCubes.toArray()));

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
            client.executeQuery();
            restAPITests.put(elastiCube.getName(), client.isCallSuccessful());

            // check if test failed and send warning and execute MonetDB query
            if (!client.isCallSuccessful()){
                SlackClient.getInstance()
                        .sendMessage(":warning: WARNING! REST API test failed for ElastiCube *" +
                                elastiCube.getName() + "* with response code *" + client.getResponseCode() + "*, response body: " + client.getCallResponse() + " ");

                setTestSuccess(false);

                try {
                    executeMonetDBTest(elastiCube);
                } catch (InterruptedException e) {
                    logger.write(methodName + "ERROR running MonetDB test: " + e.getMessage());
                }
            }
        }

        logger.write(methodName + "REST API test results:");
        logger.write(TestResultToJSONConverter.toJSON(restAPITests).toString(3));

        terminate();

    }

    private void terminate(){

        testLog.setTestEndTime(new Date());

        // send Slack notification if enabled and test failed
        if (!isTestSuccess() && isSlackEnabled){
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed for ");
        }

        // send test to web app db
        try {
            WebAppDBConnection.sendOperation(testLog.toJSON());
        } catch (IOException | ParseException | JSONException e) {
            logger.write("[App.run] WARNING - Error sending test log:" + e.getMessage());
        }
        logger.write("[MainTest.terminate] EXITING...");
        resultFile.write(testSuccess);
        System.exit(0);

    }

    private void terminate(String reasonForFailure){

        testLog.setTestEndTime(new Date());
        testLog.setReasonForFailure(reasonForFailure);

        // send Slack notification if enabled and test failed
        if (!isTestSuccess() && isSlackEnabled){
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed for ");
        }

        // send test to web app db
        try {
            WebAppDBConnection.sendOperation(testLog.toJSON());
        } catch (IOException | ParseException | JSONException e) {
            logger.write("[App.run] WARNING - Error sending test log:" + e.getMessage());
        }

        logger.write("[MainTest.terminate] EXITING...");
        resultFile.write(testSuccess);
        System.exit(0);

    }

    private void executeMonetDBTest(ElastiCube elastiCube) throws IOException, InterruptedException {

        logger.write("[MainTest.executeMonetDBTests] Running MonetDB test... ");

        MonetDBTest monetDBTest = new MonetDBTest(elastiCube);
        monetDBTest.executeQuery();

        logger.write("[MainTest.executeMonetDBTests] MonetDB query result for ElastiCube " + elastiCube.getName() + ":" + monetDBTest.isQuerySuccessful());
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
        this.numberOfElastiCubes = numberOfElastiCubes;
        testLog.setNumberOfElastiCubes(numberOfElastiCubes);
    }

}
