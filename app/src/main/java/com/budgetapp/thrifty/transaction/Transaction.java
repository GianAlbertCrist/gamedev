package com.budgetapp.thrifty.transaction;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;

public class Transaction implements Parcelable {

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

    private java.util.Date parsedDate;

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

    protected Transaction(Parcel in) {
        type = in.readString();
        category = in.readString();
        amount = in.readFloat();
        dateAndTime = in.readString();
        iconID = in.readInt();
        recurring = in.readString();
        description = in.readString();
        parsedDate = new Date(in.readLong()); // Read the Date as a long
    }

    public static final Creator<Transaction> CREATOR = new Creator<Transaction>() {
        @Override
        public Transaction createFromParcel(Parcel in) {
            return new Transaction(in);
        }

        @Override
        public Transaction[] newArray(int size) {
            return new Transaction[size];
        }
    };

    // Method to check if this transaction is due for notification
    public boolean isDueForNotification() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parsedDate); // Set the calendar to the transaction date

        switch (recurring) {
            case "Daily":
                return calendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            case "Weekly":
                return calendar.get(Calendar.WEEK_OF_YEAR) == Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
            case "Monthly":
                return calendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH);
            case "Yearly":
                return calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR);
            default:
                return false;
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    // Parcelable method to write the object to a Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(category);
        dest.writeFloat(amount);
        dest.writeString(dateAndTime);
        dest.writeInt(iconID);
        dest.writeString(recurring);
        dest.writeString(description);
        dest.writeLong(parsedDate.getTime());  // Writing the Date as long
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
        this.parsedDate = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("h:mm a - MMMM d", Locale.getDefault());
        return format.format(parsedDate);
    }

    public Date getParsedDate() {
        return parsedDate;
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


