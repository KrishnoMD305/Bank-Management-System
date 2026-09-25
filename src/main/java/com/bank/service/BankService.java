package com.bank.service;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.Customer;
import com.bank.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central, thread-safe banking engine. This is a singleton acting as the
 * single source of truth for all customers and accounts.
 *
 * Concurrency design:
 * <ul>
 *   <li>Customers and Accounts live in ConcurrentHashMaps, so lookups and
 *       inserts from multiple threads never corrupt the maps themselves.</li>
 *   <li>Each Account guards its own balance with its own ReentrantLock
 *       (see {@link Account}) - fine-grained locking instead of one big
 *       global lock, so unrelated accounts can be modified in parallel.</li>
 *   <li>Deposits/withdrawals/transfers are submitted as tasks to a fixed
 *       thread pool ({@code transactionExecutor}) and return a
 *       {@link Future}, so the JavaFX Application Thread never blocks on
 *       banking logic.</li>
 *   <li>Transfers lock both involved accounts in a fixed order (by account
 *       number) to prevent the classic dining-philosophers deadlock that
 *       naive "lock A then lock B" transfer code can produce.</li>
 *   <li>A single-thread {@link ScheduledExecutorService} periodically
 *       accrues interest on savings accounts in the background,
 *       demonstrating a recurring concurrent task independent of user
 *       actions.</li>
 *   <li>UI listeners are notified via {@code Platform.runLater}, marshaling
 *       background-thread state changes safely back onto the JavaFX
 *       Application Thread.</li>
 * </ul>
 */
public class BankService {

    private static final BankService INSTANCE = new BankService();

    private final ConcurrentHashMap<String, Customer> customers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger accountSequence = new AtomicInteger(100_000);

