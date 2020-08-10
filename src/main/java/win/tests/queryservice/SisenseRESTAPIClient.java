package tests.queryservice;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public interface SisenseRESTAPIClient {

    void initializeClient(JSONObject jaql) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException;
    void initializeClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException;
    void executeCall() throws IOException;
    void parseResponse(HttpResponse response);

    void setUri();
    String getUri();

}
