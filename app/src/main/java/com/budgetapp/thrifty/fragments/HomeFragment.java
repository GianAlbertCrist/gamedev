package com.budgetapp.thrifty.fragments;

import com.bumptech.glide.Glide;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.renderers.TransactionAdapter;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.utils.AppLogger;
import com.budgetapp.thrifty.utils.FormatUtils;
import com.budgetapp.thrifty.utils.GlowingGradientTextView;
import com.budgetapp.thrifty.utils.NotepadManager;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private View rootView, loadingSpinner;
    private RecyclerView recyclerView;
    private TextView emptyMessage, userGreet, notificationBadge;
    private ImageView profileIcon;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration profileListener, notificationsListener;
    private FrameLayout notepadPanelContainer;
    private ConstraintLayout mainContent;
    private boolean isPanelOpen = false;


    @SuppressLint("CutPasteId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        loadingSpinner = rootView.findViewById(R.id.loading_spinner);

        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerView = rootView.findViewById(R.id.home_transactions);
        emptyMessage = rootView.findViewById(R.id.empty_message);
        userGreet = rootView.findViewById(R.id.user_greet);
        profileIcon = rootView.findViewById(R.id.ic_profile);
        notificationBadge = rootView.findViewById(R.id.notification_badge);
        mainContent = rootView.findViewById(R.id.main_content);

        ImageButton notificationButton = rootView.findViewById(R.id.ic_notifications);
        notificationButton.setOnClickListener(v -> openNotificationsFragment());

        ImageButton profileButton = rootView.findViewById(R.id.ic_profile);

        profileButton.setOnClickListener(v -> {
            Fragment currentFragment = requireActivity().getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            if (!(currentFragment instanceof ProfileFragment)) {
                openProfileFragment();
            }
        });

        loadUserProfile();
        loadTransactions();
        loadNotificationCount();
        setupNotepad();

        getParentFragmentManager().setFragmentResultListener("profileUpdate", this, (requestKey, result) -> {
            int avatarId = result.getInt("avatarId", 0);
            String customAvatarUriStr = result.getString("custom_avatar_uri");
            String username = result.getString("username");

            if (profileIcon != null) {
                if (customAvatarUriStr != null) {
                    Uri customUri = Uri.parse(customAvatarUriStr);
                    Glide.with(this)
                            .load(customUri)
                            .circleCrop()
                            .into(profileIcon);
                } else if (avatarId > 0) {
                    updateAvatarImage(profileIcon, avatarId);
                }
            }

            if (userGreet != null && username != null) {
                userGreet.setText("Hello, " + username + "!");
            }
        });

        return rootView;
    }

    private void setupNotepad() {
        // Initialize notepad components
        notepadPanelContainer = rootView.findViewById(R.id.notepad_panel_container);
        View notepadHandle = rootView.findViewById(R.id.notepad_handle);

        // Initialize NotepadManager
        NotepadManager notepadManager = new NotepadManager(requireContext());

        // Setup EditText with auto-save
        EditText notepadContent = rootView.findViewById(R.id.notepad_content);
        notepadManager.setupAutoSave(notepadContent);

        // Setup touch listener for the handle
        notepadHandle.setOnClickListener(v -> toggleNotepadPanel());

        // Setup back button
        ImageButton backButton = rootView.findViewById(R.id.notepad_back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> closeNotepad());
        }
    }

    private void toggleNotepadPanel() {
        if (isPanelOpen) {
            closeNotepad();
        } else {
            notepadPanelContainer.setVisibility(View.VISIBLE);
            mainContent.setAlpha(0.3f);
            isPanelOpen = true;
        }
    }

    private void closeNotepad() {
        notepadPanelContainer.setVisibility(View.GONE);
        mainContent.setAlpha(1.0f);
        isPanelOpen = false;
    }

    private void loadNotificationCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            notificationsListener = db.collection("users").document(user.getUid())
                    .collection("notifications")
                    .whereEqualTo("read", false)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            int unreadCount = value.size();
                            updateNotificationBadge(unreadCount);
                        } else {
                            updateNotificationBadge(0);
                        }
                    });
        }
    }

    private void updateNotificationBadge(int count) {
        if (notificationBadge != null) {
            if (count > 0) {
                notificationBadge.setVisibility(View.VISIBLE);
                notificationBadge.setText(String.valueOf(count > 99 ? "99+" : count));
            } else {
                notificationBadge.setVisibility(View.GONE);
            }
        }
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
        // Load username greeting
        refreshUserGreeting();

        // Load avatar from cache and display
        refreshAvatarFromPrefs();

        // Try to fetch the latest avatarId from Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("profile").document("info")
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            updateAvatarFromDocument(document);
                        } else {
                            // Fallback to root user document
                            db.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(this::updateAvatarFromDocument);
                        }
                    });
        }
    }

    private void refreshAvatarFromPrefs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String customAvatarUriStr = prefs.getString("custom_avatar_uri", null);
        int avatarId = prefs.getInt("avatarId", 0);


        if (customAvatarUriStr != null && !customAvatarUriStr.isEmpty()) {
            // Load custom avatar
            Uri customUri = Uri.parse(customAvatarUriStr);
            Glide.with(this)
                    .load(customUri)
                    .circleCrop()
                    .placeholder(R.drawable.sample_profile)
                    .error(R.drawable.sample_profile)
                    .into(profileIcon);
        } else if (avatarId > 0) {
            // Load predefined avatar
            updateAvatarImage(profileIcon, avatarId);
        } else {
            // Fallback to default avatar
            profileIcon.setImageResource(R.drawable.sample_profile);
        }
    }


    private void updateAvatarFromDocument(com.google.firebase.firestore.DocumentSnapshot document) {
        if (document.exists()) {
            Long avatarIdLong = document.getLong("avatarId");
            String customAvatarUri = document.getString("customAvatarUri"); // Fixed field name
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            if (customAvatarUri != null && !customAvatarUri.isEmpty()) {
                // Custom avatar found
                editor.putString("custom_avatar_uri", customAvatarUri);
                editor.putInt("avatarId", 0);
                editor.apply();

                Glide.with(this)
                        .load(Uri.parse(customAvatarUri))
                        .circleCrop()
                        .placeholder(R.drawable.sample_profile)
                        .error(R.drawable.sample_profile)
                        .into(profileIcon);
            } else if (avatarIdLong != null) {
                // Predefined avatar
                int avatarId = avatarIdLong.intValue();
                editor.putInt("avatarId", avatarId);
                editor.remove("custom_avatar_uri");
                editor.apply();

                updateAvatarImage(profileIcon, avatarId);
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        refreshUserGreeting();
        refreshAvatarFromPrefs();
        loadNotificationCount();
        updateBalances();

        handleStreak(requireContext());

        if (isPanelOpen) {
            closeNotepad();
        }

        if (!TransactionsHandler.transactions.isEmpty()) {
            loadTransactions();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
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
        showLoading(true);

        recyclerView.postDelayed(() -> {
            if (TransactionsHandler.transactions.isEmpty()) {
                emptyMessage.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyMessage.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

                List<Transaction> reversedList = new ArrayList<>(TransactionsHandler.transactions);
                Collections.reverse(reversedList);

                TransactionAdapter adapter = new TransactionAdapter(getContext(), reversedList);
                recyclerView.setAdapter(adapter);
            }

            showLoading(false);
        }, 100);
    }

    private void showLoading(boolean show) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void handleStreak(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("streak_data", Context.MODE_PRIVATE);
        long lastLogin = prefs.getLong("last_login", 0);
        int currentStreak = prefs.getInt("streak", 0);

        long today = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;

        if (System.currentTimeMillis() - lastLogin >= oneDayMillis && System.currentTimeMillis() - lastLogin < 2 * oneDayMillis) {
            currentStreak++;
        } else if (System.currentTimeMillis() - lastLogin >= 2 * oneDayMillis) {
            currentStreak = 1;
        } else if (lastLogin == 0) {
            currentStreak = 1;
        }

        prefs.edit()
                .putInt("streak", currentStreak)
                .putLong("last_login", today)
                .apply();

        updateStreakDisplay(currentStreak);
    }

//    FOR TESTING
//    private void handleStreak(Context context) {
//        int currentStreak = 150;
//
//        SharedPreferences prefs = context.getSharedPreferences("streak_data", Context.MODE_PRIVATE);
//        prefs.edit()
//                .putInt("streak", currentStreak)
//                .putLong("last_login", System.currentTimeMillis())
//                .apply();
//
//        updateStreakDisplay(currentStreak);
//    }

    @SuppressLint("SetTextI18n")
    private void updateStreakDisplay(int currentStreak) {
        if (getView() == null) return;
        GlowingGradientTextView streakTextView = getView().findViewById(R.id.streakTextView);
        if (streakTextView != null) {
            streakTextView.setText("Streak " + currentStreak);
            streakTextView.setStreak(currentStreak);
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

    public void refreshTransactionList() {
        if (getView() != null) {
            showLoading(true);
            loadTransactions();
            updateBalances();
        }
    }

    private void updateAvatarImage(ImageView imageView, int avatarId) {
        int resourceId = -1;
        switch (avatarId) {
            case 1: resourceId = R.drawable.profile2; break;
            case 2: resourceId = R.drawable.profile3; break;
            case 3: resourceId = R.drawable.profile4; break;
            case 4: resourceId = R.drawable.profile5; break;
            case 5: resourceId = R.drawable.profile6; break;
            case 6: resourceId = R.drawable.profile7; break;
            case 8: resourceId = R.drawable.profile8; break;
            case 9: resourceId = R.drawable.profile9; break;
        }

        if (resourceId != -1) {
            imageView.setImageResource(resourceId);
        }
    }
}
