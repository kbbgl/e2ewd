package integrations;

import logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import java.io.IOException;

public class WebAppDBConnection {

    private static HttpClient client = HttpClientBuilder.create().build();
    private static HttpPost post = new HttpPost("https://api.mlab.com/api/1/databases/e2ewd_db/collections/operations?apiKey=k_BNdi9vZuzGGU_dHlFZnKueWx5Za6DT");

    public static void sendOperation(JSONObject requestBody) throws IOException {

        StringEntity body = new StringEntity(requestBody.toString());
        post.addHeader("Content-Type", "application/json");
        post.setEntity(body);

        HttpResponse response = client.execute(post);

        if (response.getEntity() != null){
            if (response.getStatusLine().getStatusCode() == 200){
                Logger.getInstance().write("[WebAppDBConnection.sendOperation] Sent successfully");

            }
            else {
                Logger.getInstance().write("[WebAppDBConnection.sendOperation] Call failed : " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
            }
        }


    }

}