    private final ExecutorService transactionExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "txn-worker");
        t.setDaemon(true);
        return t;
    });

    private final ScheduledExecutorService interestScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "interest-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> interestTask;

    private BankService() {
        seedAdmin();
    }

    public static BankService getInstance() {
        return INSTANCE;
    }

    private void seedAdmin() {
        Customer admin = new Customer("admin", PasswordUtil.hash("admin123"),
                "System Administrator", "admin@bank.com", "N/A", "N/A", true);
        customers.put("admin", admin);
    }

    /** Register a UI callback to be notified (on the FX thread) whenever state changes. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            javafx.application.Platform.runLater(r);
        }
    }

    // ------------------------------------------------------------------
    // Registration / Authentication
    // ------------------------------------------------------------------

    /**
     * Registers a new admin user. This application's sign-up flow is for
     * bank administrators only (see RegisterController) - the "admin" flag
     * is always true for accounts created through registration.
     */
    public synchronized Customer registerAdmin(String username, String password, String fullName,
                                                String email, String phoneNumber, String idNumber) {
        if (customers.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        Customer c = new Customer(username, PasswordUtil.hash(password), fullName, email,
                phoneNumber, idNumber, true);
        customers.put(username, c);
        return c;
    }

    public Customer authenticate(String username, String password) {
        Customer c = customers.get(username);
        if (c == null) {
            return null;
        }
        return c.getPasswordHash().equals(PasswordUtil.hash(password)) ? c : null;
    }

    public boolean isAdmin(String username) {
        Customer c = customers.get(username);
        return c != null && c.isAdmin();
    }

    // ------------------------------------------------------------------
    // Accounts
    // ------------------------------------------------------------------

    public Account openAccount(String username, AccountType type, double initialDeposit) {
        Customer c = customers.get(username);
        if (c == null) {
            throw new IllegalArgumentException("Unknown customer: " + username);
        }
        String accountNumber = "ACC" + accountSequence.incrementAndGet();
        Account account = new Account(accountNumber, username, type, 0);
        accounts.put(accountNumber, account);
        c.addAccountNumber(accountNumber);

        if (initialDeposit > 0) {
            account.deposit(initialDeposit, "Initial deposit");
        }
        notifyListeners();
        return account;
    }

    public List<Account> getAccountsForCustomer(String username) {
        Customer c = customers.get(username);
        if (c == null) {
            return List.of();
        }
        List<Account> result = new ArrayList<>();
        for (String accNo : c.getAccountNumbers()) {
            Account a = accounts.get(accNo);
            if (a != null) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    // ------------------------------------------------------------------
    // Async transaction operations - run on the transactionExecutor pool
    // ------------------------------------------------------------------

    public Future<TransactionResult> submitDeposit(String accountNumber, double amount) {
        return transactionExecutor.submit(() -> {
            Account acc = accounts.get(accountNumber);
            if (acc == null) {
                return TransactionResult.failure("Account not found");
            }
            if (amount <= 0) {
                return TransactionResult.failure("Amount must be positive");
            }
            Transaction t = acc.deposit(amount, "Deposit");
            notifyListeners();
            return TransactionResult.success(t);
        });
    }

    public Future<TransactionResult> submitWithdraw(String accountNumber, double amount) {
        return transactionExecutor.submit(() -> {
            Account acc = accounts.get(accountNumber);
            if (acc == null) {
                return TransactionResult.failure("Account not found");
            }
            if (amount <= 0) {
                return TransactionResult.failure("Amount must be positive");
            }
            Transaction t = acc.withdraw(amount, "Withdrawal");
            if (t == null) {
                return TransactionResult.failure("Insufficient funds");
            }
            notifyListeners();
            return TransactionResult.success(t);
        });
    }

    public Future<TransactionResult> submitTransfer(String fromAccountNumber, String toAccountNumber, double amount) {
        return transactionExecutor.submit(() -> {
            if (fromAccountNumber.equals(toAccountNumber)) {
                return TransactionResult.failure("Cannot transfer to the same account");
            }
            Account from = accounts.get(fromAccountNumber);
            Account to = accounts.get(toAccountNumber);
            if (from == null || to == null) {
                return TransactionResult.failure("Account not found");
            }
            if (amount <= 0) {
                return TransactionResult.failure("Amount must be positive");
            }

            // Lock both accounts in a fixed, globally-consistent order
            // (by account number) so two concurrent transfers going in
            // opposite directions (A->B and B->A) can never deadlock.
            Account first = fromAccountNumber.compareTo(toAccountNumber) < 0 ? from : to;
            Account second = fromAccountNumber.compareTo(toAccountNumber) < 0 ? to : from;

            first.getLock().lock();
            try {
                second.getLock().lock();
                try {
                    Transaction out = from.sendTransfer(amount, "Transfer to " + toAccountNumber);
                    if (out == null) {
                        return TransactionResult.failure("Insufficient funds");
                    }
                    to.receiveTransfer(amount, "Transfer from " + fromAccountNumber);
                    notifyListeners();
                    return TransactionResult.success(out);
                } finally {
                    second.getLock().unlock();
                }
            } finally {
                first.getLock().unlock();
            }
        });
    }

    // ------------------------------------------------------------------
    // Background interest accrual
    // ------------------------------------------------------------------

    /** Starts a recurring background task that credits interest to all savings accounts. */
    public void startInterestScheduler() {
        if (interestTask != null) {
            return;
        }
        // Every 30s (demo cadence) apply one day's worth of the annual rate.
        interestTask = interestScheduler.scheduleAtFixedRate(() -> {
            for (Account acc : accounts.values()) {
                if (acc.getType() == AccountType.SAVINGS) {
                    acc.applyInterest(acc.getType().getInterestRate() / 365.0);
                }
            }
            notifyListeners();
        }, 30, 30, TimeUnit.SECONDS);
    }

    /** Gracefully shuts down all background thread pools. Call on application exit. */
    public void shutdown() {
        transactionExecutor.shutdown();
        interestScheduler.shutdown();
        try {
            if (!transactionExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                transactionExecutor.shutdownNow();
            }
            if (!interestScheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                interestScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            transactionExecutor.shutdownNow();
            interestScheduler.shutdownNow();
        }
    }
}
