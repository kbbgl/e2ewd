package unit_tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainTest {

    static Runtime runtime = Runtime.getRuntime();

    public static void main(String[] args) {

        String hostname = getHostname();
        System.out.println(hostname);

    }

    public static String getHostname(){

        String hostname = "";
        String cmd = "hostname";
        try {
            Process getHostnameProcess = runtime.exec(cmd);

            BufferedReader reader = new BufferedReader(new InputStreamReader(getHostnameProcess.getInputStream()));

            String s;
            while ((s = reader.readLine()) != null){
                hostname = s;
            }
            return hostname;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

}
