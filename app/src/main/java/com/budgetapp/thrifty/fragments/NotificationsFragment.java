package com.budgetapp.thrifty.fragments;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
import com.google.firebase.Timestamp;
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

        NotificationsFragment.setNotificationListener(notification -> {
            requireActivity().runOnUiThread(() -> addNotification(notification));
        });

        isViewCreated = true;
        loadDueNotificationsFromFirestore();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
    }

    private void loadDueNotificationsFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Current user is null, cannot load notifications");
            return;
        }

        Log.d(TAG, "Starting to load due notifications from Firestore");

        // Get current date for comparison
        Date currentDate = new Date();
        Calendar currentCal = Calendar.getInstance();
        currentCal.setTime(currentDate);
        // Reset time to start of day for accurate comparison
        currentCal.set(Calendar.HOUR_OF_DAY, 0);
        currentCal.set(Calendar.MINUTE, 0);
        currentCal.set(Calendar.SECOND, 0);
        currentCal.set(Calendar.MILLISECOND, 0);
        Date todayStart = currentCal.getTime();

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
                    int filteredCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        // Check if this notification should be shown based on due date
                        if (shouldShowNotification(doc, todayStart)) {
                            Notification notification = parseNotificationFromDocument(doc);
                            if (notification != null) {
                                notificationList.add(notification);
                                processedCount++;
                            }
                        } else {
                            filteredCount++;
                        }
                    }

                    Log.d(TAG, String.format("Processed %d due notifications, filtered out %d not-due notifications",
                            processedCount, filteredCount));

                    updateNotificationUI();
                    Log.d(TAG, "Generated " + notificationList.size() + " due notifications");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                });
    }

    private boolean shouldShowNotification(DocumentSnapshot doc, Date todayStart) {
        try {
            // Get the next due date from the notification document
            Timestamp nextDueTimestamp = doc.getTimestamp("nextDueDate");
            if (nextDueTimestamp == null) {
                Log.w(TAG, "No nextDueDate found for notification: " + doc.getId());
                return false;
            }

            Date nextDueDate = nextDueTimestamp.toDate();

            // Reset time to start of day for accurate comparison
            Calendar dueCal = Calendar.getInstance();
            dueCal.setTime(nextDueDate);
            dueCal.set(Calendar.HOUR_OF_DAY, 0);
            dueCal.set(Calendar.MINUTE, 0);
            dueCal.set(Calendar.SECOND, 0);
            dueCal.set(Calendar.MILLISECOND, 0);
            Date dueStart = dueCal.getTime();

            // Show notification if due date is today or in the past (overdue)
            boolean isDue = dueStart.compareTo(todayStart) <= 0;

            Log.d(TAG, String.format("Notification %s - Due: %s, Today: %s, Should show: %s",
                    doc.getId(),
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dueStart),
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todayStart),
                    isDue));

            return isDue;

        } catch (Exception e) {
            Log.e(TAG, "Error checking if notification should be shown: " + doc.getId(), e);
            return false;
        }
    }

    private Notification parseNotificationFromDocument(DocumentSnapshot doc) {
        try {
            String type = doc.getString("type");
            String description = doc.getString("description");
            String recurring = doc.getString("recurring");
            Timestamp timestamp = doc.getTimestamp("timestamp");
            String time = "Unknown time";
            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a - EEE, MMMM d, yyyy", Locale.getDefault());
                time = sdf.format(timestamp.toDate());
            }
            Long iconID = doc.getLong("iconID");

            if (type != null && description != null && recurring != null && iconID != null) {
                return new Notification(
                        type,
                        description,
                        time,
                        recurring,
                        iconID.intValue()
                );
            }

            Log.w(TAG, "Missing required fields in notification document: " + doc.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification document: " + doc.getId(), e);
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