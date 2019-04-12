package updater;

import jdk.internal.util.xml.impl.Input;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class VersionComparer {

    private String currentRunningVersion;
    private String latestVersion;
    private Logger logger = LoggerFactory.getLogger(VersionComparer.class);

    public VersionComparer(String currentRunningVersion) throws IOException {
        this.currentRunningVersion = currentRunningVersion;
        this.latestVersion = fetchLatestVersion();
    }

    private String fetchLatestVersion() throws IOException {
        RequestConfig config = RequestConfig
                .custom()
                .setConnectionRequestTimeout(30 * 1000)
                .setConnectTimeout(30 * 1000)
                .setSocketTimeout(30 * 1000)
                .build();

        HttpClient client = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(config)
                .build();


        String url = "https://raw.githubusercontent.com/kbbgl/e2ewd/master/build/libs/version";
        HttpGet get = new HttpGet(url);

//        logger.debug("Sending request to " + url);
        System.out.println("Sending request to " + url);
        HttpResponse response = client.execute(get);

        int responseCode = response.getStatusLine().getStatusCode();
        HttpEntity responseBody = response.getEntity();

//        logger.debug("Response code: " + responseCode);
        System.out.println("Response code: " + responseCode);
        if (responseBody != null){

            try (InputStream inputStream = responseBody.getContent()){

                String version = new BufferedReader(new InputStreamReader(inputStream))
                        .readLine();

                logger.debug("Response: " + version);
                System.out.println("Version: " + version);
                return version;
            }

        } else {
            logger.error("Failed getting latest version.");
        }

        return null;

    }

    public boolean isUpToDate(){

        boolean upToDate = false;
        System.out.println("Current version: `" + currentRunningVersion + "`");
        System.out.println("Latest version: `" + latestVersion + "`");

        if (getMajor(latestVersion) == getMajor(currentRunningVersion)
            && getMinor(latestVersion) == getMinor(currentRunningVersion)
            && getBuild(latestVersion) == getBuild(currentRunningVersion)) {
            upToDate = true;
        }

        System.out.println("Is up to date: " + upToDate);
        return upToDate;

    }

    private int getMajor(String version) throws NumberFormatException{

        return Integer.parseInt(version.split("\\.")[0]);

    }

    private int getMinor(String version) throws NumberFormatException{
        return Integer.parseInt(version.split("\\.")[1]);
    }

    private int getBuild(String version) throws NumberFormatException{
        return Integer.parseInt(version.split("\\.")[2]);
    }

}
