package logging;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TestLog {

    private static TestLog instance;
    private String host;
    private String testStartTime;
    private String testEndTime;
    private boolean healthy;
    private int numberOfElastiCubes;
    private String reasonForFailure;
    private String version;

    private TestLog() {
    }

    public static TestLog getInstance() {
        if (instance == null){
            instance = new TestLog();
        }
        return instance;
    }

    private String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setTestStartTime(Date testStartTime) {
        this.testStartTime = convertDateToISOString(testStartTime);
    }

    private String getTestStartTime() {
        return this.testStartTime;
    }

    public void setTestEndTime(Date testEndTime){
        this.testEndTime = convertDateToISOString(testEndTime);
    }

    private String getTestEndTime() {
        return testEndTime;
    }

    private boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public int getNumberOfElastiCubes() {
        return numberOfElastiCubes;
    }

    public void setNumberOfElastiCubes(int numberOfElastiCubes) {
        this.numberOfElastiCubes = numberOfElastiCubes;
    }

    public String getReasonForFailure() {
        return reasonForFailure;
    }

    public void setReasonForFailure(String reasonForFailure) {
        this.reasonForFailure = reasonForFailure;
    }

    public void appendReasonForFailure(String reasonForFailure){
        this.reasonForFailure += ", " + reasonForFailure;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private String convertDateToISOString(Date date){

        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(timeZone);
        return df.format(date);

    }

    public JSONObject toJSON() throws JSONException, ParseException {
        JSONObject root = new JSONObject();

        root.put("host", getHost());

        JSONObject startDateObject = new JSONObject();
        startDateObject.put("$date", getTestStartTime());

        JSONObject endDateObject = new JSONObject();
        endDateObject.put("$date", getTestEndTime());

        root.put("start", startDateObject);
        root.put("end", endDateObject);
        root.put("duration", getDuration());
        root.put("healthy", isHealthy());

        return root;
    }

    private long getDuration() throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date start = simpleDateFormat.parse(getTestStartTime());
        Date end = simpleDateFormat.parse(getTestEndTime());

        return (end.getTime() - start.getTime())/1000;
    }

    @Override
    public String toString() {
        return "TestLog{" +
                "host='" + host + '\'' +
                ", testStartTime=" + testStartTime +
                ", healthy=" + healthy +
                ", numberOfElastiCubes=" + numberOfElastiCubes +
                ", reasonForFailure='" + reasonForFailure + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
