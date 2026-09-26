package com.bank.model;

public enum AccountType {
    SAVINGS("Savings", 0.03),   
    CURRENT("Current", 0.0),           
    FIXED_DEPOSIT("Fixed Deposit", 0.06), 
    STUDENT("Student Account", 0.015);

    private final String label;
    private final double annualInterestRate;

    AccountType(String label, double annualInterestRate) {
        this.label = label;
        this.annualInterestRate = annualInterestRate;
    }

}
