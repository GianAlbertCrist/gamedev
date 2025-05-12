package com.budgetapp.thrifty.transaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ScrollView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.budgetapp.thrifty.fragments.NotificationsFragment;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.fragments.ReportsFragment;
import com.budgetapp.thrifty.fragments.TransactionsFragment;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.budgetapp.thrifty.utils.KeyboardBehavior;


public class AddExpenseFragment extends Fragment {

    private int selectedIconResId = R.drawable.ic_transport; // default
    private String selectedRecurring = "None";

    public AddExpenseFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_expense, container, false);
        return view;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Transaction editingTransaction;
        if (getArguments() != null && getArguments().containsKey("transactionToEdit")) {
            editingTransaction = getArguments().getParcelable("transactionToEdit");
        } else {
            editingTransaction = null;
        }

        ConstraintLayout categorySelector = view.findViewById(R.id.category_selector);
        TextView categoryText = view.findViewById(R.id.category_text);
        ImageView categoryIcon = view.findViewById(R.id.category_icon);
        EditText numberInput = view.findViewById(R.id.number_input);
        EditText descriptionInput = view.findViewById(R.id.expense_description);
        ImageButton recurringButton = view.findViewById(R.id.ic_recurring);

        Button confirmBtn = view.findViewById(R.id.confirm_button);
        Button cancelBtn = view.findViewById(R.id.cancel_button);

        // 1. Select all on focus
        numberInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                numberInput.selectAll();
            }
        });

        // 2. Category recurring
        recurringButton.setOnClickListener(v -> {
            View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_recurring_menu, null);
            PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);

            // Set background and animation (optional)
            popupWindow.setElevation(10f);
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.transaction_container));
            popupWindow.setOutsideTouchable(true);

            // Set click listeners
            popupView.findViewById(R.id.option_none).setOnClickListener(item -> {
                selectedRecurring = "None";
                popupWindow.dismiss();
            });
            popupView.findViewById(R.id.option_daily).setOnClickListener(item -> {
                selectedRecurring = "Daily";
                popupWindow.dismiss();
            });
            popupView.findViewById(R.id.option_weekly).setOnClickListener(item -> {
                selectedRecurring = "Weekly";
                popupWindow.dismiss();
            });
            popupView.findViewById(R.id.option_monthly).setOnClickListener(item -> {
                selectedRecurring = "Monthly";
                popupWindow.dismiss();
            });
            popupView.findViewById(R.id.option_yearly).setOnClickListener(item -> {
                selectedRecurring = "Yearly";
                popupWindow.dismiss();
            });

            // Show the popup aligned to the recurring button
            popupWindow.showAsDropDown(recurringButton);
        });

        // 3. Category selection logic
        categorySelector.setOnClickListener(v -> {
            View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_expense_category, null);
            PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);

            popupWindow.setElevation(10f);
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.transaction_container));
            popupWindow.setOutsideTouchable(true);

            @SuppressLint("SetTextI18n") View.OnClickListener listener = clicked -> {
                int id = clicked.getId();

                if (id == R.id.category_food) {
                    categoryText.setText("Food and Drink");
                    categoryIcon.setImageResource(R.drawable.ic_food);
                    selectedIconResId = R.drawable.ic_food;
                } else if (id == R.id.category_clothes) {
                    categoryText.setText("Clothes");
                    categoryIcon.setImageResource(R.drawable.ic_clothes);
                    selectedIconResId = R.drawable.ic_clothes;
                } else if (id == R.id.category_healthcare) {
                    categoryText.setText("Healthcare");
                    categoryIcon.setImageResource(R.drawable.ic_healthcare);
                    selectedIconResId = R.drawable.ic_healthcare;
                } else if (id == R.id.category_gifts) {
                    categoryText.setText("Gifts");
                    categoryIcon.setImageResource(R.drawable.ic_gifts);
                    selectedIconResId = R.drawable.ic_gifts;
                } else if (id == R.id.category_transport) {
                    categoryText.setText("Transport");
                    categoryIcon.setImageResource(R.drawable.ic_transport);
                    selectedIconResId = R.drawable.ic_transport;
                } else if (id == R.id.category_housing) {
                    categoryText.setText("Housing");
                    categoryIcon.setImageResource(R.drawable.ic_housing);
                    selectedIconResId = R.drawable.ic_housing;
                } else if (id == R.id.category_entertainment) {
                    categoryText.setText("Entertainment");
                    categoryIcon.setImageResource(R.drawable.ic_entertainment);
                    selectedIconResId = R.drawable.ic_entertainment;
                } else if (id == R.id.category_education) {
                    categoryText.setText("Education");
                    categoryIcon.setImageResource(R.drawable.ic_education);
                    selectedIconResId = R.drawable.ic_education;
                } else if (id == R.id.category_others) {
                    categoryText.setText("Others");
                    categoryIcon.setImageResource(R.drawable.ic_others);
                    selectedIconResId = R.drawable.ic_others;
                }

                popupWindow.dismiss();
            };

            // Attach to all expense category items
            popupView.findViewById(R.id.category_food).setOnClickListener(listener);
            popupView.findViewById(R.id.category_clothes).setOnClickListener(listener);
            popupView.findViewById(R.id.category_healthcare).setOnClickListener(listener);
            popupView.findViewById(R.id.category_gifts).setOnClickListener(listener);
            popupView.findViewById(R.id.category_transport).setOnClickListener(listener);
            popupView.findViewById(R.id.category_housing).setOnClickListener(listener);
            popupView.findViewById(R.id.category_entertainment).setOnClickListener(listener);
            popupView.findViewById(R.id.category_education).setOnClickListener(listener);
            popupView.findViewById(R.id.category_others).setOnClickListener(listener);

            popupWindow.showAsDropDown(categorySelector);
        });

        // 4. Confirm button logic
        confirmBtn.setOnClickListener(v -> {
            String category = categoryText.getText().toString();
            String description = descriptionInput.getText().toString();

            // Validate amount
            String amountStr = numberInput.getText().toString().trim();
            if (amountStr.isEmpty()) {
                numberInput.setError("Amount is required");
                return;
            }
            float amount = Float.parseFloat(amountStr);

            int iconRes = selectedIconResId;

            // Create transaction
            Transaction transaction = new Transaction(
                    "Expense",
                    category,
                    amount,
                    iconRes,
                    description,
                    selectedRecurring
            );

            if (editingTransaction != null) {
                // Update existing transaction
                transaction.setId(editingTransaction.getId());
                FirestoreManager.updateTransaction(transaction);
                TransactionsHandler.transactions.remove(editingTransaction);
            } else {
                // Save new transaction
                FirestoreManager.saveTransaction(transaction);
            }

            // Add to local list
            TransactionsHandler.transactions.add(transaction);

            // Handle recurring notification
            if (!selectedRecurring.equals("None")) {
                String notificationTime = KeyboardBehavior.getCurrentTime();
                Notification newNotification = new Notification(
                        "Transaction",
                        category + " | ₱" + amount,
                        notificationTime,
                        selectedRecurring,
                        iconRes
                );
                FirestoreManager.saveNotification(newNotification, transaction.getId());
            }

            // Update reports
            ReportsFragment reportsFragment = (ReportsFragment) requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentByTag(ReportsFragment.class.getSimpleName());
            if (reportsFragment != null) {
                reportsFragment.onResume();
            }

            // Close dialog/activity
            Fragment parent = getParentFragment();
            if (parent instanceof DialogFragment) {
                ((DialogFragment) parent).dismiss();
            } else {
                requireActivity().finish();
            }
        });


        if (editingTransaction != null) {
            categoryText.setText(editingTransaction.getCategory());
            categoryIcon.setImageResource(editingTransaction.getIconID());
            numberInput.setText(String.valueOf(editingTransaction.getRawAmount()));
            descriptionInput.setText(editingTransaction.getDescription());
            selectedIconResId = editingTransaction.getIconID();
            selectedRecurring = editingTransaction.getRecurring();
        }
    }
}
