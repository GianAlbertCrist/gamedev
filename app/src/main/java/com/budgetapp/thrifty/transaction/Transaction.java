package com.budgetapp.thrifty.transaction;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Transaction implements Parcelable {
    private String id;
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

    private java.util.Date nextDueDate;

    public Transaction() {
        // Required empty constructor for Firestore
    }

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
        this.id = java.util.UUID.randomUUID().toString();

        // Calculate next due date if this is a recurring transaction
        if (!recurring.equals("None")) {
            calculateNextDueDate();
        }
    }

    protected Transaction(Parcel in) {
        type = in.readString();
        category = in.readString();
        amount = in.readFloat();
        dateAndTime = in.readString();
        iconID = in.readInt();
        recurring = in.readString();
        description = in.readString();
        long time = in.readLong();
        parsedDate = time > 0 ? new Date(time) : new Date();  // fallback to now
        long nextDueTime = in.readLong();
        nextDueDate = nextDueTime > 0 ? new Date(nextDueTime) : null;
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

    // Calculate the next due date based on recurring type
    public void calculateNextDueDate() {
        if (recurring.equals("None")) {
            this.nextDueDate = null;
            return;
        }

        Calendar calendar = Calendar.getInstance();
        if (parsedDate != null) {
            calendar.setTime(parsedDate);
        }

        // Set time to beginning of day for consistent notifications
        calendar.set(Calendar.HOUR_OF_DAY, 8); // 8:00 AM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // For the first calculation, use today or tomorrow as the start
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // If the parsed date is in the past, start from today
        if (calendar.before(today)) {
            calendar = today;
            calendar.set(Calendar.HOUR_OF_DAY, 8); // 8:00 AM
        }

        // Calculate next due date based on recurring type
        switch (recurring) {
            case "Daily":
                // For daily transactions, if it's already past 8 AM today, set to tomorrow
                if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 8 &&
                        isSameDay(calendar, today)) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
            case "Weekly":
                // Next week, same day of week
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "Monthly":
                // Next month, same day of month
                calendar.add(Calendar.MONTH, 1);
                break;
            case "Yearly":
                // Next year, same day of year
                calendar.add(Calendar.YEAR, 1);
                break;
        }

        this.nextDueDate = calendar.getTime();
        Log.d("Transaction", "Calculated next due date: " + this.nextDueDate + " for recurring: " + recurring);
    }

    // Update the next due date after a notification has been sent
    public void updateNextDueDate() {
        if (nextDueDate == null || recurring.equals("None")) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextDueDate);

        switch (recurring) {
            case "Daily":
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case "Weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "Monthly":
                calendar.add(Calendar.MONTH, 1);
                break;
            case "Yearly":
                calendar.add(Calendar.YEAR, 1);
                break;
        }

        this.nextDueDate = calendar.getTime();
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
        dest.writeLong(parsedDate != null ? parsedDate.getTime() : -1L);
        dest.writeLong(nextDueDate != null ? nextDueDate.getTime() : -1L);
    }

    // Add this method to set parsed date from Firestore timestamp
    public void setParsedDate(Date date) {
        this.parsedDate = date;
        // Update dateAndTime string
        SimpleDateFormat format = new SimpleDateFormat("h:mm a - MMMM d", Locale.getDefault());
        this.dateAndTime = format.format(date);

        // Recalculate next due date if this is a recurring transaction
        if (!recurring.equals("None") && nextDueDate == null) {
            calculateNextDueDate();
        }
    }

    public Date getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(Date nextDueDate) {
        this.nextDueDate = nextDueDate;
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
        Date now = calendar.getTime();
        this.parsedDate = now != null ? now : new Date();
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

        // Recalculate next due date if recurring type changes
        if (!recurring.equals("None")) {
            calculateNextDueDate();
        } else {
            this.nextDueDate = null;
        }
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
}
