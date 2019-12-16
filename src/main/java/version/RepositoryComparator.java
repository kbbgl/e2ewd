package version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class RepositoryComparator {

    private String runningLocation;
    GitHubClient client;
    private String installedVersion;
    private Logger logger = LoggerFactory.getLogger(RepositoryComparator.class);

    public RepositoryComparator(String runningLocation) throws URISyntaxException {

        this.runningLocation = runningLocation;
        client = GitHubClient.getInstance();

    }

    public void compareVersions() throws IOException {

        installedVersion = new VersionFile(runningLocation).getVersion();
        logger.info("e2ewd version: " + installedVersion);

        String latestVersion = client.getRemoteVersion();

        if (!installedVersion.equals(latestVersion)){



            logger.info("Newer version available: " + latestVersion);
            logger.info("Download from https://github.com/kbbgl/e2ewd");

        } else {
            logger.info("Latest version installed. No need to update.");
        }

    }

    public void compareConfig(){

    }

    public String getInstalledVersion() {
        return installedVersion;
    }
}
