package com.bank.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;

public final class FileDatabase {
    private FileDatabase() {}

    public static <T> void save(T data, File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Could not create directory: " + parent);
            }
            File tmp = new File(parent, file.getName() + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))) {
                oos.writeObject(data);
            }
            if (file.exists() && !file.delete()) {
                throw new IOException("Could not replace file: " + file);
            }
            if (!tmp.renameTo(file)) {
                throw new IOException("Could not finalize file: " + file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save legacy serialized file: " + file, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T load(File file, Class<T> type) {
        if (!file.exists()) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new UncheckedIOException(
                    "Failed to load legacy serialized file: " + file, new IOException(e));
        }
    }
}
