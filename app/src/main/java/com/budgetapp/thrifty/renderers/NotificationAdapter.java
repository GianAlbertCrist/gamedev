package com.budgetapp.thrifty.renderers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.utils.FormatUtils;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notificationList;
    private static final String TAG = "NotificationAdapter";

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);

        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.notificationTitle.setText(notification.getType());

        String description = notification.getDescription();
        String formattedDescription = formatDescriptionAmount(description);
        holder.notificationDescription.setText(formattedDescription);

        holder.notificationTime.setText(notification.getTime());

        holder.notificationRecurring.setText(notification.getRecurring());

        if ("Income Reminder".equalsIgnoreCase(notification.getType())) {
            holder.notificationIcon.setImageResource(R.drawable.ic_income);
        } else if ("Expense Reminder".equalsIgnoreCase(notification.getType())) {
            holder.notificationIcon.setImageResource(R.drawable.ic_expense);
        } else {
            holder.notificationIcon.setImageResource(R.drawable.icnotif_transactions);
        }
    }

    private String formatDescriptionAmount(String description) {
        try {
            Pattern pattern = Pattern.compile("(.*\\|\\s*₱)(\\d+(\\.\\d+)?)(.*)");
            Matcher matcher = pattern.matcher(description);

            if (matcher.find()) {
                String prefix = matcher.group(1);
                String amountStr = matcher.group(2);
                String suffix = matcher.group(4);

                assert amountStr != null;
                double amount = Double.parseDouble(amountStr);
                String formattedAmount = FormatUtils.formatAmount(amount, true);

                String result = prefix + formattedAmount + suffix;
                return result.replaceAll("\\{\\}", "").trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting notification description: " + e.getMessage());
        }

        return description.replaceAll("\\{\\}", "").trim();
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTitle, notificationDescription, notificationTime, notificationRecurring;
        ImageView notificationIcon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            notificationTitle = itemView.findViewById(R.id.notificationTitle);
            notificationDescription = itemView.findViewById(R.id.notificationDescription);
            notificationTime = itemView.findViewById(R.id.notificationTime);
            notificationIcon = itemView.findViewById(R.id.notificationIcon);
            notificationRecurring = itemView.findViewById(R.id.notificationRecurring);
        }
    }
}
