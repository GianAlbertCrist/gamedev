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
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.fragments.DescriptionDialogFragment;
import com.budgetapp.thrifty.utils.FormatUtils;

import java.util.ArrayList;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<Transaction> transactions;
    private final String filterType;

    public TransactionsAdapter(Context context, ArrayList<Transaction> transactions, String filterType) {
        this.context = context;
        this.transactions = transactions;
        this.filterType = filterType;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView category, amount, date, description;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.transaction_icon);
            category = itemView.findViewById(R.id.transaction_category);
            amount = itemView.findViewById(R.id.transaction_amount);
            date = itemView.findViewById(R.id.transaction_datetime);
            description = itemView.findViewById(R.id.transaction_description);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        // Set icon and category
        holder.icon.setImageResource(transaction.getIconID());
        holder.category.setText(transaction.getCategory());
        holder.category.setTypeface(ResourcesCompat.getFont(context, R.font.poppins));

        // Format and set amount using FormatUtils
        float rawAmount = transaction.getRawAmount();
        String displayAmount;
        int amountColor;

        if (rawAmount == 0f) {
            displayAmount = "₱" + FormatUtils.formatAmount(rawAmount, false);
            amountColor = ContextCompat.getColor(context, R.color.black);
        } else if ("Income".equalsIgnoreCase(transaction.getType())) {
            displayAmount = "+₱" + FormatUtils.formatAmount(rawAmount, true);
            amountColor = ContextCompat.getColor(context, R.color.income_green);
        } else {
            displayAmount = "-₱" + FormatUtils.formatAmount(rawAmount, true);
            amountColor = ContextCompat.getColor(context, R.color.red);
        }

        holder.amount.setText(displayAmount);
        holder.amount.setTextColor(amountColor);
        holder.amount.setTypeface(ResourcesCompat.getFont(context, R.font.poppins));

        // Set date and description
        holder.date.setText(transaction.getDateAndTime());
        holder.date.setTypeface(ResourcesCompat.getFont(context, R.font.poppins));

        holder.description.setText(transaction.getDescription());
        holder.description.setTypeface(ResourcesCompat.getFont(context, R.font.poppins));

        // Set description click listener
        holder.description.setOnClickListener(v -> {
            DescriptionDialogFragment descriptionDialogFragment = DescriptionDialogFragment.newInstance(transaction);
            descriptionDialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "descriptionDialog");
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }
}