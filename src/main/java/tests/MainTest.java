package tests;

import file_ops.ConfigFile;
import file_ops.ResultFile;
import logging.Logger;
import logging.TestLog;
import models.ElastiCube;
import org.json.JSONObject;
import run_strategy.*;

import java.util.Date;
import java.util.List;

public class MainTest {

    private int attamptNumber;
    private Date startTime;
    private Date endTime;
    private final int maxNumberAttempts = 5;
    private boolean testSuccessful;
    private String failureReason;
    private List<ElastiCube> elastiCubes;
    private Logger logger = Logger.getInstance();
    private ResultFile resultFile = ResultFile.getInstance();
    private ConfigFile configFile = ConfigFile.getInstance();
    private TestLog testLog = TestLog.getInstance();
    private StrategyContext strategyContext = new StrategyContext();

    public MainTest(List<ElastiCube> elastiCubes){
        this.elastiCubes = elastiCubes;
        this.startTime = new Date();
    }

    public void init(){
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

    private void run(int attempt){

        if (attempt > maxNumberAttempts){
            logger.write("Max number of attempts " + maxNumberAttempts + " exceeded.");
            quitWithRunFailure();
        }

    }

    private void retry(){

        run(++attamptNumber);

    }

    private void quitWithSuccess(){

        resultFile.write(true);
        logger.write("[MainTest.quit] EXITING...");
        System.exit(0);

    }

    private void quitWithRunFailure(){

        resultFile.write(false);
        logger.write("[MainTest.quitWithRunFailure] EXITING...");

        System.exit(1);
    }

    private void quitWithPreRunFailure(String reasonForQuit){

        logger.write("[MainTest.quitWithPreRunFailure] " + reasonForQuit);
        logger.write("[MainTest.quitWithPreRunFailure] EXITING...");
        System.exit(1);
    }

    public JSONObject testSummary(){

        this.endTime = new Date();
        JSONObject rootObj = new JSONObject();

        return rootObj;
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

}
