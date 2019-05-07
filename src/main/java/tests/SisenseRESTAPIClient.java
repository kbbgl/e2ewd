package tests;

import file_ops.ConfigFile;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

class SisenseRESTAPIClient{

    private static final Logger logger = LoggerFactory.getLogger(SisenseRESTAPIClient.class);
    private static final ConfigFile configFile = ConfigFile.getInstance();
    private HttpClient client;
    private HttpPost post;
    private String uri;
    private boolean isCallSuccessful;
    private String callResponse;
    private int responseCode;
    private String elastiCubeName;

    SisenseRESTAPIClient(String elastiCubeName) throws JSONException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        this.elastiCubeName = elastiCubeName;

        setUri();
        initializeClient(createJAQL(elastiCubeName));

    }

    private void initializeClient(JSONObject jaql) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                .setConnectTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                .setSocketTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder
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


        client = httpClient;
        post = new HttpPost(uri);
        post.addHeader("authorization", "Bearer " + configFile.getToken());
        post.setEntity(new StringEntity(jaql.toString(), ContentType.APPLICATION_JSON));

    }

    void executeQuery() throws IOException {

        HttpResponse response = client.execute(post);
        logger.debug("Executing REST API query...");
        parseResponse(response);

    }

    private void parseResponse(HttpResponse response){

        logger.debug("Parsing response...");
        HttpEntity entity = response.getEntity();
        int responseCode = response.getStatusLine().getStatusCode();
        setResponseCode(responseCode);

        if (entity != null){

            try(InputStream inputStream = entity.getContent()){

                String res = new BufferedReader(new InputStreamReader(inputStream))
                        .lines()
                        .collect(Collectors.joining("\n"));

                setCallResponse(res);
                logger.debug("Call response:" + getCallResponse());

                if (responseCode == 200) {

                    try {
                        JSONObject responseObject = new JSONObject(res);
                        JSONObject valuesArray = (JSONObject) responseObject.getJSONArray("values").get(0);
                        int count = valuesArray.getInt("data");

                        // Check if result is larger than 0
                        if (count > 0) {
                            setCallSuccessful(true);
                        } else {
                            logger.info("Query failed for " +
                                    elastiCubeName + " with code " +
                                    responseCode + " ,response: " +
                                    getCallResponse());
                            setCallSuccessful(false);
                        }
                    } catch (JSONException e){
                        logger.warn("Query returned no `values.data` object");
                        try {
                            JSONObject responseObject = new JSONObject(res);
                            String details = responseObject.getString("details");

                            if (details.equals("ElastiCube is processing and cannot be queried.")){
                                logger.info("ElastiCube is processing and cannot be queried.");
                                setCallSuccessful(true);
                            } else {
                                logger.error("Query failed for " +
                                        elastiCubeName + " with code " +
                                        responseCode + " , error: " +
                                        e.getMessage());
                                setCallSuccessful(false);
                            }

                        } catch (JSONException ex) {
                            logger.error("Query failed for " +
                                    elastiCubeName + " with code " +
                                    responseCode + " , error: " +
                                    e.getMessage());
                            setCallSuccessful(false);
                        }
                    }
                }

            }
            catch (IOException e){


                logger.error("Query failed for " +
                        elastiCubeName + " with code " +
                        responseCode + " , error: " +
                        e.getMessage());
                setCallSuccessful(false);
            } finally {
                logger.debug("Releasing REST API client connection");
                post.releaseConnection();
            }
        }

    }

    private void setCallSuccessful(boolean callSuccessful) {
        isCallSuccessful = callSuccessful;
    }

    boolean isCallSuccessful() {
        return isCallSuccessful;
    }

    private void setUri() {

        if (configFile.getPort() != 443){
            uri = configFile.getProtocol() +
                    "://" + configFile.getHost() + ":" +
                    configFile.getPort() + "/api/datasources/x/jaql";
        }
        else {
            uri = configFile.getProtocol() +
                    "://" + configFile.getHost() + "/api/datasources/x/jaql";
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

    int getResponseCode() {
        return responseCode;
    }

    String getCallResponse() {

        return callResponse;
    }
}
