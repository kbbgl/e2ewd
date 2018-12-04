import java.io.*;
import java.util.Properties;

public class ConfigFile {

    private String path;
    private String token;
    private String host;
    private String protocol;
    private boolean restartECS;
    private int port;

    ConfigFile(String path) {
        this.path = path;
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

    int getPort(){
        return this.port;
    }

    String getHost(){
        return this.host;
    }

    String getToken(){
        return this.token;
    }

    boolean isRestartECS() {
        return restartECS;
    }

    private void setRestartECS(boolean restartECS) {
        this.restartECS = restartECS;
    }

    void read(){
        Properties properties = new Properties();
        InputStream input;

        try {
            input = new FileInputStream(path + "/config.properties");
            properties.load(input);

            setToken(properties.getProperty("token"));
            setHost(properties.getProperty("host"));
            setPort(Integer.parseInt(properties.getProperty("port")));
            setProtocol(properties.getProperty("protocol"));
            setRestartECS(Boolean.parseBoolean(properties.getProperty("restartECS")));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String toString() {
        return "ConfigFile{\n\t" +
                "token='" + token + ",\n\t" +
                "host='" + host + ",\n\t" +
                "port=" + port + ",\n\t" +
                "protocol=" + protocol + ",\n\t" +
                "restartECS=" + restartECS + "\n\t" +
                "}";
    }

    String getProtocol() {
        return protocol;
    }

    private void setProtocol(String protocol) {
        this.protocol = protocol;
    }

}
