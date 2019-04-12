package testing;

import cmd_ops.CmdOperations;
import models.ElastiCube;
import updater.VersionComparer;

import java.io.IOException;
import java.util.Arrays;

public class MainTest {


    public static void main(String[] args) throws IOException {

        VersionComparer versionComparer = new VersionComparer("2.4.95732");
        versionComparer.isUpToDate();

//        String v = "2.4.95732";
//        System.out.println(Arrays.toString(v.split("\\.")));

    }

}
