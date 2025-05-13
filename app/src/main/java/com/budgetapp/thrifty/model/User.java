package com.budgetapp.thrifty.model;

public class User {
    private String uid, email, displayName;
    private boolean disabled;

    public User() {
        // Default constructor required for Firebase or JSON deserialization
    }

    public User(String uid, String email, String displayName, boolean disabled) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.disabled = disabled;
    }

    // Getters
    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDisabled() {
        return disabled;
    }

    // Setters
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
