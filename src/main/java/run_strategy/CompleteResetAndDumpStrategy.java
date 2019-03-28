package run_strategy;

import cmd_ops.CmdOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class CompleteResetAndDumpStrategy implements RunStrategy {

    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private final Logger logger = LoggerFactory.getLogger(CompleteResetAndDumpStrategy.class);

    @Override
    public void execute() {

        logger.info("Executing CompleteResetAndDumpStrategy...");

        try {
            cmdOperations.w3wpDump();
            cmdOperations.ecsDump();
            cmdOperations.restartECS();
            cmdOperations.restartIIS();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to run dump: " + e.getMessage());
            logger.debug(Arrays.toString(e.getStackTrace()));
        }


    }

}
