package com.budgetapp.thrifty.fragments;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.budgetapp.thrifty.MainActivity;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.renderers.TransactionAdapter;
import com.budgetapp.thrifty.utils.FormatUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class HomeFragment extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private TextView userGreet;
    private ImageView profileIcon;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration profileListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerView = rootView.findViewById(R.id.home_transactions);
        emptyMessage = rootView.findViewById(R.id.empty_message);
        userGreet = rootView.findViewById(R.id.user_greet);
        profileIcon = rootView.findViewById(R.id.ic_profile);

        ImageButton notificationButton = rootView.findViewById(R.id.ic_notifications);
        notificationButton.setOnClickListener(v -> openNotificationsFragment());

        ImageButton profileButton = rootView.findViewById(R.id.ic_profile);
        profileButton.setOnClickListener(v -> openProfileFragment());

        // Load user profile data
        loadUserProfile();

        return rootView;
    }

    public void refreshUserGreeting() {
        // First try to get from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username != null) {
            userGreet.setText("Hello, " + username + "!");
        } else {
            // If not in SharedPreferences, try Firebase Auth
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getDisplayName() != null) {
                String[] userData = user.getDisplayName().split("\\|");
                username = userData[0];
                userGreet.setText("Hello, " + username + "!");
            }
        }
    }

    private void loadUserProfile() {
        // Load username
        refreshUserGreeting();

        // Load avatar
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);
        int avatarId = prefs.getInt("avatarId", 0);

        if (avatarId > 0) {
            updateAvatarImage(profileIcon, avatarId);
        } else {
            // If not in SharedPreferences, fetch from Firestore
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                db.collection("users").document(user.getUid())
                        .collection("profile").document("info")
                        .get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                Long avatarIdLong = document.getLong("avatarId");
                                if (avatarIdLong != null) {
                                    int newAvatarId = avatarIdLong.intValue();
                                    updateAvatarImage(profileIcon, newAvatarId);

                                    // Cache for future use
                                    prefs.edit().putInt("avatarId", newAvatarId).apply();
                                }
                            } else {
                                // If profile/info doesn't exist, check the root document
                                db.collection("users").document(user.getUid())
                                        .get()
                                        .addOnSuccessListener(rootDoc -> {
                                            if (rootDoc.exists()) {
                                                Long avatarIdLong = rootDoc.getLong("avatarId");
                                                if (avatarIdLong != null) {
                                                    int newAvatarId = avatarIdLong.intValue();
                                                    updateAvatarImage(profileIcon, newAvatarId);
                                                }
                                            }
                                        });
                            }
                        });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
        updateBalances();
        refreshUserGreeting(); // Refresh the greeting when returning to this fragment

        // Also refresh avatar
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);
        int avatarId = prefs.getInt("avatarId", 0);
        if (avatarId > 0) {
            updateAvatarImage(profileIcon, avatarId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listener to prevent memory leaks
        if (profileListener != null) {
            profileListener.remove();
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

        fragmentTransaction.replace(R.id.frame_layout, notificationsFragment);

        fragmentTransaction.addToBackStack(null);

        fragmentTransaction.commit();
    }

    private void openProfileFragment() {
        ProfileFragment profileFragment = new ProfileFragment();
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, profileFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void refreshUserData() {
        // Refresh user greeting
        refreshUserGreeting();

        // Refresh avatar
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);
        int avatarId = prefs.getInt("avatarId", 0);
        if (avatarId > 0) {
            updateAvatarImage(profileIcon, avatarId);
        }
    }

    private void updateAvatarImage(ImageView imageView, int avatarId) {
        int resourceId;
        switch (avatarId) {
            case 1: resourceId = R.drawable.profile2; break;
            case 2: resourceId = R.drawable.profile3; break;
            case 3: resourceId = R.drawable.profile4; break;
            case 4: resourceId = R.drawable.profile5; break;
            case 5: resourceId = R.drawable.profile6; break;
            case 6: resourceId = R.drawable.profile7; break;
            default: resourceId = R.drawable.sample_profile; break;
        }
        imageView.setImageResource(resourceId);
    }
}
