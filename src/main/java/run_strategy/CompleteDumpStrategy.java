package run_strategy;

import cmd_ops.CmdOperations;
import logging.Logger;

public class CompleteDumpStrategy implements RunStrategy {

    private CmdOperations cmdOperations = CmdOperations.getInstance();
    private Logger logger = Logger.getInstance();

    @Override
    public void execute() {

        logger.write("CompleteDumpStrategy chosen");
        cmdOperations.ecsDump();
        cmdOperations.w3wpDump();

    }
}
