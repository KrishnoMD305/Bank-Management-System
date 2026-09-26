package com.bank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String userId;
    private final String fullName;
    private final String phoneNumber;
    private final IdType idType;
    private final String idNumber;
    private final LocalDateTime createdAt;
    private final List<String> accountNumbers = new CopyOnWriteArrayList<>();

    public User(String userId, String fullName, String phoneNumber, IdType idType, String idNumber) {
        this(userId, fullName, phoneNumber, idType, idNumber, LocalDateTime.now());
    }

    public User(String userId, String fullName, String phoneNumber, IdType idType, String idNumber, LocalDateTime createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.idType = idType;
        this.idNumber = idNumber;
        this.createdAt = createdAt;
    }

    public void addAccountNumber(String accountNumber) {
        accountNumbers.add(accountNumber);
    }
    public List<String> getAccountNumbers() {
        return accountNumbers;
    }
    public String getUserId() {
        return userId;
    }
    public String getFullName() {
        return fullName;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public IdType getIdType() {
        return idType;
    }
    public String getIdNumber() {
        return idNumber;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
