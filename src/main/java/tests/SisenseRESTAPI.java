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
import org.apache.http.impl.client.HttpClientBuilder;
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

        String query = ("SELECT 1 FROM [" + ops.getTable(elastiCubeName) + "]").replaceAll(" ", "%20");

        logger.write("[queryTableIsSuccessful] uri: " + returnUri(query, elastiCubeName));
//        System.out.println("[queryTableIsSuccessful] uri: " + returnUri(query, elastiCubeName));

        try{

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                    .setConnectTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                    .build();
            HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            HttpGet get = new HttpGet(returnUri(query, elastiCubeName));
            get.addHeader("authorization", "Bearer " + configFile.getToken());
//            get.addHeader("authorization", "Bearer .eyJ1c2VyIjoiNWJkNzQwZjJhMjc4MTQyNzUwZjM5OTI5IiwiYXBpU2VjcmV0IjoiYmMyNDc1NjYtZGQwZi0xY2I4LWJlMWYtZDU0YmEzOGM5ZWNhIiwiaWF0IjoxNTQ4Nzc1NTkyfQ.0zIoJVm1U5RBSgmb3y1Z8d0fFLS9R4Ze12qZDx7P3S8");
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            int responseCode = response.getStatusLine().getStatusCode();

            if (entity != null){

                try(InputStream inputStream = entity.getContent()) {

                    String result = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
                    logger.write("[queryTableIsSuccessful] Running query: " + query.replaceAll("%20", " "));
//                    System.out.println("[queryTableIsSuccessful] Running query: " + query.replaceAll("%20", " "));


                    if (responseCode == 200){

                        JSONObject jsonObject = new JSONObject(result);
                        int count = jsonObject.getJSONArray("values").getJSONArray(0).getInt(0);
                        logger.write("[queryTableIsSuccessful] Result: " + count);
//                        System.out.println("[queryTableIsSuccessful] Result: " + count);
                        if (count > 0){
                            return true;
                        }
                    } else {
//                        System.out.println("[queryTableIsSuccessful] query table failed.");
//                        System.out.println("[queryTableIsSuccessful] call returned " + responseCode);
//                        System.out.println("[queryTableIsSuccessful] response: " + result);
                        logger.write("[queryTableIsSuccessful] query table failed.");
                        logger.write("[queryTableIsSuccessful] called returned " + responseCode);
                        logger.write("[queryTableIsSuccessful] response: " + result);
                        return false;
                    }
                }
            }
        }catch (Exception e){
//            System.out.println("[queryTableIsSuccessful] query table failed:");
//            System.out.println(e.getMessage());
//            System.out.println(Arrays.toString(e.getStackTrace()));
            logger.write("[queryTableIsSuccessful] query table failed:");
            logger.write(e.getMessage());
            logger.write(Arrays.toString(e.getStackTrace()));
            return false;
        }

        return false;
    }

    private static String returnUri(String query, String elasticube){

        if (configFile.getPort() != 443){
            return configFile.getProtocol() +
                    "://" + configFile.getHost() + ":" +
                    configFile.getPort() + "/api/datasources/" +
                    elasticube.replaceAll(" ", "%20") +
                    "/sql?query=" + query;
        }
        else {
            return configFile.getProtocol() +
                    "://" + configFile.getHost() + "/api/datasources/" +
                    elasticube.replaceAll(" ", "%20") +
                    "/sql?query=" + query;
        }
    }

//    private static String returnUri(String query, String elasticube){
//
//            return "http://localhost:8081/api/datasources/" +
//                    elasticube.replaceAll(" ", "%20") +
//                    "/sql?query=" + query;
//    }

}
