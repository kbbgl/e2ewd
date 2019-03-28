package integrations;

import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SlackClient {

    private static SlackClient instance;
    private static Logger logger = LoggerFactory.getLogger(SlackClient.class);
    private HttpClient httpClient = HttpClientBuilder.create().build();
    private HttpPost post;
    private static String hostname;

    public static SlackClient getInstance() {

        if (instance == null){
            logger.debug("SlackClient instance created.");
            instance = new SlackClient();
        }

        // check if friendlyHostName was set in config file
        if (!ConfigFile.getInstance().getFriendlyHostName().isEmpty() ||
                !ConfigFile.getInstance().getFriendlyHostName().equals("") ||
                ConfigFile.getInstance().getFriendlyHostName() != null){

            hostname = ConfigFile.getInstance().getFriendlyHostName();
            logger.debug("Retrieved friendlyHostName:" + hostname);
        }
        else {
            try {
                hostname = CmdOperations.getInstance().getHostname();
            } catch (IOException | InterruptedException e) {
                logger.error("Error getting hostname: " + e.getMessage());
                logger.debug(Arrays.toString(e.getStackTrace()));
            }
        }
        return instance;
    }

    private SlackClient(){

        post = new HttpPost(ConfigFile.getInstance().getSlackWebhookURL());
        post.addHeader("Content-type", "application/json");

    }

    public void sendMessage(String message){

        logger.debug("Sending message " + message + " to Slack webhook " + ConfigFile.getInstance().getSlackWebhookURL());

        try {
            post.setEntity(requestBody(message));
            HttpResponse response = httpClient.execute(post);
            logger.debug("Executing POST to Slack...");
            HttpEntity responseData = response.getEntity();

            if (responseData != null){
                String result = new BufferedReader(new InputStreamReader(responseData.getContent()))
                        .lines().collect(Collectors.joining("\n"));

                logger.info("Message to webhook. Response: " + result);
            }

        } catch (UnsupportedEncodingException e) {
            logger.error("Encoding error: " + e.getMessage());
            logger.debug(Arrays.toString(e.getStackTrace()));
        } catch (ClientProtocolException e) {
            logger.error("Client protocol exception: " + e.getMessage());
            logger.debug(Arrays.toString(e.getStackTrace()));
        } catch (IOException e) {
            logger.error("IO exception: " + e.getMessage());
            logger.debug(Arrays.toString(e.getStackTrace()));
        }
    }

    private StringEntity requestBody(String message) throws UnsupportedEncodingException {

        char boldChar = '*';

        Map<String, String> map = new HashMap<>();
        map.put("text",
                boldChar +
                hostname +
                boldChar +
                " " +
                message);

        JSONObject jsonObject = new JSONObject(map);

        return new StringEntity(jsonObject.toString());
    }
}