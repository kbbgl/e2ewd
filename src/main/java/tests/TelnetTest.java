package tests;

import logging.Logger;

import java.io.IOException;
import java.net.Socket;

public class TelnetTest {

    private static final String className = "[TelnetTest] ";

    public static void isConnected(String host, int port){

        try (Socket socket = new Socket(host, port)){
            Logger.getInstance().write(className  + socket.isConnected());

        } catch (IOException e) {
            Logger.getInstance().write(className  + "ERROR: connecting to " + host + ":" + port + " - " +e.getMessage());
        }

    }
}
