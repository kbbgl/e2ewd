package installer.view;

import file_ops.ConfigFile;
import installer.InstallerMain;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.File;

public class RootLayout extends GridPane {

    private TextField timeoutInput = new TextField();
    private CheckBox restartECSCheckBox = new CheckBox();
    private CheckBox restartIISCheckBox = new CheckBox();
    private TextField slackWebhookURL = new TextField();
    private CheckBox iisDumpCheckBox = new CheckBox();
    private CheckBox ecsDumpCheckBox = new CheckBox();
    private Button submitOptionsButton = new Button("Install");


    public RootLayout(InstallerMain context){

        timeoutInput.setPromptText("Default: 300");
        slackWebhookURL.setPromptText("i.e. https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX");
        submitOptionsButton.setOnAction(event -> {

            File file = new File(context.getRunningDirectory());


        });

        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(25));
        this.setHgap(10);
        this.setVgap(10);

        this.add(new Label("Timeout (seconds)"), 0, 0);
        this.add(timeoutInput, 1,0);

        this.add(new Label("Restart ECS"), 0, 1);
        this.add(restartECSCheckBox, 1,1);

        this.add(new Label("Restart IIS"), 0, 2);
        this.add(restartIISCheckBox, 1, 2);

        this.add(new Label("Slack WebHookURL"), 0, 3);
        this.add(slackWebhookURL, 1, 3);

        this.add(new Label("Create IIS Dump"), 0, 4);
        this.add(iisDumpCheckBox, 1, 4);

        this.add(new Label("Create ECS Dump"), 0, 5);
        this.add(ecsDumpCheckBox, 1,5);

        this.add(submitOptionsButton, 0, 6);

    }

    // Get timeout - default 300 seconds
    public int getTimeout(){

        try {
            return Integer.parseInt(timeoutInput.getText());
        } catch (NumberFormatException e){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Input is not a number", ButtonType.CLOSE);
            alert.showAndWait();
        }

        return 300;
    }

    public boolean isRestartECS(){
        return restartECSCheckBox.isSelected();
    }

    public boolean isRestartIIS(){
        return restartIISCheckBox.isSelected();
    }

    public String getSlackWebhookURL(){
        return slackWebhookURL.getText();
    }

    public boolean createIISDump(){
        return iisDumpCheckBox.isSelected();
    }

    public boolean createECSDump(){
        return ecsDumpCheckBox.isSelected();
    }
}
