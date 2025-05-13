package com.budgetapp.thrifty.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.transaction.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Save user profile data
    public static void saveUserProfile(String displayName, String email, int avatarId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("displayName", displayName);
        userData.put("email", email);
        userData.put("avatarId", avatarId);
        userData.put("role", "user");

        db.collection("users")
                .document(currentUser.getUid())
                .collection("profile")
                .document("info")
                .set(userData, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user profile", e));
    }

    // Save a transaction
    public static void saveTransaction(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("type", transaction.getType());
        transactionData.put("category", transaction.getCategory());
        transactionData.put("amount", transaction.getRawAmount());
        transactionData.put("dateTime", new Date());
        transactionData.put("iconID", transaction.getIconID());
        transactionData.put("recurring", transaction.getRecurring());
        transactionData.put("description", transaction.getDescription());

        // Generate a new document ID
        DocumentReference newTransactionRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("transactions")
                .document();

        // Set the ID on the transaction object
        String transactionId = newTransactionRef.getId();
        transaction.setId(transactionId);

        Log.d(TAG, "Saving transaction with ID: " + transactionId);

        // Save to Firestore
        newTransactionRef.set(transactionData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction successfully saved with ID: " + transactionId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving transaction with ID: " + transactionId, e);
                });
    }

    // Save a notification
    public static void saveNotification(Notification notification, String transactionId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", notification.getType());
        notificationData.put("message", notification.getDescription());
        notificationData.put("timestamp", new Date());
        notificationData.put("recurring", notification.getRecurring());
        notificationData.put("iconID", notification.getIconID());
        notificationData.put("transactionId", transactionId);

        db.collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .document()
                .set(notificationData)
                .addOnFailureListener(e -> Log.e(TAG, "Error saving notification", e));
    }

    // Load user profile data
    public static void loadUserProfile(OnProfileLoadedListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .collection("profile")
                .document("info")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        listener.onProfileLoaded(document.getData());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading profile", e));
    }

    // Load user transactions
    public static void loadTransactions(OnTransactionsLoadedListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

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

                            transactions.add(transaction);
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document: " + doc.getId(), e);
                        }
                    }
                    listener.onTransactionsLoaded(transactions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transactions", e);
                    listener.onTransactionsLoaded(new ArrayList<>()); // Return empty list on error
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
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
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

    // Listener interfaces
    public interface OnProfileLoadedListener {
        void onProfileLoaded(Map<String, Object> profileData);
    }

    public interface OnTransactionsLoadedListener {
        void onTransactionsLoaded(List<Transaction> transactions);
    }
}
