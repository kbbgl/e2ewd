package models;

public class ElastiCube {

    private String name;
    private String state;

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

    @Override
    public String toString() {
        return "ElastiCube{" +
                "name='" + name + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
