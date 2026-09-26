package com.bank.controller;

import com.bank.Main;
import com.bank.model.Account;
import com.bank.model.Admin;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.service.BankService;
import com.bank.service.TransactionResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DashboardController {

    private static Admin currentAdmin;
    public static void setCurrentAdmin(Admin a) {
        currentAdmin = a;
    }
    public static Admin getCurrentAdmin() {
        return currentAdmin;
    }

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label totalBalanceLabel;

    @FXML
    private TableView<Account> accountsTable;
    @FXML
    private TableColumn<Account, String> colAccNo;
    @FXML
    private TableColumn<Account, String> colType;
    @FXML
    private TableColumn<Account, Double> colBalance;
    @FXML
    private TableColumn<Account, String> colOwner;

    @FXML
    private TableView<Transaction> historyTable;
    @FXML
    private TableColumn<Transaction, String> colTime;
    @FXML
    private TableColumn<Transaction, String> colTxnType;
    @FXML
    private TableColumn<Transaction, Double> colAmount;
    @FXML
    private TableColumn<Transaction, Double> colBalanceAfter;
    @FXML
    private TableColumn<Transaction, String> colDescription;

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> colUserId;
    @FXML
    private TableColumn<User, String> colUserName;
    @FXML
    private TableColumn<User, String> colUserPhone;
    @FXML
    private TableColumn<User, String> colUserIdType;
    @FXML
    private TableColumn<User, String> colUserIdNumber;
    @FXML
    private TableColumn<User, Number> colUserAccountCount;

    @FXML
    private Label statusLabel;

    private final ObservableList<Account> accountData = FXCollections.observableArrayList();
    private final ObservableList<Transaction> historyData = FXCollections.observableArrayList();
    private final ObservableList<User> userData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        welcomeLabel.setText("Welcome, " + currentAdmin.getFullName() + "  (Admin)");

        colAccNo.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));
        colType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType().toString()));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colOwner.setCellValueFactory(cd -> new SimpleStringProperty(ownerDisplayName(cd.getValue())));
        accountsTable.setItems(accountData);

        colTime.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFormattedTimestamp()));
        colTxnType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType().toString()));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colBalanceAfter.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        historyTable.setItems(historyData);

        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colUserIdType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getIdType().toString()));
        colUserIdNumber.setCellValueFactory(new PropertyValueFactory<>("idNumber"));
        colUserAccountCount.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getAccountNumbers().size()));
        usersTable.setItems(userData);

        accountsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> loadHistory(selected));

        BankService.getInstance().addListener(this::refreshAll);
        refreshAll();
    }

    private String ownerDisplayName(Account account) {
        User owner = BankService.getInstance().getUser(account.getOwnerUserId());
        if (owner == null) {
            return account.getOwnerUserId();
        }
        return owner.getFullName() + " (" + owner.getUserId() + ")";
    }

    private void refreshAll() {
        refreshAccounts();
        refreshUsers();
    }

    private void refreshAccounts() {
        Account selected = accountsTable.getSelectionModel().getSelectedItem();
        String selectedAccNo = selected == null ? null : selected.getAccountNumber();

        List<Account> accounts = BankService.getInstance().getAllAccounts();
        accountData.setAll(accounts);

        double total = accounts.stream().mapToDouble(Account::getBalance).sum();
        totalBalanceLabel.setText(String.format("Total Balance: ৳ %.2f", total));

        if (selectedAccNo != null) {
            accounts.stream()
                    .filter(a -> a.getAccountNumber().equals(selectedAccNo))
                    .findFirst()
                    .ifPresentOrElse(a -> {
                        accountsTable.getSelectionModel().select(a);
                        loadHistory(a);
                    }, () -> historyData.clear());
        }
    }

    private void refreshUsers() {
        userData.setAll(BankService.getInstance().getAllUsers());
    }

    private void loadHistory(Account account) {
        if (account == null) {
            historyData.clear();
            return;
        }
        List<Transaction> reversed = new ArrayList<>(account.getTransactionHistory());
        Collections.reverse(reversed);
        historyData.setAll(reversed);
    }

    @FXML
    private void handleOpenAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/OpenAccountDialog.fxml"));
            Parent root = loader.load();
            OpenAccountController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Open New Account");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            statusLabel.setText(controller.getResultMessage());
        } catch (IOException e) {
            statusLabel.setText("Failed to open the account-creation dialog.");
        }
    }

    @FXML
    private void handleDeposit() {
        Account acc = accountsTable.getSelectionModel().getSelectedItem();
        if (acc == null) {
            statusLabel.setText("Select an account first.");
            return;
        }
        showAmountDialog("Deposit",
                amount -> submitAndReport(BankService.getInstance().submitDeposit(acc.getAccountNumber(), amount)));
    }

    @FXML
    private void handleWithdraw() {
        Account acc = accountsTable.getSelectionModel().getSelectedItem();
        if (acc == null) {
            statusLabel.setText("Select an account first.");
            return;
        }
        showAmountDialog("Withdraw",
                amount -> submitAndReport(BankService.getInstance().submitWithdraw(acc.getAccountNumber(), amount)));
    }

    @FXML
    private void handleTransfer() {
        Account acc = accountsTable.getSelectionModel().getSelectedItem();
        if (acc == null) {
            statusLabel.setText("Select an account first.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/TransferDialog.fxml"));
            Parent root = loader.load();
            TransferController controller = loader.getController();
            controller.setFromAccount(acc.getAccountNumber());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Transfer Funds");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            statusLabel.setText(controller.getResultMessage());
        } catch (IOException e) {
            statusLabel.setText("Failed to open transfer dialog.");
        }
    }

    @FXML
    private void handleGenerateStatement() {
        Account acc = accountsTable.getSelectionModel().getSelectedItem();
        if (acc == null) {
            statusLabel.setText("Select an account first.");
            return;
        }

        statusLabel.setText("Generating statement...");
        Task<java.nio.file.Path> task = new Task<>() {
            @Override
            protected java.nio.file.Path call() throws IOException {
                return com.bank.service.StatementGenerator.generate(acc, BankService.getInstance().getUser(acc.getOwnerUserId()));
            }
        };
        task.setOnSucceeded(e -> {
            java.nio.file.Path savedFile = task.getValue();
            statusLabel.setText("Statement saved to " + savedFile);
            openStatementWindow(acc, savedFile);
        });
        task.setOnFailed(e -> statusLabel.setText("Failed to generate statement: " + task.getException().getMessage()));
        runBackground(task);
    }

    private void openStatementWindow(Account account, java.nio.file.Path savedFile) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/StatementView.fxml"));
            Parent root = loader.load();
            StatementController controller = loader.getController();
            controller.setStatement(account, BankService.getInstance().getUser(account.getOwnerUserId()), savedFile);

            Scene scene = new Scene(root);
            var cssUrl = Main.class.getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            Stage stage = new Stage();
            stage.setTitle("Bank Statement - " + account.getAccountNumber());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("Statement saved, but failed to open the statement window.");
        }
    }

    @FXML
    private void handleRefresh() {
        refreshAll();
        statusLabel.setText("Refreshed.");
    }

    @FXML
    private void handleLogout() {
        currentAdmin = null;
        try {
            Main.switchScene("/fxml/Login.fxml", "Bank Management System - Admin Login");
        } catch (IOException e) {
            statusLabel.setText("Failed to log out.");
        }
    }

    private interface AmountAction {
        void run(double amount);
    }

    private void showAmountDialog(String title, AmountAction action) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(title + " Amount");
        dialog.setContentText("Amount (৳):");
        dialog.showAndWait().ifPresent(text -> {
            try {
                double amount = Double.parseDouble(text.trim());
                action.run(amount);
            } catch (NumberFormatException e) {
                statusLabel.setText("Invalid amount.");
            }
        });
    }

    private void submitAndReport(Future<TransactionResult> future) {
        statusLabel.setText("Processing...");
        Task<TransactionResult> task = new Task<>() {
            @Override
            protected TransactionResult call() throws ExecutionException, InterruptedException {
                return future.get();
            }
        };
        task.setOnSucceeded(e -> {
            TransactionResult result = task.getValue();
            statusLabel.setText(result.isSuccess() ? "Success!" : "Failed: " + result.getMessage());
        });
        task.setOnFailed(e -> statusLabel.setText("Error: " + task.getException().getMessage()));
        runBackground(task);
    }

    private void runBackground(Task<?> task) {
        Thread t = new Thread(task, "dashboard-task");
        t.setDaemon(true);
        t.start();
    }
}
