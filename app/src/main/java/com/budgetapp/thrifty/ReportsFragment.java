package com.budgetapp.thrifty;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
    private PieChartManager pieChartManager;
    private BarChartManager barChartManager;
    private TextView tvBalanceAmount, tvTotalIncome, tvTotalExpense;
    private float income;
    private float expense;

    private static final String PREFS_NAME = "ReportPreferences";
    private static final String KEY_IS_INCOME = "isIncomeRanking";
    private static final String KEY_SORT_HIGH_LOW = "sortHighToLow";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        updateValues();

        try {
            // Get saved preferences
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean savedIsIncome = prefs.getBoolean(KEY_IS_INCOME, true);
            boolean savedSortHighLow = prefs.getBoolean(KEY_SORT_HIGH_LOW, true);

            // Initialize views
            PieChart pieChart = view.findViewById(R.id.pieChart);
            BarChart barChart = view.findViewById(R.id.barChart);
            TextView trendToggle = view.findViewById(R.id.trendToggle);
            TextView rankingToggle = view.findViewById(R.id.rankingToggle);
            TextView sortToggle = view.findViewById(R.id.sortToggle);
            tvBalanceAmount = view.findViewById(R.id.tvBalanceAmount);
            tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
            tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
            RecyclerView rankingRecyclerView = view.findViewById(R.id.rankingRecyclerView);

            // Initialize managers
            pieChartManager = new PieChartManager(pieChart, requireContext());
            barChartManager = new BarChartManager(barChart, trendToggle, requireContext());

            // Set initial toggle states from saved preferences
            final boolean[] isIncomeRanking = {savedIsIncome};
            final boolean[] sortHighToLow = {savedSortHighLow};

            // Set initial text based on saved states
            rankingToggle.setText(isIncomeRanking[0] ? R.string.income_ranking : R.string.expense_ranking);
            sortToggle.setText(sortHighToLow[0] ? R.string.high : R.string.low);

            final RankingAdapter[] rankingAdapter = {new RankingAdapter(
                    requireContext(),
                    TransactionsHandler.transactions,
                    isIncomeRanking[0],
                    sortHighToLow[0]
            )};

            // Set up RecyclerView for ranking
            rankingRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            rankingRecyclerView.setAdapter(rankingAdapter[0]);

            // Toggle ranking type (Income/Expense)
            rankingToggle.setOnClickListener(v -> {
                isIncomeRanking[0] = !isIncomeRanking[0];
                rankingToggle.setText(isIncomeRanking[0] ? R.string.income_ranking : R.string.expense_ranking);
                rankingAdapter[0] = new RankingAdapter(
                        requireContext(),
                        TransactionsHandler.transactions,
                        isIncomeRanking[0],
                        sortHighToLow[0]
                );
                rankingRecyclerView.setAdapter(rankingAdapter[0]);

                // Save the new state
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_IS_INCOME, isIncomeRanking[0]);
                editor.apply();
            });

            // Toggle sorting order (High-to-Low/Low-to-High)
            sortToggle.setOnClickListener(v -> {
                sortHighToLow[0] = !sortHighToLow[0];
                sortToggle.setText(sortHighToLow[0] ? R.string.high : R.string.low);
                rankingAdapter[0] = new RankingAdapter(
                        requireContext(),
                        TransactionsHandler.transactions,
                        isIncomeRanking[0],
                        sortHighToLow[0]
                );
                rankingRecyclerView.setAdapter(rankingAdapter[0]);

                // Save the new state
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_SORT_HIGH_LOW, sortHighToLow[0]);
                editor.apply();
            });

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