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

public class AddExpenseFragment extends Fragment {

    private int selectedIconResId = R.drawable.ic_transport; // default
    private String selectedRecurring = "None";
    private ScrollView scrollView;
    private EditText descriptionInput;

    public AddExpenseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_expense, container, false);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ConstraintLayout categorySelector = view.findViewById(R.id.category_selector);
        TextView categoryText = view.findViewById(R.id.category_text);
        ImageView categoryIcon = view.findViewById(R.id.category_icon);
        EditText numberInput = view.findViewById(R.id.number_input);
        EditText descriptionInput = view.findViewById(R.id.expense_description);
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
            popup.getMenuInflater().inflate(R.menu.expense_category_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

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
                    "Expense",
                    category,
                    amount,
                    iconRes,
                    description,
                    selectedRecurring
            );

            TransactionsHandler.transactions.add(transaction);
            requireActivity().finish(); // go back to MainActivity
        });

        // 5. Cancel button logic
        cancelBtn.setOnClickListener(v -> requireActivity().finish());
    }
}
