package testing;

import cmd_ops.CmdOperations;
import models.ElastiCube;

import java.io.IOException;

public class MainTest {


    public static void main(String[] args) throws IOException, InterruptedException {

        for (ElastiCube elastiCube : CmdOperations.getInstance().getListElastiCubes()){
            System.out.println("Number of connections for ElastiCube " + elastiCube.getName() + " : "+ CmdOperations.getInstance().getMonetDBConcurrentConnections(elastiCube));
        }

    }

}
