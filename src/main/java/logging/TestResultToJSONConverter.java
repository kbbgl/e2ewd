package logging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class TestResultToJSONConverter {

    public static JSONObject toJSON(Map<String, Boolean> resultMap) throws JSONException {

        JSONObject rootObject = new JSONObject();
        for (Map.Entry<String, Boolean> entry : resultMap.entrySet()) {

            rootObject.put(entry.getKey(), entry.getValue());

        }
        return rootObject;

    }

}
