package logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

public class Logger {

    private static Logger instance;

    private Logger(){

    }

    public static Logger getInstance() {
        if (instance == null){
            instance = new Logger();
        }
        return instance;
    }

    private static String executionPath(){
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


    public void write(String s){
        Date date = new Date();

        try(FileWriter fileWriter = new FileWriter(executionPath() + "/log/log.txt", true)){

            String message = "[" + date.toString() + "] - " + s;
            fileWriter.write(message);
            System.out.println(message);
            fileWriter.write("\n");


        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
