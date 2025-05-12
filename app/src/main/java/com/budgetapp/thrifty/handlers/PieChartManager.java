package com.budgetapp.thrifty.handlers;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.utils.FormatUtils;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class PieChartManager {
    private final PieChart pieChart;
    private final Context context;
    private static final String TAG = "PieChartManager";

    public PieChartManager(PieChart pieChart, Context context) {
        this.pieChart = pieChart;
        this.context = context;
        setupPieChart();
    }

    private void setupPieChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setCenterText("Balance Overview");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(ContextCompat.getColor(context, R.color.primary_color));
        pieChart.getDescription().setEnabled(false);

        Legend legend = pieChart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
    }

    public void updateChart(float income, float expense) {
        Log.d(TAG, "Updating pie chart with income: " + income + ", expense: " + expense);
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        // Calculate balance
        float balance = income - expense;
        Log.d(TAG, "Calculated balance: " + FormatUtils.formatAmount(balance, true));

        // Set center text color based on balance
        if (balance > 0) {
            pieChart.setCenterTextColor(ContextCompat.getColor(context, R.color.primary_color));
        } else if (balance < 0) {
            pieChart.setCenterTextColor(ContextCompat.getColor(context, R.color.red));
        } else {
            pieChart.setCenterTextColor(ContextCompat.getColor(context, R.color.grey));
        }

        // Set center text with balance
        pieChart.setCenterText(String.format(Locale.getDefault(), "Current Balance\n₱%.2f", balance));

        // Handle special cases
        if (income == 0 && expense == 0) {
            // No data case
            entries.add(new PieEntry(100, "No Data"));
            colors.add(ContextCompat.getColor(context, R.color.grey));
        } else if (income == 0) {
            // Only expenses
            entries.add(new PieEntry(100, "Expense"));
            colors.add(ContextCompat.getColor(context, R.color.red));
        } else if (expense == 0) {
            // Only income
            entries.add(new PieEntry(100, "Income"));
            colors.add(ContextCompat.getColor(context, R.color.primary_color));
        } else {
            // Both income and expense
            float total = income + expense; // Use total for percentage calculation

            // Add income entry (always use primary color)
            entries.add(new PieEntry((income / total) * 100, "Income"));
            colors.add(ContextCompat.getColor(context, R.color.primary_color));

            // Add expense entry (always use red)
            entries.add(new PieEntry((expense / total) * 100, "Expense"));
            colors.add(ContextCompat.getColor(context, R.color.red));
        }

        // Create and configure the dataset
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        // Create and configure the data
        PieData data = new PieData(dataSet);
        data.setDrawValues(true);
        data.setValueFormatter(new PercentFormatter(pieChart));

        // Set the data and refresh
        pieChart.setData(data);
        pieChart.invalidate();

        Log.d(TAG, "Pie chart updated with " + entries.size() + " entries");
    }
}
