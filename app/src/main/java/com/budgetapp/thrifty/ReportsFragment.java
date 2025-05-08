package com.budgetapp.thrifty;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.renderers.RankingAdapter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;

public class ReportsFragment extends Fragment {
    private static final String TAG = "ReportsFragment";
    private PieChartManager pieChartManager;
    private BarChartManager barChartManager;
    private TextView tvBalanceAmount, tvCurrentBalance, tvTotalIncome, tvTotalExpense, trendToggle;
    private BarChart barChart;
    private float income;
    private float expense;
    private RecyclerView rankingRecyclerView;
    private TextView rankingToggle, sortToggle;
    private boolean isIncomeRanking = true;
    private boolean sortHighToLow = true;

    private static final String PREFS_NAME = "ReportPreferences";
    private static final String KEY_IS_INCOME = "isIncomeRanking";
    private static final String KEY_SORT_HIGH_LOW = "sortHighToLow";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);
        Log.d(TAG, "onCreateView called");

        try {
            // Initialize views
            PieChart pieChart = view.findViewById(R.id.pieChart);
            barChart = view.findViewById(R.id.barChart);
            trendToggle = view.findViewById(R.id.trendToggle);
            rankingToggle = view.findViewById(R.id.rankingToggle);
            sortToggle = view.findViewById(R.id.sortToggle);
            tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance);
            tvBalanceAmount = view.findViewById(R.id.tvBalanceAmount);
            tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
            tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
            rankingRecyclerView = view.findViewById(R.id.rankingRecyclerView);

            // Get saved preferences
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            isIncomeRanking = prefs.getBoolean(KEY_IS_INCOME, true);
            sortHighToLow = prefs.getBoolean(KEY_SORT_HIGH_LOW, true);

            // Initialize managers
            pieChartManager = new PieChartManager(pieChart, requireContext());
            barChartManager = new BarChartManager(barChart, trendToggle, requireContext());

            // Set initial text based on saved states
            rankingToggle.setText(isIncomeRanking ? R.string.income_ranking : R.string.expense_ranking);
            sortToggle.setText(sortHighToLow ? R.string.high : R.string.low);

            // Set up RecyclerView for ranking
            rankingRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            updateRankingAdapter(); // Initialize the adapter

            // Toggle ranking type (Income/Expense)
            rankingToggle.setOnClickListener(v -> {
                isIncomeRanking = !isIncomeRanking;
                rankingToggle.setText(isIncomeRanking ? R.string.income_ranking : R.string.expense_ranking);
                updateRankingAdapter();

                // Save the new state
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_IS_INCOME, isIncomeRanking);
                editor.apply();
            });

            // Toggle sorting order (High-to-Low/Low-to-High)
            sortToggle.setOnClickListener(v -> {
                sortHighToLow = !sortHighToLow;
                sortToggle.setText(sortHighToLow ? R.string.high : R.string.low);
                updateRankingAdapter();

                // Save the new state
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_SORT_HIGH_LOW, sortHighToLow);
                editor.apply();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views or managers", e);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        updateValues();
    }

    private void updateRankingAdapter() {
        if (rankingRecyclerView != null) {
            Log.d(TAG, "Updating ranking adapter with isIncomeRanking=" + isIncomeRanking +
                    ", sortHighToLow=" + sortHighToLow);

            RankingAdapter rankingAdapter = new RankingAdapter(
                    requireContext(),
                    TransactionsHandler.transactions,
                    isIncomeRanking,
                    sortHighToLow
            );
            rankingRecyclerView.setAdapter(rankingAdapter);

            // Notify for a smooth update
            rankingAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "Cannot update ranking adapter: rankingRecyclerView is null");
        }
    }

    private void updateValues() {
        try {
            Log.d(TAG, "updateValues called");

            // Update financial values
            income = TransactionsHandler.getTotalIncome();
            expense = TransactionsHandler.getTotalExpense();
            float balance = income - expense;

            Log.d(TAG, "Income: " + income + ", Expense: " + expense + ", Balance: " + balance);

            // Update text views
            if (tvTotalIncome != null) {
                tvTotalIncome.setText(String.format(requireContext().getString(R.string.currency_format), income));
            }

            if (tvTotalExpense != null) {
                tvTotalExpense.setText(String.format(requireContext().getString(R.string.currency_format), expense));
            }

            if (tvBalanceAmount != null) {
                tvBalanceAmount.setText(String.format(requireContext().getString(R.string.currency_format), balance));
                // Set color based on balance value
                if (balance < 0) {
                    tvCurrentBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                    tvBalanceAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                } else if (balance == 0) {
                    tvCurrentBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey));
                    tvBalanceAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey));
                } else {
                    tvCurrentBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
                    tvBalanceAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
                }
            }

            // Update pie chart
            if (pieChartManager != null) {
                Log.d(TAG, "Updating pie chart");
                pieChartManager.updateChart(income, expense);
            } else {
                Log.e(TAG, "pieChartManager is null");
            }

            // Update bar chart
            if (barChartManager != null && barChart != null) {
                Log.d(TAG, "Updating bar chart");

                // Get current toggle state
                boolean isShowingIncome = trendToggle.getText().toString().contains("Income");
                Log.d(TAG, "Current toggle state: " + (isShowingIncome ? "Income" : "Expense"));

                // Update with current toggle state
                barChartManager.updateBarChart(isShowingIncome);

                // Force a redraw
                barChart.invalidate();
            } else {
                Log.e(TAG, "barChartManager or barChart is null");
            }

            // Update ranking adapter with latest transaction data
            updateRankingAdapter();

        } catch (Exception e) {
            Log.e(TAG, "Error updating values", e);
            e.printStackTrace();
        }
    }
}
