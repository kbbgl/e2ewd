package cmd_ops;

import integrations.SlackClient;
import logging.Logger;
import models.ElastiCube;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdOperations {

    private static CmdOperations instance;
    private final Runtime runtime = Runtime.getRuntime();
    private final Logger logger = Logger.getInstance();
    private final String procdumpPath = executionPath() + "\\procdump\\procdump.exe";
    private final int PROCESS_TIMEOUT = 15;

    private CmdOperations(){

    }

    public static CmdOperations getInstance(){

        if (instance == null){
            instance = new CmdOperations();
        }

        return instance;
    }

    public List<ElastiCube> getListElastiCubes(){
        List<ElastiCube> elasticubes = new ArrayList<>();

        try {
            String[] psmCmd = new String[]{
                    "cmd.exe",
                    "/c",
                    "C:\\Program Files\\Sisense\\Prism\\Psm.exe",
                    "ecs",
                    "ListCubes",
                    "serverAddress=localhost"};

            String[] environmentalVariable = { "SISENSE_PSM=true" };

            Process listCubesCommand = runtime.exec(psmCmd, environmentalVariable);
            listCubesCommand.waitFor(15, TimeUnit.SECONDS);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(listCubesCommand.getInputStream()));
            BufferedReader errorInput = new BufferedReader(new InputStreamReader(listCubesCommand.getErrorStream()));

            Pattern listCubesPattern = Pattern.compile("Cube Name \\[(.*?)] ID : \\[(.*?)] FarmPath \\[(.*?)] Status \\[(.*?)]");
            Pattern errorPattern = Pattern.compile("\\((.*?)\\)");

            // Read stdout
            String s;
            while ((s = stdInput.readLine()) != null) {
                if (s.startsWith("Cube Name")){
                    Matcher cubeNameMatcher = listCubesPattern.matcher(s);
                    while (cubeNameMatcher.find()){

                        ElastiCube elastiCube = new ElastiCube(cubeNameMatcher.group(1), cubeNameMatcher.group(4));
                        setElastiCubeProperties(elastiCube);
//                        logger.write("[getListElastiCubes] found " + elastiCube);

                        // filter out all non running ElastiCubes
                        if (elastiCube.getState().equals("RUNNING") && !elastiCube.isLocked()){
                            if (elasticubes != null) {
                                elasticubes.add(elastiCube);
                            }
                        }

                    }
                } else {
                    Matcher m = errorPattern.matcher(s);
                    while (m.find()){
                        logger.write("[getListElastiCubes] ERROR: " + m.group(1));

                        if (m.group(1).equals("the server, 'localhost', is not responding.")){
                            elasticubes = null;
                        }

                    }
                }
            }

            // Read stderr
            String e;
            while ((e = errorInput.readLine()) != null){
                    logger.write("[getListElastiCubes] ERROR: " + e);
                }

            // Check for timeout
            if (!listCubesCommand.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)){

                logger.write("[CmdOperations.getListElastiCubes] Operation timed out (" + PROCESS_TIMEOUT + "s). Destroying process...");
                listCubesCommand.destroy();
            }

        } catch (IOException | InterruptedException e) {
            logger.write("[CmdOperations.getListElastiCubes] ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return elasticubes;
    }

    private void setElastiCubeProperties(ElastiCube elastiCube) throws IOException, InterruptedException {

        String[] psmCmd = new String[]{
                "C:\\Program Files\\Sisense\\Prism\\Psm.exe",
                "ecube",
                "info",
                "name=" + elastiCube.getName(),
                "serverAddress=localhost"};

        Process ecubePortCommand = Runtime.getRuntime().exec(psmCmd);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(ecubePortCommand.getInputStream()));
        BufferedReader errorStream = new BufferedReader(new InputStreamReader(ecubePortCommand.getErrorStream()));

        // read stdin
        String s;
        while ((s = stdInput.readLine()) != null){
            if (s.startsWith("Port")){
                int port = Integer.parseInt(s.split("Port: ")[1]);
                elastiCube.setPort(port);
            } else if (s.startsWith("IsLocked")){
                boolean locked = Boolean.valueOf(s.split("IsLocked: ")[1]);
                elastiCube.setLocked(locked);
            }
        }

        // Read stderr
        String e;
        while ((e = errorStream.readLine()) != null){
            logger.write("[setElastiCubeProperties] ERROR " + e);
        }

        // Check that process hasn't timed out
        if (!ecubePortCommand.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)){
            logger.write("[CmdOperations.setElastiCubeProperties] Operation timed out (" + PROCESS_TIMEOUT + "s). Destroying process...");
            ecubePortCommand.destroy();
        }
    }

    public boolean isMonetDBQuerySuccessful(ElastiCube elastiCube) throws IOException, InterruptedException {

        boolean success = false;

        String[] psmCmd = new String[]{
                "cmd.exe",
                "/c",
                "mclient.exe",
                "-p" + elastiCube.getPort(),
                "-fcsv",
                "-s",
                "\"SELECT 1\""};

        Process monetDBQueryCmd = runtime.exec(psmCmd, null, new File(executionPath() + "\\mclient\\"));

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(monetDBQueryCmd.getInputStream()));
        BufferedReader errorStream = new BufferedReader(new InputStreamReader(monetDBQueryCmd.getErrorStream()));

        // Read stdin
        String s;
        while ((s = stdInput.readLine()) != null) {
            try {
                if (Integer.parseInt(s) == 1){
                    success = true;

                }

            } catch (NumberFormatException e){

                logger.write("[isMonetDBQuerySuccessful] ERROR - " + e.getMessage());
                return success;

            }

        }


        // Read stderr
        String e;
        while ((e = errorStream.readLine()) != null){
            logger.write("[isMonetDBQuerySuccessful] ERROR " + e);
        }

        // Check for process timeout
        if(!monetDBQueryCmd.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)){
            logger.write("[CmdOperations.isMonetDBQuerySuccessful] Operation timed out (" + PROCESS_TIMEOUT + "s). Destroying process...");
            monetDBQueryCmd.destroy();
            return false;
        }

        return success;
    }

    public String getSisenseVersion(){

        try {
            Process process = runtime.
                    exec("reg QUERY HKEY_LOCAL_MACHINE\\SOFTWARE\\Sisense\\ECS /v Version");

            StringWriter stringWriter = new StringWriter();

            try (InputStream inputStream = process.getInputStream()){

                int c;
                while ((c = inputStream .read()) != -1){
                    stringWriter.write(c);
                }
            }

            try (InputStream errorStream = process.getErrorStream()){

                int e;
                while ((e = errorStream.read()) != -1){
                    logger.write("[CmdOperations.getSisenseVersion] ERROR " + e);
                }
            }

            // Check if operation timed out
            if (!process.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)){
                logger.write("[CmdOperations.getSisenseVersion] Operation timed out (" + PROCESS_TIMEOUT + "s.)");
                process.destroy();
                return "CANNOT DETECT";
            }
            return stringWriter.toString().split("   ")[3].trim();

        } catch (IOException | InterruptedException e) {
            logger.write("ERROR: retrieving Sisense version - " + e.getMessage());
            return "CANNOT DETECT";
        }
    }

    // TODO add indication to mongo and Slack about dump occurrance
    public void w3wpDump(){

        String command = procdumpPath + " -accepteula -o -ma w3wp iis_dump.dmp";
        logger.write("[CmdOperations.w3wpDump] - Running...");

        try {

            Process process = runtime.exec(command);

            // check that process hasn't timed out
            if (!process.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)){
                logger.write("[CmdOperations.w3wpDump] Operation timed out (" + PROCESS_TIMEOUT + " s.) Destroying process...");
                process.destroy();
            } else {
                logger.write("[CmdOperations.w3wpDump] Operation successful");
            }

        } catch (IOException | InterruptedException e) {
            logger.write("ERROR: running command" + command + " - " + e.getMessage());
        }

    }

    // TODO add indication to mongo and Slack about dump occurrance
    public void ecsDump(){

        String command = procdumpPath + " -accepteula -o -ma ElastiCube.ManagementService ecs_dump.dmp";
        logger.write("[CmdOperations.ecsDump] - Running...");

        try {

            Process process = runtime.exec(command);

            // check that process hasn't timed out
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                logger.write("[CmdOperations.ecsDump] Operation timed out (" + PROCESS_TIMEOUT + "s). Destroying process...");
                process.destroy();
            }
            else {
                logger.write("[CmdOperations.ecsDump] Operation successful");
            }

        } catch (IOException | InterruptedException e) {
            logger.write("ERROR: running command" + command + " - " + e.getMessage());
        }

    }

    public void restartECS(){

        String serviceName;
        if (getSisenseVersion().startsWith("6") || getSisenseVersion().startsWith("7.1") || getSisenseVersion().startsWith("7.0")){
            serviceName = "ElastiCubeManagmentService";
        }
        else {
            serviceName = "Sisense.ECMS";
        }

        String methodName = "[restartService] ";
        String restartCommand = "powershell.exe Restart-Service -DisplayName " + serviceName + " -Force";
        logger.write( methodName + "running command " + restartCommand);
        try {
            Process psProcess = runtime.exec(restartCommand);

            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                while ((line = reader.readLine()) != null){
                    logger.write("[CmdOperations.restartECS] output: " + line);
                }
                SlackClient.getInstance().sendMessage(":recycle: ECS restarted.");
            }

            String error;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(psProcess.getErrorStream()))) {
                while ((error = errorReader.readLine()) != null){
                    logger.write("[CmdOperations.restartECS] ERROR: " + error);
                }
            }

            // check that process hasn't timed out
            if (!psProcess.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)){
                logger.write("[CmdOperations.restartECS] Operation timed out (" + PROCESS_TIMEOUT + "s.) Destroying process...");
                psProcess.destroy();
            }

        } catch (IOException | InterruptedException e) {
            logger.write( methodName + "ERROR: " + e.getMessage());
        }

    }

    public void restartIIS() {

        String methodName = "[CmdOperations.restartIIS] ";
        String restartCommand = "iisreset";
        logger.write( methodName + "running command " + restartCommand);
        try {
            Process psProcess = runtime.exec(restartCommand);

            String line;
            logger.write(methodName + "restart command output:");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                while ((line = reader.readLine()) != null){
                    logger.write("[CmdOperations.restartIIS] output: " + line);
                }
                SlackClient.getInstance().sendMessage(":recycle: IIS restarted ");
            }

            String error;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(psProcess.getErrorStream()))) {
                while ((error = errorReader.readLine()) != null){
                    logger.write("[CmdOperations.restartIIS] ERROR: " + error);
                }
            }

            // check that operation hasn't timed out
            if (!psProcess.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)){
                logger.write("[CmdOperations.restartIIS] Operation timed out (" +PROCESS_TIMEOUT + "s.) Destroying process...");
                psProcess.destroy();
            }

        } catch (IOException | InterruptedException e) {
            logger.write( "[CmdOperations.restartIIS] ERROR: " + e.getMessage());
        }
    }

    public String getHostname(){

        String hostname = "";
        String cmd = "hostname";
        try {
            Process getHostnameProcess = runtime.exec(cmd);

            BufferedReader reader = new BufferedReader(new InputStreamReader(getHostnameProcess.getInputStream()));

            String s;
            while ((s = reader.readLine()) != null){
                hostname = s;
            }

            if (!getHostnameProcess.waitFor(PROCESS_TIMEOUT, TimeUnit.SECONDS)){
                logger.write("[CmdOperations.getHostname] Operation timed out " + PROCESS_TIMEOUT + "s.) Destroying process.");
                getHostnameProcess.destroy();
                return "";
            }

            return hostname;

        } catch (IOException | InterruptedException e) {
            logger.write("[getHostname] ERROR: Can't get hostname - " + e.getMessage());
            return "";
        }

    }

    private String executionPath(){
        String jarLocation = null;
        try {
            jarLocation = new File(Logger.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
            Path path = Paths.get(jarLocation);
            return String.valueOf(path.getParent());
        } catch (IOException | URISyntaxException e) {
            System.out.println(e.getMessage());
        }
        return jarLocation;
    }
}