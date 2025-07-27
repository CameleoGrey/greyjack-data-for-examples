package org.example.model;

public class Transaction {
    private int id;
    private int customerId;
    private double amount;
    private String location;

    public Transaction(int id, int customerId, double amount, String location) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.location = location;
    }

    // --- Getters and Setters ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}