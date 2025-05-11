package com.budgetapp.thrifty.renderers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;


import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.fragments.DescriptionDialogFragment;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    private final List<Transaction> transactions;
    private final Context context;
    private final boolean isIncomeRanking; // true for income, false for expense
    private final boolean sortHighToLow; // true for high-to-low, false for low-to-high

    public RankingAdapter(Context context, List<Transaction> transactions, boolean isIncomeRanking, boolean sortHighToLow) {
        this.context = context;
        this.transactions = filterAndSortTransactions(transactions, isIncomeRanking, sortHighToLow);
        this.isIncomeRanking = isIncomeRanking;
        this.sortHighToLow = sortHighToLow;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView category, amount, datetime, description;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.transaction_icon);
            category = itemView.findViewById(R.id.transaction_category);
            amount = itemView.findViewById(R.id.transaction_amount);
            datetime = itemView.findViewById(R.id.transaction_datetime);
            description = itemView.findViewById(R.id.transaction_description);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);

        // Add margins to each item
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (params != null) {
            int margin = (int) (4 * context.getResources().getDisplayMetrics().density); // 8dp margin
            params.setMargins(0, margin, 0, margin);
            view.setLayoutParams(params);
        }

        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        holder.icon.setImageResource(transaction.getIconID());
        holder.category.setText(transaction.getCategory());
        holder.category.setTextColor(ContextCompat.getColor(context, R.color.black));

        float amount = transaction.getRawAmount();
        String displayAmount = String.format("%s₱%.2f",
                isIncomeRanking ? "+" : "-",
                amount
        );
        int color = ContextCompat.getColor(context, isIncomeRanking ? R.color.income_green : R.color.red);

        holder.amount.setText(displayAmount);
        holder.amount.setTextColor(color);

        holder.datetime.setText(transaction.getDateAndTime());
        holder.datetime.setTextColor(ContextCompat.getColor(context, R.color.background_color));

        holder.description.setText(transaction.getDescription());


        holder.description.setOnClickListener(v -> {
            DescriptionDialogFragment dialogFragment = DescriptionDialogFragment.newInstance(transaction);
            dialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "descriptionDialog");
        });
    }

    @Override
    public int getItemCount() {

        return transactions.size();
    }

    private List<Transaction> filterAndSortTransactions(List<Transaction> allTransactions, boolean isIncome, boolean highToLow) {
        List<Transaction> filteredTransactions = new ArrayList<>();
        for (Transaction transaction : allTransactions) {
            if (isIncome && "Income".equalsIgnoreCase(transaction.getType())) {
                filteredTransactions.add(transaction);
            } else if (!isIncome && "Expense".equalsIgnoreCase(transaction.getType())) {
                filteredTransactions.add(transaction);
            }
        }

        Comparator<Transaction> comparator = Comparator.comparing(Transaction::getRawAmount);
        if (highToLow) {
            comparator = comparator.reversed();
        }
        Collections.sort(filteredTransactions, comparator);

        return filteredTransactions;
    }
}