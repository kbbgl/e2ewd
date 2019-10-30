package features;

import cmd_ops.CmdOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class IISResetStrategy implements RunStrategy {

    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private final Logger logger = LoggerFactory.getLogger(IISResetStrategy.class);

    @Override
    public void execute() {

        logger.info("Executing IISResetStrategy...");

        try {
            cmdOperations.restartIIS();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to run iisreset: " + e.getMessage());
        }

    }


}
