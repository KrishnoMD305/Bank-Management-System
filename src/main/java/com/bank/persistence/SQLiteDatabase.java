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

    private SQLiteDatabase() {}

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

}
