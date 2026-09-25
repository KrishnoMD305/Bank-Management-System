package com.bank.model;

public class Admin {
    private static final long serialVersionUID = 1L;

    private final String username;
    private volatile String passwordHash;
    private final String fullName;
    private final String email;
    private final String phoneNumber;
    private final String idNumber;

    public Admin(String username, String fullName, String email, String phoneNumber, String idNumber) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.idNumber = idNumber;
    }
}
