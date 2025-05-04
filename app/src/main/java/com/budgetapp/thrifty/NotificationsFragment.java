package com.budgetapp.thrifty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);


        recyclerView = view.findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        fetchNotifications();

        return view;
    }


    private void fetchNotifications() {

        List<Notification> notificationList = new ArrayList<>();
        notificationList.add(new Notification("Transaction", "Salary | ₱15,000.00", "12:29 AM - April 9"));
        notificationList.add(new Notification("Reminder", "Pay Credit Card Bill", "8:00 AM - April 9"));
        notificationList.add(new Notification("Expense", "Lunch | ₱150.00", "1:30 PM - April 8"));
        notificationList.add(new Notification("Transaction", "Clothing | ₱2,500.00", "2:00 PM - April 8"));


        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);
    }
}
