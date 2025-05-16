package com.budgetapp.thrifty.handlers;

import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.fragments.NotificationsFragment;
import com.budgetapp.thrifty.utils.FirestoreManager;

import java.util.Calendar;
import java.util.ArrayList;

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
                    now.add(Calendar.DAY_OF_YEAR, -7);  // Filter for the last 7 days
                    if (t.getParsedDate().after(now.getTime())) {
                        filtered.add(t);
                    }
                    now.add(Calendar.DAY_OF_YEAR, 7); // Reset to the current date
                    break;
                case "Weeks":
                    now.add(Calendar.WEEK_OF_YEAR, -4);  // Filter for the last 4 weeks
                    if (t.getParsedDate().after(now.getTime())) {
                        filtered.add(t);
                    }
                    now.add(Calendar.WEEK_OF_YEAR, 4); // Reset to the current date
                    break;
                case "Months":
                    now.add(Calendar.MONTH, -1);  // Filter for the last 1 month
                    if (t.getParsedDate().after(now.getTime())) {
                        filtered.add(t);
                    }
                    now.add(Calendar.MONTH, 1); // Reset to the current date
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
        Calendar today = Calendar.getInstance();

        for (Transaction transaction : transactions) {
            if (!transaction.getRecurring().equals("None") && transaction.getNextDueDate() != null) {
                Calendar nextDueDate = Calendar.getInstance();
                nextDueDate.setTime(transaction.getNextDueDate());

                // Generate notifications for all missed due dates
                while (nextDueDate.before(today) || isSameDay(nextDueDate, today)) {
                    // Create notification message
                    String notificationMessage = createNotificationMessage(transaction);

                    // Get the corresponding notification icon
                    int iconResId = getNotificationIcon(transaction.getRecurring());

                    // Create the notification
                    Notification notification = new Notification(
                            "Expense Reminder",
                            notificationMessage,
                            getCurrentTime(),
                            transaction.getRecurring(),
                            iconResId
                    );

                    // Add notification to the NotificationsFragment
                    notificationsFragment.addNotification(notification);

                    // Update next due date
                    transaction.updateNextDueDate();
                    nextDueDate.setTime(transaction.getNextDueDate());
                }

                // Save the updated transaction to Firestore
                FirestoreManager.updateTransaction(transaction);
            }
        }
    }


    private static boolean isSameDay(Calendar date1, Calendar date2) {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR);
    }

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
