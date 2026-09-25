package com.bank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An immutable record of a single ledger entry on an account. Immutability
 * means Transaction instances can be safely shared/read across threads
 * without any synchronization once constructed.
 *
 * IDs are generated from a shared AtomicLong so that concurrently created
 * transactions (from multiple worker threads) never collide.
 */
public class Transaction {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final long id;
    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final String description;
    private final LocalDateTime timestamp;

    public Transaction(String accountNumber, TransactionType type, double amount,
                        double balanceAfter, String description) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = LocalDateTime.now();
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
        return String.format("[%s] %s %s $%.2f -> Balance: $%.2f (%s)",
                getFormattedTimestamp(), accountNumber, type, amount, balanceAfter, description);
    }
}
