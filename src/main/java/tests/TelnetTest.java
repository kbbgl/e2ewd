package tests;

import logging.Logger;

import java.io.IOException;
import java.net.Socket;

public class TelnetTest {

    private static final String className = "[TelnetTest] ";

    public static void isConnected(Logger logger, String host, int port){

        try (Socket socket = new Socket(host, port)){
            logger.write(className  + socket.isConnected());

        } catch (IOException e) {
            logger.write(className  + "ERROR: connecting to " + host + ":" + port + " - " +e.getMessage());
        }

    }
}
