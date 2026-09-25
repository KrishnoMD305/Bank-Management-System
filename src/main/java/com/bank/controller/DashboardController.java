package com.bank.controller;


import com.bank.model.Admin;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

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
    
}
