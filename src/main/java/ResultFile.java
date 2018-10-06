import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class ResultFile {

    String path;
    File file;

    ResultFile(String path) {
        this.path = path + "/run/result.txt";
    }

    void write(boolean s) throws IOException {
        System.out.println(file.getAbsolutePath());

        try(FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), false)) {
            fileWriter.write(String.valueOf(s));
        } catch (IOException e) {
        e.printStackTrace();
    }
    }

    void create() throws IOException {
        file = new File(path);
        file.createNewFile();
    }
}
