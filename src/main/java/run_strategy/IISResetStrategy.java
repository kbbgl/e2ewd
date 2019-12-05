package run_strategy;

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

            if (!cmdOperations.getSisenseVersion().startsWith("6") &&
                    !cmdOperations.getSisenseVersion().startsWith("7.0") &&
                    !cmdOperations.getSisenseVersion().startsWith("7.1") &&
                    !cmdOperations.getSisenseVersion().startsWith("7.2") &&
                    !cmdOperations.getSisenseVersion().startsWith("7.3")
            ){
                cmdOperations.restartQueryProxy();
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to run operation: " + e.getMessage());
        }

    }


}
