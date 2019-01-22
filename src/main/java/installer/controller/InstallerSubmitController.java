package installer.controller;

import installer.InstallerMain;
import installer.model.Configuration;
import installer.tasks.CreateConfigurationFileTask;
import installer.view.RootLayout;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class InstallerSubmitController {

    private RootLayout rootLayout;
    private CreateConfigurationFileTask task;

    public InstallerSubmitController(RootLayout rootLayout){
        this.rootLayout = rootLayout;
    }

    public void submitForm(){

        task = new CreateConfigurationFileTask(InstallerMain.getRunningDirectory(), this.rootLayout);
        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();

    }

}
