package run_strategy;

import cmd_ops.CmdOperations;
import logging.Logger;

public class ECSDumpStrategy implements RunStrategy {

    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private Logger logger = Logger.getInstance();

    @Override
    public void execute() {

        logger.write("ECSDumpStrategy chosen");

        cmdOperations.ecsDump();

    }
}
