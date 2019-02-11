package tests;

import cmd_ops.CmdOperations;
import models.ElastiCube;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonetDBTest {

    private List<ElastiCube> elastiCubes;

    public MonetDBTest(List<ElastiCube> elastiCubes) {
        this.elastiCubes = elastiCubes;
    }


    public Map<String, Boolean> resultSet() throws IOException, InterruptedException {

        Map<String, Boolean> map = new HashMap<>(elastiCubes.size());
        for (ElastiCube elastiCube : elastiCubes){
            map.put(elastiCube.getName(), CmdOperations.getInstance().isMonetDBQuerySuccessful(elastiCube));
        }

        return map;

    }
}
