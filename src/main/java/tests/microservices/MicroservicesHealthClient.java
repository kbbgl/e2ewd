package tests.microservices;

import cmd_ops.CmdOperations;
import conf.Configuration;
import integrations.SlackClient;
import org.apache.http.HttpEntity;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.stream.Collectors;

public class MicroservicesHealthClient {

    private static MicroservicesHealthClient instance;
    private static final Logger logger = LoggerFactory.getLogger(MicroservicesHealthClient.class);
    private static final Configuration config = Configuration.getInstance();
    private HttpClient client;
    private HttpGet get;
    private String uri;
    private SlackClient slackClient = SlackClient.getInstance();
    private CmdOperations cmdOperations = CmdOperations.getInstance();

    public static MicroservicesHealthClient getInstance() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        logger.debug("Created instance");
        if (instance == null){
            instance = new MicroservicesHealthClient();
        }
        return instance;

    }

    private MicroservicesHealthClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        setUri();
        initializeClient();
    }

    private void initializeClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(config.getRequestTimeoutInSeconds() * 1000)
                .setConnectTimeout(config.getRequestTimeoutInSeconds() * 1000)
                .setSocketTimeout(config.getRequestTimeoutInSeconds() * 1000)
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
        get = new HttpGet(uri);

        logger.debug("Initialized client");

    }

    private void setUri() {

        if (config.getPort() != 443){
            uri = config.getProtocol() +
                    "://" + config.getHost() + ":" +
                    config.getPort() + "/api/test";
        }
        else {
            uri = config.getProtocol() +
                    "://" + config.getHost() + "/api/test";
        }

        logger.debug("Set URL to " + uri);
    }

    public void executeCall() throws IOException {

        HttpResponse response = client.execute(get);
        logger.info("Executing call to 'api/test' endpoint...");
        parseResponse(response);
    }

    private void parseResponse(HttpResponse response){

        logger.debug("Parsing response...");
        HttpEntity entity = response.getEntity();
        int responseCode = response.getStatusLine().getStatusCode();


        if (entity != null){

            try(InputStream inputStream = entity.getContent()){

                String res = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                logger.debug("Call response: " + res);
                logger.debug("Call response code: " + responseCode);

                if (responseCode == 200 || responseCode == 304) {

                    if (cmdOperations.getSisenseVersion().startsWith("6") ||
                            cmdOperations.getSisenseVersion().startsWith("7.0") ||
                            cmdOperations.getSisenseVersion().startsWith("7.1")){

                        try {
                            JSONObject responseObject = new JSONObject(res);
                            if (!responseObject.getString("test").equals("pass")){
                                logger.warn("Application is down.");
                                cmdOperations.restartIIS();
                            } else {
                                logger.info("Application is up.");
                            }

                        } catch (JSONException e) {
                            logger.error("Error parsing response as JSON");
                            logger.info("Error: " + e.getMessage());
                            logger.info("Response: " + res);
                        }


                    }else {
                        try {
                            JSONObject responseObject = new JSONObject(res);
                            Iterator<String> keysIterator = responseObject.keys();

                            // Iterate over response keys (microservice)
                            while (keysIterator.hasNext()){

                                String microservice = keysIterator.next();

                                if (responseObject.get(microservice) instanceof JSONObject){
                                    boolean isMicroserviceHealthy = ((JSONObject) responseObject.get(microservice)).getBoolean("active");

                                    logger.debug(microservice + " is healthy: " + isMicroserviceHealthy);

                                    // Check if any is unhealthy
                                    if (!isMicroserviceHealthy){
                                        logger.warn(microservice + " is unhealthy!");

                                        if (slackClient != null){
                                            slackClient.sendMessage(":warning: `" + microservice + "` is unhealthy!");
                                        }

                                    }
                                }

                            }

                        } catch (JSONException e){
                            logger.error("Error parsing response as JSON");
                            logger.info("Error: " + e.getMessage());
                            logger.info("Response: " + res);
                        }
                    }
                }

                else {

                    if (cmdOperations.getSisenseVersion().startsWith("6") ||
                            cmdOperations.getSisenseVersion().startsWith("7.0") ||
                            cmdOperations.getSisenseVersion().startsWith("7.1")) {

                        logger.warn("Application is down.");
                        cmdOperations.restartIIS();

                        }


                }

            } catch (IOException e){

                logger.error("Error reading response: " + e.getMessage());

            } catch (InterruptedException e) {
                logger.error("Error retrieving Sisense version from registry: " + e.getMessage());
            } finally {
                logger.debug("Releasing api/test client connection...");
                get.releaseConnection();
                logger.debug("Released api/test client connection");
            }
        }

    }
}
