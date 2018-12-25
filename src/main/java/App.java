import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import logging.Logger;
import run_strategy.*;
import tests.SisenseRESTAPI;
import tests.TelnetTest;

//TODO
// add to configuration:
// take dump
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
        run();

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

    private static void run(){

        // Read EC and table
        String ec = operations.getElastiCubeName();

        if (ec.isEmpty()){
            runECSTelnetTests();
            setAndExecuteStrategy();
            run();
        }

        else {
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