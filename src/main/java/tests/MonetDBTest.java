package tests;

import cmd_ops.CmdOperations;
import models.ElastiCube;

import java.io.IOException;

class MonetDBTest {

    private ElastiCube elastiCube;
    private boolean isQuerySuccessful;

    MonetDBTest(ElastiCube elastiCube) {
        this.elastiCube = elastiCube;
    }

    void executeQuery() throws IOException, InterruptedException {
        setQuerySuccessful(CmdOperations.getInstance().isMonetDBQuerySuccessful(elastiCube));
    }

    private void setQuerySuccessful(boolean querySuccessful) {
        isQuerySuccessful = querySuccessful;
    }

    boolean isQuerySuccessful() {
        return isQuerySuccessful;
    }
}
