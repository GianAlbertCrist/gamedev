package com.budgetapp.thrifty.handlers;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.budgetapp.thrifty.R;
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
import java.util.Locale;

public class BarChartManager {
    private final BarChart barChart;
    private final Context context;
    private final TextView trendToggle;
    private boolean isShowingIncome = true;
    private static final String DATE_FORMAT = "MMMM d";
    private static final String TAG = "BarChartManager";

    public BarChartManager(BarChart barChart, TextView trendToggle, Context context) {
        this.barChart = barChart;
        this.trendToggle = trendToggle;
        this.context = context;
        setupBarChart();
        setupToggle();
    }

    private SimpleDateFormat getDateFormatter() {
        return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
    }

    private void setupBarChart() {
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false);
        barChart.setMaxVisibleValueCount(7);
        barChart.setVisibleXRangeMaximum(7);
        barChart.setExtraBottomOffset(12f);
        barChart.setHighlightPerTapEnabled(false);
        barChart.setHighlightPerDragEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);

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
            Log.d(TAG, "Toggle clicked, now showing: " + (isShowingIncome ? "Income" : "Expense"));
            updateBarChart(isShowingIncome);
        });
    }

    public void updateBarChart(boolean isIncome) {
        Log.d(TAG, "updateBarChart called with isIncome=" + isIncome);

        // Clear any existing data
        barChart.clear();

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> xLabels = getLastSevenDays();
        float[] dailyTotals = new float[7];

        // Get the start of the current week (Monday)
        Calendar weekStart = Calendar.getInstance();
        int dayOfWeek = weekStart.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = dayOfWeek - Calendar.MONDAY;
        if (daysToSubtract < 0) daysToSubtract += 7;
        weekStart.add(Calendar.DAY_OF_YEAR, -daysToSubtract);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);

        // Create array of week dates for comparison
        ArrayList<Calendar> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_YEAR, i);
            weekDates.add(day);
            Log.d(TAG, "Week day " + i + ": " + day.getTime());
        }

        // Debug transactions count
        Log.d(TAG, "Total transactions: " + TransactionsHandler.transactions.size());

        // Process transactions
        for (Transaction transaction : TransactionsHandler.transactions) {
            String transactionType = transaction.getType();
            if ((isIncome && "Income".equalsIgnoreCase(transactionType)) ||
                    (!isIncome && "Expense".equalsIgnoreCase(transactionType))) {

                // Parse transaction date
                Calendar txDate = Calendar.getInstance();
                txDate.setTime(transaction.getParsedDate());

                // Find which day index this transaction belongs to
                for (int i = 0; i < weekDates.size(); i++) {
                    Calendar day = weekDates.get(i);
                    if (txDate.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                            txDate.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) {
                        dailyTotals[i] += transaction.getRawAmount();
                        break;
                    }
                }
            }
        }

        // Create bar entries
        for (int i = 0; i < dailyTotals.length; i++) {
            entries.add(new BarEntry(i, dailyTotals[i]));
            Log.d(TAG, "Bar entry for day " + i + ": " + dailyTotals[i]);
        }

        // Update chart data
        BarDataSet dataSet = new BarDataSet(entries, isIncome ? "Income" : "Expense");
        dataSet.setColor(ContextCompat.getColor(context,
                isIncome ? R.color.primary_color : R.color.red));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value > 0 ? String.format(Locale.getDefault(), "₱%.0f", value) : "";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        barChart.setData(barData);
        barChart.animateY(1000);
        barChart.invalidate();

        Log.d(TAG, "Chart updated and invalidated");
    }

    private ArrayList<String> getLastSevenDays() {
        ArrayList<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Set to start of current week (Monday)
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = dayOfWeek - Calendar.MONDAY;
        if (daysToSubtract < 0) daysToSubtract += 7;
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract);

        // Add dates for the whole week
        for (int i = 0; i < 7; i++) {
            String dateStr = getDateFormatter().format(calendar.getTime());
            dates.add(dateStr);
            Log.d(TAG, "Added date label: " + dateStr);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return dates;
    }
}
