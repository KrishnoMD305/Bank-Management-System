package com.bank.controller;

import com.bank.Main;
import com.bank.model.Admin;
import com.bank.service.BankService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RegisterController {
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField idField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button registerButton;
    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String idNumber = idField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || phone.isEmpty()
                || idNumber.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all required fields.");
            return;
        }
        if (!password.equals(confirm)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }
        registerButton.setDisable(true);
        statusLabel.setText("Creating admin account...");
        Task<Admin> task = new Task<>() {
            @Override
            protected Admin call() {
                return BankService.getInstance().registerAdmin(username, password, fullName, email, phone, idNumber);
            }
        };
        task.setOnSucceeded(e -> {
            registerButton.setDisable(false);
            statusLabel.setText("Admin account created! Redirecting to login...");
            try {
                Main.switchScene("/fxml/Login.fxml", "Bank Management System - Admin Login");
            } catch (IOException ex) {
                statusLabel.setText("Registered, but failed to load login screen.");
            }
        });
        task.setOnFailed(e -> {
            registerButton.setDisable(false);
            statusLabel.setText("Error: " + task.getException().getMessage());
        });
        Thread t = new Thread(task, "register-task");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Main.switchScene("/fxml/Login.fxml", "Bank Management System - Admin Login");
        } catch (IOException e) {
            statusLabel.setText("Failed to load login screen.");
        }
    }
}
