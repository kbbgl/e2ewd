package file_ops;

import logging.Logger;
import sun.rmi.runtime.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResultFile {

    private static ResultFile instance;
    public File file;
    private static Logger logger = Logger.getInstance();

    private ResultFile() {

        file = new File( filePath());
        try {
            file.createNewFile();
            logger.write("Created result file in " + filePath());
        } catch (IOException e) {
            logger.write("ERROR: Creating new file - " + e.getMessage());
        }

    }

    public static ResultFile getInstance(){

        if (instance == null){
            instance = new ResultFile();
        }

        return instance;
    }

    public void write(boolean result) {
        try(FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), false)) {
            fileWriter.write(String.valueOf(result));
            logger.write("Test succeeded: " + result);
        } catch (IOException e) {
            logger.write("ERROR: writing to result file - " + e.getMessage());
        }
    }

    public void delete(){
        if (file.exists()) {
            logger.write("Deleting result file...");
            file.delete();
        }
    }

    private static String filePath(){
        String jarLocation = null;
        try {
            jarLocation = new File(ResultFile.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
            Path path = Paths.get(jarLocation);
            return path.getParent() + "/run/result.txt";
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return jarLocation;
    }
}
