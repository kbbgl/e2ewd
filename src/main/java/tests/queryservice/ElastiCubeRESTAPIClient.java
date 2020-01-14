package tests.queryservice;

import cmd_ops.CmdOperations;
import conf.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
import java.io.*;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
    private static final Configuration config = Configuration.getInstance();
    private HttpClient client;
    private HttpGet get;
    private String uri;
    private boolean requiresServiceRestart;
    private List<ElastiCube> listOfElastiCubes = new ArrayList<>();
    private String defaultElastiCube;

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
        logger.debug("Executing REST API call to GET api/elasticubes/servers/LocalHost...");
        parseResponse(response);

    }

    private void parseResponse(HttpResponse response){

        logger.debug("Parsing response...");
        HttpEntity entity = response.getEntity();
        int responseCode = response.getStatusLine().getStatusCode();

        // Check that response
        if (entity != null){

            try(InputStream inputStream = entity.getContent()) {

                String res = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                logger.debug("Response: " + res);

                if (responseCode == HttpStatus.SC_OK) {

                    try {
                        JSONArray elastiCubesArray = new JSONArray(res);

                        if (elastiCubesArray.getJSONObject(0).getString("title") != null){
                            defaultElastiCube = elastiCubesArray.getJSONObject(0).getString("title");
                            setDefaultElastiCube(defaultElastiCube);
                        }

                        logger.info("GET api/elasticubes/servers/LocalHost returned " + elastiCubesArray.length() + " ElastiCubes");

                        // Iterate over all ElastiCubes
                        logger.info("Skipping ElastiCubes port retrieval - runMonetDBQuery is false or running remotely");
                        for (int i = 0; i < elastiCubesArray.length(); i++){
                            JSONObject currentElastiCubeObject = (JSONObject) elastiCubesArray.get(i);
                            String elastiCubeName = currentElastiCubeObject.getString("title");
                            int elastiCubeStatus = currentElastiCubeObject.getInt("status");

                            ElastiCube currentElastiCube = new ElastiCube(elastiCubeName, elastiCubeStatus);

                            if (config.isRunMonetDBQuery() && !config.isRunningRemotely()){
                                logger.info("Getting ElastiCube port from PSM...");
                                CmdOperations.getInstance().setElastiCubePort(currentElastiCube);
                                logger.info("Finished getting ElastiCube port from PSM.");
                            }
                            ECSStateKeeper.getInstance().addToListOfElastiCubes(currentElastiCube);

                        }

                        setRequiresServiceRestart(false);

                    } catch (JSONException ex) {
                        logger.error("Error parsing response from GET '"+ getUri() + "'. Response code " +
                                responseCode + " , error: " +
                                ex.getMessage());
                        setRequiresServiceRestart(false);
                    } catch (InterruptedException e) {
                        logger.error("Error getting port for ElastiCube. Exception: \n" + Arrays.toString(e.getStackTrace()));
                        setRequiresServiceRestart(false);
                    }
                } else if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    logger.warn("Check that the token '" + config.getToken() + "' in the configuration file is valid");
                    logger.debug(res);
                    setRequiresServiceRestart(false);
                } else if (responseCode == HttpStatus.SC_NOT_FOUND){
                    logger.error("The endpoint '/api/elasticubes/servers/LocalHost' was not found (404).");
                    logger.debug(res);
                    setRequiresServiceRestart(true);
                } else if (responseCode == HttpStatus.SC_FORBIDDEN) {
                    logger.warn("Ensure that you have sufficient permissions to run calls to GET '/api/elasticubes/servers/LocalHost'");
                    logger.debug(res);
                    setRequiresServiceRestart(false);
                } else if (responseCode == HttpStatus.SC_BAD_REQUEST){
                    logger.warn("Bad GET request sent to '/api/elasticubes/servers/LocalHost'");
                    logger.debug(res);
                    setRequiresServiceRestart(false);
                } else if (responseCode == HttpStatus.SC_BAD_GATEWAY){
                    logger.error("Server returned 'Bad Gateway' (502)");
                    logger.debug(res);
                    setRequiresServiceRestart(true);
                } else if (responseCode == HttpStatus.SC_GATEWAY_TIMEOUT){
                    logger.error("Server returned 'Gateway Timeout' (504)");
                    logger.debug(res);
                    setRequiresServiceRestart(true);
                } else if (responseCode == HttpStatus.SC_INTERNAL_SERVER_ERROR){
                    logger.error("Server returned 'Internal Server Error' (500)");
                    logger.debug(res);
                    setRequiresServiceRestart(true);
                } else {
                    logger.error("Call failed with error code " + responseCode);
                    logger.debug(res);
                    setRequiresServiceRestart(false);
                }

            }
            catch (IOException e){

                logger.error("Error getting list of ElastiCubes from GET '" + getUri() +  "'. Response code " +
                        responseCode + " , error: " +
                        e.getMessage());

                setRequiresServiceRestart(false);
            } finally {
                logger.debug("Releasing REST API client connection...");
                get.releaseConnection();
                logger.debug("REST API client connection released.");
            }
        }

    }

    public void setRequiresServiceRestart(boolean requiresServiceRestart) {
        this.requiresServiceRestart = requiresServiceRestart;
    }

    public boolean isRequiresServiceRestart() {
        return requiresServiceRestart;
    }

    private void setUri() {

        String endpoint = "/api/elasticubes/servers/LocalHost";

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

    public void setDefaultElastiCube(String defaultElastiCube) {
        this.defaultElastiCube = defaultElastiCube;
    }

    public String getDefaultElastiCube() {
        return defaultElastiCube;
    }

    public String getUri() {
        return uri;
    }

    public HttpResponse startElastiCube(String elasticube) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, IOException {

        String endpoint = Configuration.getInstance().getProtocol() + "://"  +
                Configuration.getInstance().getHost() + (Configuration.getInstance().getPort() == 443 ? "" : ":" + Configuration.getInstance().getPort()) + "/api/elasticubes/LocalHost/" + elasticube.replaceAll(" ", "%20") +"/start";

        logger.info("Starting ElastiCube " + elasticube + " from REST API...");
        logger.debug("Sending POST '" + endpoint + "'...");
        HttpPost post = null;
        try {
            post = new HttpPost(endpoint);
            post.addHeader("authorization", "Bearer " + config.getToken());

            return client.execute(post);

        } catch (Exception e){
            logger.error("Error starting ElastiCube from REST API: " + e.getMessage());
            return null;
        }
    }
}
