package conf.strategies;

import cmd_ops.CmdOperations;
import conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ECSDumpStrategy implements RunStrategy {

    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private final Logger logger = LoggerFactory.getLogger(ECSDumpStrategy.class);

    @Override
    public void execute() {

        logger.info("Executing ECSDumpStrategy...");

        if (!Configuration.getInstance().isRunningRemotely()){
            try {
                cmdOperations.ecsDump();
            } catch (InterruptedException | IOException e) {
                logger.error("Error executing ECS memory dump: " + e.getMessage());
            }
        } else {
            logger.warn("runningRemotely=true, skipping ECS process dump execution...");
        }
    }
}
