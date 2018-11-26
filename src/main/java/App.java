import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

public class App {

    private static final Date runTime = new Date();
    private static final Runtime rt = Runtime.getRuntime();
    private static ConfigFile configFile;
    private static final String[] mongoCmds =  {
            "C:\\Program Files\\Sisense\\Infra\\MongoDB\\sisenseRepositoryShell.exe",
            "prismWebDB",
            "--port",
            "27018",
            "--host",
            "localhost",
            "--authenticationDatabase",
            "admin",
            "--username",
            "RootUser",
            "--password",
            "RepoAdmin!",
            "--eval",
            "\"db.elasticubes.findOne().title\"",
            "--quiet"
    };


    public static void main(String[] args) throws IOException, URISyntaxException, JSONException {

        writeToLogger("\nRun at: " + runTime.toString() + "\n-----------------------");
        writeToLogger("Deleting result file...");
        deleteResultFile();
        writeToLogger("Executing jar from " + executionPath());
        createConfigFile();

        // Read EC and table
        String ec = getElasticubeName();
        String table = getTable(ec);

        String token = configFile.getToken();
        String host = configFile.getHost();
        String protocol = configFile.getProtocol();
        int port = configFile.getPort();

        boolean isSuccessful = queryTableIsSuccessful(protocol, host, port, token, ec, table);
        writeToLogger("Table query successful: " + isSuccessful);

        createResultFile(isSuccessful);

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

    private static void writeToLogger(String s){
        Logger logger = new Logger(executionPath());
        logger.write(s);
    }

    private static void createResultFile(boolean result){
        try {
            ResultFile resultFile = new ResultFile(executionPath());
            resultFile.create();
            writeToLogger("Created file in " + resultFile.path);
            resultFile.write(result);
            writeToLogger("Test succeeded: " + result);
        } catch (IOException e) {
            writeToLogger("Couldn't create log file: \n");
            writeToLogger(Arrays.toString(e.getStackTrace()));
        }
    }

    private static void deleteResultFile(){
        try {
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
        writeToLogger("Reading config file...\n");
        configFile.read();
        writeToLogger("Config file read: \n");
        writeToLogger(configFile.toString());
    }

    private static String getElasticubeName() {
        try {
            Process readElastiCubeProcess = rt.exec(mongoCmds);
            writeToLogger("Connecting to mongo");

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(readElastiCubeProcess.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(readElastiCubeProcess.getErrorStream()));

            String s;
            String ec = null;

            //Success
            writeToLogger("Running command to retrieve test ElastiCube");
            while ((s = stdInput.readLine()) != null) {
                ec = s;
            }

            // Error
            while ((s = stdError.readLine()) != null) {

                if (!s.isEmpty() || !s.equals("")) {
                    writeToLogger("error retreiving ec. read input: " + s);
                    break;
                }
            }

            writeToLogger("EC found: " + ec);
            return ec;

        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private static String getTable(String ec){

        String[] psmCmd  = {
                "C:\\Program Files\\Sisense\\Prism\\Psm.exe",
                "ecube",
                "edit",
                "tables",
                "getListOfTables",
                "serverAddress=localhost",
                "cubeName="+ec,
                "isCustom=false"
        };

        try {
            Process readTableProcess = rt.exec(psmCmd);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(readTableProcess.getInputStream()));

            String s;
            String table = null;

            while ((s = stdInput.readLine()) != null) {
                if (!s.contains("-") && !s.isEmpty()){
                    table = s;
                    break;
                }
            }

            writeToLogger("Table found: " + table);
            return table;

        } catch (Exception e) {
            createResultFile(false);
            writeToLogger("getTable failed: ");
            writeToLogger(e.getMessage());
            writeToLogger(Arrays.toString(e.getStackTrace()));
        }

        return null;
    }

    private static boolean queryTableIsSuccessful(String protocol, String domain, int port, String token, String elasticubeName, String tableName) throws IOException, JSONException {

        boolean isSuccessful = false;
        String query = ("SELECT COUNT(*) FROM [" + tableName + "]").replaceAll(" ", "%20");
        String uri = protocol + "://" + domain + ":" + port + "/api/datasources/" + elasticubeName.replaceAll(" ", "%20") + "/sql?query=" + query;

        writeToLogger("query: " + query);
        writeToLogger("uri: " + uri);

        try{

            HttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(uri);
            get.addHeader("authorization", "Bearer " + token);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();


            if (entity != null){
                try(InputStream inputStream = entity.getContent()) {

                    String result = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
                    writeToLogger("Running query: " + query.replaceAll("%20", " "));
                    writeToLogger("GET result: " + result);
                    JSONObject jsonObject = new JSONObject(result);
                    int count = jsonObject.getJSONArray("values").getJSONArray(0).getInt(0);
                    writeToLogger("Result: " + count);

                    if (count > 0){
                        isSuccessful = true;
                    }
                }
            }
            return isSuccessful;
        }catch (Exception e){
            writeToLogger("query table failed:\n");
            writeToLogger(e.getMessage());
            writeToLogger(Arrays.toString(e.getStackTrace()));
            return false;
        }

    }
}