package com.vassarlabs.aulm.ui.view;

import com.vassarlabs.aulm.ui.api.ApiClient;
import com.vassarlabs.aulm.ui.model.CreateUserPayload;
import com.vassarlabs.aulm.ui.model.UpdateUserPayload;
import com.vassarlabs.aulm.ui.model.UserDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;

public class UserListView {

    private final Stage stage;
    private final ApiClient apiClient;
    private final UserDto currentUser;
    private final ObservableList<UserDto> users = FXCollections.observableArrayList();
    private final TableView<UserDto> table = new TableView<>(users);
    private final Label statusLabel = new Label();

    public UserListView(Stage stage, ApiClient apiClient, UserDto currentUser) {
        this.stage = stage;
        this.apiClient = apiClient;
        this.currentUser = currentUser;
    }

    public void show() {
        buildTable();

        Button addButton = new Button("Add User");
        Button editButton = new Button("Edit");
        Button deleteButton = new Button("Delete");
        Button renewButton = new Button("Renew License");
        Button refreshButton = new Button("Refresh");
        Button logoutButton = new Button("Logout");

        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        renewButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        addButton.setOnAction(e -> onAdd());
        editButton.setOnAction(e -> onEdit());
        deleteButton.setOnAction(e -> onDelete());
        renewButton.setOnAction(e -> onRenew());
        refreshButton.setOnAction(e -> refresh());
        logoutButton.setOnAction(e -> {
            apiClient.setToken(null);
            new LoginView(stage, apiClient).show();
        });

        HBox toolbar = new HBox(10, addButton, editButton, deleteButton, renewButton, refreshButton, logoutButton);
        toolbar.setPadding(new Insets(10));

        Label header = new Label("Signed in as " + currentUser.getUsername() + " (admin)");
        header.setPadding(new Insets(0, 10, 0, 10));

        statusLabel.setPadding(new Insets(5, 10, 10, 10));

        BorderPane root = new BorderPane();
        root.setTop(new javafx.scene.layout.VBox(header, toolbar));
        root.setCenter(table);
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 960, 560);
        stage.setScene(scene);
        stage.setTitle("Aqua User & License Manager");
        stage.show();

        refresh();
    }

    private void buildTable() {
        TableColumn<UserDto, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));

        TableColumn<UserDto, String> fullNameCol = new TableColumn<>("Full Name");
        fullNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));

        TableColumn<UserDto, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<UserDto, String> adminCol = new TableColumn<>("Admin");
        adminCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isAdmin() ? "Yes" : "No"));

        TableColumn<UserDto, String> permissionsCol = new TableColumn<>("Permissions");
        permissionsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().permissionsSummary()));

        TableColumn<UserDto, String> licenseTypeCol = new TableColumn<>("License Type");
        licenseTypeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLicenseType()));

        TableColumn<UserDto, String> expiryCol = new TableColumn<>("Expiry");
        expiryCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getExpiryDate() == null ? "Never" : data.getValue().getExpiryDate().toString()));

        TableColumn<UserDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> {
            UserDto user = data.getValue();
            String status;
            if (!user.isEnabled()) {
                status = "Disabled";
            } else if (user.isLicenseValid()) {
                status = "Active";
            } else {
                status = "License " + (user.getLicenseStatus() == null ? "invalid" : user.getLicenseStatus());
            }
            return new SimpleStringProperty(status);
        });

        table.getColumns().addAll(List.of(usernameCol, fullNameCol, emailCol, adminCol, permissionsCol,
                licenseTypeCol, expiryCol, statusCol));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void refresh() {
        statusLabel.setText("Loading...");
        Task<List<UserDto>> task = new Task<>() {
            @Override
            protected List<UserDto> call() {
                return apiClient.listUsers();
            }
        };
        task.setOnSucceeded(e -> {
            users.setAll(task.getValue());
            statusLabel.setText(users.size() + " user(s)");
        });
        task.setOnFailed(e -> showError(task.getException()));
        new Thread(task, "aulm-refresh").start();
    }

    private void onAdd() {
        new UserFormDialog(stage, null).showAndWaitForCreate().ifPresent(payload -> runAsync(
                () -> apiClient.createUser(payload),
                created -> {
                    statusLabel.setText("Created user " + created.getUsername());
                    refresh();
                }));
    }

    private void onEdit() {
        UserDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        new UserFormDialog(stage, selected).showAndWaitForUpdate().ifPresent(payload -> runAsync(
                () -> apiClient.updateUser(selected.getId(), payload),
                updated -> {
                    statusLabel.setText("Updated user " + updated.getUsername());
                    refresh();
                }));
    }

    private void onDelete() {
        UserDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user '" + selected.getUsername() + "' and revoke their license? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> runAsync(
                () -> {
                    apiClient.deleteUser(selected.getId());
                    return null;
                },
                v -> {
                    statusLabel.setText("Deleted user " + selected.getUsername());
                    refresh();
                }));
    }

    private void onRenew() {
        UserDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        new RenewLicenseDialog(stage, selected).showAndWait().ifPresent(payload -> runAsync(
                () -> apiClient.renewLicense(selected.getId(), payload),
                updated -> {
                    statusLabel.setText("Updated license for " + updated.getUsername());
                    refresh();
                }));
    }

    private <T> void runAsync(java.util.concurrent.Callable<T> action, java.util.function.Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> showError(task.getException()));
        new Thread(task, "aulm-action").start();
    }

    private void showError(Throwable ex) {
        Platform.runLater(() -> {
            statusLabel.setText("Error");
            Alert alert = new Alert(Alert.AlertType.ERROR, ex != null ? ex.getMessage() : "Unknown error", ButtonType.OK);
            alert.showAndWait();
        });
    }
}
