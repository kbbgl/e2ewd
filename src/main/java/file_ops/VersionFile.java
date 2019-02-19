package file_ops;

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

    public VersionFile(String runningLocation){

        setFile(runningLocation);
        setVersion();
        writeVersionToFile();
    }

    private void setFile(String runningLocation) {
        this.file = new File(runningLocation + File.separator + "version");
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeVersionToFile(){

        try(FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), false)) {
            fileWriter.write(String.valueOf(version));
        } catch (IOException e) {
            System.out.println("ERROR: writing to version file - " + e.getMessage());
        }


    }

    private void setVersion(){
        Enumeration enumeration;
        try {
            enumeration = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (enumeration.hasMoreElements()){
                try {
                    URL url = (URL) enumeration.nextElement();
                    try (InputStream inputStream = url.openStream()){
                        Manifest manifest = new Manifest(inputStream);
                        Attributes attributes = manifest.getMainAttributes();
                        String version = attributes.getValue("Implementation-Version");
                        if (version != null){
                            this.version = version;
                        }
                    }
                } catch (Exception ignored){

                }
            }
        } catch (IOException ignored){

        }
    }

    public String getVersion() {
        return version;
    }
}
