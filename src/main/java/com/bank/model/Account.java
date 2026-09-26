package com.bank.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Account implements Serializable {
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

    public Transaction deposit(double amount, String description) {
        lock.lock();
        try {
            balance += amount;
            Transaction t = new Transaction(accountNumber, TransactionType.DEPOSIT, amount, balance, description);
            transactionHistory.add(t);
            return t;
        } finally {
            lock.unlock();
        }
    }

    public Transaction withdraw(double amount, String description) {
        lock.lock();
        try {
            if (amount > balance) {
                return null;
            }
            balance -= amount;
            Transaction t = new Transaction(accountNumber, TransactionType.WITHDRAW, amount, balance, description);
            transactionHistory.add(t);
            return t;
        } finally {
            lock.unlock();
        }
    }

    public Transaction sendTransfer(double amount, String description) {
        lock.lock();
        try {
            if (amount > balance) {
                return null;
            }
            balance -= amount;
            Transaction t = new Transaction(accountNumber, TransactionType.TRANSFER_OUT, amount, balance, description);
            transactionHistory.add(t);
            return t;
        } finally {
            lock.unlock();
        }
    }

    public Transaction receiveTransfer(double amount, String description) {
        lock.lock();
        try {
            balance += amount;
            Transaction t = new Transaction(accountNumber, TransactionType.TRANSFER_IN, amount, balance, description);
            transactionHistory.add(t);
            return t;
        } finally {
            lock.unlock();
        }
    }

    public Transaction applyInterest(double periodicRate) {
        lock.lock();
        try {
            double interest = balance * periodicRate;
            if (interest <= 0) {
                return null;
            }
            balance += interest;
            Transaction t = new Transaction(accountNumber, TransactionType.INTEREST, interest, balance, "Interest credited");
            transactionHistory.add(t);
            return t;
        } finally {
            lock.unlock();
        }
    }

    public double getBalance() {
        return balance;
    }
    public String getAccountNumber() {
        return accountNumber;
    }
    public String getOwnerUserId() {
        return ownerUserId;
    }
    public AccountType getType() {
        return type;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public void restoreTransaction(Transaction transaction) {
        transactionHistory.add(transaction);
    }
}
