package com.budgetapp.thrifty.renderers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.fragments.DescriptionDialogFragment;
import com.budgetapp.thrifty.utils.FormatUtils;


import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_SPACER = 1;
    private final List<Transaction> transactions;
    private final Context context;
    private final int maxVisibleItems = 8; // 7 items + 1 spacer

    // Constructor to initialize the adapter with context and transactions
    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
    }

    // ViewHolder to hold the views for each transaction item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView category, amount, datetime, description;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.transaction_icon);
            category = itemView.findViewById(R.id.transaction_category);
            amount = itemView.findViewById(R.id.transaction_amount);
            datetime = itemView.findViewById(R.id.transaction_datetime);
            description = itemView.findViewById(R.id.transaction_description);  // Add this line for description
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (transactions.size() >= maxVisibleItems - 1 && position == maxVisibleItems - 1) {
            return VIEW_TYPE_SPACER;
        }
        return VIEW_TYPE_NORMAL;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each transaction item
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (params != null) {
            int margin = (int) (4 * context.getResources().getDisplayMetrics().density);
            params.setMargins(0, margin, 0, margin);
            view.setLayoutParams(params);
        }
        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Typeface poppins = ResourcesCompat.getFont(context, R.font.poppins);

        if (getItemViewType(position) == VIEW_TYPE_SPACER) {
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            holder.icon.setVisibility(View.INVISIBLE);
            holder.category.setText("");
            holder.amount.setText("");
            holder.datetime.setText("");
            holder.description.setVisibility(View.GONE);
        }

        int index = transactions.size() - 1 - position;
        if (index < 0 || index >= transactions.size()) return;

        Transaction t = transactions.get(index);

        holder.icon.setVisibility(View.VISIBLE);
        holder.icon.setImageResource(t.getIconID());
        holder.category.setText(t.getCategory());
        holder.category.setTypeface(poppins);
        holder.category.setTextColor(ContextCompat.getColor(context, R.color.black));

        // Display amount with proper formatting using FormatUtils
        float amt = t.getRawAmount();
        String displayAmount;
        int color;

        if (amt == 0f) {
            displayAmount = "₱0.00";
            color = ContextCompat.getColor(context, R.color.background_color);
        } else {
            boolean isIncome = "Income".equalsIgnoreCase(t.getType());
            String formattedAmount = FormatUtils.formatAmount(amt, true);
            displayAmount = String.format("%s₱%s",
                    isIncome ? "+" : "-",
                    formattedAmount
            );
            color = ContextCompat.getColor(context,
                    isIncome ? R.color.income_green : R.color.red
            );
        }

        holder.amount.setText(displayAmount);
        holder.amount.setTypeface(poppins);
        holder.amount.setTextColor(color);


        holder.description.setText(t.getDescription());


        String timeText = t.getDateAndTime();
        holder.datetime.setText(timeText);
        holder.datetime.setTypeface(poppins);
        holder.datetime.setTextColor(ContextCompat.getColor(context, R.color.background_color));


        holder.description.setOnClickListener(v -> {
            DescriptionDialogFragment dialogFragment = DescriptionDialogFragment.newInstance(t);
            dialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "descriptionDialog");
        });
    }

    @Override
    public int getItemCount() {
        int size = transactions.size();
        return size >= maxVisibleItems - 1 ? maxVisibleItems : size;
    }
}
