package integrations;

import cmd_ops.CmdOperations;
import conf.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SlackClient {

    private static SlackClient instance;
    private static final Logger logger = LoggerFactory.getLogger(SlackClient.class);
    private HttpClient client;
    private HttpPost post;
    private static String hostname;
    private final int REQUEST_TIMEOUT_SECONDS = 5;

    public static SlackClient getInstance() {

        if (instance == null){
            instance = new SlackClient();
            logger.debug("SlackClient instance created.");
        }

        if (!Configuration.getInstance().isSlackEnabled()){
            instance = null;
        }

        return instance;
    }

    private SlackClient(){

        logger.debug("Initialized slack client, set timeout: " + REQUEST_TIMEOUT_SECONDS);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(REQUEST_TIMEOUT_SECONDS * 1000)
                .setConnectTimeout(REQUEST_TIMEOUT_SECONDS * 1000)
                .setSocketTimeout(REQUEST_TIMEOUT_SECONDS * 1000)
                .build();

        // check if friendlyHostName was set in config file
        if (!Configuration.getInstance().getFriendlyHostName().isEmpty() ||
                !Configuration.getInstance().getFriendlyHostName().equals("") ||
                Configuration.getInstance().getFriendlyHostName() != null){

            hostname = Configuration.getInstance().getFriendlyHostName();
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

        client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        post = new HttpPost(Configuration.getInstance().getSlackWebhookURL());
        post.addHeader("Content-type", "application/json");

    }

    public void sendMessage(String message){

        logger.debug("Sending message '" + message + "' to Slack webhook " + Configuration.getInstance().getSlackWebhookURL());

        try {
            post.setEntity(requestBody(message));
            logger.debug("Executing POST to Slack..");
            HttpResponse response = client.execute(post);

            logger.debug("Response received from Slack");
            HttpEntity responseData = response.getEntity();

            if (responseData != null){
                String result = new BufferedReader(new InputStreamReader(responseData.getContent(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                logger.debug("Response from Slack: " + result);
            } else {
                logger.debug("Response is empty");
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
        } catch (JSONException e){
            logger.error("JSON exception: ");
            logger.debug(Arrays.toString(e.getStackTrace()));
        } finally {
            logger.debug("Releasing Slack connection");
            post.releaseConnection();
        }
    }

    private StringEntity requestBody(String message) throws UnsupportedEncodingException, JSONException {

        char boldChar = '*';

        Map<String, String> map = new HashMap<>();
        map.put("text",
                boldChar +
                hostname +
                boldChar +
                " " +
                message);

        JSONObject jsonObject = new JSONObject(map);

        logger.debug("Slack message request body: ");
        logger.debug(jsonObject.toString(3));
        return new StringEntity(jsonObject.toString());
    }
}