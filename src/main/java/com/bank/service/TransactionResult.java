package com.bank.service;
import com.bank.model.Transaction;

public class TransactionResult {
    private final boolean success;
    private final String message;
    private final Transaction transaction;

    private TransactionResult(boolean success, String message, Transaction transaction) {
        this.success = success;
        this.message = message;
        this.transaction = transaction;
    }

    public static TransactionResult success(Transaction t) {
        return new TransactionResult(true, "OK", t);
    }
    public static TransactionResult failure(String message) {
        return new TransactionResult(false, message, null);
    }
    public boolean isSuccess() {
        return success;
    }
    public String getMessage() {
        return message;
    }
    public Transaction getTransaction() {
        return transaction;
    }
}
