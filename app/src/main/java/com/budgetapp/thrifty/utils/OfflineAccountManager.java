package com.budgetapp.thrifty.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;

public class OfflineAccountManager {
    private static final String PREFS_NAME = "OfflineAccountPrefs";
    private static final String KEY_EMAIL = "pending_email";
    private static final String KEY_PASSWORD = "pending_password";

    public static void savePendingAccount(Context context, String email, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .apply();
    }

    public static String getPendingEmail(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_EMAIL, null);
    }

    public static String getPendingPassword(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PASSWORD, null);
    }

    public static void clearPendingAccount(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static boolean hasPendingAccount(Context context) {
        return getPendingEmail(context) != null && getPendingPassword(context) != null;
    }

    public static void syncUserData(Context context, SyncCallback callback) {
        if (!hasPendingAccount(context)) return;

        String email = getPendingEmail(context);
        String password = getPendingPassword(context);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        clearPendingAccount(context);
                        if (callback != null) callback.onSuccess();
                    } else {
                        Exception e = task.getException();
                        if (callback != null) callback.onFailure(e);
                    }
                });
    }

    public interface SyncCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}