package com.budgetapp.thrifty.handlers;

import com.budgetapp.thrifty.transaction.Transaction;

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

}

