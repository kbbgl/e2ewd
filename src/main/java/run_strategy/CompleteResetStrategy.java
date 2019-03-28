package run_strategy;

import cmd_ops.CmdOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class CompleteResetStrategy implements RunStrategy {
    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private final Logger logger = LoggerFactory.getLogger(CompleteResetStrategy.class);

    @Override
    public void execute() {

        logger.info("Executing CompleteResetStrategy...");

        try {
            cmdOperations.restartECS();
            cmdOperations.restartIIS();
        } catch (InterruptedException | IOException e) {
            logger.error("Failed to run dump: " + e.getMessage());
            logger.debug(Arrays.toString(e.getStackTrace()));
        }
    }
}
