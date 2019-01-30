package testing;

import cmd_ops.CmdOperations;
import models.ElastiCube;
import tests.SisenseRESTAPI;

import java.util.List;

public class MainTest {

    public static void main(String[] args) {

        List<ElastiCube> elasticubes =  CmdOperations.getInstance().getListElastiCubes();
        for (ElastiCube elasticube : elasticubes){
            SisenseRESTAPI.queryTableIsSuccessful(elasticube.getName());
        }

    }
}
