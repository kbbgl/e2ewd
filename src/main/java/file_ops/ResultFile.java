package file_ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResultFile {

    private static ResultFile instance;
    private static File file;
    private static final Logger logger = LoggerFactory.getLogger(ResultFile.class);

    private ResultFile() {
    }

    public static ResultFile getInstance(){

        if (instance == null){
            instance = new ResultFile();

            if (filePath() != null){
                file = new File(filePath());
            } else {
                logger.error("Result path returned null");
            }
            logger.debug("Created instance of ResultFile.");
        }

        return instance;
    }

    public void create(){
        if (!exists()){
            logger.debug("Result file doesn't exist.");
            try {
                if (file.createNewFile()){
                    logger.debug("Created new result file in " + file.getAbsolutePath());
                } else {
                    logger.info("Failed to create new result file.");
                }
            } catch (IOException e) {
                logger.error("Error creating result file: " + e.getMessage());
            }
        }
    }

    public void write(boolean result) {
        try(FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), false)) {
            fileWriter.write(String.valueOf(result));
            logger.info("Test succeeded: " + result);
        } catch (IOException e) {
            logger.error("Error writing result to file; " + e.getMessage());
        }
    }

    public void delete(){
        logger.debug("Deleting result file...");
        if (exists()) {
            logger.debug("Result file exists.");
            if (file.delete()){
                logger.debug("Result file deleted.");
            } else {
                logger.info("Result file not deleted.");
            }
        }
    }

    private boolean exists(){
        return file.exists();
    }

    private static String filePath(){
        try {
            Path path = Paths.get(new File(ResultFile.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath());
            logger.debug("Result file path " + path.getParent() + "\\run\\result.txt");
            return path.getParent() + "\\run\\result.txt";
        } catch (IOException | URISyntaxException e) {
            logger.error("Error reading file path: " + e.getMessage());
            return null;
        }
    }
}
