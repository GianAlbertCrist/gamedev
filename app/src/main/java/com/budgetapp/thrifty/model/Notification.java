package com.budgetapp.thrifty.model;

public class Notification {
    private String title;
    private String description;
    private String time;

    private String recurringText; // Recurring type (Daily, Weekly, etc.)

    private int iconResId; // Icon resource ID for the notification

    // Constructor that accepts all necessary fields including recurring text and iconResId
    public Notification(String title, String description, String time, String recurringText, int iconResId) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.recurringText = recurringText;  // Set recurring type (Daily, Weekly, etc.)
        this.iconResId = iconResId;  // Set the icon resource ID
    }

    // Getter methods
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }

    public int getIconResId() {
        return iconResId;  // Return the icon resource ID
    }

    public String getRecurringText() {
        return recurringText;  // Return the recurring type (Daily, Weekly, etc.)
    }
}
