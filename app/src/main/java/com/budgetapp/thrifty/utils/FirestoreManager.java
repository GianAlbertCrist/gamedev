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

        String transactionId = db.collection("users")
                .document(currentUser.getUid())
                .collection("transactions")
                .document().getId();

        db.collection("users")
                .document(currentUser.getUid())
                .collection("transactions")
                .document(transactionId)
                .set(transactionData)
                .addOnFailureListener(e -> Log.e(TAG, "Error saving transaction", e));
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

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("type", transaction.getType());
        transactionData.put("category", transaction.getCategory());
        transactionData.put("amount", transaction.getRawAmount());
        transactionData.put("dateTime", transaction.getParsedDate());
        transactionData.put("iconID", transaction.getIconID());
        transactionData.put("recurring", transaction.getRecurring());
        transactionData.put("description", transaction.getDescription());

        db.collection("users")
                .document(currentUser.getUid())
                .collection("transactions")
                .document(transaction.getId())
                .set(transactionData)
                .addOnSuccessListener(unused -> Log.d(TAG, "Transaction updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating transaction", e));
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
        DocumentReference transactionRef = db.collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId);

        // Add debug logging
        Log.d(TAG, "Attempting to delete transaction: " + transactionId);
        Log.d(TAG, "Full path: users/" + userId + "/transactions/" + transactionId);

        transactionRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "Transaction document not found at path: users/" + userId + "/transactions/" + transactionId);
                        if (listener != null) listener.onFailure(new Exception("Transaction not found"));
                        return;
                    }

                    // Document exists, log its data
                    Log.d(TAG, "Found transaction data: " + documentSnapshot.getData());

                    transactionRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Transaction document deleted successfully");
                                deleteAssociatedNotifications(userId, transactionId, listener);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting transaction document", e);
                                if (listener != null) listener.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking transaction existence", e);
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
                        if (listener != null) listener.onSuccess();
                        return;
                    }

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
                                if (listener != null) listener.onSuccess();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding notifications for transaction", e);
                    if (listener != null) listener.onSuccess();
                });
    }

    // Listener interfaces
    public interface OnProfileLoadedListener {
        void onProfileLoaded(Map<String, Object> profileData);
    }

    public interface OnTransactionsLoadedListener {
        void onTransactionsLoaded(List<Transaction> transactions);
    }
}