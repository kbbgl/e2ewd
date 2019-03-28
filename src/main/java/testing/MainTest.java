package testing;

import file_ops.ConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTest {

    static final Logger logger = LoggerFactory.getLogger(MainTest.class);

    public static void main(String[] args) {

//        logger.info(ConfigFile.getInstance().toString());
//        System.out.println(ConfigFile.getInstance().toString());

        print();
        logger.info(System.getProperty("user.home"));

    }

    private static void print(){
        logger.info(ConfigFile.getInstance().toString());
    }
}
