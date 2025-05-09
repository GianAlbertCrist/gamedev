package com.budgetapp.thrifty.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.renderers.TransactionAdapter;
import com.budgetapp.thrifty.utils.FormatUtils;

public class HomeFragment extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private TextView emptyMessage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = rootView.findViewById(R.id.home_transactions);
        emptyMessage = rootView.findViewById(R.id.empty_message);

        ImageButton notificationButton = rootView.findViewById(R.id.ic_notifcations);
        notificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotificationsFragment();
            }
        });


        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
        updateBalances();
    }

    @SuppressLint("DefaultLocale")
    private void updateBalances() {
        TextView balanceText = rootView.findViewById(R.id.total_balance);
        TextView incomeText = rootView.findViewById(R.id.total_income);
        TextView expenseText = rootView.findViewById(R.id.total_expense);

        double balance = TransactionsHandler.getBalance();
        double income = TransactionsHandler.getTotalIncome();
        double expense = TransactionsHandler.getTotalExpense();

        balanceText.setText(String.format("₱ %s", FormatUtils.formatAmount(balance, false)));
        incomeText.setText(String.format("₱ %s", FormatUtils.formatAmount(income, true)));
        expenseText.setText(String.format("₱ %s", FormatUtils.formatAmount(expense, true)));
    }

    private void loadTransactions() {
        if (TransactionsHandler.transactions.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            TransactionAdapter adapter = new TransactionAdapter(getContext(), TransactionsHandler.transactions);
            recyclerView.setAdapter(adapter);
        }
    }

    private void openNotificationsFragment() {
        NotificationsFragment notificationsFragment = new NotificationsFragment();

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Replace the current fragment with NotificationsFragment
        fragmentTransaction.replace(R.id.frame_layout, notificationsFragment);

        // Add to the back stack so the user can navigate back
        fragmentTransaction.addToBackStack(null);

        // Commit the transaction
        fragmentTransaction.commit();
    }
}
