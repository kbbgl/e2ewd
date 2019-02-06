package tests;

import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import logging.Logger;
import models.ElastiCube;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SisenseRESTAPI {

    private static final Logger logger = Logger.getInstance();
    private static final ConfigFile configFile = ConfigFile.getInstance();
    private static final CmdOperations ops = CmdOperations.getInstance();

    public static boolean queryTableIsSuccessful(String elastiCubeName) {

        JSONObject jaql;

        try{

            jaql = createJAQL(elastiCubeName);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                    .setConnectTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                    .build();

            HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            HttpPost post = new HttpPost(returnUri());
            post.addHeader("authorization", "Bearer " + configFile.getToken());
            post.setEntity(new StringEntity(jaql.toString(), ContentType.APPLICATION_JSON));
            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();
            int responseCode = response.getStatusLine().getStatusCode();

            if (entity != null){

                try(InputStream inputStream = entity.getContent()) {

                    String result = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
//                    logger.write("[queryTableIsSuccessful] Sending POST " + post.getURI().toString() + ", JAQL: " + jaql.toString());
//                    System.out.println("Response code: " + responseCode);
//                    System.out.println("Result " + result);

                    if (responseCode == 200){

                        JSONObject responseObject = new JSONObject(result);
                        JSONObject valuesArray = (JSONObject) responseObject.getJSONArray("values").get(0);
                        int count = valuesArray.getInt("data");
//                        System.out.println("[queryTableIsSuccessful] Count: " + count);

                        if (count > 0){
                            return true;
                        } else {
                            logger.write("[queryTableIsSuccessful] WARNING: JAQLRunner returned unexpected result " + result);
                        }

                    } else {
                        logger.write("[queryTableIsSuccessful] query failed for " + elastiCubeName + ".");
                        logger.write("[queryTableIsSuccessful] call response code " + responseCode);
                        if (!result.isEmpty()){
                            logger.write("[queryTableIsSuccessful] response: " + result);
                        }
                        return false;
                    }
                }
            }
        }catch (Exception e){
            logger.write("[queryTableIsSuccessful] query failed for " + elastiCubeName + ".");
            logger.write(e.getMessage());
            return false;
        }

        return false;
    }

    // SQLRunner URI
//    private static String returnUri(String query, String elasticube){
//
//        if (configFile.getPort() != 443){
//            return configFile.getProtocol() +
//                    "://" + configFile.getHost() + ":" +
//                    configFile.getPort() + "/api/datasources/" +
//                    elasticube.replaceAll(" ", "%20") +
//                    "/sql?query=" + query;
//        }
//        else {
//            return configFile.getProtocol() +
//                    "://" + configFile.getHost() + "/api/datasources/" +
//                    elasticube.replaceAll(" ", "%20") +
//                    "/sql?query=" + query;
//        }
//    }

    // JAQLRunner URI
    private static String returnUri(){

        if (configFile.getPort() != 443){
            return configFile.getProtocol() +
                    "://" + configFile.getHost() + ":" +
                    configFile.getPort() + "/api/datasources/x/jaql";
        }
        else {
            return configFile.getProtocol() +
                    "://" + configFile.getHost() + "/api/datasources/x/jaql";
        }
    }

    // Count for table and column
//    private static JSONObject createJAQL(String elastiCube, String table, String column) throws JSONException {
//
//        JSONObject rootObject = new JSONObject();
//        JSONArray metadataArray = new JSONArray();
//        JSONObject metadataObject = new JSONObject();
//
//        rootObject.put("datasource", elastiCube);
//        metadataObject.put("dim", "[" + table + "." + column + "]");
//        metadataObject.put("agg", "count");
//        metadataArray.put(metadataObject);
//        rootObject.put("metadata", metadataArray);
//
//        return rootObject;
//
//    }

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

}
