package logging;

import java.io.FileWriter;
import java.io.IOException;

public class Logger {

    private String path;

    public Logger(String path) {
        this.path = path;
    }

    private String getPath() {
        return path;
    }

    public void write(String s){
        try(FileWriter fileWriter = new FileWriter(String.valueOf(getPath()) + "/log/log.txt", true)){

            fileWriter.write(s);
            fileWriter.write("\n");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
