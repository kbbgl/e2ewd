import java.io.IOException;

public class EnvVariableTest {

    public static void main(String[] args) {

        Runtime rt = Runtime.getRuntime();
        try {

            Process setEnvironmentalVariable = rt.exec("cmd.exe SET SISENSE_PSM=true");
            setEnvironmentalVariable.waitFor();


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
