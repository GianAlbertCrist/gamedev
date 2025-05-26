package com.budgetapp.thrifty.fragments;

import java.text.ParseException;
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
import com.budgetapp.thrifty.utils.FirestoreManager;
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

        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        isViewCreated = true;
        loadDueNotificationsFromFirestore();
        FirestoreManager.markAllDueNotificationsAsViewed();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload notifications when fragment resumes to get fresh data
        loadDueNotificationsFromFirestore();
        FirestoreManager.markAllDueNotificationsAsViewed();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;

        // Clear the static listener to prevent memory leaks
        notificationListener = null;

        Bundle result = new Bundle();
        result.putBoolean("notificationsViewed", true);
        getParentFragmentManager().setFragmentResult("notificationsViewed", result);
    }

    private void loadDueNotificationsFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Current user is null, cannot load notifications");
            return;
        }

        Log.d(TAG, "Starting to load due notifications from Firestore");

        // FIXED: Clear the list at the start to prevent accumulation
        notificationList.clear();

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

                    // Don't clear here since we already cleared at the start
                    int processedCount = 0;
                    int filteredCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        // Check if this notification should be shown based on due date
                        if (shouldShowNotification(doc, todayStart)) {
                            Notification notification = parseNotificationFromDocument(doc);
                            if (notification != null) {
                                // FIXED: Use addNotificationSafely to prevent duplicates
                                addNotificationSafely(notification);
                                processedCount++;
                            }
                        } else {
                            filteredCount++;
                        }
                    }

                    Log.d(TAG, String.format("Processed %d due notifications, filtered out %d not-due notifications",
                            processedCount, filteredCount));

                    updateNotificationUI();
                    Log.d(TAG, "Loaded " + notificationList.size() + " due notifications");

                    // Mark all displayed notifications as viewed
                    FirestoreManager.markAllDueNotificationsAsViewed();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                });
    }

    private boolean shouldShowNotification(DocumentSnapshot doc, Date todayStart) {
        try {
            Boolean isViewed = doc.getBoolean("isViewed");

            Timestamp nextDueTimestamp = doc.getTimestamp("nextDueDate");
            if (nextDueTimestamp == null) {
                String dueDateStr = doc.getString("dueDate");
                if (dueDateStr == null || dueDateStr.isEmpty()) {
                    return true; // Show notifications without a due date
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date dueDate = sdf.parse(dueDateStr);
                if (dueDate != null) {
                    return !dueDate.before(todayStart); // Show if due date is today or in the future
                } else {
                    return true; // Show if there's an issue parsing the date
                }
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
            boolean isDueToday = dueStart.compareTo(todayStart) == 0;
            boolean isOverdue = dueStart.compareTo(todayStart) < 0;
            boolean shouldShow = isDueToday || isOverdue;

            // However, hide notifications that are more than 7 days old to prevent clutter
            Calendar weekAgoStart = Calendar.getInstance();
            weekAgoStart.setTime(todayStart);
            weekAgoStart.add(Calendar.DAY_OF_MONTH, -7);

            boolean isTooOld = dueStart.compareTo(weekAgoStart.getTime()) < 0;
            if (isTooOld) {
                shouldShow = false;
            }

            return shouldShow;

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing due date", e);
            return true; // Show if there's an error
        } catch (Exception e) {
            Log.e(TAG, "Error checking if notification should be shown", e);
            return true; // Show if there's an error
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

    private void addNotificationSafely(Notification notification) {
        // Check for duplicates before adding
        boolean isDuplicate = false;
        for (Notification existing : notificationList) {
            if (existing.getDescription().equals(notification.getDescription()) &&
                    existing.getType().equals(notification.getType()) &&
                    existing.getRecurring().equals(notification.getRecurring())) {
                isDuplicate = true;
                Log.d(TAG, "Duplicate notification detected, skipping: " + notification.getDescription());
                break;
            }
        }

        if (!isDuplicate) {
            notificationList.add(notification);
            Log.d(TAG, "Added notification: " + notification.getDescription());
        }
    }

    // FIXED: Updated addNotification method to use safe adding
    public void addNotification(Notification notification) {
        addNotificationSafely(notification);
        updateNotificationUI();
    }

    // Keep the interface and static methods for backward compatibility, but make them safer
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