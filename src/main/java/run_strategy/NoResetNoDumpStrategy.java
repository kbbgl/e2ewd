package run_strategy;

import file_ops.ConfigFile;
import file_ops.ResultFile;
import integrations.SlackClient;
import integrations.WebAppDBConnection;
import logging.TestLog;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

public class NoResetNoDumpStrategy implements RunStrategy {
    private ResultFile resultFile = ResultFile.getInstance();
    private final Logger logger = LoggerFactory.getLogger(NoResetNoDumpStrategy.class);

    @Override
    public void execute() {

        logger.info("Executing NoResetNoDumpStrategy...");
        logger.info("All restart and dump options are false. Exiting...");
        resultFile.write(false);
        TestLog.getInstance().setHealthy(false);
        TestLog.getInstance().setReasonForFailure("Tests failed and all restart and dump options are false");
        TestLog.getInstance().setTestEndTime(new Date());

        if (!ConfigFile.getInstance().getSlackWebhookURL().isEmpty()){
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed and no service restart mechanism chosen");
        }

        try {
            WebAppDBConnection.sendOperation(TestLog.getInstance().toJSON());
        } catch (IOException | JSONException | ParseException e) {
            logger.warn("No data sent to e2ewd dashboard DB");
        }

        System.exit(0);

    }
}