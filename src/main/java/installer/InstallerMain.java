package installer;

import installer.view.RootLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.net.URISyntaxException;

public class InstallerMain extends Application {

    private RootLayout rootLayout;
    private String runningDirectory;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        rootLayout = new RootLayout(this);

        try {
            runningDirectory = new File(InstallerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            System.out.println(runningDirectory);
        } catch (URISyntaxException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Couldn't get running directory.\n" + e.getMessage());
            alert.showAndWait();
        }

        Scene scene = new Scene(rootLayout, 400, 300);

        primaryStage.setTitle("e2ewd Installer");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

//    public String getRunningDirectory() {
//        return runningDirectory;
//    }

    public static String getRunningDirectory(){
        return "C:\\temp\\config.properties";
    }
}
