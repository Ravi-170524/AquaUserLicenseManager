package com.vassarlabs.aulm.ui.view;

import com.vassarlabs.aulm.ui.model.CreateUserPayload;
import com.vassarlabs.aulm.ui.model.UpdateUserPayload;
import com.vassarlabs.aulm.ui.model.UserDto;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** Modal form used both to add a new user and to edit an existing one. */
public class UserFormDialog {

    private final Stage dialogStage;
    private final boolean editMode;
    private boolean confirmed = false;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField fullNameField = new TextField();
    private final TextField emailField = new TextField();
    private final CheckBox adminCheckBox = new CheckBox("Admin (can manage users in this tool)");
    private final CheckBox enabledCheckBox = new CheckBox("Enabled");
    private final CheckBox accessCheckBox = new CheckBox("Access");
    private final CheckBox modifyCheckBox = new CheckBox("Modify");
    private final CheckBox approveCheckBox = new CheckBox("Approve");
    private final ComboBox<String> licenseTypeCombo =
            new ComboBox<>(FXCollections.observableArrayList("TRIAL", "STANDARD", "PREMIUM", "ADMIN"));
    private final DatePicker expiryDatePicker = new DatePicker();
    private final CheckBox neverExpiresCheckBox = new CheckBox("Never expires");
    private final Label errorLabel = new Label();

    public UserFormDialog(Stage owner, UserDto existing) {
        this.editMode = existing != null;

        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(editMode ? "Edit User" : "Add User");
        dialogStage.setResizable(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;
        grid.addRow(row++, new Label("Username"), usernameField);
        grid.addRow(row++, new Label(editMode ? "New password (leave blank to keep)" : "Password"), passwordField);
        grid.addRow(row++, new Label("Full name"), fullNameField);
        grid.addRow(row++, new Label("Email"), emailField);
        grid.addRow(row++, new Label("Permissions"), new HBox(12, accessCheckBox, modifyCheckBox, approveCheckBox));
        grid.addRow(row++, new Label(""), adminCheckBox);
        if (editMode) {
            grid.addRow(row++, new Label(""), enabledCheckBox);
        } else {
            grid.addRow(row++, new Label("License type"), licenseTypeCombo);
            grid.addRow(row++, new Label("Expiry date"), new HBox(10, expiryDatePicker, neverExpiresCheckBox));
        }

        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        grid.add(errorLabel, 0, row++, 2, 1);

        if (editMode) {
            usernameField.setText(existing.getUsername());
            usernameField.setDisable(true);
            fullNameField.setText(existing.getFullName());
            emailField.setText(existing.getEmail());
            adminCheckBox.setSelected(existing.isAdmin());
            enabledCheckBox.setSelected(existing.isEnabled());
            Set<String> permissions = existing.getPermissions();
            if (permissions != null) {
                accessCheckBox.setSelected(permissions.contains("ACCESS"));
                modifyCheckBox.setSelected(permissions.contains("MODIFY"));
                approveCheckBox.setSelected(permissions.contains("APPROVE"));
            }
        } else {
            licenseTypeCombo.getSelectionModel().select("STANDARD");
            expiryDatePicker.setValue(LocalDate.now().plusYears(1));
        }

        neverExpiresCheckBox.selectedProperty().addListener((obs, old, isSelected) -> expiryDatePicker.setDisable(isSelected));

        Button saveButton = new Button(editMode ? "Save" : "Create");
        Button cancelButton = new Button("Cancel");
        saveButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);
        saveButton.setOnAction(e -> onSave());
        cancelButton.setOnAction(e -> dialogStage.close());

        HBox buttons = new HBox(10, saveButton, cancelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttons, 0, row, 2, 1);

        dialogStage.setScene(new Scene(grid));
    }

    private void onSave() {
        if (usernameField.getText().isBlank()) {
            errorLabel.setText("Username is required.");
            return;
        }
        if (!editMode && passwordField.getText().isBlank()) {
            errorLabel.setText("Password is required.");
            return;
        }
        confirmed = true;
        dialogStage.close();
    }

    private Set<String> collectPermissions() {
        Set<String> permissions = new LinkedHashSet<>();
        if (accessCheckBox.isSelected()) permissions.add("ACCESS");
        if (modifyCheckBox.isSelected()) permissions.add("MODIFY");
        if (approveCheckBox.isSelected()) permissions.add("APPROVE");
        return permissions;
    }

    public Optional<CreateUserPayload> showAndWaitForCreate() {
        dialogStage.showAndWait();
        if (!confirmed) {
            return Optional.empty();
        }
        LocalDate expiry = neverExpiresCheckBox.isSelected() ? null : expiryDatePicker.getValue();
        return Optional.of(new CreateUserPayload(
                usernameField.getText().trim(),
                passwordField.getText(),
                fullNameField.getText(),
                emailField.getText(),
                adminCheckBox.isSelected(),
                collectPermissions(),
                licenseTypeCombo.getValue(),
                expiry
        ));
    }

    public Optional<UpdateUserPayload> showAndWaitForUpdate() {
        dialogStage.showAndWait();
        if (!confirmed) {
            return Optional.empty();
        }
        String newPassword = passwordField.getText().isBlank() ? null : passwordField.getText();
        return Optional.of(new UpdateUserPayload(
                fullNameField.getText(),
                emailField.getText(),
                enabledCheckBox.isSelected(),
                adminCheckBox.isSelected(),
                collectPermissions(),
                newPassword
        ));
    }
}
