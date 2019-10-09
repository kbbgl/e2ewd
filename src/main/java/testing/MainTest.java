package testing;

import tests.BrokerHealthClient;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class MainTest {

    public static void main(String[] args) {

        try {
            BrokerHealthClient brokerHealthClient = BrokerHealthClient.getInstance();
            brokerHealthClient.executeQuery();
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }


    }

}
