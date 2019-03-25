package integrations;

import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SlackClient {

    private static SlackClient instance;
    private Logger logger = Logger.getInstance();
    private HttpClient httpClient = HttpClientBuilder.create().build();
    private HttpPost post;
    private static String hostname;

    public static SlackClient getInstance() {

        if (instance == null){
            instance = new SlackClient();
        }

        // check if friendlyHostName was set in config file
        if (!ConfigFile.getInstance().getFriendlyHostName().isEmpty() ||
                !ConfigFile.getInstance().getFriendlyHostName().equals("") ||
                ConfigFile.getInstance().getFriendlyHostName() != null){
            hostname = ConfigFile.getInstance().getFriendlyHostName();
        }
        else {
            hostname = CmdOperations.getInstance().getHostname();
        }
        return instance;
    }

    private SlackClient(){

        post = new HttpPost(ConfigFile.getInstance().getSlackWebhookURL());
        post.addHeader("Content-type", "application/json");


    }

    public void sendMessage(String message){

        try {
            post.setEntity(requestBody(message));
            HttpResponse response = httpClient.execute(post);
            HttpEntity responseData = response.getEntity();

            String result = new BufferedReader(new InputStreamReader(responseData.getContent()))
                    .lines().collect(Collectors.joining("\n"));

            logger.write("[SlackClient.sendMessage] Sent message to webhook. Response: " + result);

        } catch (UnsupportedEncodingException e) {
            logger.write("[SlackClient.sendMessage] ERROR: Encoding exception " + e.getMessage());
        } catch (ClientProtocolException e) {
            logger.write("[SlackClient.sendMessage] ERROR: Client protocol " + e.getMessage());
        } catch (IOException e) {
            logger.write("[SlackClient.sendMessage] ERROR: IO Exception " + e.getMessage());
        }
    }

    private StringEntity requestBody(String message) throws UnsupportedEncodingException {

        char boldChar = '*';

        Map<String, String> map = new HashMap<>();
        map.put("text", message + boldChar + " for host "  + hostname + boldChar);
        JSONObject jsonObject = new JSONObject(map);

        return new StringEntity(jsonObject.toString());
    }
}