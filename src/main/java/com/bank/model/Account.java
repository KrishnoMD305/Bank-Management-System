package com.bank.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    private static final long serialVersionUID = 1L;
    private final String accountNumber;
    private final String ownerUserId;
    private final AccountType type;
    private volatile double balance;
    private transient ReentrantLock lock = new ReentrantLock();
    private final List<Transaction> transactionHistory = new CopyOnWriteArrayList<>();
    private final LocalDateTime createdAt;

    public Account(String accountNumber, String ownerUserId, AccountType type, double initialBalance) {
        this(accountNumber, ownerUserId, type, initialBalance, LocalDateTime.now());
    }

    public Account(String accountNumber, String ownerUserId, AccountType type, double initialBalance, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.ownerUserId = ownerUserId;
        this.type = type;
        this.balance = initialBalance;
        this.createdAt = createdAt;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        lock = new ReentrantLock();
    }
    
    public ReentrantLock getLock() {
        return lock;
    }
}
