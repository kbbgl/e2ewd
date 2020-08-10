package tests.queryservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.Arrays;

public class TelnetTest {

    private static final Logger logger = LoggerFactory.getLogger(TelnetTest.class);

    public static void isConnected(int port){

        logger.info("Checking socket connectivity for " + "localhost" + ":" + port + "..");

        try (Socket socket = new Socket("localhost", port)){
            logger.info("Socket open: " + socket.isConnected());

        } catch (Exception e) {
            logger.error("Socket test failed: " + e.getMessage());
            logger.debug(Arrays.toString(e.getStackTrace()));
        }

    }
}
