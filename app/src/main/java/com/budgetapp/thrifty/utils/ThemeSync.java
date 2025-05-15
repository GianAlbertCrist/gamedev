package com.budgetapp.thrifty.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Window;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.budgetapp.thrifty.R;

public class ThemeSync {

    public static void syncNotificationBarColor(Window window, Context context) {
        int targetColor = ContextCompat.getColor(context, R.color.primary_color);

        animateSystemBarColor(window, targetColor);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());

        boolean isLight = isColorLight(targetColor);
        controller.setAppearanceLightStatusBars(isLight);
        controller.setAppearanceLightNavigationBars(isLight);
    }

    private static void animateSystemBarColor(Window window, int toColor) {
        int fromStatusBarColor = window.getStatusBarColor();
        int fromNavBarColor = window.getNavigationBarColor();

        ValueAnimator statusAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), fromStatusBarColor, toColor);
        ValueAnimator navAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), fromNavBarColor, toColor);

        statusAnimator.addUpdateListener(anim -> window.setStatusBarColor((int) anim.getAnimatedValue()));
        navAnimator.addUpdateListener(anim -> window.setNavigationBarColor((int) anim.getAnimatedValue()));

        statusAnimator.setDuration(300); // milliseconds
        navAnimator.setDuration(300);

        statusAnimator.start();
        navAnimator.start();
    }

    private static boolean isColorLight(int color) {
        double luminance = (0.299 * ((color >> 16) & 0xFF)
                + 0.587 * ((color >> 8) & 0xFF)
                + 0.114 * (color & 0xFF)) / 255;
        return luminance > 0.5;
    }
}