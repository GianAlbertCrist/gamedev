package com.budgetapp.thrifty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.renderers.TransactionAdapter;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Populate transactions
        TransactionsHandler transactions = new TransactionsHandler();
        transactions.addTransaction(new Transaction("Expense", "Lunch", 120f, R.drawable.ic_food));
        transactions.addTransaction(new Transaction("Income", "Salary", 15000f, R.drawable.ic_housing));
        transactions.addTransaction(new Transaction("Expense", "Coffee", 85f, R.drawable.ic_food));
        transactions.addTransaction(new Transaction("Expense", "Groceries", 740f, R.drawable.ic_gifts));
        transactions.addTransaction(new Transaction("Expense", "Electricity Bill", 1800f, R.drawable.ic_gifts));
        transactions.addTransaction(new Transaction("Expense", "Water Bill", 600f, R.drawable.ic_gifts));
        transactions.addTransaction(new Transaction("Income", "Freelance", 3200f, R.drawable.income));
        transactions.addTransaction(new Transaction("Expense", "Movie", 250f, R.drawable.ic_gifts));
        transactions.addTransaction(new Transaction("Expense", "Clothes", 2150f, R.drawable.ic_clothes));
        transactions.addTransaction(new Transaction("Expense", "Internet", 999f, R.drawable.ic_transport));
        transactions.addTransaction(new Transaction("Expense", "Medicine", 470f, R.drawable.ic_healthcare));
        transactions.addTransaction(new Transaction("Expense", "Transportation", 150f, R.drawable.ic_transport));
        transactions.addTransaction(new Transaction("Income", "Bonus", 50f, R.drawable.ic_transport));
        transactions.addTransaction(new Transaction("Expense", "Books", 350f, R.drawable.ic_gifts));
        transactions.addTransaction(new Transaction("Expense", "Subscription", 299f, R.drawable.ic_transport));

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.home_transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        TransactionAdapter adapter = new TransactionAdapter(getContext(), TransactionsHandler.transactions);
        recyclerView.setAdapter(adapter);

        return view;
    }
}