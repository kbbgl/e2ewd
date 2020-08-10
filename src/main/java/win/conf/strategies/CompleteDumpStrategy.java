package conf.strategies;

import cmd_ops.CmdOperations;
import conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class CompleteDumpStrategy implements RunStrategy {

    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(CompleteDumpStrategy.class);

    @Override
    public void execute() {

        logger.info("Executing CompleteDumpStrategy...");

        if (!Configuration.getInstance().isRunningRemotely()){
            try {
                cmdOperations.w3wpDump();
                cmdOperations.ecsDump();
            } catch (InterruptedException | IOException e) {
                logger.error("Failed to run dump: " + e.getMessage());
                logger.debug(Arrays.toString(e.getStackTrace()));
            }
        } else {
            logger.warn("runningRemotely=true, skipping ECS and IIS process dump executing...");
        }


    }
}
