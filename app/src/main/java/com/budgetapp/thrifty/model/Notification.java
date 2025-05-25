package com.budgetapp.thrifty.model;

public class Notification {
    private String type, description, time, recurring;
    private int iconID;
    private boolean isNotified;

    public Notification(String type, String description, String time, String recurring, int iconID) {
        this.type = type;
        this.description = description;
        this.time = time;
        this.recurring = recurring;
        this.iconID = iconID;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTime() {
        return time;
    }

    public String getRecurring() {
        return recurring;
    }

    public void setRecurring(String recurring) {
        this.recurring = recurring;
    }

    public boolean isNotified() {
        return isNotified;
    }

    public void setNotified(boolean notified) {
        this.isNotified = notified;
    }
    public int getIconID() {
        return iconID;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", time='" + time + '\'' +
                ", recurring='" + recurring + '\'' +
                ", iconID=" + iconID +
                '}';
    }
}