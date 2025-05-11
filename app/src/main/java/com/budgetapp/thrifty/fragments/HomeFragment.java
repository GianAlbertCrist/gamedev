package com.budgetapp.thrifty.fragments;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.renderers.TransactionAdapter;
import com.budgetapp.thrifty.utils.FormatUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private TextView userGreet;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener profileListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI components
        recyclerView = rootView.findViewById(R.id.home_transactions);
        emptyMessage = rootView.findViewById(R.id.empty_message);
        userGreet = rootView.findViewById(R.id.user_greet);

        ImageButton notificationButton = rootView.findViewById(R.id.ic_notifcations);
        notificationButton.setOnClickListener(v -> openNotificationsFragment());

        // Load user profile data
        loadUserProfile();

        return rootView;
    }

    public void refreshUserGreeting() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        // First try to load from SharedPreferences for immediate display
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);

        String username = prefs.getString("username", "");

        if (!username.isEmpty()) {
            userGreet.setText("Hello, " + username + "!");
        }

        // Then try to get from Firebase for the most up-to-date data
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            // Remove any existing listener
            if (profileListener != null) {
                mDatabase.child("users").child(uid).removeEventListener(profileListener);
            }

            // Add real-time listener for profile changes
            profileListener = mDatabase.child("users").child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").exists() ?
                                dataSnapshot.child("username").getValue(String.class) : "";

                        if (username != null && !username.isEmpty()) {
                            userGreet.setText("Hello, " + username + "!");

                            // Also save to SharedPreferences as backup
                            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                                    requireActivity().MODE_PRIVATE);
                            prefs.edit().putString("username", username).apply();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle possible errors
                }
            });
        } else {
            userGreet.setText("Hello, User!");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
        updateBalances();
        refreshUserGreeting(); // Refresh the greeting when returning to this fragment
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listener to prevent memory leaks
        if (profileListener != null && mAuth.getCurrentUser() != null) {
            mDatabase.child("users").child(mAuth.getCurrentUser().getUid())
                    .removeEventListener(profileListener);
            profileListener = null;
        }
    }

    @SuppressLint("DefaultLocale")
    private void updateBalances() {
        TextView balanceText = rootView.findViewById(R.id.total_balance);
        TextView incomeText = rootView.findViewById(R.id.total_income);
        TextView expenseText = rootView.findViewById(R.id.total_expense);

        double balance = TransactionsHandler.getBalance();
        double income = TransactionsHandler.getTotalIncome();
        double expense = TransactionsHandler.getTotalExpense();

        balanceText.setText(String.format("₱ %s", FormatUtils.formatAmount(balance, false)));
        incomeText.setText(String.format("₱ %s", FormatUtils.formatAmount(income, true)));
        expenseText.setText(String.format("₱ %s", FormatUtils.formatAmount(expense, true)));
    }

    private void loadTransactions() {
        if (TransactionsHandler.transactions.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            TransactionAdapter adapter = new TransactionAdapter(getContext(), TransactionsHandler.transactions);
            recyclerView.setAdapter(adapter);
        }
    }

    private void openNotificationsFragment() {
        NotificationsFragment notificationsFragment = new NotificationsFragment();

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Replace the current fragment with NotificationsFragment
        fragmentTransaction.replace(R.id.frame_layout, notificationsFragment);

        // Add to the back stack so the user can navigate back
        fragmentTransaction.addToBackStack(null);

        // Commit the transaction
        fragmentTransaction.commit();
    }
}
