package com.bank.controller;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.IdType;
import com.bank.service.BankService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class OpenAccountController {
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<IdType> idTypeBox;
    @FXML
    private TextField idNumberField;
    @FXML
    private ComboBox<AccountType> accountTypeBox;
    @FXML
    private TextField depositField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;

    private String resultMessage = "";
    public String getResultMessage() {
        return resultMessage;
    }

    @FXML
    private void initialize() {
        idTypeBox.getItems().addAll(IdType.values());
        idTypeBox.getSelectionModel().selectFirst();

        accountTypeBox.getItems().addAll(AccountType.values());
        accountTypeBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleConfirm() {
        String fullName = fullNameField.getText().trim();
        String phone = phoneField.getText().trim();
        IdType idType = idTypeBox.getValue();
        String idNumber = idNumberField.getText().trim();
        AccountType accountType = accountTypeBox.getValue();
        String depositText = depositField.getText().trim();

        if (fullName.isEmpty() || phone.isEmpty() || idNumber.isEmpty() || accountType == null || idType == null) {
            statusLabel.setText("Please fill in all required fields.");
            return;
        }

        final double deposit;
        try {
            deposit = depositText.isEmpty() ? 0 : Double.parseDouble(depositText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid initial deposit amount.");
            return;
        }
        if (deposit < 0) {
            statusLabel.setText("Initial deposit cannot be negative.");
            return;
        }

        confirmButton.setDisable(true);
        statusLabel.setText("Opening account...");

        Task<Account> task = new Task<>() {
            @Override
            protected Account call() {
                return BankService.getInstance()
                        .openAccountForUser(fullName, phone, idType, idNumber, accountType, deposit);
            }
        };

        task.setOnSucceeded(e -> {
            Account acc = task.getValue();
            resultMessage = "Account opened successfully. Account Number: " + acc.getAccountNumber();
            closeWindow();
        });

        task.setOnFailed(e -> {
            confirmButton.setDisable(false);
            String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
            statusLabel.setText("Error: " + msg);
        });

        Thread t = new Thread(task, "open-account-task");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleCancel() {
        resultMessage = "Account creation cancelled.";
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
