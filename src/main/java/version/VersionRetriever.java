package version;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class VersionRetriever {

    public static String getVersion() throws IOException {

        Process process = null;
        try {
            process = Runtime.getRuntime().exec("reg QUERY HKEY_LOCAL_MACHINE\\SOFTWARE\\Sisense\\ECS /v Version");
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringWriter stringWriter = new StringWriter();

        InputStream is = process.getInputStream();
        int c;
        while((c = is.read()) != -1){
            stringWriter.write(c);
        }
        is.close();
        return extractVersionFromRegistry(stringWriter.toString().trim());
    }

    private static String extractVersionFromRegistry(String s){

        return s.split("    ")[3];
    }
}
