package com.bank.controller;

import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatementController {

    @FXML
    private Label accountIdLabel;
    @FXML
    private Label accountTypeLabel;
    @FXML
    private Label customerLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label generatedAtLabel;
    @FXML
    private Label savedFileLabel;
    @FXML
    private TableView<Transaction> statementTable;
    @FXML
    private TableColumn<Transaction, String> colTime;
    @FXML
    private TableColumn<Transaction, String> colType;
    @FXML
    private TableColumn<Transaction, Double> colAmount;
    @FXML
    private TableColumn<Transaction, Double> colBalanceAfter;
    @FXML
    private TableColumn<Transaction, String> colDescription;
    private final ObservableList<Transaction> transactionData = FXCollections.observableArrayList();
    @FXML
    private void initialize() {
        colTime.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFormattedTimestamp()));
        colType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType().toString()));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colBalanceAfter.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        statementTable.setItems(transactionData);
    }

    public void setStatement(Account account, User owner, Path savedFile) {
        accountIdLabel.setText("Account #: " + account.getAccountNumber());
        accountTypeLabel.setText("Type: " + account.getType());
        customerLabel.setText(owner != null ? "Customer: " + owner.getFullName() + " (" + owner.getUserId() + ")" : "Customer: " + account.getOwnerUserId());
        balanceLabel.setText(String.format("Current Balance: $%.2f", account.getBalance()));
        generatedAtLabel.setText("Generated: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        savedFileLabel.setText("Saved to: " + savedFile.toAbsolutePath());

        List<Transaction> reversed = new ArrayList<>(account.getTransactionHistory());
        Collections.reverse(reversed);
        transactionData.setAll(reversed);
    }
}
