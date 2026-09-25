package com.bank.model;

/**
 * The kind of ledger entry a Transaction represents.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER_IN,
    TRANSFER_OUT,
    INTEREST
}
