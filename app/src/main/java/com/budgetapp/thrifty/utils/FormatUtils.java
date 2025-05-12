package com.budgetapp.thrifty.utils;

import android.annotation.SuppressLint;
import java.text.DecimalFormat;

public class FormatUtils {

    @SuppressLint("DefaultLocale")
    public static String formatAmount(double amount, boolean isIncomeOrExpense) {
        if (isIncomeOrExpense) {
            // Array of suffixes for different scales
            String[] suffixes = {"", "K", "M", "B", "T", "Qa", "E", "Z", "Y"};
            int suffixIndex = 0;
            double scaledAmount = amount;

            // Scale down the number and determine appropriate suffix
            while (scaledAmount >= 1000 && suffixIndex < suffixes.length - 1) {
                scaledAmount /= 1000;
                suffixIndex++;
            }

            // Format with appropriate precision
            if (suffixIndex > 0) {
                if (scaledAmount % 1 == 0) {
                    return String.format("%.0f%s", scaledAmount, suffixes[suffixIndex]);
                } else {
                    return String.format("%.1f%s", scaledAmount, suffixes[suffixIndex]);
                }
            }
        }

        // Fallback: Format with commas and two decimal places
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }
}