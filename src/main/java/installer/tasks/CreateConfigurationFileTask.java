package installer.tasks;

import installer.model.Configuration;
import installer.view.RootLayout;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class CreateConfigurationFileTask extends Task<File> {

    private File configurationFile;
    private String runningLocation;
    private RootLayout layout;
    private Configuration configuration = Configuration.getInstance();

    public CreateConfigurationFileTask(String location, RootLayout layout){
        this.runningLocation = location;
        this.layout = layout;
    }

    @Override
    protected File call() {
        try {
            createConfigurationFile();
            return configurationFile;
        } catch (IOException | JSONException e) {
            layout.showError(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void getServerInformation() throws IOException, JSONException {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:3030/configurations/system");
        get.addHeader("Content-Type", "application/json");
        HttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();

        if (entity != null){
            try (InputStream inputStream = entity.getContent()){

                String result = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
                JSONObject object = new JSONObject(result);
                int port = object.getInt("client_port");
                String host = object.getString("webServer.host");
                boolean sslEnabled = object.getBoolean("webServer.https.enabled");

                configuration.setPort(port);
                if (host.equals("null")){
                    configuration.setHost("localhost");
                } else {
                    configuration.setHost(host);
                }

                if (sslEnabled){
                    configuration.setProtocol("https");
                } else {
                    configuration.setProtocol("http");
                }
            }
        }

    }

    private void getFormInformation(){
        configuration.setSlackWebhookURL(layout.getSlackWebhookURL());
        configuration.setQueryTimeoutInSeconds(layout.getTimeout());
        configuration.setRestartECS(layout.isRestartECS());
        configuration.setRestartIIS(layout.isRestartIIS());
        configuration.setCreateECSDump(layout.createECSDump());
        configuration.setCreateIISDump(layout.createIISDump());
    }

    private void createConfigurationFile() throws IOException, JSONException {

        getFormInformation();
        getServerInformation();

        configurationFile = new File(runningLocation);

        try {

            // true if mew file was created, false otherwise
            if (!configurationFile.createNewFile()){

                Platform.runLater(() -> {
                    Optional<ButtonType> overwriteFileOption =  layout.fileAlreadyExistsAlertResult(configurationFile);
                    if (overwriteFileOption.isPresent()){
                        if (overwriteFileOption.get() == ButtonType.YES){
                            configurationFile.delete();

                            try {
                                if (configurationFile.createNewFile()){
                                    writeConfigurationToFile(configurationFile);
                                }
                            } catch (IOException e){
                                layout.showError(e.getMessage());
                            }

                        }
                        else if (overwriteFileOption.get() == ButtonType.NO){
                            layout.showTerminateInstallationAlert();
                        }
                    }
                });

            }
        } catch (IOException e) {
            Platform.runLater(() -> layout.showError(e.getMessage()));
            e.printStackTrace();
        }
    }

    private void writeConfigurationToFile(File configurationFile) throws IOException {

        Properties properties = new Properties();
        properties.setProperty("token", "");
        properties.setProperty("protocol", configuration.getProtocol());
        properties.setProperty("host", configuration.getHost());
        properties.setProperty("port", String.valueOf(configuration.getPort()));
        properties.setProperty("slackWebhookURL", layout.getSlackWebhookURL());
        properties.setProperty("requestTimeoutInSeconds", String.valueOf(layout.getTimeout()));
        properties.setProperty("restartECS", String.valueOf(layout.isRestartECS()));
        properties.setProperty("restartIIS", String.valueOf(layout.isRestartIIS()));
        properties.setProperty("ecsDump", String.valueOf(layout.createECSDump()));
        properties.setProperty("iisDump", String.valueOf(layout.createIISDump()));

        OutputStream outputStream = new FileOutputStream(configurationFile);
        properties.store(outputStream, "e2ewd configuration created by installer");

        Platform.runLater(() -> layout.showConfirmationAlert(configurationFile));

    }
}
