package run_strategy;

import file_ops.ResultFile;
import integrations.SlackClient;
import logging.Logger;

public class NoResetNoDumpStrategy implements RunStrategy {
    private ResultFile resultFile = ResultFile.getInstance();
    private Logger logger = Logger.getInstance();

    @Override
    public void execute() {

        logger.write("NoResetNoDumpStrategy chosen");
        logger.write("All restart and dump options are false. Exiting...");
        resultFile.write(false);

        SlackClient.getInstance().sendMessage();

        System.exit(0);

    }
}
