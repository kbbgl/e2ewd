package run_strategy;

import file_ops.ResultFile;
import integrations.SlackClient;
import integrations.WebAppDBConnection;
import logging.Logger;
import logging.TestLog;
import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

public class NoResetNoDumpStrategy implements RunStrategy {
    private ResultFile resultFile = ResultFile.getInstance();
    private Logger logger = Logger.getInstance();

    @Override
    public void execute() {

        logger.write("NoResetNoDumpStrategy chosen");
        logger.write("All restart and dump options are false. Exiting...");
        resultFile.write(false);
        TestLog.getInstance().setHealthy(false);
        TestLog.getInstance().setReasonForFailure("Tests failed and all restart and dump options are false");
        TestLog.getInstance().setTestEndTime(new Date());
        try {
            WebAppDBConnection.sendOperation(TestLog.getInstance().toJSON());
        } catch (IOException | JSONException | ParseException e) {
            logger.write("[NoResetNoDumpStrategy.execute] WARNING - No data sent to e2ewd dashboard DB");
        }

        SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed and no service restart mechanism chosen ");

        System.exit(0);

    }
}