import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import integrations.SlackClient;
import logging.Logger;
import run_strategy.*;
import tests.SisenseRESTAPI;
import tests.TelnetTest;

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

        logger.write("Starting...");
        if (!operations.getSisenseVersion().equals("CANNOT DETECT")) {
            logger.write("Sisense version: " + operations.getSisenseVersion());
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

        if (operations.getElastiCubeName().isEmpty()){
            runECSTelnetTests();
            setAndExecuteStrategy();
            run(++attempt);
        }

        else {
            logger.write("[App.run] Found ElastiCube: " + operations.getElastiCubeName());

            boolean testResult = SisenseRESTAPI.queryTableIsSuccessful();

            // Check whether the test failed and Slack webhook is set
            if (!testResult && !ConfigFile.getInstance().getSlackWebhookURL().isEmpty()){
                SlackClient.getInstance().sendMessage();
            }

            resultFile.write(testResult);

            resultFile.write(SisenseRESTAPI.queryTableIsSuccessful());
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