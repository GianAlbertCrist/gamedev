package com.budgetapp.thrifty;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

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
        ArrayList<PieEntry> entries = new ArrayList<>();

        // Calculate remaining income (current balance)
        float remainingIncome = income - expense;

        // Use expense and remaining income as percentages of total income
        float expensePercentage = (expense / income) * 100;
        float remainingPercentage = (remainingIncome / income) * 100;

        // Add entries in reverse order (expense first, then remaining income)
        if (expense > 0) {
            entries.add(new PieEntry(expensePercentage, "Expense"));
        }
        if (remainingIncome > 0) {
            entries.add(new PieEntry(remainingPercentage, "Income"));
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(100, "No Data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);

        ArrayList<Integer> colors = new ArrayList<>();
        if (income > 0) colors.add(ContextCompat.getColor(context, R.color.primary_color));
        if (expense > 0) colors.add(ContextCompat.getColor(context, R.color.red));
        if (entries.size() == 1 && income == 0 && expense == 0) {
            colors.add(Color.GRAY);
        }

        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        data.setDrawValues(true);
        data.setValueFormatter(new PercentFormatter(pieChart));

        float balance = income - expense;
        pieChart.setCenterText(String.format(Locale.getDefault(), "Current Balance\n₱%.2f", balance));

        pieChart.setData(data);
        pieChart.invalidate();
    }
}