package com.budgetapp.thrifty.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.budgetapp.thrifty.utils.FormatUtils;
import com.budgetapp.thrifty.utils.ThemeSync;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private TextView filter;
    private String currentFilterType = "Today";
    private ArrayList<Transaction> allTransactions = new ArrayList<>();
    private ArrayList<Transaction> visibleTransactions = new ArrayList<>();
    private int currentIndex = 0;
    private final int pageSize = 10;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);
        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        filter = view.findViewById(R.id.todayLabel);

        LinearLayout filterButton = view.findViewById(R.id.filter_button); // Entire row acts as button

        filterButton.setOnClickListener(this::showPopupMenu);

        ScrollView scrollView = view.findViewById(R.id.scrollView);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View contentView = scrollView.getChildAt(0);
            if (contentView.getBottom() <= (scrollView.getHeight() + scrollView.getScrollY())) {
                if (currentIndex < allTransactions.size()) {
                    loadNextPage();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        applyDefaultFilter();
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

            filter.setText(filterText);
            currentFilterType = filterText;

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

            // Use our adapter's binding logic to set up the item view
            ImageView icon = itemView.findViewById(R.id.transaction_icon);
            TextView category = itemView.findViewById(R.id.transaction_category);
            TextView amount = itemView.findViewById(R.id.transaction_amount);
            TextView date = itemView.findViewById(R.id.transaction_datetime);
            TextView description = itemView.findViewById(R.id.transaction_description);

            icon.setImageResource(t.getIconID());
            category.setText(t.getCategory());
            category.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));

            itemView.setOnLongClickListener(v -> {
                @SuppressLint("InflateParams") View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_transaction_menu, null);
                PopupWindow popupWindow = new PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                );

                popupWindow.setElevation(10f);
                popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.transaction_container));
                popupWindow.setOutsideTouchable(true);
                popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

                int popupWidth = popupView.getMeasuredWidth();
                int popupHeight = popupView.getMeasuredHeight();

                int[] location = new int[2];
                v.getLocationOnScreen(location);

                int xOffset = location[0] + (v.getWidth() / 2) - (popupWidth / 2);
                int yOffset = location[1] + (v.getHeight() / 2) - (popupHeight / 2);

                popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, xOffset, yOffset);

                // Handle Edit
                popupView.findViewById(R.id.edit_button).setOnClickListener(btn -> {
                    TransactionEditDialogFragment dialog = TransactionEditDialogFragment.newInstance(t);
                    dialog.setOnDismissListener(this::refreshTransactions);
                    dialog.show(requireActivity().getSupportFragmentManager(), "editDialog");
                    popupWindow.dismiss();
                });

                // Handle Delete
                popupView.findViewById(R.id.delete_button).setOnClickListener(btn -> {
                    // Show loading dialog
                    android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(requireContext())
                            .setMessage("Deleting transaction...")
                            .setCancelable(false)
                            .create();
                    loadingDialog.show();

                    // Delete from Firestore first
                    FirestoreManager.deleteTransaction(t.getId(), new FirestoreManager.OnDeleteTransactionListener() {
                        @Override
                        public void onSuccess() {
                            requireActivity().runOnUiThread(() -> {
                                // Only remove from local list after successful Firestore deletion
                                TransactionsHandler.transactions.remove(t);
                                loadingDialog.dismiss();
                                refreshTransactions(); // Refresh UI
                                android.widget.Toast.makeText(requireContext(),
                                        "Transaction deleted successfully",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            });
                            popupWindow.dismiss();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                loadingDialog.dismiss();
                                refreshTransactions(); // Refresh UI
                                android.widget.Toast.makeText(requireContext(),
                                        "Failed to delete transaction: " + e.getMessage(),
                                        android.widget.Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                });

                return true;
            });


            // Format amount using FormatUtils
            float rawAmount = t.getRawAmount();
            String displayAmount;
            int amountColor;

            if (rawAmount == 0f) {
                displayAmount = "₱" + FormatUtils.formatAmount(rawAmount, false);
                amountColor = ContextCompat.getColor(requireContext(), R.color.black);
            } else if ("Income".equalsIgnoreCase(t.getType())) {
                displayAmount = "+₱" + FormatUtils.formatAmount(rawAmount, true);
                amountColor = ContextCompat.getColor(requireContext(), R.color.income_green);
            } else {
                displayAmount = "-₱" + FormatUtils.formatAmount(rawAmount, true);
                amountColor = ContextCompat.getColor(requireContext(), R.color.red);
            }

            amount.setText(displayAmount);
            amount.setTextColor(amountColor);
            amount.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));

            date.setText(t.getDateAndTime());
            date.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));

            description.setText(t.getDescription());
            description.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));

            description.setOnClickListener(v -> {
                DescriptionDialogFragment descriptionDialogFragment = DescriptionDialogFragment.newInstance(t);
                descriptionDialogFragment.show(requireActivity().getSupportFragmentManager(), "descriptionDialog");
            });

            container.addView(itemView);
        }
    }

    private void applyDefaultFilter() {
        currentIndex = 0;
        allTransactions = TransactionsHandler.getFilteredTransactions(currentFilterType);
        visibleTransactions.clear();
        loadNextPage(); // Initial load
    }

    private void loadNextPage() {
        int end = Math.min(currentIndex + pageSize, allTransactions.size());
        List<Transaction> nextBatch = allTransactions.subList(currentIndex, end);
        visibleTransactions.addAll(nextBatch);
        currentIndex = end;

        updateTransactionList(new ArrayList<>(visibleTransactions));
    }

    public void refreshTransactions() {
        requireView().post(() ->
                ThemeSync.syncNotificationBarColor(requireActivity().getWindow(), requireContext())
        );
        filter.setText(currentFilterType);
        ArrayList<Transaction> filteredList = TransactionsHandler.getFilteredTransactions(currentFilterType);
        updateTransactionList(filteredList);
    }

}
