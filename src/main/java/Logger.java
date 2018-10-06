import java.io.FileWriter;
import java.io.IOException;

class Logger {

    private String path;

    Logger(String path) {
        this.path = path;
    }

    private String getPath() {
        return path;
    }

    void write(String s){
        try(FileWriter fileWriter = new FileWriter(String.valueOf(getPath()) + "/log/log.txt", true)){

            fileWriter.write(s);
            fileWriter.write("\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
