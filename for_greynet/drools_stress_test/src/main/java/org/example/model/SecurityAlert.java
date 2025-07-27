package org.example.model;

public class SecurityAlert {
    private String location;
    private int severity; // 1 to 5

    public SecurityAlert(String location, int severity) {
        this.location = location;
        this.severity = severity;
    }

    // --- Getters and Setters ---
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }
}
