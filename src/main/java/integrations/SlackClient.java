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
    private String hostname = CmdOperations.getInstance().getHostname();

    public static SlackClient getInstance() {

        if (instance == null){
            instance = new SlackClient();
        }
        return instance;
    }

    private SlackClient(){

        System.out.println(ConfigFile.getInstance().getSlackWebhookURL());
        post = new HttpPost(ConfigFile.getInstance().getSlackWebhookURL());
        post.addHeader("Content-type", "application/json");


    }

    public void sendMessage(){

        try {
            post.setEntity(requestBody());
            HttpResponse response = httpClient.execute(post);
            HttpEntity responseData = response.getEntity();

            String result = new BufferedReader(new InputStreamReader(responseData.getContent()))
                    .lines().collect(Collectors.joining("\n"));

            System.out.println(result);
            logger.write("[SlackClient.sendMessage] response: " + result);

        } catch (UnsupportedEncodingException e) {
            logger.write("[SlackClient.sendMessage] ERROR: Encoding exception " + e.getMessage());
        } catch (ClientProtocolException e) {
            logger.write("[SlackClient.sendMessage] ERROR: Client protocol " + e.getMessage());
        } catch (IOException e) {
            logger.write("[SlackClient.sendMessage] ERROR: IO Exception " + e.getMessage());
        }
    }

    private StringEntity requestBody() throws UnsupportedEncodingException {

        Map<String, String> map = new HashMap<>();
        map.put("text", ":rotating_light: CRITICAL! Watchdog test failed for *" + hostname + "*");
        JSONObject jsonObject = new JSONObject(map);

        return new StringEntity(jsonObject.toString());
    }
}
