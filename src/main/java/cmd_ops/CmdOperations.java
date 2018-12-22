package cmd_ops;

import file_ops.ResultFile;
import logging.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdOperations {

    private static CmdOperations instance;
    private static final Runtime runtime = Runtime.getRuntime();
    private static final Logger logger = Logger.getInstance();
    private static final ResultFile resultFile = ResultFile.getInstance();

    private CmdOperations(){

    }

    public static CmdOperations getInstance(){

        if (instance == null){
            instance = new CmdOperations();
        }

        return instance;
    }

    public String getTable(){
        String[] psmCmd  = {
                "C:\\Program Files\\Sisense\\Prism\\Psm.exe",
                "ecube",
                "edit",
                "tables",
                "getListOfTables",
                "serverAddress=localhost",
                "cubeName=" + getElastiCubeName(),
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

    public String getElastiCubeName(){
        String ec = "";

        try {
            String[] psmCmd = new String[]{
                    "cmd.exe",
                    "/c",
                    "SET SISENSE_PSM=true&&\"C:\\Program Files\\Sisense\\Prism\\Psm.exe\"",
                    "ecs",
                    "ListCubes",
                    "serverAddress=localhost"};

            Process listCubesCommand = runtime.exec(psmCmd);
            logger.write("[getElasticubeName] running commands: " + Arrays.toString(psmCmd));

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(listCubesCommand.getInputStream()));

            Pattern cubeNamePattern = Pattern.compile("\\[(.*?)]");
            Pattern errorPattern = Pattern.compile("\\((.*?)\\)");

            String s;

            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                if (s.startsWith("Cube Name")){
                    Matcher m = cubeNamePattern.matcher(s);
                    while (m.find()){
                        ec = m.group(1);
                        logger.write("[getElasticubeName] ElastiCube found: " + ec);
                        return ec;
                    }
                }
                else {
                    Matcher m = errorPattern.matcher(s);
                    while (m.find()){
                        logger.write("[getElasticubeName] ERROR: " + m.group(1));

                        if (m.group(1).equals("the server, 'localhost', is not responding.")){
                            return ec;
                        }

                    }
                }

            }
        } catch (IOException e) {
            logger.write("[getElasticubeName] ERROR: " + e.getMessage());
        }

        return ec;
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

            return stringWriter.toString().trim().split("   ")[3];

        } catch (IOException e) {
            logger.write("ERROR: retrieving Sisense version - " + e.getMessage());
            return "CANNOT DETECT";
        }

    }

    public void restartService(String serviceName){

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
}
