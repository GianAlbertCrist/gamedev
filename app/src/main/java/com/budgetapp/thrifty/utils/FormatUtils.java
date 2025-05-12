package com.budgetapp.thrifty.utils;

import android.annotation.SuppressLint;
import java.text.DecimalFormat;

public class FormatUtils {

    @SuppressLint("DefaultLocale")
    public static String formatAmount(double amount, boolean isIncomeOrExpense) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }
}