package tests;

import cmd_ops.CmdOperations;
import models.ElastiCube;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class MonetDBTest {

    private ElastiCube elastiCube;
    private boolean isQuerySuccessful;
    private final Logger logger = LoggerFactory.getLogger(MonetDBTest.class);

    MonetDBTest(ElastiCube elastiCube) {
        this.elastiCube = elastiCube;
    }

    void executeQuery() throws IOException, InterruptedException {
        logger.debug("Executing MonetDB query for " + elastiCube.getName() + "..");
        setQuerySuccessful(CmdOperations.getInstance().isMonetDBQuerySuccessful(elastiCube));
    }

    private void setQuerySuccessful(boolean querySuccessful) {
        isQuerySuccessful = querySuccessful;
    }

    boolean isQuerySuccessful() {
        return isQuerySuccessful;
    }
}
