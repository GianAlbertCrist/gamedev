package com.budgetapp.thrifty.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KeyboardBehavior {
    /**
     * Hides the soft keyboard
     * @param activity Current activity
     * @param view The view that currently has focus
     */
    public static void hideKeyboard(Activity activity, View view) {
        if (activity != null && view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /**
     * Returns the current time in a specific format
     * @return A string representing the current time
     */
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a - MMM dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
