package com.budgetapp.thrifty;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.transaction.Transaction;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class BarChartManager {
    private final BarChart barChart;
    private final Context context;
    private final TextView trendToggle;
    private boolean isShowingIncome = true;

    public BarChartManager(BarChart barChart, TextView trendToggle, Context context) {
        this.barChart = barChart;
        this.trendToggle = trendToggle;
        this.context = context;
        setupBarChart();
        setupToggle();
    }

    private void setupBarChart() {
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false);
        barChart.setMaxVisibleValueCount(7);
        barChart.setVisibleXRangeMaximum(7);
        barChart.setHighlightPerTapEnabled(false);
        barChart.setHighlightPerDragEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setSpaceTop(35f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(12f);

        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
    }

    private void setupToggle() {
        trendToggle.setOnClickListener(v -> {
            isShowingIncome = !isShowingIncome;
            trendToggle.setText(isShowingIncome ? "Income Trend" : "Expense Trend");
            updateBarChart(isShowingIncome);
        });
    }

    public void updateBarChart(boolean isIncome) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> xLabels = getLastSevenDays();
        float[] dailyTotals = new float[7];

        // Fetch transactions and filter by type
        for (Transaction transaction : TransactionsHandler.transactions) {
            String transactionType = transaction.getType();
            if ((isIncome && "Income".equalsIgnoreCase(transactionType)) ||
                    (!isIncome && "Expense".equalsIgnoreCase(transactionType))) {
                String transactionDate = transaction.getDateAndTime().split(" - ")[1]; // Extract date
                int index = xLabels.indexOf(transactionDate);
                if (index != -1) {
                    dailyTotals[index] += transaction.getRawAmount();
                }
            }
        }

        // Populate entries for the bar graph
        for (int i = 0; i < dailyTotals.length; i++) {
            entries.add(new BarEntry(i, dailyTotals[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, isIncome ? "Income" : "Expense");
        dataSet.setColor(isIncome ?
                ContextCompat.getColor(context, R.color.primary_color) :
                ContextCompat.getColor(context, R.color.red));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "₱%.0f", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        barChart.setData(barData);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private ArrayList<String> getLastSevenDays() {
        ArrayList<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            dates.add(sdf.format(calendar.getTime()));
        }
        Collections.reverse(dates);
        return dates;
    }
}