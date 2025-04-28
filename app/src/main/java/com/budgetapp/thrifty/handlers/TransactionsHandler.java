package com.budgetapp.thrifty.handlers;

import com.budgetapp.thrifty.transaction.Transaction;

import java.util.ArrayList;

public class TransactionsHandler {

    public static ArrayList<Transaction> transactions = new ArrayList<>();

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }
}
