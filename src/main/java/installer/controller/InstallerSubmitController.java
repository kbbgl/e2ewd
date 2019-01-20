package installer.controller;

import installer.view.RootLayout;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Properties;


// TODO two actions:
// 1) Get server information from ZK/Config Manager
// 2) Create config.properties
// 3) Write to config file.

public class InstallerSubmitController {

    private RootLayout rootLayout;

    public InstallerSubmitController(RootLayout rootLayout){
        this.rootLayout = rootLayout;
    }

    // TODO get server information from Config Manager
    private void getServerInformation(){

    }

    public void submitForm(){
        File file = new File(rootLayout.getAppContext().getRunningDirectory());

        try {
            if (!file.createNewFile()){

                System.out.println("File already exists");

                Optional<ButtonType> result = rootLayout.fileAlreadyExistsAlertResult(file);

                if (!result.isPresent()){
                    System.out.println("No option chosen. Operation cancelled");
                } else if (result.get() == ButtonType.YES){
                    file.delete();
                    file.createNewFile();

                    Properties properties = new Properties();
                    properties.setProperty("slackWebhookURL", rootLayout.getSlackWebhookURL());
                    properties.setProperty("requestTimeoutInSeconds", String.valueOf(rootLayout.getTimeout()));
                    properties.setProperty("restartECS", String.valueOf(rootLayout.isRestartECS()));
                    properties.setProperty("restartIIS", String.valueOf(rootLayout.isRestartIIS()));
                    properties.setProperty("ecsDump", String.valueOf(rootLayout.createECSDump()));
                    properties.setProperty("iisDump", String.valueOf(rootLayout.createIISDump()));

                    OutputStream outputStream = new FileOutputStream(file);
                    properties.store(outputStream, "e2ewd configuration created by installer");

                    rootLayout.showConfirmationAlert(file);

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
