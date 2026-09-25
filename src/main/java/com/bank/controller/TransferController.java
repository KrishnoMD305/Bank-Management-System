package com.bank.controller;

import com.bank.service.BankService;
import com.bank.service.TransactionResult;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Controller for the modal transfer dialog. The actual fund movement is
 * delegated to BankService.submitTransfer, which performs deadlock-safe
 * ordered locking of both accounts on a background thread; this controller
 * just waits on the returned Future inside a Task so the dialog's UI
 * thread stays responsive.
 */
public class TransferController {

    @FXML
    private Label fromAccountLabel;
    @FXML
    private TextField toAccountField;
    @FXML
    private TextField amountField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;

    private String fromAccountNumber;
    private String resultMessage = "";

    public void setFromAccount(String accountNumber) {
        this.fromAccountNumber = accountNumber;
        fromAccountLabel.setText("From: " + accountNumber);
    }

    public String getResultMessage() {
        return resultMessage;
    }

    @FXML
    private void handleConfirm() {
        String toAcc = toAccountField.getText().trim();
        String amountText = amountField.getText().trim();

        if (toAcc.isEmpty() || amountText.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        final double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid amount.");
            return;
        }

        confirmButton.setDisable(true);
        statusLabel.setText("Processing transfer...");

        Future<TransactionResult> future = BankService.getInstance().submitTransfer(fromAccountNumber, toAcc, amount);

        Task<TransactionResult> task = new Task<>() {
            @Override
            protected TransactionResult call() throws ExecutionException, InterruptedException {
                return future.get();
            }
        };

        task.setOnSucceeded(e -> {
            TransactionResult result = task.getValue();
            confirmButton.setDisable(false);
            if (result.isSuccess()) {
                resultMessage = "Transfer successful.";
                closeWindow();
            } else {
                statusLabel.setText("Failed: " + result.getMessage());
                resultMessage = "Transfer failed: " + result.getMessage();
            }
        });

        task.setOnFailed(e -> {
            confirmButton.setDisable(false);
            statusLabel.setText("Error: " + task.getException().getMessage());
        });

        Thread t = new Thread(task, "transfer-task");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleCancel() {
        resultMessage = "Transfer cancelled.";
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
