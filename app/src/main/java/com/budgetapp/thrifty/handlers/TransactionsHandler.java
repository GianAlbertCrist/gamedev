package com.budgetapp.thrifty.handlers;

import com.budgetapp.thrifty.transaction.Transaction;

import java.util.Calendar;
import java.util.ArrayList;

public class TransactionsHandler {

    public static ArrayList<Transaction> transactions = new ArrayList<>();

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
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

    public static float getTotalExpense() {
        float total = 0f;
        for (Transaction t : transactions) {
            if ("Expense".equalsIgnoreCase(t.getType())) {
                total += t.getRawAmount();
            }
        }
        return total;
    }

    public static float getBalance() {
        return getTotalIncome() - getTotalExpense();
    }

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
                    now.add(Calendar.DAY_OF_YEAR, 7); // reset
                    break;
                case "Weeks":
                    now.add(Calendar.WEEK_OF_YEAR, -4);
                    if (t.getParsedDate().after(now.getTime())) {
                        filtered.add(t);
                    }
                    now.add(Calendar.WEEK_OF_YEAR, 4); // reset
                    break;
                case "Months":
                    now.add(Calendar.MONTH, -1);
                    if (t.getParsedDate().after(now.getTime())) {
                        filtered.add(t);
                    }
                    now.add(Calendar.MONTH, 1); // reset
                    break;
            }
        }

        return filtered;
    }
}

