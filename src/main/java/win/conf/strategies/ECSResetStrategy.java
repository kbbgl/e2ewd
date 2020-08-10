package conf.strategies;

import cmd_ops.CmdOperations;
import conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ECSResetStrategy implements RunStrategy {

    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private final Logger logger = LoggerFactory.getLogger(ECSResetStrategy.class);

    @Override
    public void execute() {

        logger.info("Executing ECSResetStrategy...");

        if (!Configuration.getInstance().isRunningRemotely()){
            try {
                cmdOperations.restartECS();
            } catch (IOException | InterruptedException e) {
                logger.error("Error restarting ECS: " + e.getMessage());
            }
        } else {
            logger.warn("runningRemotely=true, skipping ECS reset execution...");
        }
    }
}
