package conf;

//import dao.WebAppRepositoryClient;
import file_ops.ResultFile;
import integrations.SlackClient;
import logging.TestLog;
import logging.TestLogConverter;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        if (!Configuration.getInstance().getSlackWebhookURL().isEmpty()){
            SlackClient.getInstance().sendMessage(":rotating_light: CRITICAL! Watchdog test failed and no service restart mechanism chosen");
        }

//        try {
//            if (WebAppRepositoryClient.getInstance() != null){
//                WebAppRepositoryClient.getInstance().insertTest(TestLogConverter.toDocument(TestLog.getInstance().toJSON()));
//            }
//        } catch (JSONException | ParseException e) {
//            logger.warn("Error writing test to mongo: ", e);
//        }

        System.exit(0);

    }
}