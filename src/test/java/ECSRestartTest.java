import logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ECSRestartTest {

    public static void main(String[] args) {

    Runtime runtime = Runtime.getRuntime();
    restartECS(runtime, "Sisense.ECMS");

    }

    public static void restartECS(Runtime runtime, String serviceName){

        String methodName = "[restartECS] ";
        String restartCommand = "powershell.exe Restart-Service -DisplayName " + serviceName + " -Force";
        System.out.println(( methodName + "running command " + restartCommand));
        try {
            Process psProcess = runtime.exec(restartCommand);
            psProcess.waitFor();
            psProcess.getOutputStream().close();

            String line;

            System.out.println(methodName + "restart command output:");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                while ((line = reader.readLine()) != null){
                    System.out.println(line);
                }
            }

            String error;
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(psProcess.getErrorStream()))) {
                while ((error = errorReader.readLine()) != null){
                    System.out.println(error);
                }
            }

        } catch (IOException | InterruptedException e) {
            System.out.println(methodName + "ERROR: " + e.getMessage());
        }

    }

    public static void restartECS(Runtime runtime, String serviceName, Logger logger){

        String methodName = "[restartECS] ";
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
