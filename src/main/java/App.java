import cmd_ops.CmdOperations;
import conf.Configuration;
//import dao.WebAppRepositoryClient;
import version.RepositoryComparator;
import version.VersionFile;
import logging.TestLog;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tests.MainTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static String runningLocation;
    private static final CmdOperations operations = CmdOperations.getInstance();
    private static TestLog testLog = TestLog.getInstance();
    private static String host = System.getenv("COMPUTERNAME");
    private static MainTest mainTest = new MainTest();

    public static void main(String[] args) throws JSONException {

        // Initialize test
        setRunningLocation();
        testLog.setHost(host);
        testLog.setTestStartTime(new Date());
        logger.info("STARTING...");

        // Check application version
        try {
            RepositoryComparator repoComparator = new RepositoryComparator(runningLocation);
            repoComparator.compareVersions();
            testLog.setVersion(repoComparator.getInstalledVersion());
        } catch (URISyntaxException | IOException e) {
            logger.warn("Cannot check for latest version: ", e);
            logger.info("Ignoring...");
        }

        // Check to see if Sisense is installed
        try {
            String sisenseVersion = operations.getSisenseVersion();

            if (sisenseVersion.equals("CANNOT DETECT")) {
                logger.error("Sisense is not installed or cannot detect version from registry.");
                logger.info("EXITING...");
                System.exit(0);
            } else {
                logger.info("Sisense version: " + sisenseVersion);
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Failed retrieving Sisense version from registry: " + e.getMessage());
            logger.info("EXITING...");
            logger.debug(Arrays.toString(e.getStackTrace()));
            System.exit(0);
        }


        // Check if config.properties is valid
        logger.info(Configuration.getInstance().toString());
        if (Configuration.getInstance().isConfigFileValid()){

            // Start test
            mainTest.init();
        } else {
            mainTest.terminate("Configuration file is invalid.");
        }
    }

    private static void setRunningLocation(){
        try {
            runningLocation = new File(App.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getParent();
        } catch (URISyntaxException e) {
            logger.error("Failed to retrieve JAR running location: " + e.getMessage());
        }
    }

}