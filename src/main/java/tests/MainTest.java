package tests;

import logging.TestLog;
import models.ElastiCube;

import java.util.List;

public class MainTest {

    private boolean isSuccess;
    private List<ElastiCube> elastiCubes;
    private TestLog testLog = TestLog.getInstance();

    public MainTest(List<ElastiCube> elastiCubes) {
        this.elastiCubes = elastiCubes;
        init(elastiCubes);
    }

    private void init(List<ElastiCube> elastiCubes) {

    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}
