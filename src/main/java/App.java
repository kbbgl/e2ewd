import cmd_ops.CmdOperations;
import env_var.EnvironmentalVariables;
import file_ops.ConfigFile;
import file_ops.ResultFile;
import logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import version.VersionRetriever;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class App {

    private static final Date runTime = new Date();
    private static final Runtime rt = Runtime.getRuntime();
    private static ConfigFile configFile;
    private static Logger logger;

    public static void main(String[] args) {

        writeToLogger("\nRun at: " + runTime.toString() + "\n-----------------------");
        writeToLogger("Sisense version detected: " + getSisenseVersion() + "\n");
        preRun();
        run();

    }

    private static void preRun(){

        deleteResultFile();
        createConfigFile();

    }

    private static void run(){

        // read configuration file props
        String token = configFile.getToken();
        String host = configFile.getHost();
        String protocol = configFile.getProtocol();
        int port = configFile.getPort();
        boolean restartECS = configFile.isRestartECS();

        // Read EC and table
        String ec = getElasticubeName();

        if (ec.isEmpty() && restartECS){
            restartECS(getSisenseVersion());
            run();
        }
        else if (ec.isEmpty()){
            writeToLogger("[main] EC result is empty and restartECS=false. Exiting...");
            System.exit(0);
        }
        else {
            String table = getTable(ec);

            boolean isSuccessful = queryTableIsSuccessful(protocol, host, port, token, ec, table);
            writeToLogger("[queryTableIsSuccessful] Table query successful: " + isSuccessful);

            createResultFile(isSuccessful);
        }
    }

    private static String executionPath(){
        String jarLocation = null;
        try {
            jarLocation = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
            Path path = Paths.get(jarLocation);
            return String.valueOf(path.getParent());
        } catch (IOException | URISyntaxException e) {
            writeToLogger("Couldn't retrieve jar execution path:");
            writeToLogger(e.getMessage());
            writeToLogger(Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    private static String getSisenseVersion(){

        String version = null;
        try {
            version = VersionRetriever.getVersion();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return version;
    }

    private static void writeToLogger(String s){
        logger = new Logger(executionPath());
        logger.write(s);
        System.out.println(s);
    }

    private static void createResultFile(boolean result){
        try {
            ResultFile resultFile = new ResultFile(executionPath());
            resultFile.create();
            writeToLogger("[createResultFile] Created file in " + resultFile.path);
            resultFile.write(result);
            writeToLogger("[createResultFile] Test succeeded: " + result);
        } catch (IOException e) {
            writeToLogger("Couldn't create log file: \n");
            writeToLogger(Arrays.toString(e.getStackTrace()));
        }
    }

    private static void deleteResultFile(){
        try {
            writeToLogger("[deleteResultFile] Deleting result file...");
            File file = new File(executionPath() + "/run/result.txt");
            file.delete();
        } catch (Exception e) {
            writeToLogger("failed to delete result.txt file:\n");
            writeToLogger(e.getMessage());
            writeToLogger(Arrays.toString(e.getStackTrace()));
            createResultFile(false);
            e.printStackTrace();
        }

    }

    private static void createConfigFile() {

        configFile = new ConfigFile(executionPath());
        writeToLogger("[createConfigFile] Reading config file...");
        configFile.read();
        writeToLogger("[createConfigFile] Config file read:");
        writeToLogger(configFile.toString());
    }

    private static String getElasticubeName() {

        String ec = "";

        try {

            Runtime rt = Runtime.getRuntime();
            String[] psmCmd = new String[]{
                    "cmd.exe",
                    "/c",
                    "SET SISENSE_PSM=true&&\"C:\\Program Files\\Sisense\\Prism\\Psm.exe\"",
                    "ecs",
                    "ListCubes",
                    "serverAddress=localhost"};

            Process listCubesCommand = rt.exec(psmCmd);
            writeToLogger("[getElasticubeName] running commands: " + Arrays.toString(psmCmd));
            listCubesCommand.waitFor();

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(listCubesCommand.getInputStream()));

            Pattern cubeNamePattern = Pattern.compile("\\[(.*?)]");
            Pattern errorPattern = Pattern.compile("\\((.*?)\\)");

            String s;

            while ((s = stdInput.readLine()) != null) {
                if (s.startsWith("Cube Name")){
                    Matcher m = cubeNamePattern.matcher(s);
                    while (m.find()){
                        ec = m.group(1);
                        writeToLogger("[getElasticubeName] ElastiCube found: " + ec);
                        return ec;
                    }
                }
                else {
                    Matcher m = errorPattern.matcher(s);
                    while (m.find()){
                        writeToLogger("[getElasticubeName] ERROR: " + m.group(1));

                        if (m.group(1).equals("the server, 'localhost', is not responding.")){
                            return ec;
                        }

                    }
                }

            }
        } catch (IOException | InterruptedException e) {
            writeToLogger("[getElasticubeName] ERROR: " + e.getMessage());
        }

        return ec;
    }

    private static String getTable(String ec){

        String[] psmCmd  = {
                "C:\\Program Files\\Sisense\\Prism\\Psm.exe",
                "ecube",
                "edit",
                "tables",
                "getListOfTables",
                "serverAddress=localhost",
                "cubeName=" + ec,
                "isCustom=false"
        };

        try {
            Process readTableProcess = rt.exec(psmCmd);
            writeToLogger("[getTable] Command sent: " + Arrays.toString(psmCmd));

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(readTableProcess.getInputStream()));

            String s;
            String table = null;

            while ((s = stdInput.readLine()) != null) {
                if (s.contains("Could not find a cube by the name")){
                    createResultFile(false);
                }

                if (!s.contains("-") && !s.isEmpty()){
                    table = s;
                    writeToLogger("[getTable] Table found: " + table);
                    break;
                }
            }

            return table;

        } catch (Exception e) {
            createResultFile(false);
            writeToLogger("[getTable] getTable failed: ");
            writeToLogger(e.getMessage());
            writeToLogger(Arrays.toString(e.getStackTrace()));
        }

        return null;
    }

    private static void restartECS(String version){

        String serviceName;

        if (version.startsWith("7.2")){
            serviceName = "Sisense.ECMS";
        }
        else {
            serviceName = "ElastiCubeManagmentService";
        }

        writeToLogger("[restartECS] Service to restart: " + serviceName);
        CmdOperations.restartECS(rt, serviceName, logger);

    }

    private static boolean queryTableIsSuccessful(String protocol, String domain, int port, String token, String elasticubeName, String tableName) {

        boolean isSuccessful = false;
        String query = ("SELECT COUNT(*) FROM [" + tableName + "]").replaceAll(" ", "%20");
        String uri = protocol + "://" + domain + ":" + port + "/api/datasources/" + elasticubeName.replaceAll(" ", "%20") + "/sql?query=" + query;

        writeToLogger("[queryTableIsSuccessful] uri: " + uri);

        try{

            HttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(uri);
            get.addHeader("authorization", "Bearer " + token);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();


            if (entity != null){
                try(InputStream inputStream = entity.getContent()) {

                    String result = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
                    writeToLogger("[queryTableIsSuccessful] Running query: " + query.replaceAll("%20", " "));
                    JSONObject jsonObject = new JSONObject(result);
                    int count = jsonObject.getJSONArray("values").getJSONArray(0).getInt(0);
                    writeToLogger("[queryTableIsSuccessful] Result: " + count);

                    if (count > 0){
                        isSuccessful = true;
                    }
                }
            }
            return isSuccessful;
        }catch (Exception e){
            writeToLogger("[queryTableIsSuccessful] query table failed:\n");
            writeToLogger(e.getMessage());
            writeToLogger(Arrays.toString(e.getStackTrace()));
            return false;
        }

    }
}