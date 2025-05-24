package com.budgetapp.thrifty.fragments;

import android.annotation.SuppressLint;
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
import com.budgetapp.thrifty.utils.AppLogger;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.checkerframework.checker.units.qual.A;

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

        NotificationsFragment.setNotificationListener(notification -> {
            requireActivity().runOnUiThread(() -> addNotification(notification));
        });

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
        if (currentUser == null) {
            Log.e(TAG, "Current user is null, cannot load notifications");
            return;
        }

        Log.d(TAG, "Starting to load notifications from Firestore");
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || !isViewCreated) {
                        Log.w(TAG, "Fragment not attached or view destroyed, skipping UI update");
                        return;
                    }

                    notificationList.clear();

                    int processedCount = 0;
                    int failedCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Notification notification = parseNotificationFromDocument(doc);
                        if (notification != null) {
                            notificationList.add(notification);
                            processedCount++;
                        } else {
                            failedCount++;
                        }
                    }

                    Log.d(TAG, String.format("Processed %d notifications, %d failed parsing",
                            processedCount, failedCount));
                    updateNotificationUI();

                    updateNotificationUI();
                    Log.d(TAG, "Generated " + notificationList.size() + " notifications from transactions");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transactions for notifications", e);
                });
    }

    private Notification parseNotificationFromDocument(DocumentSnapshot doc) {
        try {
            String type = doc.getString("type");
            String description = doc.getString("description");
            String recurring = doc.getString("recurring");
            Double amount = doc.getDouble("amount");

            if (recurring != null && amount != null && type != null) {
                @SuppressLint("DefaultLocale") String message = String.format("%s %s reminder: ₱%.2f - {%s} is due today.",
                        recurring,
                        type.toLowerCase(),
                        amount,
                        description);

                return new Notification(
                        type + " Reminder",
                        message,
                        doc.getDate("nextDueDate") != null ?
                                doc.getDate("nextDueDate").toString() : "Unknown time",
                        recurring,
                        R.drawable.icnotif_transactions
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing transaction for notification", e);
        }
        return null;
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

    // In NotificationsFragment.java
    public interface NotificationListener {
        void onNewNotification(Notification notification);
    }

    private static NotificationListener notificationListener;

    public static void setNotificationListener(NotificationListener listener) {
        notificationListener = listener;
    }

    public static void notifyNewNotification(Notification notification) {
        if (notificationListener != null) {
            notificationListener.onNewNotification(notification);
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
