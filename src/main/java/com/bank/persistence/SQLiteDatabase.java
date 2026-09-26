package com.bank.persistence;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.Admin;
import com.bank.model.IdType;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.model.User;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SQLiteDatabase {

    private static final String SQLITE_HEADER = "SQLite format 3\u0000";

    private SQLiteDatabase() {
    }

    public static Connection connect(File file) throws SQLException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new SQLException("Could not create database directory: " + parent);
        }

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }


    public static boolean isSQLiteDatabase(File file) {
        if (!file.isFile() || file.length() < 16) {
            return false;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[16];
            raf.readFully(header);
            return SQLITE_HEADER.equals(new String(header, java.nio.charset.StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            return false;
        }
    }

    public static void initialize(File adminsDb, File usersDb) {
        initializeAdmins(adminsDb);
        initializeUsers(usersDb);
    }

    private static void initializeAdmins(File file) {
        try (Connection c = connect(file);
             Statement s = c.createStatement()) {
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS admins (
                        username TEXT PRIMARY KEY,
                        password_hash TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        email TEXT,
                        phone_number TEXT,
                        id_number TEXT
                    )
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize SQLite database: " + file, e);
        }
    }

    private static void initializeUsers(File file) {
        try (Connection c = connect(file); Statement s = c.createStatement()) {

            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id TEXT PRIMARY KEY,
                        full_name TEXT NOT NULL,
                        phone_number TEXT,
                        id_type TEXT NOT NULL,
                        id_number TEXT NOT NULL UNIQUE,
                        created_at TEXT NOT NULL
                    )
                    """);

            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        account_number TEXT PRIMARY KEY,
                        owner_user_id TEXT NOT NULL,
                        account_type TEXT NOT NULL,
                        balance REAL NOT NULL DEFAULT 0,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY (owner_user_id) REFERENCES users(user_id)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """);

            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY,
                        account_number TEXT NOT NULL,
                        transaction_type TEXT NOT NULL,
                        amount REAL NOT NULL,
                        balance_after REAL NOT NULL,
                        description TEXT,
                        timestamp TEXT NOT NULL,
                        FOREIGN KEY (account_number) REFERENCES accounts(account_number)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """);

            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS metadata (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize SQLite database: " + file, e);
        }
    }

    public static Map<String, Admin> loadAdmins(File file) {
        Map<String, Admin> result = new ConcurrentHashMap<>();
        String sql = "SELECT username, password_hash, full_name, email, phone_number, id_number FROM admins";

        try (Connection c = connect(file);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Admin admin = new Admin(rs.getString("username"), rs.getString("password_hash"), rs.getString("full_name"), rs.getString("email"), rs.getString("phone_number"), rs.getString("id_number"));
                result.put(admin.getUsername(), admin);
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load admins from " + file, e);
        }
    }

    public static void saveAdmins(File file, Collection<Admin> admins) {
        String sql = """
                INSERT INTO admins(username, password_hash, full_name, email, phone_number, id_number)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = connect(file)) {
            c.setAutoCommit(false);
            try {
                try (Statement clear = c.createStatement()) {
                    clear.executeUpdate("DELETE FROM admins");
                }
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    for (Admin admin : admins) {
                        ps.setString(1, admin.getUsername());
                        ps.setString(2, admin.getPasswordHash());
                        ps.setString(3, admin.getFullName());
                        ps.setString(4, admin.getEmail());
                        ps.setString(5, admin.getPhoneNumber());
                        ps.setString(6, admin.getIdNumber());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save admins to " + file, e);
        }
    }

    public static LoadedUsers loadUsers(File file) {
        Map<String, User> users = new ConcurrentHashMap<>();
        Map<String, Account> accounts = new ConcurrentHashMap<>();

        try (Connection c = connect(file)) {
            String userSql = """
                    SELECT user_id, full_name, phone_number, id_type, id_number, created_at
                    FROM users
                    ORDER BY user_id
                    """;
            try (PreparedStatement ps = c.prepareStatement(userSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User(rs.getString("user_id"), rs.getString("full_name"), rs.getString("phone_number"), IdType.valueOf(rs.getString("id_type")), rs.getString("id_number"), parseDateTime(rs.getString("created_at")));
                    users.put(user.getUserId(), user);
                }
            }

            String accountSql = """
                    SELECT account_number, owner_user_id, account_type, balance, created_at
                    FROM accounts
                    ORDER BY account_number
                    """;
            try (PreparedStatement ps = c.prepareStatement(accountSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account account = new Account(rs.getString("account_number"), rs.getString("owner_user_id"), AccountType.valueOf(rs.getString("account_type")), rs.getDouble("balance"), parseDateTime(rs.getString("created_at")));
                    accounts.put(account.getAccountNumber(), account);

                    User owner = users.get(account.getOwnerUserId());
                    if (owner != null) {
                        owner.addAccountNumber(account.getAccountNumber());
                    }
                }
            }

            String transactionSql = """
                    SELECT id, account_number, transaction_type, amount, balance_after, description, timestamp
                    FROM transactions
                    ORDER BY id
                    """;
            long maxTransactionId = 0;
            try (PreparedStatement ps = c.prepareStatement(transactionSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    Transaction transaction = new Transaction(
                            id,
                            rs.getString("account_number"),
                            TransactionType.valueOf(rs.getString("transaction_type")),
                            rs.getDouble("amount"),
                            rs.getDouble("balance_after"),
                            rs.getString("description"),
                            parseDateTime(rs.getString("timestamp"))
                    );
                    Account account = accounts.get(transaction.getAccountNumber());
                    if (account != null) {
                        account.restoreTransaction(transaction);
                    }
                    maxTransactionId = Math.max(maxTransactionId, id);
                }
            }
            Transaction.ensureNextId(maxTransactionId + 1);
            int userSequence = readIntMetadata(c, "user_sequence", 100_000);
            int accountSequence = readIntMetadata(c, "account_sequence", 100_000);

            return new LoadedUsers(users, accounts, userSequence, accountSequence);
        } catch (SQLException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not load users/accounts from " + file, e);
        }
    }

    public static void saveUsers(File file, Collection<User> users, Collection<Account> accounts, int userSequence, int accountSequence) {
        try (Connection c = connect(file)) {
            c.setAutoCommit(false);
            try {
                try (Statement clear = c.createStatement()) {
                    clear.executeUpdate("DELETE FROM transactions");
                    clear.executeUpdate("DELETE FROM accounts");
                    clear.executeUpdate("DELETE FROM users");
                }

                String userSql = """
                        INSERT INTO users(user_id, full_name, phone_number, id_type, id_number, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement ps = c.prepareStatement(userSql)) {
                    for (User user : users) {
                        ps.setString(1, user.getUserId());
                        ps.setString(2, user.getFullName());
                        ps.setString(3, user.getPhoneNumber());
                        ps.setString(4, user.getIdType().name());
                        ps.setString(5, user.getIdNumber());
                        ps.setString(6, user.getCreatedAt().toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                String accountSql = """
                        INSERT INTO accounts(account_number, owner_user_id, account_type, balance, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement ps = c.prepareStatement(accountSql)) {
                    for (Account account : accounts) {
                        ps.setString(1, account.getAccountNumber());
                        ps.setString(2, account.getOwnerUserId());
                        ps.setString(3, account.getType().name());
                        ps.setDouble(4, account.getBalance());
                        ps.setString(5, account.getCreatedAt().toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                String transactionSql = """
                        INSERT INTO transactions(
                            id, account_number, transaction_type, amount,
                            balance_after, description, timestamp
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement ps = c.prepareStatement(transactionSql)) {
                    for (Account account : accounts) {
                        for (Transaction transaction : account.getTransactionHistory()) {
                            ps.setLong(1, transaction.getId());
                            ps.setString(2, transaction.getAccountNumber());
                            ps.setString(3, transaction.getType().name());
                            ps.setDouble(4, transaction.getAmount());
                            ps.setDouble(5, transaction.getBalanceAfter());
                            ps.setString(6, transaction.getDescription());
                            ps.setString(7, transaction.getTimestamp().toString());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }

                writeMetadata(c, "user_sequence", Integer.toString(userSequence));
                writeMetadata(c, "account_sequence", Integer.toString(accountSequence));

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save users/accounts to " + file, e);
        }
    }


    
}
