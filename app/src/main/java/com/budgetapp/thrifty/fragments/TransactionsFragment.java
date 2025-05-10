package com.budgetapp.thrifty.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.transaction.Transaction;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private TextView filter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        filter = view.findViewById(R.id.todayLabel);
        LinearLayout filterButton = view.findViewById(R.id.filter_button); // Entire row acts as button

        filterButton.setOnClickListener(this::showPopupMenu);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        applyDefaultFilter();
    }

    private void applyDefaultFilter() {
        String filterText = "Today";
        filter.setText(filterText);
        ArrayList<Transaction> filteredList = TransactionsHandler.getFilteredTransactions(filterText);
        updateTransactionList(filteredList);
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view, R.style.CustomPopupMenu);
        popupMenu.getMenuInflater().inflate(R.menu.transaction_filter_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            String filterText = "";

            int id = item.getItemId();
            if (id == R.id.filter_today) {
                filterText = "Today";
            } else if (id == R.id.filter_days) {
                filterText = "Days";
            } else if (id == R.id.filter_weeks) {
                filterText = "Weeks";
            } else if (id == R.id.filter_months) {
                filterText = "Months";
            } else {
                return false;
            }

            // Show selected filter in UI
            filter.setText(filterText);

            // Get filtered transactions
            ArrayList<Transaction> filteredList = TransactionsHandler.getFilteredTransactions(filterText);

            // Update your transaction list view
            updateTransactionList(filteredList);

            return true;
        });

        popupMenu.show();
    }

    @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
    private void updateTransactionList(ArrayList<Transaction> transactions) {
        LinearLayout container = requireView().findViewById(R.id.transactionListContainer);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (transactions.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("No records found.");
            emptyView.setTextSize(16);
            emptyView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            emptyView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));
            emptyView.setGravity(android.view.Gravity.CENTER);

            LinearLayout wrapper = new LinearLayout(getContext());
            wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            wrapper.setGravity(android.view.Gravity.CENTER);
            wrapper.addView(emptyView);

            container.addView(wrapper);
            return;
        }

        String filterType = filter.getText().toString();
        String currentGroup = "";

        for (Transaction t : transactions) {
            String groupLabel = "";
            Calendar cal = Calendar.getInstance();
            cal.setTime(t.getParsedDate());

            switch (filterType) {
                case "Today":
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    String period = hour < 12 ? "12:00 AM – 12:00 PM" : "12:00 PM – 11:59 PM";
                    groupLabel = "Today, " + period;
                    break;

                case "Days":
                    String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.getTime());
                    int h = cal.get(Calendar.HOUR_OF_DAY);
                    String timeBlock = h < 12 ? "12:00 AM – 12:00 PM" : "12:00 PM – 11:59 PM";
                    groupLabel = day + ", " + timeBlock;
                    break;

                case "Weeks":
                    Calendar now = Calendar.getInstance();
                    Calendar startOfThisWeek = (Calendar) now.clone();
                    startOfThisWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

                    Calendar startOfLastWeek = (Calendar) startOfThisWeek.clone();
                    startOfLastWeek.add(Calendar.WEEK_OF_YEAR, -1);

                    Calendar endOfLastWeek = (Calendar) startOfThisWeek.clone();
                    endOfLastWeek.add(Calendar.DAY_OF_YEAR, -1);

                    if (cal.after(startOfThisWeek)) {
                        groupLabel = "This Week";
                    } else if (!cal.before(startOfLastWeek) && !cal.after(endOfLastWeek)) {
                        groupLabel = "Last Week";
                    } else {
                        groupLabel = "Older";
                    }
                    break;

                case "Months":
                    int month = cal.get(Calendar.MONTH); // 0-based
                    int year = cal.get(Calendar.YEAR);
                    if (month < 3) {
                        groupLabel = "Jan – Mar " + year;
                    } else if (month < 6) {
                        groupLabel = "Apr – Jun " + year;
                    } else if (month < 9) {
                        groupLabel = "Jul – Sep " + year;
                    } else {
                        groupLabel = "Oct – Dec " + year;
                    }
                    break;
            }

            if (!groupLabel.equals(currentGroup)) {
                currentGroup = groupLabel;

                TextView header = new TextView(getContext());
                header.setText("     " + groupLabel);
                header.setTextSize(14);
                header.setTextColor(ContextCompat.getColor(requireContext(), R.color.background_color));
                header.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.income_color));
                header.setPadding(8, 16, 8, 8);
                header.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));
                container.addView(header);
            }

            View itemView = inflater.inflate(R.layout.item_transaction, container, false);

            ImageView icon = itemView.findViewById(R.id.transaction_icon);
            TextView category = itemView.findViewById(R.id.transaction_category);
            TextView amount = itemView.findViewById(R.id.transaction_amount);
            TextView date = itemView.findViewById(R.id.transaction_datetime);

            icon.setImageResource(t.getIconID());
            category.setText(t.getCategory());

            float rawAmount = t.getRawAmount();
            String displayAmount;
            int amountColor;

            if (rawAmount == 0f) {
                displayAmount = String.format("₱%.2f", rawAmount);
                amountColor = ContextCompat.getColor(requireContext(), R.color.black);
            } else if ("Income".equalsIgnoreCase(t.getType())) {
                displayAmount = String.format("+₱%.2f", rawAmount);
                amountColor = ContextCompat.getColor(requireContext(), R.color.income_green);
            } else {
                displayAmount = String.format("-₱%.2f", rawAmount);
                amountColor = ContextCompat.getColor(requireContext(), R.color.red);
            }

            amount.setText(displayAmount);
            amount.setTextColor(amountColor);
            amount.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));

            date.setText(t.getDateAndTime());
            category.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));
            amount.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));
            date.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));

            TextView description = itemView.findViewById(R.id.transaction_description);
            description.setText(t.getDescription());

            container.addView(itemView);
        }
    }

    private void openTransactionDetailFragment(Transaction transaction) {

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            DescriptionDialogFragment descriptionDialogFragment = DescriptionDialogFragment.newInstance(transaction);


            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, descriptionDialogFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}