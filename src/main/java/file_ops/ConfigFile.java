package file_ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class ConfigFile {

    private static ConfigFile configFileInstance;
    private static final Logger logger = LoggerFactory.getLogger(ConfigFile.class);
    private String token;
    private String host;
    private String protocol;
    private int requestTimeoutInSeconds;
    private boolean restartECS;
    private boolean restartIIS;
    private boolean ecsDump;
    private boolean iisDump;
    private int port;
    private String slackWebhookURL;
    private String friendlyHostName;

    private ConfigFile(){

        Properties properties = new Properties();
        InputStream input;

        logger.debug("Execution path: " + executionPath());
        if (executionPath() != null){
            try {
                input = new FileInputStream(executionPath() + "/config.properties");
                logger.debug("Reading config.properties file...");
                properties.load(input);

                setToken(properties.getProperty("token"));
                setHost(properties.getProperty("host"));
                setProtocol(properties.getProperty("protocol"));
                setRestartECS(Boolean.parseBoolean(properties.getProperty("restartECS")));
                setRestartIIS(Boolean.parseBoolean(properties.getProperty("restartIIS")));
                setEcsDump(Boolean.parseBoolean(properties.getProperty("ecsDump")));
                setIisDump(Boolean.parseBoolean(properties.getProperty("iisDump")));
                setRequestTimeoutInSeconds(Integer.parseInt(properties.getProperty("requestTimeoutInSeconds")));
                setPort(Integer.parseInt(properties.getProperty("port")));
                setSlackWebhookURL(properties.getProperty("slackWebhookURL"));
                setFriendlyHostName(properties.getProperty("friendlyHostName"));

            } catch (IOException e) {
                logger.error("Error reading config.properties: " + e.getMessage() + ". Terminating...");
                System.exit(0);
            } catch (NumberFormatException e){
                logger.error("Error parsing port number " + e.getMessage() + ". Terminating...");
                System.exit(0);
            }
        } else {
            logger.error("JAR execution path returned null. Terminating...");
            System.exit(0);
        }

    }

    public static ConfigFile getInstance(){
        if (configFileInstance == null){
            configFileInstance = new ConfigFile();
            logger.debug("Created ConfigFile instance");
        }
        return configFileInstance;
    }

    private String executionPath(){
        try {
            Path path = Paths.get(new File(ConfigFile.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath());
            return String.valueOf(path.getParent());
        } catch (IOException | URISyntaxException e) {
            logger.error("Error getting JAR execution path: " + e.getMessage());
            return null;
        }
    }

    private void setToken(String token) {
        this.token = token;
    }

    private void setHost(String host) {
        this.host = host;
    }

    private void setPort(int port) {
        this.port = port;
    }

    public int getPort(){
        return this.port;
    }

    public String getHost(){
        return this.host;
    }

    public String getToken(){
        return this.token;
    }

    public boolean restartECS() {
        return restartECS;
    }

    private void setRestartECS(boolean restartECS) {
        this.restartECS = restartECS;
    }

    public String getProtocol() {
        return protocol;
    }

    private void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getRequestTimeoutInSeconds() {
        return requestTimeoutInSeconds;
    }

    private void setRequestTimeoutInSeconds(int requestTimeoutInSeconds) {
        this.requestTimeoutInSeconds = requestTimeoutInSeconds;
    }

    public boolean restartIIS() {
        return restartIIS;
    }

    private void setRestartIIS(boolean restartIIS) {
        this.restartIIS = restartIIS;
    }

    public boolean isEcsDump() {
        return ecsDump;
    }

    private void setEcsDump(boolean ecsDump) {
        this.ecsDump = ecsDump;
    }

    public boolean isIisDump() {
        return iisDump;
    }

    private void setIisDump(boolean iisDump) {
        this.iisDump = iisDump;
    }

    public String getSlackWebhookURL() {
        return slackWebhookURL;
    }

    private void setSlackWebhookURL(String slackWebhookURL) {
        this.slackWebhookURL = slackWebhookURL;
    }


    private void setFriendlyHostName(String friendlyHostName) {
        this.friendlyHostName = friendlyHostName;
    }

    public String getFriendlyHostName() {
        return friendlyHostName;
    }

    public boolean isConfigFileValid(){
        HashMap<String, String> configMap = new HashMap<>(5);
        configMap.put("token", token);
        configMap.put("host", host);
        configMap.put("protocol", protocol);
        configMap.put("port", String.valueOf(port));
        configMap.put("requestTimeoutInSeconds", String.valueOf(requestTimeoutInSeconds));

        Set set = configMap.entrySet();

        for (Object o : set) {
            Map.Entry mapEntry = (Map.Entry) o;
            if (String.valueOf(mapEntry.getValue()).isEmpty()) {
                logger.error(mapEntry.getKey() + " is empty.");
                return false;
            } else if (String.valueOf(mapEntry.getValue()).equals("0")) {
                logger.error(mapEntry.getKey() + " is 0.");
                return false;
            } else if (mapEntry.getKey().equals("port") || mapEntry.getKey().equals("requestTimeoutInSeconds")){
                try {
                    Integer.parseInt(String.valueOf(mapEntry.getValue()));
                } catch (NumberFormatException e){
                    logger.error(mapEntry.getKey() + " is not of type int. Current value is:" + mapEntry.getValue());
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public String toString() {
        return "ConfigFile{\n\t" +
                "token:" + token + ",\n\t" +
                "host:" + host + ",\n\t" +
                "port:" + port + ",\n\t" +
                "protocol:" + protocol + ",\n\t" +
                "requestTimeoutInSeconds:" + requestTimeoutInSeconds + ",\n\t" +
                "restartECS:" + restartECS + ",\n\t" +
                "restartIIS:" + restartIIS + ",\n\t" +
                "iisDump:" + iisDump + ",\n\t" +
                "ecsDump:" + ecsDump + "\n\t" +
                "slackWebhookURL:" + slackWebhookURL + "\n\t" +
                "friendlyHostName :" + friendlyHostName + "\n\t" +
                "}";
    }
}