import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.VersionFile;
import logging.TestLog;
import models.ElastiCube;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tests.MainTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
        VersionFile versionFile = new VersionFile(runningLocation);
        logger.info("Application version: " + versionFile.getVersion());
        testLog.setVersion(versionFile.getVersion());

        // Check to see if Sisense is installed
        try {
            String sisenseVersion = operations.getSisenseVersion();

            if (sisenseVersion.equals("CANNOT DETECT")) {
                logger.info("Sisense version: " + sisenseVersion);
                logger.error("Sisense is not installed or cannot detect version from registry.");
                logger.info("EXITING...");
                System.exit(0);
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Failed retrieving Sisense version from registry: " + e.getMessage());
            logger.info("EXITING...");
            logger.debug(Arrays.toString(e.getStackTrace()));
            System.exit(0);
        }


        // Check if config.properties is valid
        logger.info(ConfigFile.getInstance().toString());
        if (ConfigFile.getInstance().isConfigFileValid()){

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