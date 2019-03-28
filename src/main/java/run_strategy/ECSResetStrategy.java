package run_strategy;

import cmd_ops.CmdOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ECSResetStrategy implements RunStrategy {

    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private final Logger logger = LoggerFactory.getLogger(ECSResetStrategy.class);

    @Override
    public void execute() {

        logger.info("Executing ECSResetStrategy...");

        try {
            cmdOperations.restartECS();
        } catch (IOException | InterruptedException e) {
            logger.error("Error restarting ECS: " + e.getMessage());
        }

    }
}
