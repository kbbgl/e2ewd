package testing;

import cmd_ops.CmdOperations;
import models.ElastiCube;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainTest {


    public static void main(String[] args) throws IOException, InterruptedException {


//        File fe=new File("C:\\Users\\kobbi.gal.CORP\\IdeaProjects\\e2ewd\\src\\main\\java\\testing\\test.txt");
//        FileInputStream fis =new FileInputStream(fe);
//
//        BufferedReader stdInput = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
//        String s;
//        while ((s = stdInput.readLine()) != null) {
//
//
//            System.out.println(s);
//
//
//        }

        CmdOperations.getInstance().ecsDump();
        CmdOperations.getInstance().w3wpDump();
    }

}
