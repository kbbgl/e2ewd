package file_ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class VersionFile {

    private File file;
    private String version;
    private Logger logger = LoggerFactory.getLogger(VersionFile.class);

    public VersionFile(String runningLocation){

        setFile(runningLocation);
        setVersion();
        writeVersionToFile();
    }

    private void setFile(String runningLocation) {
        this.file = new File(runningLocation + File.separator + "version");
        if (!file.exists()){
            logger.debug("Version file doesn't exist.");
            try {
                if (file.createNewFile()){
                    logger.debug("Version file created in " + file.getAbsolutePath());
                } else {
                    logger.warn("Version file wasn't created");
                }

            } catch (IOException e) {
                logger.error("Error setting version file: " + e.getMessage());
            }
        }
    }

    private void writeVersionToFile(){

        logger.debug("Writing version to file...");

        try(FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), false)) {
            fileWriter.write(String.valueOf(version));
            logger.debug("Version written to file: " + version);

        } catch (IOException e) {
            logger.error("Error writing to file "+ file.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    private void setVersion(){
        Enumeration enumeration;
        try {
            enumeration = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (enumeration.hasMoreElements()) {
                    URL url = (URL) enumeration.nextElement();
                    try (InputStream inputStream = url.openStream()) {
                        Manifest manifest = new Manifest(inputStream);
                        Attributes attributes = manifest.getMainAttributes();
                        String version = attributes.getValue("Implementation-Version");
                        if (version != null) {
                            this.version = version;
                        }
                }
            }
        } catch (IOException e){
            logger.error("Error setting version from manifest: " + e.getMessage());
        }
    }

    public String getVersion() {
        return version;
    }
}
