package com.bank.model;

public enum IdType {
    NID("NID"),
    DRIVING_LICENSE("Driving License"),
    PASSPORT("Passport");

    private final String label;
    IdType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
