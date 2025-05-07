package com.budgetapp.thrifty;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.transaction.Transaction;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;

import java.util.List;
import java.util.stream.Collectors;

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
            TextView rankingToggle = view.findViewById(R.id.rankingToggle);
            TextView sortToggle = view.findViewById(R.id.sortToggle);
            LinearLayout rankingContainer = view.findViewById(R.id.rankingContainer);
            tvBalanceAmount = view.findViewById(R.id.tvBalanceAmount);
            tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
            tvTotalExpense = view.findViewById(R.id.tvTotalExpense);

            // Initialize managers
            pieChartManager = new PieChartManager(pieChart, requireContext());
            barChartManager = new BarChartManager(barChart, trendToggle, requireContext());

            // Set up ranking toggle
            rankingToggle.setOnClickListener(v -> {
                if (rankingToggle.getText().toString().equals(getString(R.string.expense_ranking))) {
                    rankingToggle.setText(getString(R.string.income_ranking));
                    updateRankingList(rankingContainer, true, sortToggle.getText().toString().equals(getString(R.string.high)));
                } else {
                    rankingToggle.setText(getString(R.string.expense_ranking));
                    updateRankingList(rankingContainer, false, sortToggle.getText().toString().equals(getString(R.string.high)));
                }
            });

            // Set up sort toggle
            sortToggle.setOnClickListener(v -> {
                if (sortToggle.getText().toString().equals(getString(R.string.high))) {
                    sortToggle.setText(getString(R.string.low));
                } else {
                    sortToggle.setText(getString(R.string.high));
                }
                boolean isIncome = rankingToggle.getText().toString().equals(getString(R.string.income_ranking));
                updateRankingList(rankingContainer, isIncome, sortToggle.getText().toString().equals(getString(R.string.high)));
            });

            // Initial ranking list
            updateRankingList(rankingContainer, false, true);

        } catch (Exception e) {
            Log.e("ReportsFragment", "Error initializing views or managers", e);
        }

        return view;
    }

    private void updateValues() {
        income = TransactionsHandler.getTotalIncome();
        expense = TransactionsHandler.getTotalExpense();
        float balance = TransactionsHandler.getBalance();

        tvTotalIncome.setText(String.format(getString(R.string.currency_format), income));
        tvTotalExpense.setText(String.format(getString(R.string.currency_format), expense));
        tvBalanceAmount.setText(String.format(getString(R.string.currency_format), balance));
    }

    private void updateRankingList(LinearLayout container, boolean isIncome, boolean isHighToLow) {
        container.removeAllViews();

        // Filter and sort transactions
        List<Transaction> filteredTransactions = TransactionsHandler.transactions.stream()
                .filter(t -> isIncome ? "Income".equalsIgnoreCase(t.getType()) : "Expense".equalsIgnoreCase(t.getType()))
                .sorted((t1, t2) -> isHighToLow ?
                        Float.compare(t2.getRawAmount(), t1.getRawAmount()) :
                        Float.compare(t1.getRawAmount(), t2.getRawAmount()))
                .collect(Collectors.toList());

        // Populate the ranking list
        for (Transaction transaction : filteredTransactions) {
            View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_transaction, container, false);

            TextView category = itemView.findViewById(R.id.transaction_category);
            TextView amount = itemView.findViewById(R.id.transaction_amount);
            TextView datetime = itemView.findViewById(R.id.transaction_datetime);

            category.setText(transaction.getCategory());
            amount.setText(transaction.getAmount());
            datetime.setText(transaction.getDateAndTime());

            container.addView(itemView);
        }
    }
}