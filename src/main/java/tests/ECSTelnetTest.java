package tests;

import java.io.IOException;
import java.net.Socket;

public class ECSTelnetTest {

    public static boolean port812Reachable() {
        try (Socket socket = new Socket("localhost", 812)) {

            return true;

        } catch (IOException ignored) {

        }

        return false;

    }

    public static boolean port811Reachable() {

        try (Socket socket = new Socket("localhost", 811)) {

            return true;

        } catch (IOException ignored) {

        }

        return false;

    }

}
