import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import integrations.SlackClient;
import logging.Logger;
import logging.TestResultToJSONConverter;
import models.ElastiCube;
import org.json.JSONException;
import run_strategy.*;
import tests.MonetDBTest;
import tests.SisenseRESTAPI;
import tests.TelnetTest;

import java.io.IOException;
import java.util.*;

public class App {

    private static final ConfigFile configFile = ConfigFile.getInstance();
    private static ResultFile resultFile = ResultFile.getInstance();;
    private static final CmdOperations operations = CmdOperations.getInstance();
    private static Logger logger = Logger.getInstance();
    private static StrategyContext strategyContext = new StrategyContext();

    public static void main(String[] args) throws JSONException {


        logger.write("[App.main] STARTING...");
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
        }

        logger.write("[App.run] Found " + elastiCubeList.size() + " running ElastiCubes: \n" + Arrays.toString(elastiCubeList.toArray()));

        // Check if ECS returned 0 ElastiCubes without error
        if (elastiCubeList.size() == 0){
            logger.write("[App.run] No ElastiCubes found., no errors from ECS. Exiting...");
            resultFile.write(true);
            System.exit(1);
        }

        else {

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

                } catch (IOException e) {
                    logger.write("[App.run] ERROR - " + e.getMessage());
                }
            }
            // Check whether the test failed and Slack webhook is set
            if (!testResult && !ConfigFile.getInstance().getSlackWebhookURL().isEmpty()){
                SlackClient.getInstance().sendMessage();
            }

            resultFile.write(testResult);
            logger.write("[App.run] EXITING...");
            System.exit(1);
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
}