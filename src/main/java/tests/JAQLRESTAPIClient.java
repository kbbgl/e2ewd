package tests;

import file_ops.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run_strategy.StrategyExecutor;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

class JAQLRESTAPIClient {

    private static final Logger logger = LoggerFactory.getLogger(JAQLRESTAPIClient.class);
    private static final Configuration config = Configuration.getInstance();
    private HttpClient client;
    private HttpPost post;
    private String uri;
    private boolean isCallSuccessful;
    private boolean isUnauthorized = false;
    private boolean requiresServiceRestart;
    private String callResponse;
    private int responseCode;
    private String elastiCubeName;
    private StrategyExecutor strategyExecutor = StrategyExecutor.getInstance();

    JAQLRESTAPIClient(String elastiCubeName) throws JSONException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        this.elastiCubeName = elastiCubeName;

        setUri();
        initializeClient(createJAQL(elastiCubeName));

    }

    private void initializeClient(JSONObject jaql) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

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
        post = new HttpPost(uri);
        post.addHeader("authorization", "Bearer " + config.getToken());
        post.setEntity(new StringEntity(jaql.toString(), ContentType.APPLICATION_JSON));

    }

    void executeQuery() throws IOException {

        HttpResponse response = client.execute(post);
        logger.debug("Executing JAQL for ElastiCube '" + elastiCubeName +"'...");
        parseResponse(response);

    }

    private void parseResponse(HttpResponse response){

        logger.debug("Parsing response...");
        HttpEntity entity = response.getEntity();
        int responseCode = response.getStatusLine().getStatusCode();
        setResponseCode(responseCode);

        if (entity != null){

            try(InputStream inputStream = entity.getContent()){

                String res = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                setCallResponse(res);
                if (responseCode == 200) {

                    try {
                        JSONObject responseObject = new JSONObject(res);
                        JSONObject valuesArray = (JSONObject) responseObject.getJSONArray("values").get(0);
                        int count = valuesArray.getInt("data");

                        // Check if result is larger than 0
                        if (count > 0) {
                            setRequiresServiceRestart(false);
                            setCallSuccessful(true);
                        } else {
                            logger.info("Query failed for " +
                                    elastiCubeName + " with code " +
                                    responseCode + " ,response: " +
                                    getCallResponse());
                            setRequiresServiceRestart(false);
                            setCallSuccessful(false);
                        }
                    } catch (JSONException e){
                        logger.warn("Query returned no `values.data` object");
                        try {
                            JSONObject responseObject = new JSONObject(res);

                            if (responseObject.has("details")){
                                String details = responseObject.getString("details");
                                logger.info(details);
                                setRequiresServiceRestart(false);
                                setCallSuccessful(false);

                            } else {
                                logger.error("Query failed for " +
                                        elastiCubeName + " with code " +
                                        responseCode + " , error: " +
                                        e.getMessage());
                                setRequiresServiceRestart(false);
                                setCallSuccessful(false);
                            }

                        } catch (JSONException ex) {
                            logger.error("Query failed for " +
                                    elastiCubeName + " with code " +
                                    responseCode + " , error: " +
                                    e.getMessage());
                            setRequiresServiceRestart(false);
                            setCallSuccessful(false);
                        }
                    }
                } else if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    logger.warn("Check that the token '" + config.getToken() + "' in the configuration file is valid");
                    logger.debug(res);
                    setRequiresServiceRestart(false);
                    setCallSuccessful(false);
                    setUnauthorized(true);
                } else if (responseCode == HttpStatus.SC_NOT_FOUND){
                    logger.error("The endpoint '" + getUri() +"'' was not found (404).");
                    logger.debug(res);
                    setRequiresServiceRestart(true);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_FORBIDDEN) {
                    logger.warn("Ensure that you have sufficient permissions to run calls to GET '" + getUri() +"'");
                    logger.debug(res);
                    setRequiresServiceRestart(false);
                    setCallSuccessful(false);
                    setUnauthorized(true);
                } else if (responseCode == HttpStatus.SC_BAD_REQUEST){
                    logger.warn("Bad GET request sent to '" + getUri() + "'");
                    logger.debug(res);
                    setRequiresServiceRestart(false);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_BAD_GATEWAY){
                    logger.error("Server returned 'Bad Gateway' (502)");
                    logger.debug(res);
                    setRequiresServiceRestart(true);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_GATEWAY_TIMEOUT){
                    logger.error("Server returned 'Gateway Timeout' (504)");
                    logger.debug(res);
                    setRequiresServiceRestart(true);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_INTERNAL_SERVER_ERROR){
                    logger.error("Server returned 'Internal Server Error' (500)");
                    logger.debug(res);
                    setRequiresServiceRestart(true);
                    setCallSuccessful(false);
                } else {
                    logger.error("Call failed with error code " + responseCode);
                    logger.debug(res);
                    setRequiresServiceRestart(false);
                    setCallSuccessful(false);
                }

            }
            catch (IOException e){

                logger.error("Query failed for " +
                        elastiCubeName + " with code " +
                        responseCode + " , error: " +
                        e.getMessage());

                setRequiresServiceRestart(false);
            } finally {
                logger.debug("Releasing JAQL REST API client connection...");
                post.releaseConnection();
                logger.debug("JAQL REST API client connection released");
            }
        }

    }

    public void setRequiresServiceRestart(boolean requiresServiceRestart) {
        this.requiresServiceRestart = requiresServiceRestart;
    }

    public boolean isRequiresServiceRestart() {
        return requiresServiceRestart;
    }

    private void setCallSuccessful(boolean callSuccessful) {
        isCallSuccessful = callSuccessful;
    }

    boolean isCallSuccessful() {
        return isCallSuccessful;
    }

    private void setUri() {

        if (config.getPort() != 443){
            uri = config.getProtocol() +
                    "://" + config.getHost() + ":" +
                    config.getPort() + "/api/datasources/x/jaql";
        }
        else {
            uri = config.getProtocol() +
                    "://" + config.getHost() + "/api/datasources/x/jaql";
        }

    }

    private static JSONObject createJAQL(String elastiCube) throws JSONException {

        JSONObject rootObject = new JSONObject();
        JSONArray metadataArray = new JSONArray();

        JSONObject jaqlObject = new JSONObject();

        rootObject.put("datasource", elastiCube);
        jaqlObject.put("formula", "1");
        metadataArray.put(jaqlObject);
        rootObject.put("metadata", metadataArray);

        return rootObject;
    }

    private void setCallResponse(String callResponse) {
        try {
            JSONObject response = new JSONObject(callResponse);
            this.callResponse = response.toString(3);
        } catch (JSONException e) {
            logger.warn("WARNING: Couldn't parse response as valid JSON");
            this.callResponse = callResponse;
        }
    }

    private void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setUnauthorized(boolean unauthorized) {
        isUnauthorized = unauthorized;
    }

    public boolean isUnauthorized() {
        return isUnauthorized;
    }

    int getResponseCode() {
        return responseCode;
    }

    String getCallResponse() {

        return callResponse;
    }

    public String getUri() {
        return uri;
    }
}
