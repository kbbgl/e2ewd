package conf;

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

            if (!cmdOperations.getSisenseVersion().startsWith("6") &&
                    !cmdOperations.getSisenseVersion().startsWith("7.0") &&
                    !cmdOperations.getSisenseVersion().startsWith("7.1") &&
                    !cmdOperations.getSisenseVersion().startsWith("7.2") &&
                    !cmdOperations.getSisenseVersion().startsWith("7.3")
            ){
                cmdOperations.restartQueryProxy();
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Failed to run operation: " + e.getMessage());
            logger.debug(Arrays.toString(e.getStackTrace()));
        }
    }
}
