package com.budgetapp.thrifty.transaction;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Transaction {

    // Income or Expense
    private String type;

    // eg. Lunch, Dinner, Health and so on
    private String category;

    private float amount;

    private String dateAndTime;

    private int iconID;

    // "None", "Daily", "Weekly", "Monthly", "Yearly"
    private String recurring;

    private String description;

    // Constructor with default recurring = "None"
    public Transaction(String type, String category, float amount, int iconID, String description) {
        this(type, category, amount, iconID, description, "None");
    }

    // Full constructor with description and recurring
    public Transaction(String type, String category, float amount, int iconID, String description, String recurring) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.iconID = iconID;
        this.description = description;
        this.recurring = recurring;
        this.dateAndTime = getCurrentDateTime();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @SuppressLint("DefaultLocale")
    public CharSequence getAmount() {
        return String.format(type.equals("Income") ? "+₱%.2f" : "-₱%.2f", amount);
    }

    private String getCurrentDateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("h:mm a - MMMM d", Locale.getDefault());
        return format.format(calendar.getTime());
    }

    public String getDateAndTime() {
        return this.dateAndTime;
    }

    public void setDateAndTime(String dateAndTime) {
        this.dateAndTime = dateAndTime;
    }

    public int getIconID() {
        return iconID;
    }

    public void setIconID(int iconID) {
        this.iconID = iconID;
    }

    public String getRecurring() {
        return recurring;
    }

    public void setRecurring(String recurring) {
        this.recurring = recurring;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getRawAmount() {
        return amount;
    }
}
