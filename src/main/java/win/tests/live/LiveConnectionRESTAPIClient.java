package tests.live;

import conf.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LiveConnectionRESTAPIClient {

    private static final Logger logger = LoggerFactory.getLogger(LiveConnectionRESTAPIClient.class);
    private static final Configuration config = Configuration.getInstance();
    private HttpClient client;
    private HttpGet get;
    private String uri;
    private boolean isCallSuccessful;
    private List<String> listLiveConnections = new ArrayList<>();
    private int responseCode;

    public LiveConnectionRESTAPIClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

        setUri();
        initializeClient();
        executeCall();
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
        get.addHeader("authorization", "Bearer " + config.getToken());

    }

    void executeCall() throws IOException {

        HttpResponse response = client.execute(get);
        logger.debug("Executing REST API call to GET " + getUri() + "...");
        parseResponse(response);

    }

    private void parseResponse(HttpResponse response){

        logger.debug("Parsing response...");
        HttpEntity entity = response.getEntity();
        int responseCode = response.getStatusLine().getStatusCode();
        setResponseCode(responseCode);

        // Check that response
        if (entity != null){

            try(InputStream inputStream = entity.getContent()) {

                String res = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                // todo add logger.debug with response for each
                if (responseCode == HttpStatus.SC_OK) {

                    try {
                        JSONArray liveConnections = new JSONArray(res);

                        logger.info("GET " + getUri() + " returned " + liveConnections.length() + " Live Connections");

                        // Iterate over all Live Connections
                        for (int i = 0; i < liveConnections.length(); i++){
                            JSONObject currentLiveConnection = (JSONObject) liveConnections.get(i);
                            String liveConnectionString = currentLiveConnection.getString("title");

                            addLiveConnectionToList(liveConnectionString);
                        }

                        setCallSuccessful(true);

                    } catch (JSONException ex) {
                        logger.error("Error parsing response from GET" + getUri() + ". Response code " +
                                responseCode + " , error: " +
                                ex.getMessage());
                        setCallSuccessful(false);
                    }
                } else if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    logger.warn("Check that the token '" + config.getToken() + "' in the configuration file is valid");
                    logger.info(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_NOT_FOUND){
                    logger.error("The endpoint " + getUri() + " was not found (404). Check '/app/paths' for the microservice serving this endpoint");
                    logger.info(res);
                  setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_FORBIDDEN) {
                    logger.warn("Ensure that you have sufficient permissions to run calls to GET '" + getUri() + "'");
                    logger.info(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_BAD_REQUEST){
                    logger.warn("Bad GET request sent to '" + getUri() + "'");
                    logger.info(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_BAD_GATEWAY){
                    logger.error("Server returned 'Bad Gateway' (502)");
                    logger.info(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_GATEWAY_TIMEOUT){
                    logger.error("Server returned 'Gateway Timeout' (504)");
                    logger.info(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_INTERNAL_SERVER_ERROR){
                    logger.error("Server returned 'Internal Server Error' (500)");
                    logger.info(res);
                    setCallSuccessful(false);
                } else {
                    logger.error("Call failed with error code " + responseCode);
                    logger.info(res);
                    setCallSuccessful(false);
                }

            }
            catch (IOException e){

                logger.error("Error getting list of Live Connections from GET " + getUri() + ". Response code " +
                        responseCode + " , error: " +
                        e.getMessage());

                setCallSuccessful(false);
            } finally {
                logger.debug("Releasing REST API client connection...");
                get.releaseConnection();
                logger.debug("REST API client connection released.");
            }
        }

    }

    private void setCallSuccessful(boolean callSuccessful) {
        isCallSuccessful = callSuccessful;
    }

    public boolean isCallSuccessful() {
        return isCallSuccessful;
    }

    private void setUri() {

        String endpoint = "/api/v1/elasticubes/live";

        if (config.getPort() != 443){
            uri = config.getProtocol() +
                    "://" + config.getHost() + ":" +
                    config.getPort() + endpoint;
        }
        else {
            uri = config.getProtocol() +
                    "://" + config.getHost() + endpoint;
        }

    }

    public String getUri() {
        return uri;
    }

    public void addLiveConnectionToList(String liveConnectionTitle ) {
        logger.debug("Added " + liveConnectionTitle + " to list of Live Connections.");
        this.listLiveConnections.add(liveConnectionTitle);
    }

    public List<String> getListLiveConnections() {
        return listLiveConnections;
    }

    private void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
