package file_ops;

import logging.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResultFile {

    private static ResultFile instance;
    private final File file = new File(filePath());
    private static Logger logger = Logger.getInstance();

    private ResultFile() {
    }

    public static ResultFile getInstance(){

        if (instance == null){
            instance = new ResultFile();
        }

        return instance;
    }

    public void create(){
        if (!exists()){
            try {
                file.createNewFile();
//                logger.write("[ResultFile.create] Created result file in " + filePath());
            } catch (IOException e) {
                logger.write("[ResultFile.create] ERROR: Creating new file - " + e.getMessage());
            }
        }
    }

    public void write(boolean result) {
        try(FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), false)) {
            fileWriter.write(String.valueOf(result));
            logger.write("[ResultFile.write] Test succeeded: " + result);
        } catch (IOException e) {
            logger.write("[ResultFile.write] ERROR: writing to result file - " + e.getMessage());
        }
    }

    public void delete(){
        if (exists()) {
//            logger.write("[ResultFile.delete] Deleting result file...");
            file.delete();
        }
    }

    private boolean exists(){
        return file.exists();
    }

    private static String filePath(){
        String jarLocation = null;
        try {
            jarLocation = new File(ResultFile.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
            Path path = Paths.get(jarLocation);
            return path.getParent() + "\\run\\result.txt";
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return jarLocation;
    }
}
