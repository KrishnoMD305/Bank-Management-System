package com.bank.model;

/**
 * Types of bank accounts supported by the system. Savings accounts accrue
 * interest via the background scheduler in BankService; checking accounts
 * do not.
 */
public enum AccountType {
    SAVINGS(0.03),   // 3% annual interest
    CHECKING(0.0);   // no interest

    private final double annualInterestRate;

    AccountType(double annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public double getInterestRate() {
        return annualInterestRate;
    }
}
