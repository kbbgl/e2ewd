package testing;

import org.json.JSONException;
import org.json.JSONObject;
import tests.MicroservicesHealthClient;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class MainTest {

    public static void main(String[] args) {

//        String res = "{\"ai-integration\":{\"urls\":[{\"url\":\"127.0.0.1:15010\"}],\"active\":true},\"configuration-service\":{\"urls\":[{\"url\":\"10.50.65.209:15001\"}],\"active\":true},\"plugins-service\":{\"urls\":[],\"active\":false},\"storage-manager\":{\"urls\":[{\"url\":\"10.50.65.209:15006\"}],\"active\":true},\"reporting-service\":{\"urls\":[{\"url\":\"10.50.65.209:15009\"}],\"active\":true},\"analytical-engine\":{\"urls\":[],\"active\":false},\"galaxy\":{\"urls\":[{\"url\":\"10.50.65.209:15003\"},{\"url\":\"10.50.65.209:15003\"},{\"url\":\"10.50.65.209:15003\"}],\"active\":true,\"roundRobinCount\":2},\"query-proxy\":{\"urls\":[{\"url\":\"10.50.65.209:14996\"}],\"active\":true},\"identity-service\":{\"urls\":[{\"url\":\"10.50.65.209:15002\"}],\"active\":true},\"blox\":{\"urls\":[{\"url\":\"10.50.65.209:15014\"}],\"active\":true},\"ecm\":{\"urls\":[{\"url\":\"10.50.65.209:15004\"}],\"active\":true},\"intelligence-service\":{\"urls\":[{\"url\":\"127.0.0.1:15012\"}],\"active\":true}}";
//
//        try {
//            JSONObject resJSON = new JSONObject(res);
//            Iterator<String> keysIterator = resJSON.keys();
//
//
//            while (keysIterator.hasNext()){
//                String microservice = keysIterator.next();
//
//                if (resJSON.get(microservice) instanceof JSONObject){
//
//
//                    boolean isMicroserviceHealthy = ((JSONObject) resJSON.get(microservice)).getBoolean("active");
//                    System.out.println(microservice + " : " + isMicroserviceHealthy);
//
//                    if (!isMicroserviceHealthy){
//                        System.out.println(microservice + " is unhealthy!");
//                    }
//
//
//                }
//
//
//            }
//
//
//        } catch (JSONException e){
//            e.printStackTrace();
//        }

        try {
            MicroservicesHealthClient client = MicroservicesHealthClient.getInstance();
            client.executeCall();
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

}
