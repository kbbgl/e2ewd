package conf.strategies;

import conf.Configuration;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/*
    This class receives the status text and status code
    from the REST API calls and determines whether a service
    restart is necessary
*/
public class StrategyExecutor {

    private final Logger logger = LoggerFactory.getLogger(StrategyExecutor.class);
    private static StrategyExecutor instance;
    private String apiResponseText;
    private int apiResponseCode;
    private String endpoint;
    private StrategyContext strategyContext;
    private Configuration config = Configuration.getInstance();

    private StrategyExecutor() {
    }

    public static StrategyExecutor getInstance() {
        if(instance == null) {
            instance = new StrategyExecutor();
        }

        return instance;
    }

    public void execute() {
        switch (apiResponseCode){
            // If we have a 200, ensure that it doesn't contain an error
            case HttpStatus.SC_OK:
                if (responseContainsError()) {
                    strategyContext.runStrategy();
                }
                break;
            case HttpStatus.SC_BAD_REQUEST:
                logger.warn("Bad GET request sent to '" + endpoint + "'");
                logger.debug(apiResponseText);
                break;
            case HttpStatus.SC_FORBIDDEN:
                logger.warn("Ensure that you have sufficient permissions to run calls to GET '" + endpoint + "'");
                logger.debug(apiResponseText);
                break;
            case HttpStatus.SC_UNAUTHORIZED:
                logger.warn("Check that the token '" + config.getToken() + "' in the configuration file is valid");
                logger.debug(apiResponseText);
                break;
            case HttpStatus.SC_NOT_FOUND:
                logger.error("The endpoint '" + endpoint + "' was not found (404).");
                logger.debug(apiResponseText);
                break;
            case HttpStatus.SC_GATEWAY_TIMEOUT:
                logger.error("Server returned 'Gateway Timeout' (504)");
                logger.debug(apiResponseText);
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                logger.error("Server returned 'Internal Server Error' (500)");
                logger.debug(apiResponseText);
                strategyContext.runStrategy();
                break;
            default:
                logger.error("The endpoint '" + endpoint + "' returned " + apiResponseCode);
                logger.debug(apiResponseText);
                break;
        }
    }

    private boolean responseContainsError(){
        boolean hasError = false;

        try {
            JSONObject response = new JSONObject(apiResponseText);
            if (response.has("error") && response.getBoolean("error")){
                String responseDetails = response.getString("details");
                logger.error("API call returned an error: " + responseDetails);

                if (responseDetails.contains("net.tcp://localhost:812/AbacusQueryService")){
                    hasError = true;
                }
            }
        } catch (JSONException e) {
            logger.warn("Response '" + apiResponseText + "' from API is not valid JSON");
            logger.info(Arrays.toString(e.getStackTrace()));
        }

        return hasError;
    }

}
