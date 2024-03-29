package tests;

import cmd_ops.CmdOperations;
import conf.*;
//import dao.WebAppRepositoryClient;
import conf.strategies.*;
import file_ops.ResultFile;
import integrations.SlackClient;
import logging.TestLog;
import logging.TestLogConverter;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
//import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tests.broker.BrokerHealthClient;
import tests.live.LiveConnectionRESTAPIClient;
import tests.microservices.MicroservicesHealthClient;
import tests.queryservice.*;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MainTest {

    private final Logger logger = LoggerFactory.getLogger(MainTest.class);
    private boolean testSuccess;
    private int attemptNumber;
    private final int maxNumberAttempts = 5;
    private List<ElastiCube> runningElastiCubes;
    private List<String> liveConnections;
    private ResultFile resultFile = ResultFile.getInstance();
    private Configuration config = Configuration.getInstance();
    private TestLog testLog = TestLog.getInstance();
    private StrategyContext strategyContext = new StrategyContext();

    public MainTest(){
    }

    public void init() throws JSONException {
        logger.debug("Initiating test...");

        preRun();

        attemptNumber = 1;
        run(attemptNumber);
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
        if (!config.isRunningRemotely() && config.isRunBrokerHealthCheck()){

            try {
                BrokerHealthClient brokerHealthClient = BrokerHealthClient.getInstance();
                brokerHealthClient.executeQuery();
            } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                logger.error("Error initializing broker health client: " + e.getMessage());
            }

        } else {
            logger.info("'runningRemotely=true'. Skipping rabbitmq health check...");
        }


        // Run microservices health test
        // TODO - add response from 7.1 =<
        if (config.isRunMicroservicesHealthCheck()){
            try {
                MicroservicesHealthClient microservicesHealthClient = MicroservicesHealthClient.getInstance();
                microservicesHealthClient.executeCall();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
                logger.error("Error running microservices health test: " + e.getMessage());
            }
        }

        // Run Live Connection test
        if (config.isCheckLiveConnections()){
            try {
                LiveConnectionRESTAPIClient liveConnectionClient = new LiveConnectionRESTAPIClient();
                liveConnections = liveConnectionClient.getListLiveConnections();

                if (liveConnectionClient.isCallSuccessful() && liveConnections.size() == 0){
                    logger.info("No Live Connections found. Consider setting `checkLiveConnections=false` in 'config.properties' to skip testing for Live Connections.");
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
            ElastiCubeRESTAPIClient ecClient = new ElastiCubeRESTAPIClient(this);
            runningElastiCubes = ECSStateKeeper.getInstance().getRunningElastiCubes();

            // Case when API call to get ElastiCubes succeeded but 0 returned
            // Start a default ElastiCube and retry
            if (!ecClient.isRequiresServiceRestart() && runningElastiCubes.size() == 0){

                logger.info("No ElastiCubes in RUNNING mode.");

                String defaultEC = ECSStateKeeper.getInstance().getAvailableElastiCubes().get(0).getName();
                if (defaultEC != null){
                    logger.info("Chosen default ElastiCube to start: " + defaultEC);

                    // Check whether we're running remotely and need to use the REST API to run start operation
                    if (Configuration.getInstance().isRunningRemotely()){
                        HttpResponse response = ecClient.startElastiCube(defaultEC);

                        if (response != null){
                            int responseCode = response.getStatusLine().getStatusCode();
                            String responseText = response.getStatusLine().getReasonPhrase();

                            logger.info("API call to start ElastiCube " + defaultEC + " responded with " + responseCode);

                            if (responseCode != 200){
                                logger.warn("Response: " + responseText);
                            }
                        } else {
                            logger.error("Failed to start ElastiCube " + defaultEC + " from REST API.");
                        }

                    } else {
                        CmdOperations.getInstance().runDefaultElastiCube(defaultEC);
                    }

                    retry();
                } else {
                    if (Configuration.getInstance().restartECS() && !Configuration.getInstance().isRunningRemotely()){
                        CmdOperations.getInstance().restartECS();
                        retry();
                    }
                }
            }
            // The call to retrieve ECs from endpoint failed (400,404,500,502,504) => run config strategy and retry
            else if (ecClient.isRequiresServiceRestart()){
                logger.error("API call to retrieve ElastiCubes was not successful");
                runStrategyAndRetryLogic();
            }
            // Happy path. More than 0 ElastiCubes returned and we can start the JAQL test
            else {
                setNumberOfElastiCubes(runningElastiCubes.size());

                JSONArray elastiCubesJSONArray = new JSONArray(runningElastiCubes.toArray());
                logger.info("Found " + runningElastiCubes.size() + " running ElastiCubes: \n" + elastiCubesJSONArray.toString(3));

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

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | InterruptedException e) {
            logger.error("Error running API call to retrieve ElastiCubes. Exception:", e);
            setTestSuccess(false);
            terminate("Could not get ElastiCubes from API: " + e.getMessage());
        } catch (IOException e){
            logger.error("Error running API call to retrieve ElastiCubes. Exception: ", e);
            setTestSuccess(false);
            terminate("Could not get ElastiCubes from API: " + e.getMessage());
            runStrategyAndRetryLogic();
        }
    }

    private void retry() throws JSONException {

        logger.info("Retrying...");
        run(++attemptNumber);

    }

    private void setAndExecuteStrategy(){
        if (config.isEcsDump() && config.isIisDump() && !config.restartECS() && !config.restartIIS()){
            strategyContext.setStrategy(new CompleteDumpStrategy());
        } else if (config.isEcsDump() && config.isIisDump() && config.restartECS() && config.restartIIS()){
            strategyContext.setStrategy(new CompleteResetAndDumpStrategy());
        } else if (!config.isEcsDump() && !config.isIisDump() && config.restartECS() && config.restartIIS()){
            strategyContext.setStrategy(new CompleteResetStrategy());
        } else if (config.isEcsDump() && !config.isIisDump() && !config.restartECS() && !config.restartIIS()){
            strategyContext.setStrategy(new ECSDumpStrategy());
        } else if (!config.isEcsDump() && !config.isIisDump() && config.restartECS() && !config.restartIIS()){
            strategyContext.setStrategy(new ECSResetStrategy());
        } else if (!config.isEcsDump() && config.isIisDump() && !config.restartECS() && !config.restartIIS()){
            strategyContext.setStrategy(new IISDumpStrategy());
        } else if (!config.isEcsDump() && !config.isIisDump() && !config.restartECS() && config.restartIIS()){
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
        Map<String, Boolean> restAPITests = new HashMap<>(runningElastiCubes.size());
        logger.info("Running ElastiCube JAQL API tests...");

        boolean callFailed = false;
        for (ElastiCube elastiCube : runningElastiCubes){

            // Execute REST API call to /jaql endpoint with supplied ElastiCube
            JAQLRESTAPIClient client = new JAQLRESTAPIClient(elastiCube.getName());
            logger.info("Executing JAQL to '" + elastiCube.getName() + "'...");
            client.executeQuery();
            logger.info("Finished executing JAQL to '" + elastiCube.getName() + "'.");

            if (client.isUnauthorized()){
                setTestSuccess(false);
                terminate("Unauthorized");
            }

            restAPITests.put(elastiCube.getName(), client.isCallSuccessful());

            // check if test failed
            // and send warning and execute MonetDB query
            if (client.isRequiresServiceRestart()){
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
                if (config.isRunMonetDBQuery()){
                    try {
                        executeMonetDBTest(elastiCube);

                    } catch (InterruptedException e) {
                        logger.error("Error running MonetDB test: " +e.getMessage());
                        logger.debug(Arrays.toString(e.getStackTrace()));
                        setTestSuccess(false);
                        terminate("Error running MonetDB test: " + e.getMessage());
                    }
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

        logger.info("ElastiCube JAQL API test results:");
        logger.info(TestLogConverter.toJSON(restAPITests).toString(3));

        // If the call failed (200 with error) or any 404/500/502/504 error
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
            JAQLRESTAPIClient client = new JAQLRESTAPIClient(liveConnection);
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
        logger.info(TestLogConverter.toJSON(restAPITests).toString(3));

    }

    private void terminate(){

        testLog.setTestEndTime(new Date());
        testLog.setHealthy(testSuccess);

        // Write test to mongo e2ewd.testlog
        // todo figure out why wrong credentials throws exception that causes process crash
//        if (config.isWriteTestToRepository()){
//            writeToMongo();
//        }

        // send Slack notification if enabled and test failed
        if (!isTestSuccess() && SlackClient.getInstance() != null){
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed ");
        }


        ResultFile.getInstance().write(testSuccess);
        logger.info("Test result: " + testSuccess);
        logger.info("EXITING...");
        System.exit(0);

    }

    public void terminate(String reasonForFailure){

        testLog.setTestEndTime(new Date());
        testLog.setReasonForFailure(reasonForFailure);
        logger.info("Reason for failure: " + reasonForFailure);
        testLog.setHealthy(false);

//        if (config.isWriteTestToRepository()){
//            writeToMongo();
//        }

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
        ResultFile.getInstance().write(false);
        logger.info("Test result: " + false);
        logger.info("EXITING...");
        System.exit(0);

    }

//    private void writeToMongo() {
//
//        Document testToInsert = null;
//        try {
//            testToInsert = TestLogConverter.toDocument(testLog.toJSON());
//            if ( WebAppRepositoryClient.getInstance() != null){
//                WebAppRepositoryClient.getInstance().insertTest(testToInsert);
//            } else {
//                logger.warn("Could not add test to Mongo");
//            }
//        } catch (JSONException | ParseException e) {
//            logger.warn("Error inserting test to mongo: ", e);
//        }
//    }

    private void executeMonetDBTest(ElastiCube elastiCube) throws IOException, InterruptedException {

        logger.info("Running MonetDB test...");

        MonetDBTest monetDBTest = new MonetDBTest(elastiCube);
        monetDBTest.executeQuery();

        logger.info("MonetDB query result for ElastiCube " + elastiCube.getName() + ":" + monetDBTest.isQuerySuccessful());
        if (!monetDBTest.isQuerySuccessful() && config.isEcDump()){
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

        if (!Configuration.getInstance().isRunningRemotely()){
            runECSTelnetTests();
            setAndExecuteStrategy();
        }
        retry();
    }

}
