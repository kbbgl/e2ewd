import org.json.JSONException;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

public class App {

    private static final Date runTime = new Date();

    public static void main(String[] args) throws IOException, JSONException, URISyntaxException {

        writeToLogger("\nRun at: " + runTime.toString() + "\n-----------------------");
        writeToLogger("Executing jar from " + executionPath());
        writeToLogger("Reading config file...\n");
        createConfigFile();
        createResultFile(false);

    }

    private static String executionPath() throws URISyntaxException, IOException {
        String jarLocation = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
        Path path = Paths.get(jarLocation);

        return String.valueOf(path.getParent());
    }

    private static void writeToLogger(String s){
        try {
            Logger logger = new Logger(executionPath());
            logger.write(s);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void createResultFile(boolean result){
        try {
            ResultFile resultFile = new ResultFile(executionPath());
            resultFile.create();
            writeToLogger("Created file in " + resultFile.path);
            resultFile.write(result);
            writeToLogger("Test succeeded: " + result);
        } catch (URISyntaxException | IOException e) {
            writeToLogger("Couldn't create log file: \n");
            writeToLogger(Arrays.toString(e.getStackTrace()));
        }
    }

    private static void createConfigFile() {

        try {
            ConfigFile configFile = new ConfigFile(executionPath());
            configFile.read();
            writeToLogger("Config file read: \n");
            writeToLogger(configFile.toString());
        } catch (URISyntaxException | IOException e) {
            writeToLogger("Couldn't read config file: \n");
            writeToLogger(Arrays.toString(e.getStackTrace()));
        }
    }
}