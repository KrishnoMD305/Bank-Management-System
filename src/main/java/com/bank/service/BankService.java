package com.bank.service;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.Admin;
import com.bank.model.IdType;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.persistence.FileDatabase;
import com.bank.persistence.SQLiteDatabase;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import java.io.File;
import java.io.Serializable;
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

public class BankService {

    private static final File DATA_DIR = new File("data");
    private static final File ADMINS_DB = new File(DATA_DIR, "Admins.db");
    private static final File USERS_DB = new File(DATA_DIR, "Users.db");

    private static final BankService INSTANCE = new BankService();

    private ConcurrentHashMap<String, Admin> admins;
    private ConcurrentHashMap<String, User> users;
    private ConcurrentHashMap<String, Account> accounts;
    private AtomicInteger userSequence;
    private AtomicInteger accountSequence;

    private final Object adminsFileLock = new Object();
    private final Object usersFileLock = new Object();

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
        LegacyStores legacy = readLegacyStores();
        SQLiteDatabase.initialize(ADMINS_DB, USERS_DB);
        loadAdmins();
        if (legacy.adminStore != null && admins.isEmpty()) {
            admins.putAll(legacy.adminStore.admins);
            saveAdmins();
        }
        loadUsers();
        if (legacy.userStore != null && users.isEmpty() && accounts.isEmpty()) {
            users.putAll(legacy.userStore.users);
            accounts.putAll(legacy.userStore.accounts);
            userSequence.set(legacy.userStore.userSequence);
            accountSequence.set(legacy.userStore.accountSequence);
            saveUsers();
        }
    }

    public static BankService getInstance() {
        return INSTANCE;
    }
    private static class AdminStore implements Serializable {
        private static final long serialVersionUID = 1L;
        ConcurrentHashMap<String, Admin> admins = new ConcurrentHashMap<>();
    }

    private static class UserStore implements Serializable {
        private static final long serialVersionUID = 1L;
        ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
        int userSequence = 100_000;
        int accountSequence = 100_000;
    }

    private record LegacyStores(AdminStore adminStore, UserStore userStore) {}

    private LegacyStores readLegacyStores() {
        AdminStore legacyAdmins = readLegacyAdminStore();
        UserStore legacyUsers = readLegacyUserStore();
        return new LegacyStores(legacyAdmins, legacyUsers);
    }

    private AdminStore readLegacyAdminStore() {
        if (!ADMINS_DB.exists() || SQLiteDatabase.isSQLiteDatabase(ADMINS_DB)) {
            return null;
        }

        AdminStore store = null;
        try {
            store = FileDatabase.load(ADMINS_DB, AdminStore.class);
        } catch (RuntimeException ignored) {

        }
        backupLegacyFile(ADMINS_DB);
        return store;
    }

    private UserStore readLegacyUserStore() {
        if (!USERS_DB.exists() || SQLiteDatabase.isSQLiteDatabase(USERS_DB)) {
            return null;
        }

        UserStore store = null;
        try {
            store = FileDatabase.load(USERS_DB, UserStore.class);
        } catch (RuntimeException ignored) {

        }
        backupLegacyFile(USERS_DB);
        return store;
    }

    private void backupLegacyFile(File file) {
        String timestamp = LocalDateTime.now().toString().replace(':', '-').replace('.', '-');

        File backup = new File(file.getParentFile(), file.getName() + ".legacy-" + timestamp);
        try {
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new IllegalStateException("Could not back up old database file before creating SQLite database: " + file, e);
        }
    }

    private void loadAdmins() {
        this.admins = new ConcurrentHashMap<>(SQLiteDatabase.loadAdmins(ADMINS_DB));

        if (admins.isEmpty()) {
            Admin defaultAdmin = new Admin("admin", PasswordUtil.hash("admin123"), "System Administrator", "admin@bank.com", "N/A", "N/A");
            admins.put("admin", defaultAdmin);
            saveAdmins();
        }
    }

    private void loadUsers() {
        SQLiteDatabase.LoadedUsers loaded = SQLiteDatabase.loadUsers(USERS_DB);
        this.users = new ConcurrentHashMap<>(loaded.users());
        this.accounts = new ConcurrentHashMap<>(loaded.accounts());
        this.userSequence = new AtomicInteger(loaded.userSequence());
        this.accountSequence = new AtomicInteger(loaded.accountSequence());
    }

    private void saveAdmins() {
        synchronized (adminsFileLock) {
            SQLiteDatabase.saveAdmins(ADMINS_DB, admins.values());
        }
    }

    private void saveUsers() {
        synchronized (usersFileLock) {
            SQLiteDatabase.saveUsers(USERS_DB, users.values(), accounts.values(), userSequence.get(), accountSequence.get());
        }
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            javafx.application.Platform.runLater(r);
        }
    }

    public synchronized Admin registerAdmin(String username, String password, String fullName, String email, String phoneNumber, String idNumber) {
        if (admins.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        Admin a = new Admin(username, PasswordUtil.hash(password), fullName, email, phoneNumber, idNumber);
        admins.put(username, a);
        saveAdmins();
        return a;
    }

    public Admin authenticateAdmin(String username, String password) {
        Admin a = admins.get(username);
        if (a == null) {
            return null;
        }
        return a.getPasswordHash().equals(PasswordUtil.hash(password)) ? a : null;
    }

    public synchronized Account openAccountForUser(String fullName, String phoneNumber, IdType idType, String idNumber, AccountType type, double initialDeposit) {
        User user = findUserByIdNumber(idNumber);
        if (user == null) {
            String userId = "USR" + userSequence.incrementAndGet();
            user = new User(userId, fullName, phoneNumber, idType, idNumber);
            users.put(userId, user);
        }

        String accountNumber = "ACC" + accountSequence.incrementAndGet();
        Account account = new Account(accountNumber, user.getUserId(), type, 0);
        accounts.put(accountNumber, account);
        user.addAccountNumber(accountNumber);

        if (initialDeposit > 0) {
            account.deposit(initialDeposit, "Initial deposit");
        }
        saveUsers();
        notifyListeners();
        return account;
    }

    public User findUserByIdNumber(String idNumber) {
        for (User u : users.values()) {
            if (u.getIdNumber().equalsIgnoreCase(idNumber)) {
                return u;
            }
        }
        return null;
    }

    public User getUser(String userId) {
        return users.get(userId);
    }
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    public List<Account> getAccountsForUser(String userId) {
        User u = users.get(userId);
        if (u == null) {
            return List.of();
        }
        List<Account> result = new ArrayList<>();
        for (String accNo : u.getAccountNumbers()) {
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
            saveUsers();
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
            saveUsers();
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
                    saveUsers();
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

    public void startInterestScheduler() {
        if (interestTask != null) {
            return;
        }
        interestTask = interestScheduler.scheduleAtFixedRate(() -> {
            boolean changed = false;
            for (Account acc : accounts.values()) {
                if (acc.getType().getInterestRate() > 0) {
                    Transaction t = acc.applyInterest(acc.getType().getInterestRate() / 365.0);
                    if (t != null) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                saveUsers();
            }
            notifyListeners();
        }, 30, 30, TimeUnit.SECONDS);
    }

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
        saveAdmins();
        saveUsers();
    }
}
