package com.budgetapp.thrifty.utils;

import android.annotation.SuppressLint;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtils {

    @SuppressLint("DefaultLocale")
    public static String formatAmount(double amount, boolean isIncomeOrExpense) {
        if (isIncomeOrExpense) {
            if (amount >= 1_000_000) {
                return (amount % 1_000_000 == 0)
                        ? String.format("%.0fM", amount / 1_000_000)
                        : String.format("%.1fM", amount / 1_000_000);
            } else if (amount >= 200_000) {
                return (amount % 1_000 == 0)
                        ? String.format("%.0fK", amount / 1_000)
                        : String.format("%.1fK", amount / 1_000);
            }
        } else {
            if (amount >= 1_000_000) {
                return String.format("%.2fM", amount / 1_000_000);
            }
        }

        // Fallback: Force .00 if whole number, with commas (e.g. 12,000.00)
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }
}
