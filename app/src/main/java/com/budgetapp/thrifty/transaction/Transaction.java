package com.budgetapp.thrifty.transaction;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public Transaction(String type, String category, float amount, int iconID) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.dateAndTime = getCurrentDateTime();
        this.iconID = iconID;
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
}
