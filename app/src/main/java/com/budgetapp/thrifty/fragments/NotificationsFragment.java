package com.budgetapp.thrifty.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.renderers.NotificationAdapter;
import com.budgetapp.thrifty.utils.ThemeSync;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {
    private ArrayList<Notification> notificationList = new ArrayList<>();
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        recyclerView = view.findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the adapter with the notification list
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    // Method to add notifications to the list and refresh the UI
    public void addNotification(Notification notification) {
        notificationList.add(notification);
        if (adapter != null) {
            adapter.notifyDataSetChanged();  // Refresh the notification list
        }
    }

    public ArrayList<Notification> getNotificationList() {
        return notificationList;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public NotificationAdapter getAdapter() {
        return adapter;
    }
}
