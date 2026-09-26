package com.bank.service;

import com.bank.model.Account;
import com.bank.model.Admin;
import com.bank.model.User;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;
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
}
