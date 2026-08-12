package com.vassarlabs.aulm.ui;

import com.vassarlabs.aulm.ui.api.ApiClient;
import com.vassarlabs.aulm.ui.view.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static final String DEFAULT_BASE_URL = "http://localhost:8181";

    @Override
    public void start(Stage stage) {
        String baseUrl = System.getProperty("aulm.backend.url", DEFAULT_BASE_URL);
        ApiClient apiClient = new ApiClient(baseUrl);
        stage.setTitle("Aqua User & License Manager");
        new LoginView(stage, apiClient).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
