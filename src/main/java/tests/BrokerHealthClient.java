package tests;


import integrations.SlackClient;
import models.BrokerQueue;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

public class BrokerHealthClient{

    private static BrokerHealthClient instance;
    private URL QUEUES_API_URL = new URL("http://localhost:15672/api/queues");
    private HttpClient client;
    private HttpGet get;
    private static final Logger logger = LoggerFactory.getLogger(BrokerHealthClient.class);
    private SlackClient slackClient = SlackClient.getInstance();

    private BrokerHealthClient() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        initializeClient();
    }

    public static BrokerHealthClient getInstance() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        if (instance == null){
            logger.debug("Instance created");
            instance = new BrokerHealthClient();
        }
        return instance;
    }

    private void initializeClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        logger.debug("Inializing client...");
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(30 * 1000)
                .setConnectTimeout(30 * 1000)
                .setSocketTimeout(30 * 1000)
                .build();


        client = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(requestConfig)
                .setSSLContext(sslContext)
                .setConnectionManager(
                        new PoolingHttpClientConnectionManager(
                                RegistryBuilder.<ConnectionSocketFactory>create()
                                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                                        .register("https", new SSLConnectionSocketFactory(sslContext,
                                                NoopHostnameVerifier.INSTANCE))
                                        .build()
                        )
                ).build();

        logger.debug("Client configuration: " + client.toString());

        get = new HttpGet(QUEUES_API_URL.toString());
        get.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeaders());
        logger.debug("Finished initializing client");

    }

    private String getAuthHeaders(){

        String auth = "guest:guest";
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);

        return authHeader;

    }

    public void executeQuery() throws IOException {

        logger.info("Checking Broker health...");
        HttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        int responseCode = response.getStatusLine().getStatusCode();
        logger.debug("Executing call to " + QUEUES_API_URL.toString());

        if (entity != null){
            logger.debug("Response returned not empty");

            try(InputStream inputStream = entity.getContent()){

                String res = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                logger.debug("Response: " + res);

                logger.debug("Response code: " + responseCode);

                if (responseCode == 200) {
                    try {
                        JSONArray queues = new JSONArray(res);
                        int numberOfQueues = queues.length();
                        logger.info("Found " + numberOfQueues + " queues");

                        logger.debug("Iterating over all queues...");
                        for (int i = 0; i < numberOfQueues; i++){
                            JSONObject queueJson = queues.getJSONObject(i);
                            BrokerQueue queue = new BrokerQueue(
                                    queueJson.getInt("consumers"),
                                    queueJson.getInt("message_bytes"),
                                    queueJson.getInt("messages"),
                                    queueJson.getInt("memory"),
                                    queueJson.getString("name")
                            );

                            // Check if one of the queues us unhealthy
                            if (queue.getNumberOfMessages() > 0 || queue.getNumberOfConsumers() == 0){
                                logger.warn("Queue '" + queue.getName() + "' unhealthy! Has " + queue.getNumberOfConsumers() + " consumers, " + queue.getNumberOfMessages() + "messages stuck in queue");
                                slackClient.sendMessage(":warning: Queue `" + queue.getName() + "` is unhealthy! \n```" + queue.toString() + "```" );
                                logger.debug(queue.toString());
                            }
                        }


                    } catch (JSONException e){

                        logger.error("Error parsing response as valid JSON: " + e.getMessage());

                    }
                }

            }
            catch (IOException e){

                logger.error("Error reading response: " + e.getMessage());

            } finally {
                logger.debug("Releasing REST API client connection");
                get.releaseConnection();
            }
        }

        logger.info("Broker health checked finished.");
    }

}
