package com.budgetapp.thrifty.utils;

import android.content.Context;
import android.view.Window;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.budgetapp.thrifty.R;

public class ThemeSync {

    public static void syncNotificationBarColor(Window window, Context context) {
        // Change status bar color to Lavender
        window.setStatusBarColor(ContextCompat.getColor(context, R.color.primary_color)); // Lavender color

        // Set the status bar icons to dark or light based on the color
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        insetsController.setAppearanceLightNavigationBars(true); // For navigation bar
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(true);
    }
}
