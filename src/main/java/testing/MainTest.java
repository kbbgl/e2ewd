package testing;

import version.GitHubClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class MainTest {

    public static void main(String[] args) {

        try {
            GitHubClient client = GitHubClient.getInstance();
            String remoteVersion = client.getRemoteVersion();
            System.out.println(remoteVersion);

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }


    }

}
