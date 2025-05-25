package com.budgetapp.thrifty.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.fragments.NotificationsFragment;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.transaction.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Save user profile data
    public static void saveUserProfile(String displayName, String email, int avatarId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        String[] nameParts = displayName != null ? displayName.split("\\|") : new String[]{displayName};
        String username = nameParts[0];
        String fullname = nameParts.length > 1 ? nameParts[1] : username;

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("fullname", fullname);
        userData.put("email", email);
        userData.put("avatarId", avatarId);

        db.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            String rootRole = snapshot.contains("role") ? snapshot.getString("role") : null;
            if (!"admin".equalsIgnoreCase(rootRole)) {
                userData.put("role", "user");
            }

            db.collection("users").document(uid)
                    .collection("profile").document("info")
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User profile/info saved successfully");

                        SharedPreferences prefs = FirebaseAuth.getInstance().getApp().getApplicationContext()
                                .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("username", username)
                                .putString("fullname", fullname)
                                .putInt("avatarId", avatarId)
                                .apply();

                        saveFCMToken();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving user profile", e));
        });
    }

    // Save FCM token to Firestore
    public static void saveFCMToken() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save token to Firestore
                    Map<String, Object> tokenData = new HashMap<>();
                    tokenData.put("fcmToken", token);
                    tokenData.put("lastUpdated", new Date());
                    tokenData.put("platform", "android");

                    db.collection("users").document(uid)
                            .collection("tokens").document("fcm")
                            .set(tokenData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved successfully"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM token", e));
                });
    }


    // Save a transaction
    public static void saveTransaction(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("type", transaction.getType());
        transactionData.put("category", transaction.getCategory());
        transactionData.put("amount", transaction.getRawAmount());
        transactionData.put("dateTime", new Date());
        transactionData.put("iconID", transaction.getIconID());
        transactionData.put("recurring", transaction.getRecurring());
        transactionData.put("description", transaction.getDescription());

        // Add next due date for recurring transactions
        if (!transaction.getRecurring().equals("None") && transaction.getNextDueDate() != null) {
            transactionData.put("nextDueDate", transaction.getNextDueDate());
        }

        // Generate a new document ID
        DocumentReference newTransactionRef = db.collection("users")
                .document(uid)
                .collection("transactions")
                .document();

        String transactionId = newTransactionRef.getId();
        transaction.setId(transactionId);

        Log.d(TAG, "Saving transaction with ID: " + transactionId);

        newTransactionRef.set(transactionData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction successfully saved with ID: " + transactionId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving transaction with ID: " + transactionId, e);
                });
    }


    public static void saveNotification(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Only create notification if transaction has a valid next due date
        if (transaction.getNextDueDate() == null) {
            Log.w(TAG, "No next due date for transaction, skipping notification creation");
            return;
        }

        // Build notification data for Firestore
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("transactionId", transaction.getId());
        notificationData.put("lastNotified", null); // Will be set when actually notified
        notificationData.put("nextDueDate", transaction.getNextDueDate());
        notificationData.put("recurring", transaction.getRecurring());
        notificationData.put("timestamp", new Date());
        notificationData.put("isNotified", false); // Mark as not yet notified

        String type = transaction.getType() + " Reminder";
        String description;
        if (transaction.getDescription() == null || transaction.getDescription().isEmpty()) {
             description = String.format("%s %s reminder: ₱%.2f - is due today.",
                    transaction.getRecurring(),
                    transaction.getType().toLowerCase(),
                    transaction.getRawAmount());
        } else {
             description = String.format("%s %s reminder: ₱%.2f - {%s} is due today.",
                    transaction.getRecurring(),
                    transaction.getType().toLowerCase(),
                    transaction.getRawAmount(),
                    transaction.getDescription());
        }
        String time = new SimpleDateFormat("h:mm a - MMMM d", Locale.getDefault()).format(new Date());
        int iconID = R.drawable.icnotif_transactions;

        notificationData.put("type", type);
        notificationData.put("description", description);
        notificationData.put("time", time);
        notificationData.put("iconID", iconID);

        // Use a unique document ID based on transaction ID and due date
        String docId = transaction.getId() + "_" + transaction.getNextDueDate().getTime();

        db.collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .document(docId)
                .set(notificationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification saved with next due date: " + transaction.getNextDueDate());

                    // Only create in-app notification if due date is today or past
                    Date currentDate = new Date();
                    if (transaction.getNextDueDate().compareTo(currentDate) <= 0) {
                        Notification notification = new Notification(type, description, time, transaction.getRecurring(), iconID);
                        NotificationsFragment.notifyNewNotification(notification);
                        sendPushNotification(transaction);

                        // Mark as notified and create next occurrence
                        markNotificationAsNotified(docId);

                        // Calculate and create next recurring notification
                        if (!transaction.getRecurring().equals("None")) {
                            Date nextDueDate = calculateNextDueDate(transaction.getNextDueDate(), transaction.getRecurring());
                            if (nextDueDate != null) {
                                // Update the transaction's next due date
                                updateTransactionNextDueDate(transaction.getId(), nextDueDate);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving notification", e));
    }

    public static void createNextRecurringNotification(DocumentSnapshot currentNotificationDoc, Date newNextDueDate) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        try {
            // Get data from current notification
            String transactionId = currentNotificationDoc.getString("transactionId");
            String type = currentNotificationDoc.getString("type");
            String description = currentNotificationDoc.getString("description");
            String recurring = currentNotificationDoc.getString("recurring");
            Long iconID = currentNotificationDoc.getLong("iconID");

            if (transactionId == null || type == null || description == null ||
                    recurring == null || iconID == null) {
                Log.e(TAG, "Missing required fields in current notification");
                return;
            }

            // Create new notification data for next occurrence
            Map<String, Object> newNotificationData = new HashMap<>();
            newNotificationData.put("transactionId", transactionId);
            newNotificationData.put("lastNotified", null); // Will be set when actually notified
            newNotificationData.put("nextDueDate", newNextDueDate);
            newNotificationData.put("recurring", recurring);
            newNotificationData.put("timestamp", new Date());
            newNotificationData.put("type", type);
            newNotificationData.put("description", description);
            newNotificationData.put("time", new SimpleDateFormat("h:mm a - MMMM d", Locale.getDefault()).format(new Date()));
            newNotificationData.put("iconID", iconID);
            newNotificationData.put("isNotified", false); // Mark as not yet notified

            // Create unique document ID for next occurrence
            String newDocId = transactionId + "_" + newNextDueDate.getTime();

            db.collection("users")
                    .document(currentUser.getUid())
                    .collection("notifications")
                    .document(newDocId)
                    .set(newNotificationData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, String.format("Next recurring notification created for transaction %s, due: %s",
                                transactionId,
                                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newNextDueDate)));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating next recurring notification", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error processing recurring notification", e);
        }
    }

    public static void markNotificationAsNotified(String notificationId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isNotified", true);
        updates.put("lastNotified", new Date());

        db.collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .document(notificationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as notified: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as notified", e);
                });
    }

    // Send a push notification via FCM
    private static void sendPushNotification(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Format notification title and body
        String title = transaction.getType() + " Reminder";
        String body = String.format("%s %s reminder: ₱%.2f - {%s} is due today.",
                transaction.getRecurring(),
                transaction.getType().toLowerCase(),
                transaction.getRawAmount(),
                transaction.getDescription());

        // Get the user's FCM token
        db.collection("users")
                .document(currentUser.getUid())
                .collection("tokens")
                .document("fcm")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getString("fcmToken") != null) {
                        String fcmToken = documentSnapshot.getString("fcmToken");

                        // Create FCM message
                        Map<String, Object> message = new HashMap<>();
                        Map<String, Object> notification = new HashMap<>();
                        Map<String, Object> data = new HashMap<>();

                        notification.put("title", title);
                        notification.put("body", body);

                        data.put("transactionId", transaction.getId());
                        data.put("recurring", transaction.getRecurring());
                        data.put("type", transaction.getType());

                        message.put("notification", notification);
                        message.put("data", data);
                        message.put("token", fcmToken);

                        // Log the notification for debugging
                        Log.d(TAG, "Sending FCM notification: " + title + " - " + body);
                        Log.d(TAG, "To token: " + fcmToken);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error fetching FCM token", e)
                );
    }

    public static void getDueNotificationCount(OnNotificationCountListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onCountReceived(0);
            return;
        }

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

        db.collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int dueCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (shouldShowNotificationForBadge(doc, todayStart)) {
                            dueCount++;
                        }
                    }

                    Log.d(TAG, "Due notifications count for badge: " + dueCount);
                    listener.onCountReceived(dueCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting notification count", e);
                    listener.onCountReceived(0);
                });
    }

    private static boolean shouldShowNotificationForBadge(DocumentSnapshot doc, Date todayStart) {
        try {
            // Get the next due date from the notification document
            com.google.firebase.Timestamp nextDueTimestamp = doc.getTimestamp("nextDueDate");
            if (nextDueTimestamp == null) {
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
            boolean isDueToday = dueStart.compareTo(todayStart) == 0;
            boolean isOverdue = dueStart.compareTo(todayStart) < 0;
            boolean shouldShow = isDueToday || isOverdue;

            // However, hide notifications that are more than 1 day old to prevent clutter
            Calendar yesterdayStart = Calendar.getInstance();
            yesterdayStart.setTime(todayStart);
            yesterdayStart.add(Calendar.DAY_OF_MONTH, -1);

            boolean isTooOld = dueStart.compareTo(yesterdayStart.getTime()) < 0;
            if (isTooOld) {
                shouldShow = false;
            }

            return shouldShow;

        } catch (Exception e) {
            Log.e(TAG, "Error checking if notification should be shown for badge: " + doc.getId(), e);
            return false;
        }
    }

    public interface OnNotificationCountListener {
        void onCountReceived(int count);
    }

    // Load user transactions
    public static void loadTransactions(OnTransactionsLoadedListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onTransactionsLoaded(new ArrayList<>());
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .collection("transactions")
                .orderBy("dateTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            // Create new transaction with required fields
                            Transaction transaction = new Transaction(
                                    doc.getString("type"),
                                    doc.getString("category"),
                                    doc.getDouble("amount").floatValue(),
                                    doc.getLong("iconID").intValue(),
                                    doc.getString("description"),
                                    doc.getString("recurring")
                            );

                            // Set ID and date
                            transaction.setId(doc.getId());
                            if (doc.getDate("dateTime") != null) {
                                transaction.setParsedDate(doc.getDate("dateTime"));
                            }

                            // Set next due date if available
                            if (doc.getDate("nextDueDate") != null) {
                                transaction.setNextDueDate(doc.getDate("nextDueDate"));
                            } else if (!transaction.getRecurring().equals("None")) {
                                // Calculate next due date if not available
                                transaction.calculateNextDueDate();

                                // Update the transaction in Firestore with the calculated next due date
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("nextDueDate", transaction.getNextDueDate());

                                db.collection("users")
                                        .document(currentUser.getUid())
                                        .collection("transactions")
                                        .document(transaction.getId())
                                        .update(updates)
                                        .addOnSuccessListener(aVoid ->
                                                Log.d(TAG, "Next due date updated for transaction: " + transaction.getId()))
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "Error updating next due date", e));
                            }

                            transactions.add(transaction);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document: " + doc.getId(), e);
                        }
                    }
                    listener.onTransactionsLoaded(transactions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transactions", e);
                    listener.onTransactionsLoaded(new ArrayList<>());
                });
    }

    public static void updateTransaction(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String transactionId = transaction.getId();

        Log.d(TAG, "Updating transaction with ID: " + transactionId);

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("type", transaction.getType());
        transactionData.put("category", transaction.getCategory());
        transactionData.put("amount", transaction.getRawAmount());
        transactionData.put("iconID", transaction.getIconID());
        transactionData.put("recurring", transaction.getRecurring());
        transactionData.put("description", transaction.getDescription());
        transactionData.put("dateTime", transaction.getParsedDate() != null ? transaction.getParsedDate() : new Date());

        // Add next due date for recurring transactions
        if (!transaction.getRecurring().equals("None") && transaction.getNextDueDate() != null) {
            transactionData.put("nextDueDate", transaction.getNextDueDate());
        } else if (transaction.getRecurring().equals("None")) {
            // Remove next due date if not recurring
            transactionData.put("nextDueDate", FieldValue.delete());
        }

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        documentSnapshot.getReference().update(transactionData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Transaction successfully updated");

                                    if (!transaction.getRecurring().equals("None")) {
                                        Notification updatedNotification = new Notification(
                                                "Transaction",
                                                transaction.getCategory() + " | ₱" + transaction.getRawAmount(),
                                                KeyboardBehavior.getCurrentTime(),
                                                transaction.getRecurring(),
                                                transaction.getIconID()
                                        );
                                        updateNotification(updatedNotification, transactionId);
                                    } else {
                                        // Delete notifications if recurring is set to None
                                        deleteNotificationsForTransaction(transactionId);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating transaction", e));
                    } else {
                        Log.e(TAG, "Transaction document not found: " + transactionId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking transaction existence", e));
    }

    public interface OnDeleteTransactionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Delete a transaction and its associated notifications
    public static void deleteTransaction(String transactionId, OnDeleteTransactionListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (listener != null) listener.onFailure(new Exception("User not logged in"));
            return;
        }

        String userId = currentUser.getUid();

        // Add extensive debug logging
        Log.d(TAG, "Starting deletion process for transaction: " + transactionId);
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Full path: users/" + userId + "/transactions/" + transactionId);

        // First check if the transaction exists to provide better debugging
        db.collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Transaction found in Firestore. Data: " + documentSnapshot.getData());

                        // Now delete it
                        documentSnapshot.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Transaction successfully deleted from Firestore");
                                    deleteAssociatedNotifications(userId, transactionId, listener);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting transaction from Firestore", e);
                                    if (listener != null) listener.onFailure(e);
                                });
                    } else {
                        Log.w(TAG, "Transaction not found in Firestore. This could be because:");
                        Log.w(TAG, "1. The transaction was never saved to Firestore");
                        Log.w(TAG, "2. The transaction ID is incorrect");
                        Log.w(TAG, "3. The transaction was already deleted");

                        // Try to delete it anyway in case there's a sync issue
                        db.collection("users")
                                .document(userId)
                                .collection("transactions")
                                .document(transactionId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Delete operation completed (though document didn't exist)");
                                    if (listener != null) listener.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error in delete operation", e);
                                    if (listener != null) listener.onFailure(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if transaction exists", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    private static void deleteAssociatedNotifications(String userId, String transactionId, OnDeleteTransactionListener listener) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("transactionId", transactionId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No associated notifications found for transaction: " + transactionId);
                        if (listener != null) listener.onSuccess();
                        return;
                    }

                    Log.d(TAG, "Found " + querySnapshot.size() + " notifications to delete");
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        batch.delete(document.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(batchResult -> {
                                Log.d(TAG, "Associated notifications successfully deleted");
                                if (listener != null) listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting notifications", e);
                                // Still consider the transaction deletion a success
                                if (listener != null) listener.onSuccess();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding notifications for transaction", e);
                    if (listener != null) listener.onSuccess();
                });
    }

    public static void deleteNotificationsForTransaction(String transactionId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("transactionId", transactionId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        batch.delete(document.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Notifications deleted successfully"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Error deleting notifications", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error querying notifications", e));
    }

    // Update notification for a transaction
    public static void updateNotification(Notification notification, String transactionId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", notification.getType());
        notificationData.put("message", notification.getDescription());
        notificationData.put("timestamp", new Date());
        notificationData.put("recurring", notification.getRecurring());
        notificationData.put("iconID", notification.getIconID());
        notificationData.put("transactionId", transactionId);

        Log.d(TAG, "Updating notifications for transaction ID: " + transactionId);

        // Find all notifications for this transaction
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("transactionId", transactionId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int notificationCount = querySnapshot.size();
                    Log.d(TAG, "Found " + notificationCount + " notifications for transaction ID: " + transactionId);

                    if (notificationCount > 0) {
                        // Update the first notification
                        DocumentSnapshot firstDoc = querySnapshot.getDocuments().get(0);
                        firstDoc.getReference().update(notificationData)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "First notification updated successfully"))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Error updating first notification", e));

                        // Delete any additional notifications (if there are duplicates)
                        if (notificationCount > 1) {
                            Log.d(TAG, "Found " + (notificationCount - 1) + " duplicate notifications to delete");
                            WriteBatch batch = db.batch();
                            for (int i = 1; i < querySnapshot.size(); i++) {
                                DocumentSnapshot doc = querySnapshot.getDocuments().get(i);
                                batch.delete(doc.getReference());
                            }

                            batch.commit()
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "Successfully deleted " + (notificationCount - 1) + " duplicate notifications"))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error deleting duplicate notifications", e));
                        }
                    } else {
                        // Create new notification if none exists
                        db.collection("users")
                                .document(userId)
                                .collection("notifications")
                                .add(notificationData)
                                .addOnSuccessListener(documentReference ->
                                        Log.d(TAG, "New notification created with ID: " + documentReference.getId()))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Error creating notification", e));
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error querying notifications", e));
    }

    // Update the next due date for a transaction
    public static void updateNextDueDate(String transactionId, Date newNextDueDate) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("nextDueDate", newNextDueDate);

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Next due date updated for transaction: " + transactionId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating next due date", e));
    }

    private static Date calculateNextDueDate(Date currentDueDate, String recurring) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDueDate);

        switch (recurring.toLowerCase()) {
            case "daily":
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case "weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "monthly":
                calendar.add(Calendar.MONTH, 1);
                break;
            case "yearly":
                calendar.add(Calendar.YEAR, 1);
                break;
            default:
                Log.w(TAG, "Unknown recurring type: " + recurring);
                return null;
        }

        return calendar.getTime();
    }

    private static void updateTransactionNextDueDate(String transactionId, Date newNextDueDate) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("nextDueDate", newNextDueDate);

        db.collection("users")
                .document(currentUser.getUid())
                .collection("transactions")
                .document(transactionId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, String.format("Transaction %s next due date updated to: %s",
                            transactionId,
                            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newNextDueDate)));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating transaction next due date", e);
                });
    }



    public interface OnTransactionsLoadedListener {
        void onTransactionsLoaded(List<Transaction> transactions);
    }
}