package version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionModel {

    private String version;
    private int major;
    private int minor;
    private int path;
    private final Logger logger = LoggerFactory.getLogger(VersionModel.class);

    public VersionModel(String version){

        this.version = version;
        parseVersion(version);

    }

    private void parseVersion(String version) {

        String[] versionParts = version.split("\\.");

        try {
            this.major = Integer.parseInt(versionParts[0]);
            this.minor = Integer.parseInt(versionParts[1]);
            this.path = Integer.parseInt(versionParts[2]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
            logger.warn("Error parsing version: ", e);
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }
}
