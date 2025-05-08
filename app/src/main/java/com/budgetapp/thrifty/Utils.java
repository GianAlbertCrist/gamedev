package com.budgetapp.thrifty;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Utils {
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
}
