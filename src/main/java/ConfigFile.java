import java.io.*;
import java.util.Properties;

public class ConfigFile {

    String token;
    String host;
    int port;

    ConfigFile() {
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

    void read(String path){
        Properties properties = new Properties();
        InputStream input;

        try {
            input = new FileInputStream(path + "/config.properties");
            properties.load(input);

            setToken(properties.getProperty("token"));
            setHost(properties.getProperty("host"));
            setPort(Integer.parseInt(properties.getProperty("port")));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String toString() {
        return "ConfigFile{" +
                "token='" + token + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
