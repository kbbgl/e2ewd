package tests;

import cmd_ops.CmdOperations;
import cmd_ops.ElastiCubeRESTAPIClient;
import cmd_ops.LiveConnectionRESTAPIClient;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import integrations.SlackClient;
import logging.TestLog;
import logging.TestResultToJSONConverter;
import models.ElastiCube;
import org.apache.http.HttpStatus;
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
import java.util.*;

public class MainTest {

    private final Logger logger = LoggerFactory.getLogger(MainTest.class);
    private boolean testSuccess;
    private int attamptNumber;
    private final int maxNumberAttempts = 5;
    private List<ElastiCube> elastiCubes;
    private List<String> liveConnections;
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
        // TODO - add response from 7.1 =<
        if (configFile.isRunMicroservicesHealthCheck()){
            try {
                MicroservicesHealthClient microservicesHealthClient = MicroservicesHealthClient.getInstance();
                microservicesHealthClient.executeCall();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
                logger.error("Error running microservices health test: " + e.getMessage());
            }
        }

        // Run Live Connection test
        if (configFile.isCheckLiveConnections()){
            try {
                LiveConnectionRESTAPIClient liveConnectionClient = new LiveConnectionRESTAPIClient();
                liveConnections = liveConnectionClient.getListLiveConnections();

                if (liveConnectionClient.isCallSuccessful() && liveConnections.size() == 0){
                    logger.info("No Live Connections found. Consider setting `checkLiveConnections=false` in config.properties' to skip testing for Live Connections.");
                } else if (!liveConnectionClient.isCallSuccessful()){
                    logger.error("API call to retrieve Live Connections was not successful");
                } else {
                    JSONArray liveConnectionsArray = new JSONArray(liveConnections.toArray());
                    logger.info("Found " + liveConnections.size() + " Live Connections: \n" + liveConnectionsArray.toString(3));
                    executeLiveConnectionJAQLCalls();
                }

            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | JSONException | IOException e) {
                logger.error("Error creating Live Connection REST API client. Reason: " + e.getMessage());
                logger.debug(Arrays.toString(e.getStackTrace()));
            }
        }

        // Retrieve list of RUNNING ElastiCubes
        logger.info("Retrieving list of ElastiCubes...");

