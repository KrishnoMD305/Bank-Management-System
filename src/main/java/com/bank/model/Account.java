package com.bank.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A bank account. This is the core thread-safety boundary of the whole
 * application: every balance-mutating operation acquires this account's
 * own {@link ReentrantLock} before touching {@code balance}, so concurrent
 * deposits/withdrawals/transfers from multiple worker threads can never
 * race on the same account.
 *
 * The lock is re-entrant, which matters for transfers: BankService locks
 * both accounts involved in a transfer (in a fixed, deadlock-safe order)
 * and then calls methods here that lock again internally - safe because a
 * ReentrantLock allows the same thread to re-acquire it.
 *
 * Transaction history is stored in a CopyOnWriteArrayList, which is safe
 * for the read-heavy/low-write-concurrency pattern of a UI polling history
 * while background threads occasionally append to it.
 */
public class Account {

    private final String accountNumber;
    private final String ownerUsername;
    private final AccountType type;
    private volatile double balance;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Transaction> transactionHistory = new CopyOnWriteArrayList<>();
    private final LocalDateTime createdAt;

    public Account(String accountNumber, String ownerUsername, AccountType type, double initialBalance) {
        this.accountNumber = accountNumber;
        this.ownerUsername = ownerUsername;
        this.type = type;
        this.balance = initialBalance;
        this.createdAt = LocalDateTime.now();
    }

    /** Exposed so BankService can perform ordered dual-locking for transfers. */
    public ReentrantLock getLock() {
        return lock;
    }

    /** Thread-safe deposit. */
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

    /** Thread-safe withdraw. Returns null if funds are insufficient. */
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

    /** Debit side of a transfer. Returns null if funds are insufficient. */
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

    /** Credit side of a transfer. */
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

    /** Applies interest for one accrual period at the given periodic rate. */
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

    public String getOwnerUsername() {
        return ownerUsername;
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
}
