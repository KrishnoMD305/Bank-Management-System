package com.bank.controller;

import com.bank.Main;
import com.bank.model.Customer;
import com.bank.service.BankService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controller for the login screen. Authentication runs on a background
 * daemon thread via a javafx.concurrent.Task so the UI never freezes,
 * even though BankService's password hashing/lookup is cheap here -
 * this pattern is what you'd want if authentication involved network
 * or database latency.
 */
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
        statusLabel.setText("Default admin login: admin / admin123");
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

        Task<Customer> loginTask = new Task<>() {
            @Override
            protected Customer call() {
                return BankService.getInstance().authenticate(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            Customer c = loginTask.getValue();
            if (c == null) {
                statusLabel.setText("Invalid username or password.");
                return;
            }
            try {
                DashboardController.setCurrentUser(c);
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
