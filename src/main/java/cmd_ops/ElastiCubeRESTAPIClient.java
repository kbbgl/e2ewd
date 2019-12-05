package cmd_ops;

import file_ops.ConfigFile;
import models.ElastiCube;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ElastiCubeRESTAPIClient {

    private static final Logger logger = LoggerFactory.getLogger(ElastiCubeRESTAPIClient.class);
    private static final ConfigFile configFile = ConfigFile.getInstance();
    private HttpClient client;
    private HttpGet get;
    private String uri;
    private boolean isCallSuccessful;
    private List<ElastiCube> listOfElastiScubes = new ArrayList<>();
    private String defaultElastiCube;
    private int responseCode;

    public ElastiCubeRESTAPIClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

        setUri();
        initializeClient();
        executeCall();
    }

    private void initializeClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                .setConnectTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                .setSocketTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
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
        get.addHeader("authorization", "Bearer " + configFile.getToken());

    }

    void executeCall() throws IOException {

        HttpResponse response = client.execute(get);
        logger.debug("Executing REST API call to GET /getElastiCubes...");
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
                        JSONArray elastiCubesArray = new JSONArray(res);
                        defaultElastiCube = elastiCubesArray.getJSONObject(0).getString("title");
                        setDefaultElastiCube(defaultElastiCube);

                        logger.info("GET api/elasticubes/servers/LocalHost returned " + elastiCubesArray.length() + " ElastiCubes");

                        // Iterate over all ElastiCubes
                        for (int i = 0; i < elastiCubesArray.length(); i++){
                            JSONObject currentElastiCubeObject = (JSONObject) elastiCubesArray.get(i);
                            String elastiCubeName = currentElastiCubeObject.getString("title");
                            int elastiCubeStatus = currentElastiCubeObject.getInt("status");

                            // Check that ElastiCube is running (status == 2 is running)
                            if (elastiCubeStatus == 2){
                                ElastiCube currentElastiCube = new ElastiCube(elastiCubeName, "RUNNING");
                                logger.debug("Getting ElastiCube port from PSM...");
                                CmdOperations.getInstance().setElastiCubePort(currentElastiCube);
                                logger.debug("Finished getting ElastiCube port from PSM.");
                                addElastiCubeToList(currentElastiCube);

                            } else {
                                logger.debug("ElastiCube " + elastiCubeName + " not in RUNNING mode.");
                            }
                        }

                        setCallSuccessful(true);

                    } catch (JSONException ex) {
                        logger.error("Error parsing response from GET /getElastiCubes. Response code " +
                                responseCode + " , error: " +
                                ex.getMessage());
                        setCallSuccessful(false);
                    } catch (InterruptedException e) {
                        logger.error("Error getting port for ElastiCube. Exception: \n" + Arrays.toString(e.getStackTrace()));
                    }
                } else if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    logger.warn("Check that the token '" + configFile.getToken() + "' in the configuration file is valid");
                    logger.debug(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_NOT_FOUND){
                    logger.error("The endpoint '/api/elasticubes/servers/LocalHost' was not found (404).");
                    logger.debug(res);
                  setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_FORBIDDEN) {
                    logger.warn("Ensure that you have sufficient permissions to run calls to GET '/api/elasticubes/servers/LocalHost'");
                    logger.debug(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_BAD_REQUEST){
                    logger.warn("Bad GET request sent to '/api/elasticubes/servers/LocalHost'");
                    logger.debug(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_BAD_GATEWAY){
                    logger.error("Server returned 'Bad Gateway' (502)");
                    logger.debug(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_GATEWAY_TIMEOUT){
                    logger.error("Server returned 'Gateway Timeout' (504)");
                    logger.debug(res);
                    setCallSuccessful(false);
                } else if (responseCode == HttpStatus.SC_INTERNAL_SERVER_ERROR){
                    logger.error("Server returned 'Internal Server Error' (500)");
                    logger.debug(res);
                    setCallSuccessful(false);
                } else {
                    logger.error("Call failed with error code " + responseCode);
                    logger.debug(res);
                    setCallSuccessful(false);
                }

            }
            catch (IOException e){

                logger.error("Error getting list of ElastiCubes from GET api/elasticubes/servers/LocalHost. Response code " +
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

        String endpoint = "/api/elasticubes/servers/LocalHost";

        if (configFile.getPort() != 443){
            uri = configFile.getProtocol() +
                    "://" + configFile.getHost() + ":" +
                    configFile.getPort() + endpoint;
        }
        else {
            uri = configFile.getProtocol() +
                    "://" + configFile.getHost() + endpoint;
        }

    }

    public void addElastiCubeToList(ElastiCube ec) {
        logger.debug("Added " + ec.toString() + " to list of ElastiCubes.");
        this.listOfElastiScubes.add(ec);
    }

    public List<ElastiCube> getListOfElastiScubes() {
        return listOfElastiScubes;
    }

    public void setDefaultElastiCube(String defaultElastiCube) {
        this.defaultElastiCube = defaultElastiCube;
    }

    public String getDefaultElastiCube() {
        return defaultElastiCube;
    }

    private void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
