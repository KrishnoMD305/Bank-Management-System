package com.bank.service;

import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.model.User;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class StatementGenerator {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path STATEMENTS_DIR = Paths.get("statements");

    private StatementGenerator() {}

    public static Path generate(Account account, User owner) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String fileName = account.getAccountNumber() + "_" + now.format(FILE_STAMP) + ".json";
        Files.createDirectories(STATEMENTS_DIR);
        Path outputFile = STATEMENTS_DIR.resolve(fileName);
        String json = toJson(account, owner, now);
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
        return outputFile;
    }

    static String toJson(Account account, User owner, LocalDateTime generatedAt) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"statementGeneratedAt\": ").append(quote(generatedAt.format(DISPLAY_STAMP))).append(",\n");
        sb.append("  \"accountId\": ").append(quote(account.getAccountNumber())).append(",\n");
        sb.append("  \"accountType\": ").append(quote(account.getType().toString())).append(",\n");
        sb.append("  \"accountCreatedAt\": ").append(quote(account.getCreatedAt().format(DISPLAY_STAMP))).append(",\n");
        sb.append("  \"currentBalance\": ").append(number(account.getBalance())).append(",\n");

        sb.append("  \"customer\": {\n");
        if (owner != null) {
            sb.append("    \"userId\": ").append(quote(owner.getUserId())).append(",\n");
            sb.append("    \"fullName\": ").append(quote(owner.getFullName())).append(",\n");
            sb.append("    \"phoneNumber\": ").append(quote(owner.getPhoneNumber())).append(",\n");
            sb.append("    \"idType\": ").append(quote(owner.getIdType().toString())).append(",\n");
            sb.append("    \"idNumber\": ").append(quote(owner.getIdNumber())).append("\n");
        } else {
            sb.append("    \"userId\": ").append(quote(account.getOwnerUserId())).append("\n");
        }
        sb.append("  },\n");

        List<Transaction> history = account.getTransactionHistory();
        sb.append("  \"transactionCount\": ").append(history.size()).append(",\n");
        sb.append("  \"transactions\": [\n");
        for (int i = 0; i < history.size(); i++) {
            Transaction t = history.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": ").append(t.getId()).append(",\n");
            sb.append("      \"timestamp\": ").append(quote(t.getFormattedTimestamp())).append(",\n");
            sb.append("      \"type\": ").append(quote(t.getType().toString())).append(",\n");
            sb.append("      \"amount\": ").append(number(t.getAmount())).append(",\n");
            sb.append("      \"balanceAfter\": ").append(number(t.getBalanceAfter())).append(",\n");
            sb.append("      \"description\": ").append(quote(t.getDescription())).append("\n");
            sb.append("    }").append(i < history.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String number(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
