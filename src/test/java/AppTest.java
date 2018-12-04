import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import env_var.EnvironmentalVariables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppTest {

    public static void main(String[] args) {
        System.out.println(getElasticubeName());
    }


    private static String getElasticubeName() {

        String ec = null;

        try {

            Runtime rt = Runtime.getRuntime();
            Process listCubesCommand = rt.exec(new String[]{"cmd.exe", "/c", "SET SISENSE_PSM=true&&", "", "psm", "ecs", "ListCubes", "serverAddress=localhost"});

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(listCubesCommand.getInputStream()));

            Pattern cubeNamePattern = Pattern.compile("\\[(.*?)]");
            Pattern errorPattern = Pattern.compile("\\((.*?)\\)");


            String s;
            while ((s = stdInput.readLine()) != null) {
                if (s.startsWith("Cube Name")){
                    System.out.println(s);
                    Matcher m = cubeNamePattern.matcher(s);
                    while (m.find()){
                        ec = "\"" + m.group(1) + "\"";
                        return ec;
                    }
                    break;
                }
                else {
                    Matcher m = errorPattern.matcher(s);
                    while (m.find()){
                        System.out.println(m.group(1));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ec;
    }
}