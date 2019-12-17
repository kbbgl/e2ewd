package testing;

import version.GitHubClient;
import version.VersionModel;

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

        VersionModel installed = new VersionModel("7.0.0");
        VersionModel latest = new VersionModel("8.2.0");

        if (latest.getMajor() > installed.getMajor()){

            System.out.println("Newer version available: " + latest.getVersion());
            System.out.println("Download from https://github.com/kbbgl/e2ewd");

        } else if (latest.getMinor() > installed.getMinor()){

            System.out.println("New minor version available: " + latest.getVersion());
            System.out.println("Download from https://github.com/kbbgl/e2ewd");

        } else if (latest.getPath() > installed.getPath()){

            System.out.println("New patch version available: " + latest.getVersion());
            System.out.println("Download from https://github.com/kbbgl/e2ewd");

        }
        else {
            System.out.println("Latest version installed. No need to update.");
        }



    }

}
