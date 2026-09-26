package com.bank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class Transaction {
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
}
