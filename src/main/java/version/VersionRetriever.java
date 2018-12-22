package version;

import logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class VersionRetriever {

    public static String getVersion(Logger logger) {

        try {
            Process process = Runtime.getRuntime().exec("reg QUERY HKEY_LOCAL_MACHINE\\SOFTWARE\\Sisense\\ECS /v Version");
            StringWriter stringWriter = new StringWriter();

            InputStream is = process.getInputStream();
            int c;
            while((c = is.read()) != -1){
                stringWriter.write(c);
            }
            is.close();
            return extractVersionFromRegistry(stringWriter.toString().trim());
        } catch (IOException e) {
            logger.write("[getVersion] - ERROR: " + e.getMessage());
        }

        return "CANNOT_DETECT";
    }

    private static String extractVersionFromRegistry(String s){

        return s.split("    ")[3];
    }
}
