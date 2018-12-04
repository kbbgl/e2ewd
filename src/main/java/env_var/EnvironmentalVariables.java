package env_var;

import logging.Logger;

import java.io.IOException;

public class EnvironmentalVariables {

    private static final String CMD = "cmd.exe SET SISENSE_PSM=true";

    public static void setSisenseDebugMode(Runtime runtime, Logger logger){

        try {

            Process setEnvironmentalVariable = runtime.exec(CMD);
            logger.write("Running " + CMD);

        } catch (IOException e) {
            logger.write("ERROR setting SISENSE_PSM=true, " + e.getMessage());
        }

    }

}
