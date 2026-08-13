package com.vassarlabs.aulm.ui.view;

import com.vassarlabs.aulm.ui.model.RenewLicensePayload;
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
import java.util.Optional;

public class RenewLicenseDialog {

    private final Stage dialogStage;
    private boolean confirmed = false;

    private final ComboBox<String> licenseTypeCombo =
            new ComboBox<>(FXCollections.observableArrayList("TRIAL", "STANDARD", "PREMIUM", "ADMIN"));
    private final DatePicker expiryDatePicker = new DatePicker();
    private final CheckBox neverExpiresCheckBox = new CheckBox("Never expires");
    private final CheckBox revokeCheckBox = new CheckBox("Revoke this license (blocks login immediately)");
    private final Label errorLabel = new Label();

    public RenewLicenseDialog(Stage owner, UserDto user) {
        dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Renew License - " + user.getUsername());
        dialogStage.setResizable(false);

        licenseTypeCombo.getSelectionModel().select(
                user.getLicenseType() != null ? user.getLicenseType() : "STANDARD");
        if (user.getExpiryDate() == null) {
            neverExpiresCheckBox.setSelected(true);
            expiryDatePicker.setDisable(true);
        } else {
            expiryDatePicker.setValue(user.getExpiryDate());
        }
        neverExpiresCheckBox.selectedProperty().addListener((obs, old, isSelected) -> expiryDatePicker.setDisable(isSelected));
        restrictToTodayOrLater(expiryDatePicker);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;
        grid.addRow(row++, new Label("License type"), licenseTypeCombo);
        grid.addRow(row++, new Label("Expiry date"), new HBox(10, expiryDatePicker, neverExpiresCheckBox));
        grid.add(revokeCheckBox, 0, row++, 2, 1);

        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        grid.add(errorLabel, 0, row++, 2, 1);

        Button saveButton = new Button("Save");
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
        if (!neverExpiresCheckBox.isSelected()) {
            LocalDate expiry = expiryDatePicker.getValue();
            if (expiry == null || expiry.isBefore(LocalDate.now())) {
                errorLabel.setText("Expiry date cannot be in the past.");
                return;
            }
        }
        confirmed = true;
        dialogStage.close();
    }

    /** Greys out and blocks selection of any day before today. */
    private static void restrictToTodayOrLater(DatePicker picker) {
        picker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;");
                }
            }
        });
    }

    public Optional<RenewLicensePayload> showAndWait() {
        dialogStage.showAndWait();
        if (!confirmed) {
            return Optional.empty();
        }
        LocalDate expiry = neverExpiresCheckBox.isSelected() ? null : expiryDatePicker.getValue();
        return Optional.of(new RenewLicensePayload(licenseTypeCombo.getValue(), expiry, revokeCheckBox.isSelected()));
    }
}
