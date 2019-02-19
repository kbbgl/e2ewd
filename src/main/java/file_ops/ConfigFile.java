package file_ops;

import logging.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class ConfigFile {

    private static ConfigFile configFileInstance = new ConfigFile();
    private Logger logger = Logger.getInstance();
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

    private ConfigFile(){
        Properties properties = new Properties();
        InputStream input;

        try {
            input = new FileInputStream(executionPath() + "/config.properties");
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

        } catch (IOException e) {
            logger.write("[ConfigFile.instance] ERROR: reading configuration file - " + e.getMessage());
        } catch (NumberFormatException e){
            logger.write("ERROR: reading reading port - " + e.getMessage());
        }
    }

    public static ConfigFile getInstance(){
        if (configFileInstance == null){
            configFileInstance = new ConfigFile();
        }
        return configFileInstance;
    }

    private String executionPath(){
        String jarLocation = null;
        try {
            jarLocation = new File(ConfigFile.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
            Path path = Paths.get(jarLocation);
            return String.valueOf(path.getParent());
        } catch (IOException | URISyntaxException e) {
            logger.write("ERROR: " + e.getMessage());
        }
        return jarLocation;
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

    public boolean isConfigFileValid(){
        HashMap<String, String> configMap = new HashMap<>(5);
        configMap.put("token", token);
        configMap.put("host", host);
        configMap.put("protocol", protocol);
        configMap.put("port", String.valueOf(port));
        configMap.put("restartECS", String.valueOf(restartECS));
        configMap.put("restartIIS", String.valueOf(restartIIS));
        configMap.put("iisDump", String.valueOf(iisDump));
        configMap.put("ecsDump", String.valueOf(ecsDump));
        configMap.put("requestTimeoutInSeconds", String.valueOf(requestTimeoutInSeconds));
        configMap.put("slackWebhookURL", slackWebhookURL);

        Set set = configMap.entrySet();

        for (Object o : set) {
            Map.Entry mapEntry = (Map.Entry) o;
            if (String.valueOf(mapEntry.getValue()).isEmpty()) {
                logger.write(mapEntry.getKey() + " is empty.");
                return false;
            } else if (String.valueOf(mapEntry.getValue()).equals("0")) {
                logger.write(mapEntry.getKey() + " is 0.");
                return false;
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
                "}";
    }
}