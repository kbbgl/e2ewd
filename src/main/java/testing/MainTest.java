package testing;

import version.GitHubClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class MainTest {

    public static void main(String[] args) {

        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(new FileInputStream("/Users/kobbigal/dev/work/e2ewd/build/libs/config.properties")));

            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
