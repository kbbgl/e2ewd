package tests.queryservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ECSStateKeeper {

    private static ECSStateKeeper instance;
    private List<ElastiCube> availableElastiCubes = new ArrayList<>();
    private Logger logger = LoggerFactory.getLogger(ECSStateKeeper.class);

    private ECSStateKeeper(){

        logger.debug("Created ECSStateKeeper instance");

    }

    public static ECSStateKeeper getInstance() {
        if (instance == null){
            instance = new ECSStateKeeper();
        }
        return instance;
    }

    public void addToListOfElastiCubes(ElastiCube elastiCube){

        // Check whether the ElastiCube is not building and add it to the list
        if (elastiCube.getStatusCode() != 514){
            logger.debug("Added " + elastiCube.getName() + " to list of ElastiCubes");
            this.availableElastiCubes.add(elastiCube);
        } else {
            logger.info("ElastiCube '" + elastiCube.getName() + "' is currently building and will not be added to list of available ElastiCubes.");
        }

    }

    public List<ElastiCube> getAvailableElastiCubes() {
        return availableElastiCubes;
    }

    public List<ElastiCube> getRunningElastiCubes() {

        List<ElastiCube> runningElastiCubes = new ArrayList<>();

        for (ElastiCube elastiCube : availableElastiCubes){
            if (elastiCube.getStatusCode() == 2){
                runningElastiCubes.add(elastiCube);
            }
        }

        return runningElastiCubes;
    }

    public boolean isBuilding() {

        for (ElastiCube elastiCube : availableElastiCubes){
            if (elastiCube.getStatusCode() == 514){
                return true;
            }
        }

        return false;

    }

}
