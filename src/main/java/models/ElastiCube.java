package models;

public class ElastiCube {

    private String name;
    private String state;
    private int port;

    public ElastiCube(String name, String state){
        this.name = name;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ElastiCube{" +
                "name='" + name + '\'' +
                ", state='" + state + '\'' +
                ", port=" + port +
                '}';
    }

    @Override
    public boolean equals(Object elastiCubeName) {
        if (elastiCubeName == null) return false;
        if (!(elastiCubeName instanceof String)) return false;

        return elastiCubeName.equals(this.name);
    }
}
