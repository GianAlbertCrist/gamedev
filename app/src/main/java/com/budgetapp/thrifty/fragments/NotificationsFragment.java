package com.budgetapp.thrifty.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.renderers.NotificationAdapter;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";
    private ArrayList<Notification> notificationList = new ArrayList<>();
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private TextView noNotificationsText;
    private boolean isViewCreated = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        recyclerView = view.findViewById(R.id.recyclerViewNotifications);
        noNotificationsText = view.findViewById(R.id.no_notifications_text);

        // Initialize the adapter with the notification list
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        isViewCreated = true;

        // Load notifications from Firestore
        loadNotificationsFromFirestore();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
    }

    private void loadNotificationsFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Check if fragment is still attached to activity and view exists
                    if (!isAdded() || !isViewCreated) {
                        Log.d(TAG, "Fragment not attached or view destroyed, skipping UI update");
                        return;
                    }

                    notificationList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            String title = doc.getString("title");
                            String message = doc.getString("message");
                            String recurring = doc.getString("recurring");
                            Long iconIDLong = doc.getLong("iconID");
                            int iconID = iconIDLong != null ? iconIDLong.intValue() : R.drawable.icnotif_transactions;

                            // Create timestamp string from Firestore timestamp
                            String timestamp = "Unknown time";
                            if (doc.getDate("timestamp") != null) {
                                timestamp = doc.getDate("timestamp").toString();
                            }

                            Notification notification = new Notification(
                                    title != null ? title : "Notification",
                                    message != null ? message : "",
                                    timestamp,
                                    recurring != null ? recurring : "None",
                                    iconID
                            );

                            notificationList.add(notification);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification", e);
                        }
                    }

                    // Update UI based on whether we have notifications
                    updateNotificationUI();

                    Log.d(TAG, "Loaded " + notificationList.size() + " notifications");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                });
    }

    private void updateNotificationUI() {
        // Only update UI if fragment is still attached and view exists
        if (!isAdded() || !isViewCreated) {
            Log.d(TAG, "Fragment not attached or view destroyed, skipping UI update");
            return;
        }

        if (noNotificationsText != null && recyclerView != null) {
            if (notificationList.isEmpty()) {
                noNotificationsText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                noNotificationsText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // Method to add notifications to the list and refresh the UI
    public void addNotification(Notification notification) {
        notificationList.add(notification);
        updateNotificationUI();
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
