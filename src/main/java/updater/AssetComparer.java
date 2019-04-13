package updater;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.stream.Collectors;

public class AssetComparer {

    private Logger logger = LoggerFactory.getLogger(AssetComparer.class);
    File[] currentFiles;
    File[] repoFiles;
    String runningLocation;

    public AssetComparer(String runningLocation){
        this.runningLocation = runningLocation;
    }

    public void compare(){

        currentFiles = new File(runningLocation).listFiles();



    }

    private void getRepoFiles() throws IOException {

        String url = "https://api.github.com/repos/kbbgl/e2ewd/contents/libs";

        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(30 * 1000)
                .setConnectTimeout(30 * 1000)
                .setConnectionRequestTimeout(30 * 1000)
                .build();

        HttpClient client = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(config)
                .build();

        HttpGet get = new HttpGet(url);
        get.addHeader("Accept", "application/vnd.github.v3+json");
        get.addHeader("User-Agent", "kbbgl-e2ewd-client");


        logger.info("Executing call to " + url + "..");
        HttpResponse response = client.execute(get);
        HttpEntity resEntity = response.getEntity();
        int reponseCode = response.getStatusLine().getStatusCode();

        logger.info("Response code: " + reponseCode);
        if (resEntity != null){

            try(InputStream inputStream = resEntity.getContent()){

                String result = new BufferedReader(new InputStreamReader(inputStream))
                        .lines()
                        .collect(Collectors.joining("\n"));

                logger.debug("Result from call: " + result);

            }

        } else {
            logger.warn("Empty response");
        }


        /*
                hostname: 'api.github.com',
                port: 443,
                path: '/repos/kbbgl/e2ewd/contents/build/libs',
                method: 'GET',
                headers: {
                    "Accept": "application/vnd.github.v3+json",
                    "User-Agent": "kbbgl-electron-e2ewd-client"
    }

         */
    }

}
