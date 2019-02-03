import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import integrations.SlackClient;
import logging.Logger;
import models.ElastiCube;
import run_strategy.*;
import tests.SisenseRESTAPI;
import tests.TelnetTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO
// add to configuration:
// email that dump occurred or event viewer with dump location
// number of retries


public class App {

    private static final ConfigFile configFile = ConfigFile.getInstance();
    private static ResultFile resultFile = ResultFile.getInstance();;
    private static final CmdOperations operations = CmdOperations.getInstance();
    private static Logger logger = Logger.getInstance();
    private static StrategyContext strategyContext = new StrategyContext();

    public static void main(String[] args) {


        logger.write("[App.main] - Starting...");
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

    private static void run(int attempt){

        if (attempt > 5) {
            logger.write("5 run attempts exceeded. Exiting...");

            if (!ConfigFile.getInstance().getSlackWebhookURL().isEmpty()){
                SlackClient.getInstance().sendMessage();
            }

            System.exit(0);
        }

        logger.write("[App.run] - Attempt number " + attempt);
        List<ElastiCube> elastiCubeList = operations.getListElastiCubes();
        logger.write("[App.run] Found " + elastiCubeList.size() + " running ElastiCubes.");

        if (elastiCubeList.size() == 0){
            runECSTelnetTests();
            setAndExecuteStrategy();
            run(++attempt);
        }

        else {

            Map<String, Boolean> tests = new HashMap<>(elastiCubeList.size());
            for (ElastiCube elasticube : elastiCubeList){
                tests.put(elasticube.getName(), SisenseRESTAPI.queryTableIsSuccessful(elasticube.getName()));
            }

            boolean testResult = true;
            logger.write("[App.run] Cube test results:");
            logger.write("[App.run] " + Arrays.toString(tests.entrySet().toArray()));
            for (Map.Entry<String, Boolean> entry : tests.entrySet()){
                if (!entry.getValue()){
                    testResult = false;
                    logger.write("[App.run] Test failed for " + entry.getKey());
                }
            }

            // Check whether the test failed and Slack webhook is set
            if (!testResult && !ConfigFile.getInstance().getSlackWebhookURL().isEmpty()){
                SlackClient.getInstance().sendMessage();
            }

            resultFile.write(testResult);
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