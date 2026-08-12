package com.vassarlabs.aulm.ui.view;

import com.vassarlabs.aulm.ui.api.ApiClient;
import com.vassarlabs.aulm.ui.model.LoginResponseDto;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView {

    private final Stage stage;
    private final ApiClient apiClient;

    public LoginView(Stage stage, ApiClient apiClient) {
        this.stage = stage;
        this.apiClient = apiClient;
    }

    public void show() {
        Label title = new Label("Aqua User & License Manager");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setWrapText(true);

        Button loginButton = new Button("Log In");
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter a username and password.");
                return;
            }

            errorLabel.setText("");
            loginButton.setDisable(true);

            Task<LoginResponseDto> task = new Task<>() {
                @Override
                protected LoginResponseDto call() {
                    return apiClient.login(username, password);
                }
            };
            task.setOnSucceeded(evt -> {
                loginButton.setDisable(false);
                LoginResponseDto response = task.getValue();
                if (!response.getUser().isAdmin()) {
                    errorLabel.setText("This account is not authorized to manage users and licenses.");
                    return;
                }
                apiClient.setToken(response.getToken());
                new UserListView(stage, apiClient, response.getUser()).show();
            });
            task.setOnFailed(evt -> {
                loginButton.setDisable(false);
                Throwable ex = task.getException();
                errorLabel.setText(ex != null ? ex.getMessage() : "Login failed.");
            });
            new Thread(task, "aulm-login").start();
        });

        VBox form = new VBox(12, title, usernameField, passwordField, loginButton, errorLabel);
        form.setPadding(new Insets(30));
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(320);

        StackPane root = new StackPane(form);
        Scene scene = new Scene(root, 440, 340);
        stage.setScene(scene);
        stage.show();
    }
}
