package com.bank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final long id;
    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final String description;
    private final LocalDateTime timestamp;

    public Transaction(String accountNumber, TransactionType type, double amount, double balanceAfter, String description) {
        this(ID_GENERATOR.getAndIncrement(), accountNumber, type, amount, balanceAfter, description, LocalDateTime.now());
    }

    public Transaction(long id, String accountNumber, TransactionType type, double amount, double balanceAfter, String description, LocalDateTime timestamp) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = timestamp;
    }

    public static void ensureNextId(long nextId) {
        ID_GENERATOR.updateAndGet(current -> Math.max(current, nextId));
    }
    public long getId() {
        return id;
    }
    public String getAccountNumber() {
        return accountNumber;
    }
    public TransactionType getType() {
        return type;
    }
    public double getAmount() {
        return amount;
    }
    public double getBalanceAfter() {
        return balanceAfter;
    }
    public String getDescription() {
        return description;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s $%.2f -> Balance: ৳ %.2f (%s)", getFormattedTimestamp(), accountNumber, type, amount, balanceAfter, description);
    }
}
