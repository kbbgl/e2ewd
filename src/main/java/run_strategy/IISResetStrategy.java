package run_strategy;

import cmd_ops.CmdOperations;
import logging.Logger;

public class IISResetStrategy implements RunStrategy {

    private Logger logger = Logger.getInstance();
    private CmdOperations cmdOperations = CmdOperations.getInstance();

    @Override
    public void execute() {

        logger.write("IISResetStrategy chosen");
        cmdOperations.restartIIS();

    }


}
