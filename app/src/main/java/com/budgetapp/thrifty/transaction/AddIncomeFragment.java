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
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

public class AddIncomeFragment extends Fragment {

    private int selectedIconResId = R.drawable.ic_salary;
    private String selectedRecurring = "None";
    private EditText descriptionInput;

    public AddIncomeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_income, container, false);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
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
        descriptionInput = view.findViewById(R.id.income_description);
        ImageButton recurringButton = view.findViewById(R.id.ic_recurring);

        Button confirmBtn = requireActivity().findViewById(R.id.confirm_button);
        Button cancelBtn = requireActivity().findViewById(R.id.cancel_button);

        // 1. Select all on focus
        numberInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                numberInput.selectAll();
            }
        });

        // 2. Category recurring
        recurringButton.setOnClickListener(v -> {
            @SuppressLint("InflateParams") View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_recurring_menu, null);
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
            @SuppressLint("InflateParams") View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_income_category, null);
            PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);

            popupWindow.setElevation(10f);
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.transaction_container));
            popupWindow.setOutsideTouchable(true);

            // Helper method to attach click behavior
            @SuppressLint("SetTextI18n") View.OnClickListener assignCategory = clicked -> {
                int id = clicked.getId();

                if (id == R.id.category_salary) {
                    categoryText.setText("Salary");
                    categoryIcon.setImageResource(R.drawable.ic_salary);
                    selectedIconResId = R.drawable.ic_salary;
                } else if (id == R.id.category_investment) {
                    categoryText.setText("Investment");
                    categoryIcon.setImageResource(R.drawable.ic_investment);
                    selectedIconResId = R.drawable.ic_investment;
                } else if (id == R.id.category_allowance) {
                    categoryText.setText("Allowance");
                    categoryIcon.setImageResource(R.drawable.ic_allowance);
                    selectedIconResId = R.drawable.ic_allowance;
                } else if (id == R.id.category_bonus) {
                    categoryText.setText("Bonus");
                    categoryIcon.setImageResource(R.drawable.ic_bonus);
                    selectedIconResId = R.drawable.ic_bonus;
                } else if (id == R.id.category_award) {
                    categoryText.setText("Award");
                    categoryIcon.setImageResource(R.drawable.ic_award);
                    selectedIconResId = R.drawable.ic_award;
                } else if (id == R.id.category_divident) {
                    categoryText.setText("Divident");
                    categoryIcon.setImageResource(R.drawable.ic_divident);
                    selectedIconResId = R.drawable.ic_divident;
                } else if (id == R.id.category_gambling) {
                    categoryText.setText("Gambling");
                    categoryIcon.setImageResource(R.drawable.ic_gambling);
                    selectedIconResId = R.drawable.ic_gambling;
                } else if (id == R.id.category_tips) {
                    categoryText.setText("Tips");
                    categoryIcon.setImageResource(R.drawable.ic_tip);
                    selectedIconResId = R.drawable.ic_tip;
                } else if (id == R.id.category_others) {
                    categoryText.setText("Others");
                    categoryIcon.setImageResource(R.drawable.ic_others);
                    selectedIconResId = R.drawable.ic_others;
                }

                popupWindow.dismiss();
            };

            // Attach listener to each category block
            popupView.findViewById(R.id.category_salary).setOnClickListener(assignCategory);
            popupView.findViewById(R.id.category_investment).setOnClickListener(assignCategory);
            popupView.findViewById(R.id.category_allowance).setOnClickListener(assignCategory);
            popupView.findViewById(R.id.category_bonus).setOnClickListener(assignCategory);
            popupView.findViewById(R.id.category_award).setOnClickListener(assignCategory);
            popupView.findViewById(R.id.category_divident).setOnClickListener(assignCategory);
            popupView.findViewById(R.id.category_gambling).setOnClickListener(assignCategory);
            popupView.findViewById(R.id.category_tips).setOnClickListener(assignCategory);
            popupView.findViewById(R.id.category_others).setOnClickListener(assignCategory);

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
                    "Income",
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

        cancelBtn.setOnClickListener(v -> {
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