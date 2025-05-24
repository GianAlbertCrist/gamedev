package com.budgetapp.thrifty.handlers;

import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.fragments.NotificationsFragment;
import com.budgetapp.thrifty.utils.AppLogger;
import com.budgetapp.thrifty.utils.FirestoreManager;

import java.util.Calendar;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.util.Log;

public class TransactionsHandler {

    public static ArrayList<Transaction> transactions = new ArrayList<>();

    public static ArrayList<Transaction> getFilteredTransactions(String filterType) {
        ArrayList<Transaction> filtered = new ArrayList<>();
        Calendar now = Calendar.getInstance();

        for (Transaction t : transactions) {
            Calendar txDate = Calendar.getInstance();
            txDate.setTime(t.getParsedDate());

            switch (filterType) {
                case "Today":
                    if (now.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                            now.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)) {
                        filtered.add(t);
                    }
                    break;
                case "Days":
                    now.add(Calendar.DAY_OF_YEAR, -7);
                    if (t.getParsedDate().after(now.getTime())) {
                        filtered.add(t);
                    }
                    now.add(Calendar.DAY_OF_YEAR, 7);
                    break;
                case "Weeks":
                    now.add(Calendar.WEEK_OF_YEAR, -4);
                    if (t.getParsedDate().after(now.getTime())) {
                        filtered.add(t);
                    }
                    now.add(Calendar.WEEK_OF_YEAR, 4);
                    break;
                case "Months":
                    now.add(Calendar.MONTH, -1);
                    if (t.getParsedDate().after(now.getTime())) {
                        filtered.add(t);
                    }
                    now.add(Calendar.MONTH, 1);
                    break;
            }
        }

        return filtered;
    }

    public static float getTotalIncome() {
        float total = 0f;
        for (Transaction t : transactions) {
            if ("Income".equalsIgnoreCase(t.getType())) {
                total += t.getRawAmount();
            }
        }
        return total;
    }

    public static float getBalance() {
        return getTotalIncome() - getTotalExpense();  // Call the static methods for total income and total expense
    }

    // Static method to get total expense
    public static float getTotalExpense() {
        float total = 0f;
        for (Transaction t : transactions) {
            if ("Expense".equalsIgnoreCase(t.getType())) {
                total += t.getRawAmount();
            }
        }
        return total;
    }

    // Method to check recurring transactions and trigger notifications
    public static void checkRecurringTransactions(NotificationsFragment notificationsFragment) {
        if (notificationsFragment == null) {
            Log.e("TransactionsHandler", "NotificationsFragment is null, cannot add notifications");
            return;
        }

        Calendar today = Calendar.getInstance();
        Log.d("TransactionsHandler", "Checking recurring transactions. Today: " + today.getTime());
        Log.d("TransactionsHandler", "Total transactions to check: " + transactions.size());

        for (Transaction transaction : transactions) {
            if (!transaction.getRecurring().equals("None") && transaction.getNextDueDate() != null) {
                Calendar nextDueDate = Calendar.getInstance();
                nextDueDate.setTime(transaction.getNextDueDate());

                Log.d("TransactionsHandler", "Checking transaction: " + transaction.getId() +
                        ", Category: " + transaction.getCategory() +
                        ", Recurring: " + transaction.getRecurring() +
                        ", Next due date: " + nextDueDate.getTime());

                // While it's due today or earlier, repeat
                while (nextDueDate.before(today) || isSameDay(nextDueDate, today)) {
                    Log.d("TransactionsHandler", "Recurring due, cloning: " + transaction.getId());

                    // Create a new transaction clone with current date
                    Transaction clone = new Transaction(
                            transaction.getType(),
                            transaction.getCategory(),
                            transaction.getDescription(),
                            transaction.getRawAmount(),
                            "None",
                            new java.util.Date(),
                            transaction.getIconID()
                    );

                    FirestoreManager.saveTransaction(clone);
                    if (!clone.getRecurring().equals("None")) {
                        FirestoreManager.saveNotification(clone);
                    }

                    // Create and add a notification
                    String message = createNotificationMessage(transaction);
                    Notification notification = new Notification(
                            "Recurring Reminder",
                            message,
                            getCurrentTime(),
                            transaction.getRecurring(),
                            getNotificationIcon(transaction.getRecurring())
                    );
                    notificationsFragment.addNotification(notification);

                    // Update original transaction's next due date
                    transaction.updateNextDueDate();
                    nextDueDate.setTime(transaction.getNextDueDate());

                    Log.d("TransactionsHandler", "Next due date updated to: " + nextDueDate.getTime());
                }

                // Save updated nextDueDate in the original transaction
                FirestoreManager.updateTransaction(transaction);
            }
        }
    }


    private static boolean isSameDay(Calendar date1, Calendar date2) {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR);
    }

    @SuppressLint("DefaultLocale")
    private static String createNotificationMessage(Transaction transaction) {
        // Check if the transaction type is Income or Expense
        if ("Income".equalsIgnoreCase(transaction.getType())) {
            return String.format("%s income reminder: ₱%.2f - {%s} is due today.",
                    transaction.getRecurring(),
                    transaction.getRawAmount(),
                    transaction.getDescription());

        } else {
            // Expense notification format
            return String.format("%s expense reminder: ₱%.2f - {%s} is due today.",
                    transaction.getRecurring(),
                    transaction.getRawAmount(),
                    transaction.getDescription());
        }
    }

    // Helper method to get the notification icon based on transaction type (Income/Expense)
    private static int getNotificationIcon(String recurring) {
        switch (recurring) {
            case "Daily":
                return R.drawable.icnotif_transactions;
            case "Weekly":
                return R.drawable.icnotif_transactions;
            case "Monthly":
                return R.drawable.icnotif_transactions;
            case "Yearly":
                return R.drawable.icnotif_transactions;
            default:
                return R.drawable.icnotif_transactions;
        }
    }

    // Helper method to get the current time in string format
    private static String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTime().toString();
    }
}