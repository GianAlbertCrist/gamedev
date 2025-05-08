package com.budgetapp.thrifty;

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
import android.widget.TextView;
import android.widget.ScrollView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.utils.KeyboardBehavior;

public class AddIncomeFragment extends Fragment {

    private int selectedIconResId = R.drawable.ic_salary;
    private String selectedRecurring = "None";
    private ScrollView scrollView;
    private EditText descriptionInput;

    public AddIncomeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_income, container, false);
        scrollView = (ScrollView) view;
        return view;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // Setup description input focus listener to scroll to it when focused
        descriptionInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Post with a delay to ensure keyboard is visible
                v.postDelayed(() -> {
                    scrollView.smoothScrollTo(0, descriptionInput.getBottom());
                }, 300);
            }
        });

        // 2. Category recurring
        recurringButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), recurringButton, 0, 0, R.style.CustomPopupMenu);
            popup.getMenuInflater().inflate(R.menu.recurring_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.category_none) {
                    selectedRecurring = "None";
                } else if (id == R.id.category_daily) {
                    selectedRecurring = "Daily";
                } else if (id == R.id.category_weekly) {
                    selectedRecurring = "Weekly";
                } else if (id == R.id.category_monthly) {
                    selectedRecurring = "Montly";
                } else if (id == R.id.category_yearly) {
                    selectedRecurring = "Yeary";
                }

                return true;
            });

            popup.show();
        });

        // 3. Category selection logic
        categorySelector.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), categorySelector, 0, 0, R.style.CustomPopupMenu);
            popup.getMenuInflater().inflate(R.menu.income_category_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

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

                return true;
            });

            popup.show();
        });

        // 4. Confirm button logic
        confirmBtn.setOnClickListener(v -> {
            String category = categoryText.getText().toString();
            String description = descriptionInput.getText().toString();

            String amountStr = numberInput.getText().toString().trim();
            float amount = amountStr.isEmpty() ? 0 : Float.parseFloat(amountStr);

            int iconRes = selectedIconResId;

            Transaction transaction = new Transaction(
                    "Income",
                    category,
                    amount,
                    iconRes,
                    description,
                    selectedRecurring
            );

            TransactionsHandler.transactions.add(transaction);
            String notificationTime = KeyboardBehavior.getCurrentTime();

            Notification newNotification = new Notification("Transaction", category + " | ₱" + amount, notificationTime);

            NotificationsFragment.addNotification(newNotification);

            requireActivity().finish();
        });


        cancelBtn.setOnClickListener(v -> requireActivity().finish());
    }
}