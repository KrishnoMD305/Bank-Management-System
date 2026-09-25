package com.bank.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A registered bank user (customer or admin). Holds only account NUMBERS,
 * not Account references - the BankService's ConcurrentHashMap is the
 * single source of truth for account objects.
 */
public class Customer {

    private final String username;
    private volatile String passwordHash;
    private final String fullName;
    private final String email;
    private final String phoneNumber;
    private final String idNumber;
    private final boolean admin;
    private final List<String> accountNumbers = new CopyOnWriteArrayList<>();

    public Customer(String username, String passwordHash, String fullName, String email,
                     String phoneNumber, String idNumber, boolean admin) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.idNumber = idNumber;
        this.admin = admin;
    }

    public void addAccountNumber(String accountNumber) {
        accountNumbers.add(accountNumber);
    }

    public List<String> getAccountNumbers() {
        return accountNumbers;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public boolean isAdmin() {
        return admin;
    }
}
