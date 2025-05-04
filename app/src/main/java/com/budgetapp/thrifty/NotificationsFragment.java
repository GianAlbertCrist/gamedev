package com.budgetapp.thrifty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.budgetapp.thrifty.NotificationAdapter;
import com.budgetapp.thrifty.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);


        recyclerView = view.findViewById(R.id.recyclerViewNotifications);


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        notificationList = new ArrayList<>();
        loadNotifications();


        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        return view;
    }


    private void loadNotifications() {
        notificationList.add(new Notification("Transactions", "A new transaction has been registered: Salary | ₱15,000.00", "12:29 AM - April 9"));
        notificationList.add(new Notification("Transactions", "A new transaction has been registered: Clothing | ₱12,000.24", "10:45 AM - April 9"));
        notificationList.add(new Notification("Transactions", "A new transaction has been registered: Lunch | ₱150.45", "1:30 PM - April 8"));
        notificationList.add(new Notification("Reminder", "Set up your automatic savings to meet your savings goal...", "1:00 PM - April 8"));
        notificationList.add(new Notification("Expense Record", "We recommend that you be more attentive to your finances.", "9:18 AM - April 8"));
    }
}
