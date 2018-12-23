import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import logging.Logger;
import tests.SisenseRESTAPI;
import tests.TelnetTest;

public class App {

    private static final ConfigFile configFile = ConfigFile.getInstance();
    private static ResultFile resultFile = ResultFile.getInstance();;
    private static final CmdOperations operations = CmdOperations.getInstance();
    private static Logger logger = Logger.getInstance();

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

        if (ec.isEmpty() && configFile.isRestartECS()){
            runECSTelnetTests();
            restartECS();
            run();
        }
        else if (ec.isEmpty()){
            runECSTelnetTests();
            logger.write("[main] EC result is empty and restartECS=false. Exiting...");
            resultFile.write(false);
            System.exit(0);
        }
        else {
            resultFile.write(SisenseRESTAPI.queryTableIsSuccessful());
        }
    }

    private static void runECSTelnetTests(){
        TelnetTest.isConnected(logger, "localhost", 811);
        TelnetTest.isConnected(logger, "localhost", 812);
    }

    private static void restartECS(){

        String serviceName;

        if (operations.getSisenseVersion().startsWith("7.2")){
            serviceName = "Sisense.ECMS";
        }
        else {
            serviceName = "ElastiCubeManagmentService";
        }

        logger.write("[restartECS] Service to restart: " + serviceName);
        operations.restartService(serviceName);

    }
}