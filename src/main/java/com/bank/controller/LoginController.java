package com.bank.controller;

import com.bank.Main;
import com.bank.model.Admin;
import com.bank.service.BankService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink registerLink;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        statusLabel.setText("");
    }
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }
        loginButton.setDisable(true);
        statusLabel.setText("Signing in...");

        Task<Admin> loginTask = new Task<>() {
            @Override
            protected Admin call() {
                return BankService.getInstance().authenticateAdmin(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            Admin a = loginTask.getValue();
            if (a == null) {
                statusLabel.setText("Invalid username or password.");
                return;
            }
            try {
                DashboardController.setCurrentAdmin(a);
                Main.switchScene("/fxml/Dashboard.fxml", "Bank Management System - Dashboard");
            } catch (IOException ex) {
                statusLabel.setText("Failed to load dashboard: " + ex.getMessage());
            }
        });

        loginTask.setOnFailed(e -> {
            loginButton.setDisable(false);
            statusLabel.setText("Login error: " + loginTask.getException().getMessage());
        });

        Thread t = new Thread(loginTask, "login-task");
        t.setDaemon(true);
        t.start();
    }
    @FXML
    private void handleGoToRegister() {
        try {
            Main.switchScene("/fxml/Register.fxml", "Bank Management System - Admin Registration");
        } catch (IOException e) {
            statusLabel.setText("Failed to load register screen.");
        }
    }
}
