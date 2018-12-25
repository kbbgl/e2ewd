package run_strategy;

import cmd_ops.CmdOperations;
import logging.Logger;

public class CompleteResetStrategy implements RunStrategy {
    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private Logger logger = Logger.getInstance();

    @Override
    public void execute() {

        logger.write("CompleteResetStrategy chosen");

        cmdOperations.restartECS();
        cmdOperations.restartIIS();

    }

}
