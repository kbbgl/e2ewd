package tests.queryservice;

/*
    POJO for ElatiCube model
    statusCode is:
    1 - Stopped
    2 - Running
    514 - Building
 */

import java.util.Map;

public class ElastiCube {

    private String name;
    private int statusCode;
    private int port;
    private boolean isLocked;

    public ElastiCube(String name, int statusCode){
        this.name = name;
        this.statusCode = statusCode;
    }

    public String getName() {
        return name;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    private String getState(int statusCode){

        switch (statusCode){
            case 1:
                return "Stopped";
            case 2:
                return "Running";
            case 514:
                return "Building";
            default:
                return "Unknown status: " + statusCode;
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("name: '").append(name).append("'");

        if (port > 0){
            sb.append(", port: ").append(port);
        }
        sb.append("}");

        return sb.toString();

//        return "\n\t" +
//                "name:" + name + "" +
//                ", state:" + getState(this.statusCode) + "" +
//                (port > 0 ? ", port" + port : "") +
//                '}';
    }

    @Override
    public boolean equals(Object elastiCubeName) {
        if (elastiCubeName == null) return false;
        if (!(elastiCubeName instanceof String)) return false;

        return elastiCubeName.equals(this.name);
    }
}
