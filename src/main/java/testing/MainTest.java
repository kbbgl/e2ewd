package testing;

import file_ops.ConfigFile;

public class MainTest {

    public static void main(String[] args) {

        System.out.println(ConfigFile.getInstance().toString());

    }
}
