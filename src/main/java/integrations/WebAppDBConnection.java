package integrations;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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
import java.util.stream.Collectors;

public class WebAppDBConnection {

    private static final Logger logger = LoggerFactory.getLogger(WebAppDBConnection.class);
    private static HttpClient client = HttpClientBuilder.create().build();
    private static HttpPost post = new HttpPost("https://api.mlab.com/api/1/databases/e2ewd_db/collections/operations?apiKey=k_BNdi9vZuzGGU_dHlFZnKueWx5Za6DT");

    public static void sendOperation(JSONObject requestBody) throws IOException, JSONException {

        StringEntity body = new StringEntity(requestBody.toString());
        post.addHeader("Content-Type", "application/json");
        post.setEntity(body);

        HttpResponse response = client.execute(post);
        logger.debug("Executing POST to mongo...");


        if (response.getEntity() != null){
            HttpEntity responseData = response.getEntity();

            if (response.getStatusLine().getStatusCode() == 200){
                logger.info("Operation sent successfully");
            }
            else {
                String result = new BufferedReader(new InputStreamReader(responseData.getContent()))
                        .lines().collect(Collectors.joining("\n"));
                JSONObject responseJSON = new JSONObject(result);
                logger.warn("Call failed with response code " + response.getStatusLine().getStatusCode());
                logger.debug(responseJSON.toString(3));
            }
        }


    }

}