        // Create EC client and retrieve list of ElastiCubes
        try {
            ElastiCubeRESTAPIClient ecClient = new ElastiCubeRESTAPIClient();
            elastiCubes = ecClient.getListOfElastiCubes();

            // Case when API call to get ElastiCubes succeeded but 0 returned
            // Start a default ElastiCube and retry
            if (ecClient.isCallSuccessful() && elastiCubes.size() == 0){
                String defaultEC = ecClient.getDefaultElastiCube();
                logger.info("No ElastiCubes in RUNNING mode.");
                logger.info("Chosen default ElastiCube to start: " + defaultEC);
                CmdOperations.getInstance().runDefaultElastiCube(defaultEC);
                retry();
            } // The call to retrieve ECs from endpoint failed (4XX,5XX) => run config strategy and retry
            else if (!ecClient.isCallSuccessful()){
                logger.error("API call to retrieve ElastiCubes was not successful");
                runStrategyAndRetryLogic();
            } // Happy path. More than 0 ElastiCubes returned and we can start the JAQL test
            else {
                setNumberOfElastiCubes(elastiCubes.size());

                JSONArray elastiCubesJSONArray = new JSONArray(elastiCubes.toArray());
                logger.info("Found " + elastiCubes.size() + " running ElastiCubes: \n" + elastiCubesJSONArray.toString(3));

                // execute REST API tests and remove ElastiCubes that their REST API tests succeeded
                try {
                    executeElastiCubeJAQLCalls();

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

    private void executeElastiCubeJAQLCalls() throws JSONException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Map<String, Boolean> restAPITests = new HashMap<>(elastiCubes.size());
        logger.info("Running ElastiCube JAQL API tests...");

        boolean callFailed = false;
        for (ElastiCube elastiCube : elastiCubes){

            // Execute REST API call to /jaql endpoint with supplied ElastiCube
            SisenseRESTAPIClient client = new SisenseRESTAPIClient(elastiCube.getName());
            client.executeQuery();
            restAPITests.put(elastiCube.getName(), client.isCallSuccessful());

            // check if test failed
            // and send warning and execute MonetDB query
            if (!client.isCallSuccessful()){
                logger.warn("REST API JAQL test failed for ElastiCube '" +
                            elastiCube.getName() +
                            "' with response code " +
                            client.getResponseCode() +
                            ", response body: \n" +
                            client.getCallResponse());

                SlackClient.getInstance()
                        .sendMessage(":warning: WARNING! REST API test failed for ElastiCube *" +
                                elastiCube.getName() + "* with response code `" + client.getResponseCode() + "`, response body: \n```" + client.getCallResponse() + "```");

                setTestSuccess(false);
                callFailed = true;

                // Run MonetDB test
                try {
                    executeMonetDBTest(elastiCube);

                } catch (InterruptedException e) {
                    logger.error("Error running MonetDB test: " +e.getMessage());
                    logger.debug(Arrays.toString(e.getStackTrace()));
                    setTestSuccess(false);
                    terminate("Error running MonetDB test: " + e.getMessage());
                }
                break;
            }

            // Deal with cases where response is 200 but contains error
            else if (client.getResponseCode() == HttpStatus.SC_OK){

                // Check if response is valid JSON
                try {
                    String clientResponseContent = client.getCallResponse();
                    JSONObject response = new JSONObject(clientResponseContent);

                    // Verify the response is actually an error
                    if (response.has("error") && response.getBoolean("error")){
                        String responseDetails = response.getString("details");
                        logger.error("JAQL endpoint returned an error: '" + responseDetails +"'.");

                        if (responseDetails.contains("net.tcp://localhost:812/AbacusQueryService")){
                            callFailed = true;
                        }

                    }
                } catch (JSONException e){
                    logger.warn("Response '" + client.getCallResponse() + "' from JAQL API is not valid JSON");
                    logger.info(Arrays.toString(e.getStackTrace()));
                }
            }

        }

        logger.info("ElasitCube JAQL API test results:");
        logger.info(TestResultToJSONConverter.toJSON(restAPITests).toString(3));

        // If the call failed (200 with error) or any 4XX/5XX error
        // Run strategy
        if (callFailed){
            runStrategyAndRetryLogic();
        } else {
            terminate();
        }
    }

    private void executeLiveConnectionJAQLCalls() throws JSONException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Map<String, Boolean> restAPITests = new HashMap<>(liveConnections.size());
        logger.info("Running Live Connection JAQL API tests...");

        for (String liveConnection : liveConnections){

            // Execute REST API call to /jaql endpoint with supplied ElastiCube
            SisenseRESTAPIClient client = new SisenseRESTAPIClient(liveConnection);
            client.executeQuery();
            restAPITests.put(liveConnection, client.isCallSuccessful());

            // check if test failed
            // and send warning and execute MonetDB query
            if (!client.isCallSuccessful()){
                logger.warn("REST API JAQL test failed for Live Connection '" +
                        liveConnection +
                        "' with response code " +
                        client.getResponseCode() +
                        ", response body: \n" +
                        client.getCallResponse());

                SlackClient.getInstance()
                        .sendMessage(":warning: WARNING! REST API test failed for Live Connection *" +
                                liveConnection + "* with response code `" + client.getResponseCode() + "`, response body: \n```" + client.getCallResponse() + "```");

                break;
            }

        }

        logger.info("Live Connection JAQL API test results:");
        logger.info(TestResultToJSONConverter.toJSON(restAPITests).toString(3));

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

    private void runStrategyAndRetryLogic() throws JSONException {

        runECSTelnetTests();
        setAndExecuteStrategy();
        retry();
    }

}
