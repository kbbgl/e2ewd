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

        VersionModel installed = new VersionModel(installedVersion);
        VersionModel latest = new VersionModel(latestVersion);

        if (latest.getMajor() > installed.getMajor()){

            logger.info("Newer version available: " + latestVersion);
            logger.info("Download from https://github.com/kbbgl/e2ewd");

        } else if (latest.getMajor() <= installed.getMajor()){

            logger.info("Installed version more updated than repository");

        }
        else if (latest.getMinor() > installed.getMinor()){

            logger.info("New minor version available: " + latestVersion);
            logger.info("Download from https://github.com/kbbgl/e2ewd");

        } else if (latest.getPath() > installed.getPath()){

            logger.info("New patch version available: " + latestVersion);
            logger.info("Download from https://github.com/kbbgl/e2ewd");

        }
        else {
            logger.info("Latest version installed. No need to update.");
        }

    }

    public void compareConfig(){

    }

    public String getInstalledVersion() {
        return installedVersion;
    }


}
