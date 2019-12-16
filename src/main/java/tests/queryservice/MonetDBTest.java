package tests.queryservice;

import cmd_ops.CmdOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MonetDBTest {

    private ElastiCube elastiCube;
    private boolean isQuerySuccessful;
    private final Logger logger = LoggerFactory.getLogger(MonetDBTest.class);

    public MonetDBTest(ElastiCube elastiCube) {
        this.elastiCube = elastiCube;
    }

    public void executeQuery() throws IOException, InterruptedException {
        logger.debug("Executing MonetDB query for " + elastiCube.getName() + "..");
        setQuerySuccessful(CmdOperations.getInstance().isMonetDBQuerySuccessful(elastiCube));
    }

    private void setQuerySuccessful(boolean querySuccessful) {
        isQuerySuccessful = querySuccessful;
    }

    public boolean isQuerySuccessful() {
        return isQuerySuccessful;
    }
}
