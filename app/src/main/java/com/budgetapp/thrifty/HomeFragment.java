package com.budgetapp.thrifty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.renderers.TransactionsRenderer;
import com.budgetapp.thrifty.transaction.Transaction;

import android.widget.ImageButton;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);


        TransactionsHandler transactions = new TransactionsHandler();
        Transaction t1 = new Transaction("Expense", "Lunch", 10000, R.drawable.ic_food);
        Transaction t2 = new Transaction("Expense", "Dinner", 10000, R.drawable.ic_food);

        transactions.addTransaction(t1);
        transactions.addTransaction(t2);


        TransactionsRenderer renderer = new TransactionsRenderer(requireContext(), view);
        renderer.setUpHome();


        ImageButton notificationButton = view.findViewById(R.id.ic_notifcations);


        notificationButton.setOnClickListener(v -> openNotificationsFragment());

        return view;
    }


    private void openNotificationsFragment() {

        NotificationsFragment notificationsFragment = new NotificationsFragment();


        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();


        fragmentTransaction.replace(R.id.frame_layout, notificationsFragment);


        fragmentTransaction.addToBackStack(null);


        fragmentTransaction.commit();
    }
}
