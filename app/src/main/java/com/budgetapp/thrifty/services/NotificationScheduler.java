package com.budgetapp.thrifty.services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.budgetapp.thrifty.MainActivity;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Date;

public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    private static final String CHANNEL_ID = "transaction_reminders";
    private static final int DAILY_CHECK_REQUEST_CODE = 1001;

    public static void scheduleDaily(Context context) {
        Log.d(TAG, "Scheduling daily notification check");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_CHECK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule for 8 AM daily
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If it's already past 8 AM today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );

        Log.d(TAG, "Daily notification scheduled for: " + calendar.getTime());
    }

    public static void cancelDaily(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_CHECK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Daily notification cancelled");
    }

    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "=== DAILY NOTIFICATION CHECK TRIGGERED ===");
            checkDueTransactions(context);
        }

        private void checkDueTransactions(Context context) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.w(TAG, "No user logged in, skipping notification check");
                return;
            }

            Log.d(TAG, "Checking due transactions for user: " + currentUser.getUid());

            FirestoreManager.loadTransactions(transactions -> {
                Date today = new Date();
                Calendar todayCal = Calendar.getInstance();
                todayCal.setTime(today);
                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                todayCal.set(Calendar.MINUTE, 0);
                todayCal.set(Calendar.SECOND, 0);
                todayCal.set(Calendar.MILLISECOND, 0);
                Date todayStart = todayCal.getTime();

                Calendar todayEnd = Calendar.getInstance();
                todayEnd.setTime(todayStart);
                todayEnd.set(Calendar.HOUR_OF_DAY, 23);
                todayEnd.set(Calendar.MINUTE, 59);
                todayEnd.set(Calendar.SECOND, 59);
                Date todayEndTime = todayEnd.getTime();

                int notificationCount = 0;

                for (Transaction transaction : transactions) {
                    if (transaction.getNextDueDate() != null &&
                            !transaction.getRecurring().equals("None")) {

                        Date dueDate = transaction.getNextDueDate();

                        // Check if due today
                        if (dueDate.compareTo(todayStart) >= 0 &&
                                dueDate.compareTo(todayEndTime) <= 0) {

                            Log.d(TAG, "Found due transaction: " + transaction.getDescription());
                            showNotification(context, transaction);
                            notificationCount++;
                        }
                    }
                }

                Log.d(TAG, "Sent " + notificationCount + " notifications");
            });
        }

        @SuppressLint("DefaultLocale")
        private void showNotification(Context context, Transaction transaction) {
            createNotificationChannel(context);

            String title = transaction.getType() + " Reminder";
            String body;
            if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                body = String.format("Reminder: ₱%.2f for '%s' is due today.",
                        transaction.getRawAmount(), transaction.getDescription());
            } else {
                body = String.format("Reminder: ₱%.2f %s is due today.",
                        transaction.getRawAmount(), transaction.getType().toLowerCase());
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("navigate_to", "transactions");
            intent.putExtra("transaction_id", transaction.getId());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    (int) System.currentTimeMillis(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());

            Log.d(TAG, "Notification shown: " + title);
        }

        private void createNotificationChannel(Context context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Transaction Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for recurring transactions");
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setShowBadge(true);

                NotificationManager notificationManager =
                        context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}