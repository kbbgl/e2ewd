package file_ops;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ResultFile {

    public String path;
    private File file;

    public ResultFile(String path) {
        this.path = path + "/run/result.txt";
    }

    public void write(boolean s) throws IOException {
        try(FileWriter fileWriter = new FileWriter(file.getAbsolutePath(), false)) {
            fileWriter.write(String.valueOf(s));
        } catch (IOException e) {
            e.printStackTrace();
    }
    }

    public void create() throws IOException {
        file = new File(path);
        file.createNewFile();
    }

    void delete(){
        if (file.exists()) {
            file.delete();
        }
    }
}
