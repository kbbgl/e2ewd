import cmd_ops.CmdOperations;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import tests.TelnetTest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class App {

    private static final Date runTime = new Date();
    private static final ConfigFile configFile = ConfigFile.getInstance();
    private static final ResultFile resultFile = ResultFile.getInstance();
    private static final CmdOperations operations = CmdOperations.getInstance();
    private static Logger logger = Logger.getInstance();

    public static void main(String[] args) {

        logger.write("\nRun at: " + runTime.toString() + "\n-----------------------");
        logger.write("Sisense version detected: " + operations.getSisenseVersion());
        preRun();
        run();

    }

    private static void preRun(){

        resultFile.delete();
        if (!configFile.isConfigFileValid()){
            resultFile.delete();
            logger.write("Exiting...");
            System.exit(1);
        }
        else {
            logger.write(configFile.toString());
        }
    }

    private static void run(){

        // Read EC and table
        String ec = operations.getElastiCubeName();

        if (ec.isEmpty() && configFile.isRestartECS()){
            runECSTelnetTests();
            restartECS(operations.getSisenseVersion());
            run();
        }
        else if (ec.isEmpty()){
            runECSTelnetTests();
            logger.write("[main] EC result is empty and restartECS=false. Exiting...");
            resultFile.write(false);
            System.exit(0);
        }
        else {
            String table = operations.getTable();

            boolean isSuccessful = queryTableIsSuccessful(configFile.getProtocol(), configFile.getHost(), configFile.getPort(), configFile.getToken(), ec, table);
            logger.write("[queryTableIsSuccessful] Table query successful: " + isSuccessful);

            resultFile.write(isSuccessful);
        }
    }

    private static void runECSTelnetTests(){
        TelnetTest.isConnected(logger, "localhost", 811);
        TelnetTest.isConnected(logger, "localhost", 812);
    }

    private static void restartECS(String version){

        String serviceName;

        if (version.startsWith("7.2")){
            serviceName = "Sisense.ECMS";
        }
        else {
            serviceName = "ElastiCubeManagmentService";
        }

        logger.write("[restartECS] Service to restart: " + serviceName);
        operations.restartService(serviceName);

    }

    private static boolean queryTableIsSuccessful(String protocol, String domain, int port, String token, String elasticubeName, String tableName) {

        boolean isSuccessful = false;
        String query = ("SELECT COUNT(*) FROM [" + tableName + "]").replaceAll(" ", "%20");
        String uri = protocol + "://" + domain + ":" + port + "/api/datasources/" + elasticubeName.replaceAll(" ", "%20") + "/sql?query=" + query;

        logger.write("[queryTableIsSuccessful] uri: " + uri);

        try{

            HttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(uri);
            get.addHeader("authorization", "Bearer " + token);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();


            if (entity != null){
                try(InputStream inputStream = entity.getContent()) {

                    String result = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
                    logger.write("[queryTableIsSuccessful] Running query: " + query.replaceAll("%20", " "));
                    JSONObject jsonObject = new JSONObject(result);
                    int count = jsonObject.getJSONArray("values").getJSONArray(0).getInt(0);
                    logger.write("[queryTableIsSuccessful] Result: " + count);

                    if (count > 0){
                        isSuccessful = true;
                    }
                }
            }
            return isSuccessful;
        }catch (Exception e){
            logger.write("[queryTableIsSuccessful] query table failed:\n");
            logger.write(e.getMessage());
            logger.write(Arrays.toString(e.getStackTrace()));
            return false;
        }

    }
}