package com.budgetapp.thrifty.services;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.budgetapp.thrifty.MainActivity;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;
import java.util.Map;

public class ThriftyMessagingService extends FirebaseMessagingService {
    private static final String TAG = "ThriftyMessagingService";
    private static final String CHANNEL_ID = "transaction_reminders";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== ThriftyMessagingService Created ===");
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "=== FCM MESSAGE RECEIVED ===");
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message ID: " + remoteMessage.getMessageId());
        Log.d(TAG, "Data size: " + remoteMessage.getData().size());
        Log.d(TAG, "Data: " + remoteMessage.getData());
        Log.d(TAG, "Has notification: " + (remoteMessage.getNotification() != null));

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "Notification body: " + remoteMessage.getNotification().getBody());
        }

        // Check if app is in foreground
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
        boolean isInForeground = false;
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (String activeProcess : processInfo.pkgList) {
                    if (activeProcess.equals(getPackageName())) {
                        isInForeground = true;
                        break;
                    }
                }
            }
        }
        Log.d(TAG, "App is in foreground: " + isInForeground);

        // Always create notification regardless of app state
        String title = "Transaction Reminder";
        String body = "You have a transaction reminder";
        String transactionId = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            if (data.get("title") != null) title = data.get("title");
            if (data.get("body") != null) body = data.get("body");
            transactionId = data.get("transactionId");
        }

        Log.d(TAG, "Creating notification with title: " + title + ", body: " + body);
        sendNotification(title, body, transactionId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel: " + CHANNEL_ID);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Transaction Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for recurring transactions");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            // Verify channel creation
            NotificationChannel createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            Log.d(TAG, "Channel created successfully: " + (createdChannel != null));
            if (createdChannel != null) {
                Log.d(TAG, "Channel importance: " + createdChannel.getImportance());
                Log.d(TAG, "Channel can show badge: " + createdChannel.canShowBadge());
            }
        }
    }

    private void sendNotification(String title, String messageBody, String transactionId) {
        Log.d(TAG, "=== CREATING NOTIFICATION ===");
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Body: " + messageBody);
        Log.d(TAG, "Transaction ID: " + transactionId);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        if (transactionId != null) {
            intent.putExtra("navigate_to", "transactions");
            intent.putExtra("transaction_id", transactionId);
        }

        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pendingIntent)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Check if notifications are enabled
        boolean notificationsEnabled = notificationManager.areNotificationsEnabled();
        Log.d(TAG, "Notifications enabled: " + notificationsEnabled);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (channel != null) {
                Log.d(TAG, "Channel importance: " + channel.getImportance());
                Log.d(TAG, "Channel blocked: " + (channel.getImportance() == NotificationManager.IMPORTANCE_NONE));
            } else {
                Log.e(TAG, "Notification channel not found!");
                createNotificationChannel();
            }
        }

        int notificationId = requestCode;
        notificationManager.notify(notificationId, notificationBuilder.build());

        Log.d(TAG, "Notification posted with ID: " + notificationId);
        Log.d(TAG, "=== NOTIFICATION CREATION COMPLETE ===");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "=== NEW FCM TOKEN RECEIVED ===");
        Log.d(TAG, "New token: " + token);
        FirestoreManager.saveFCMToken();
    }
}