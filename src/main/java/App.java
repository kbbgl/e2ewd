import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.VersionFile;
import logging.Logger;
import logging.TestLog;
import models.ElastiCube;
import org.json.JSONException;
import tests.MainTest;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

public class App {

    private static String runningLocation;
    private static final ConfigFile configFile = ConfigFile.getInstance();
    private static final CmdOperations operations = CmdOperations.getInstance();
    private static Logger logger = Logger.getInstance();
    private static TestLog testLog = TestLog.getInstance();
    private static String host = System.getenv("COMPUTERNAME");

    public static void main(String[] args) throws JSONException {

        // Initialize test
        setRunningLocation();
        testLog.setHost(host);
        testLog.setTestStartTime(new Date());
        logger.write("[App.main] STARTING...");

        // Check application version
        VersionFile versionFile = new VersionFile(runningLocation);
        logger.write("[App.main] Application version: " + versionFile.getVersion());
        testLog.setVersion(versionFile.getVersion());

        // Check to see if Sisense is installed
        if (!operations.getSisenseVersion().equals("CANNOT DETECT")) {
            logger.write("[App.main] Sisense version: " + operations.getSisenseVersion());
        }

        // Retrieve list of RUNNING ElastiCubes
        logger.write("[App.main] Retrieving list of ElastiCubes...");
        List<ElastiCube> elastiCubeList = operations.getListElastiCubes();

        // Start test
        MainTest test = new MainTest(elastiCubeList);
        test.init();

    }

    private static void setRunningLocation(){
        try {
            runningLocation = new File(App.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getParent();
        } catch (URISyntaxException e) {
            logger.write("[App.runningLocation] ERROR : " + e.getMessage());
        }
    }

}