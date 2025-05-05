package com.budgetapp.thrifty.model;

public class Notification {

    private String title;
    private String description;
    private String time;

    public Notification(String title, String description, String time) {
        this.title = title;
        this.description = description;
        this.time = time;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }
}

