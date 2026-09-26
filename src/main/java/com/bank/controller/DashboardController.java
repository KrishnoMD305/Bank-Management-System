package com.bank.controller;


import com.bank.model.Account;
import com.bank.model.Admin;
import com.bank.model.Transaction;
import com.bank.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DashboardController{
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

}
