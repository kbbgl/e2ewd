package installer.model;

public class Configuration {

    private static Configuration instance;
    private String token;
    private String protocol;
    private String host;
    private int port;
    private boolean restartECS;
    private boolean restartIIS;
    private boolean createECSDump;
    private boolean createIISDump;
    private int queryTimeoutInSeconds;
    private String slackWebhookURL;

    public static Configuration getInstance() {
        if (instance == null){
            instance = new Configuration();
        }
        return instance;
    }

    private Configuration(){

    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isRestartECS() {
        return restartECS;
    }

    public void setRestartECS(boolean restartECS) {
        this.restartECS = restartECS;
    }

    public boolean isRestartIIS() {
        return restartIIS;
    }

    public void setRestartIIS(boolean restartIIS) {
        this.restartIIS = restartIIS;
    }

    public boolean isCreateECSDump() {
        return createECSDump;
    }

    public void setCreateECSDump(boolean createECSDump) {
        this.createECSDump = createECSDump;
    }

    public boolean isCreateIISDump() {
        return createIISDump;
    }

    public void setCreateIISDump(boolean createIISDump) {
        this.createIISDump = createIISDump;
    }

    public int getQueryTimeoutInSeconds() {
        return queryTimeoutInSeconds;
    }

    public void setQueryTimeoutInSeconds(int queryTimeoutInSeconds) {
        this.queryTimeoutInSeconds = queryTimeoutInSeconds;
    }

    public String getSlackWebhookURL() {
        return slackWebhookURL;
    }

    public void setSlackWebhookURL(String slackWebhookURL) {
        this.slackWebhookURL = slackWebhookURL;
    }

    @Override
    public String toString() {
        return "Configuration{" + "\n\t" +
                "token=" + token + ",\n\t" +
                "protocol=" + protocol + ",\n\t" +
                "host=" + host + ",\n\t" +
                "port=" + port + ",\n\t" +
                "restartECS=" + restartECS + ",\n\t" +
                "restartIIS=" + restartIIS + ",\n\t" +
                "createECSDump=" + createECSDump + ",\n\t" +
                "createIISDump=" + createIISDump + ",\n\t" +
                "queryTimeoutInSeconds=" + queryTimeoutInSeconds + ",\n\t" +
                "slackWebhookURL=" + slackWebhookURL + "\n" +
                '}';
    }
}
