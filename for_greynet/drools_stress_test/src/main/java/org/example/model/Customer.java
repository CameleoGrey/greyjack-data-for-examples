package org.example.model;

// --- File: src/main/java/com/example/model/Customer.java ---

public class Customer {
    private int id;
    private String riskLevel; // 'low', 'medium', 'high'
    private String status;    // 'active', 'inactive'

    public Customer(int id, String riskLevel, String status) {
        this.id = id;
        this.riskLevel = riskLevel;
        this.status = status;
    }

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

