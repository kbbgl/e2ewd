package version;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GitHubClient {

    private static GitHubClient instance;
    private CloseableHttpClient client;
    private static String baseUrl = "https://raw.githubusercontent.com/kbbgl/e2ewd/master/build/libs";
    private static Logger logger = LoggerFactory.getLogger(GitHubClient.class);

    private GitHubClient() {

        client = HttpClients.createDefault();

    }

    public static GitHubClient getInstance() throws URISyntaxException {

        if (instance == null){
            instance = new GitHubClient();
            logger.debug("Created instance of GitHub HTTP client");
        }

        return instance;

    }

    public String getRemoteVersion() throws IOException {

        String version = "";
        String url = baseUrl + "/version";

        HttpGet getVersion = new HttpGet(url);
        logger.debug("Sending request to " + url);

        logger.debug("Retrieving version from repository...");
        try {
            CloseableHttpResponse response = client.execute(getVersion);
            HttpEntity entity =  response.getEntity();
            int responseCode = response.getStatusLine().getStatusCode();

            if (responseCode != HttpStatus.SC_OK && responseCode != HttpStatus.SC_NOT_MODIFIED) {

                logger.warn("Request returned with status code " +responseCode + " with response '" + response);

            } else {
                if (entity != null) {

                    try (InputStream inputStream = entity.getContent()) {

                        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).readLine();

                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error sending request ", e);
        } finally {
            client.close();
        }

        return version;
    }

    public List<String> getConfig(){

        List<String> configFileRows = new ArrayList<>();

        HttpGet getConfig = new HttpGet(baseUrl + "/config.properties");

        return configFileRows;
    }
}
