package com.budgetapp.thrifty;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;

public class ReportsFragment extends Fragment {
    private PieChartManager pieChartManager;
    private BarChartManager barChartManager;
    private TextView tvBalanceAmount, tvTotalIncome, tvTotalExpense;

    private float income;
    private float expense;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        updateValues();

        try {
            // Initialize views
            PieChart pieChart = view.findViewById(R.id.pieChart);
            BarChart barChart = view.findViewById(R.id.barChart);
            TextView trendToggle = view.findViewById(R.id.trendToggle);
            tvBalanceAmount = view.findViewById(R.id.tvBalanceAmount);
            tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
            tvTotalExpense = view.findViewById(R.id.tvTotalExpense);

            // Initialize managers
            pieChartManager = new PieChartManager(pieChart, requireContext());
            barChartManager = new BarChartManager(barChart, trendToggle, requireContext());

        } catch (Exception e) {
            Log.e("ReportsFragment", "Error initializing views or managers", e);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateValues();
    }

    private void updateValues() {
        try {
            income = TransactionsHandler.getTotalIncome();
            expense = TransactionsHandler.getTotalExpense();
            float balance = income - expense;

            tvTotalIncome.setText(String.format(requireContext().getString(R.string.currency_format), income));
            tvTotalExpense.setText(String.format(requireContext().getString(R.string.currency_format), expense));
            tvBalanceAmount.setText(String.format(requireContext().getString(R.string.currency_format), balance));

            pieChartManager.updateChart(income, expense);
            barChartManager.updateBarChart(true);

        } catch (Exception e) {
            Log.e("ReportsFragment", "Error updating values", e);
        }
    }
}