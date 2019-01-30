package tests;

import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import sun.rmi.runtime.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SisenseRESTAPI {

    private static final Logger logger = Logger.getInstance();
    private static final ConfigFile configFile = ConfigFile.getInstance();
    private static final CmdOperations ops = CmdOperations.getInstance();

    public static boolean queryTableIsSuccessful() {

        String query = ("SELECT COUNT(*) FROM [" + ops.getTable() + "]").replaceAll(" ", "%20");


        logger.write("[queryTableIsSuccessful] uri: " + returnUri(query));

        try{

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                    .setConnectTimeout(configFile.getRequestTimeoutInSeconds() * 1000)
                    .build();
            HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            HttpGet get = new HttpGet(returnUri(query));
            get.addHeader("authorization", "Bearer " + configFile.getToken());
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            int responseCode = response.getStatusLine().getStatusCode();

            if (entity != null){

                try(InputStream inputStream = entity.getContent()) {

                    String result = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
                    logger.write("[queryTableIsSuccessful] Running query: " + query.replaceAll("%20", " "));

                    if (responseCode == 200){

                        JSONObject jsonObject = new JSONObject(result);
                        int count = jsonObject.getJSONArray("values").getJSONArray(0).getInt(0);
                        logger.write("[queryTableIsSuccessful] Result: " + count);

                        if (count > 0){
                            return true;
                        }
                    } else {
                        logger.write("[queryTableIsSuccessful] query table failed.");
                        logger.write("[queryTableIsSuccessful] called returned " + responseCode);
                        logger.write("[queryTableIsSuccessful] response: " + result);
                        return false;
                    }
                }
            }
        }catch (Exception e){
            logger.write("[queryTableIsSuccessful] query table failed:");
            logger.write(e.getMessage());
            logger.write(Arrays.toString(e.getStackTrace()));
            return false;
        }

        return false;
    }

    private static String returnUri(String query){

        if (configFile.getPort() != 443){
            return configFile.getProtocol() +
                    "://" + configFile.getHost() + ":" +
                    configFile.getPort() + "/api/datasources/" +
                    ops.getElastiCubeName().replaceAll(" ", "%20") +
                    "/sql?query=" + query;
        }
        else {
            return configFile.getProtocol() +
                    "://" + configFile.getHost() + "/api/datasources/" +
                    ops.getElastiCubeName().replaceAll(" ", "%20") +
                    "/sql?query=" + query;
        }
    }

}
