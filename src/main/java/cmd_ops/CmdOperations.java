package cmd_ops;

import file_ops.ResultFile;
import logging.Logger;
import models.ElastiCube;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdOperations {

    private static CmdOperations instance;
    private final Runtime runtime = Runtime.getRuntime();
    private final Logger logger = Logger.getInstance();
    private final ResultFile resultFile = ResultFile.getInstance();
    private final String path = executionPath() + "\\procdump\\procdump.exe";

    private CmdOperations(){

    }

    public static CmdOperations getInstance(){

        if (instance == null){
            instance = new CmdOperations();
        }

        return instance;
    }

    public String getTable(String elastiCube){
        String[] psmCmd  = {
                "C:\\Program Files\\Sisense\\Prism\\Psm.exe",
                "ecube",
                "edit",
                "tables",
                "getListOfTables",
                "serverAddress=localhost",
                "cubeName=" + elastiCube,
                "isCustom=false"
        };

        try {
            Process readTableProcess = runtime.exec(psmCmd);
            logger.write("[getTable] Command sent: " + Arrays.toString(psmCmd));

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(readTableProcess.getInputStream()));

            String s;
            String table = null;

            while ((s = stdInput.readLine()) != null) {
                if (s.contains("Could not find a cube by the name")){
                    resultFile.write(false);
                }

                if (!s.contains("-") && !s.isEmpty()){
                    table = s;
                    logger.write("[getTable] Table found: " + table);
                    break;
                }
            }

            return table;

        } catch (Exception e) {
            resultFile.write(false);
            logger.write("[getTable] getTable failed: ");
            logger.write(e.getMessage());
            logger.write(Arrays.toString(e.getStackTrace()));
        }

        return null;
    }

//    public String getElastiCubeName(){
//        String ec = "";
//
//        try {
//            String[] psmCmd = new String[]{
//                    "cmd.exe",
//                    "/c",
//                    "SET SISENSE_PSM=true&&\"C:\\Program Files\\Sisense\\Prism\\Psm.exe\"",
//                    "ecs",
//                    "ListCubes",
//                    "serverAddress=localhost"};
//
//            Process listCubesCommand = runtime.exec(psmCmd);
//
//            BufferedReader stdInput = new BufferedReader(new InputStreamReader(listCubesCommand.getInputStream()));
//
//            Pattern cubeNamePattern = Pattern.compile("\\[(.*?)]");
//            Pattern errorPattern = Pattern.compile("\\((.*?)\\)");
//
//            String s;
//
//            while ((s = stdInput.readLine()) != null) {
//                if (s.startsWith("Cube Name")){
//                    Matcher m = cubeNamePattern.matcher(s);
//                    while (m.find()){
//                        ec = m.group(1);
//                        return ec;
//                    }
//                }
//                else {
//                    Matcher m = errorPattern.matcher(s);
//                    while (m.find()){
//                        logger.write("[getElasticubeName] ERROR: " + m.group(1));
//
//                        if (m.group(1).equals("the server, 'localhost', is not responding.")){
//                            return ec;
//                        }
//
//                    }
//                }
//
//            }
//        } catch (IOException e) {
//            logger.write("[getElasticubeName] ERROR: " + e.getMessage());
//        }
//
//        return ec;
//    }

    private void setElastiCubePort(ElastiCube elastiCube) throws IOException {

        String[] psmCmd = new String[]{
                "C:\\Program Files\\Sisense\\Prism\\Psm.exe",
                "ecube",
                "info",
                "name=" + elastiCube.getName(),
                "serverAddress=localhost"};

        Process ecubePortCommand = Runtime.getRuntime().exec(psmCmd);
        logger.write("[setElastiCubePort] Command sent: " + Arrays.toString(psmCmd));

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(ecubePortCommand.getInputStream()));
        BufferedReader errorStream = new BufferedReader(new InputStreamReader(ecubePortCommand.getErrorStream()));

        String s;
        while ((s = stdInput.readLine()) != null){
            if (s.startsWith("Port")){
                int port = Integer.parseInt(s.split("Port: ")[1]);
                elastiCube.setPort(port);
            }
        }

        String e;
        while ((e = errorStream.readLine()) != null){
            logger.write("[setElastiCubePort] ERROR " + e);
        }

    }

    public List<ElastiCube> getListElastiCubes(){
        List<ElastiCube> elasticubes = new ArrayList<>();

        try {
            String[] psmCmd = new String[]{
                    "cmd.exe",
                    "/c",
                    "SET SISENSE_PSM=true&&\"C:\\Program Files\\Sisense\\Prism\\Psm.exe\"",
                    "ecs",
                    "ListCubes",
                    "serverAddress=localhost"};

            Process listCubesCommand = runtime.exec(psmCmd);
            logger.write("[getListElastiCubes] Command sent: " + Arrays.toString(psmCmd));

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(listCubesCommand.getInputStream()));

            Pattern listCubesPattern = Pattern.compile("Cube Name \\[(.*?)] ID : \\[(.*?)] FarmPath \\[(.*?)] Status \\[(.*?)]");
            Pattern errorPattern = Pattern.compile("\\((.*?)\\)");

            String s;

            while ((s = stdInput.readLine()) != null) {

                if (s.startsWith("Cube Name")){
                    Matcher cubeNameMatcher = listCubesPattern.matcher(s);
                    while (cubeNameMatcher.find()){
                        // TODO parse state
                        ElastiCube elastiCube = new ElastiCube(cubeNameMatcher.group(1), cubeNameMatcher.group(4));
                        setElastiCubePort(elastiCube);

                        logger.write("[getListElastiCubes] found " + elastiCube);

                        // filter out all non running ElastiCubes
                        if (elastiCube.getState().equals("RUNNING")){
                            elasticubes.add(elastiCube);
                        }

                    }
                } else {
                    Matcher m = errorPattern.matcher(s);
                    while (m.find()){
                        logger.write("[getListElastiCubes] ERROR: " + m.group(1));

                        if (m.group(1).equals("the server, 'localhost', is not responding.")){
                            return null;
                        }

                    }
                }
            }
        } catch (IOException e) {
            logger.write("[getListElastiCubes] ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return elasticubes;
    }

    public String getSisenseVersion(){

        try {
            Process process = runtime.
                    exec("reg QUERY HKEY_LOCAL_MACHINE\\SOFTWARE\\Sisense\\ECS /v Version");

            StringWriter stringWriter = new StringWriter();

            InputStream stream = process.getInputStream();

            int c;
            while ((c = stream.read()) != -1){
                stringWriter.write(c);
            }
            stream.close();

            return stringWriter.toString().split("   ")[3].trim();

        } catch (IOException e) {
            logger.write("ERROR: retrieving Sisense version - " + e.getMessage());
            return "CANNOT DETECT";
        }

    }

    public void w3wpDump(){

        String command = path + " -accepteula -o -ma w3wp iis_dump.dmp";
        logger.write("[w3wpDump] - running...");

        try {

            Process process = runtime.exec(command);
            process.waitFor();


        } catch (IOException | InterruptedException e) {
            logger.write("ERROR: running command" + command + " - " + e.getMessage());
        }

        logger.write("[w3wpDump] - end run");

    }

    public void ecsDump(){

        String command = path + " -accepteula -o -ma ElastiCube.ManagementService ecs_dump.dmp";
        logger.write("[ecsDump] - running...");

        try {

            Process process = runtime.exec(command);
            process.waitFor();


        } catch (IOException | InterruptedException e) {
            logger.write("ERROR: running command" + command + " - " + e.getMessage());
        }

        logger.write("[ecsDump] - end run");

    }

    public void restartECS(){

        String serviceName;
        if (getSisenseVersion().startsWith("7.2")){
            serviceName = "Sisense.ECMS";
        }
        else {
            serviceName = "ElastiCubeManagmentService";
        }

        String methodName = "[restartService] ";
        String restartCommand = "powershell.exe Restart-Service -DisplayName " + serviceName + " -Force";
        logger.write( methodName + "running command " + restartCommand);
        try {
            Process psProcess = runtime.exec(restartCommand);
            psProcess.waitFor();
            psProcess.getOutputStream().close();

            String line;

            logger.write(methodName + "restart command output:");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                while ((line = reader.readLine()) != null){
                    logger.write(line);
                }
            }

            String error;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(psProcess.getErrorStream()))) {
                while ((error = errorReader.readLine()) != null){
                    logger.write(error);
                }
            }

        } catch (IOException | InterruptedException e) {
            logger.write( methodName + "ERROR: " + e.getMessage());
        }

    }

    public void restartIIS(){

        String methodName = "[restartIIS] ";
        String restartCommand = "iisreset";
        logger.write( methodName + "running command " + restartCommand);
        try {
            Process psProcess = runtime.exec(restartCommand);
            psProcess.waitFor();
            psProcess.getOutputStream().close();

            String line;

            logger.write(methodName + "restart command output:");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                while ((line = reader.readLine()) != null){
                    logger.write(line);
                }
            }

            String error;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(psProcess.getErrorStream()))) {
                while ((error = errorReader.readLine()) != null){
                    logger.write(error);
                }
            }

        } catch (IOException | InterruptedException e) {
            logger.write( methodName + "ERROR: " + e.getMessage());
        }

    }

    public String getHostname(){

        String hostname = "";
        String cmd = "hostname";
        try {
            Process getHostnameProcess = runtime.exec(cmd);
            logger.write("[getHostname] sending command '" + cmd + "'...");

            BufferedReader reader = new BufferedReader(new InputStreamReader(getHostnameProcess.getInputStream()));

            String s;
            while ((s = reader.readLine()) != null){
                hostname = s;
            }

            return hostname;

        } catch (IOException e) {
            logger.write("[getHostname] ERROR: Can't get hostname - " + e.getMessage());
        }

        return null;

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