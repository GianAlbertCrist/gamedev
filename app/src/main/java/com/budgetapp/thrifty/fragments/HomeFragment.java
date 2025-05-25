package com.budgetapp.thrifty.fragments;

import com.budgetapp.thrifty.utils.FirestoreManager;
import com.bumptech.glide.Glide;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.budgetapp.thrifty.utils.FormatUtils;
import com.budgetapp.thrifty.utils.GlowingGradientTextView;
import com.budgetapp.thrifty.utils.NotepadManager;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment {

    private View rootView, loadingSpinner;
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private TextView userGreet;
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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = rootView.findViewById(R.id.home_transactions);
        emptyMessage = rootView.findViewById(R.id.empty_message);
        userGreet = rootView.findViewById(R.id.user_greet);
        profileIcon = rootView.findViewById(R.id.ic_profile);
        TextView notificationBadge = rootView.findViewById(R.id.notification_badge);
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

        getParentFragmentManager().setFragmentResultListener("notificationsViewed", this, (requestKey, result) -> {
            int newCount = result.getInt("unreadCount", -1);
            if (newCount >= 0) {
                updateNotificationBadge();
            } else {
                loadNotificationCount();
            }
        });

        updateNotificationBadge();

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
            db.collection("users").document(user.getUid())
                    .collection("notifications")
                    .whereEqualTo("isNotified", false)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int count = 0;
                        Date today = resetToStartOfDay(new Date());

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Timestamp nextDue = doc.getTimestamp("nextDueDate");
                            if (nextDue != null && !nextDue.toDate().after(today)) {
                                count++;
                            }
                        }

                        Log.d("HomeFragment", "Due notifications today or earlier: " + count);
                        updateNotificationBadge();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("HomeFragment", "Failed to load notifications", e);
                    });
        }
    }

    private Date resetToStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void updateNotificationBadge() {
        FirestoreManager.getDueNotificationCount(count -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    TextView notificationBadge = getView().findViewById(R.id.notification_badge);

                    if (count > 0) {
                        notificationBadge.setText(String.valueOf(count));
                        notificationBadge.setVisibility(View.VISIBLE);
                        Log.d("HomeFragment", "Showing notification badge with count: " + count);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                        Log.d("HomeFragment", "Hiding notification badge - no due notifications");
                    }
                });
            }
        });
    }

    public void refreshUserGreeting() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username != null) {
            userGreet.setText("Hello, " + username + "!");
        } else {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getDisplayName() != null) {
                String[] userData = user.getDisplayName().split("\\|");
                username = userData[0];
                userGreet.setText("Hello, " + username + "!");
            }
        }
    }

    private void loadUserProfile() {
        refreshUserGreeting();

        refreshAvatarFromPrefs();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("profile").document("info")
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            updateAvatarFromDocument(document);
                        } else {
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
            Uri customUri = Uri.parse(customAvatarUriStr);
            Glide.with(this)
                    .load(customUri)
                    .circleCrop()
                    .placeholder(R.drawable.sample_profile)
                    .error(R.drawable.sample_profile)
                    .into(profileIcon);
        } else if (avatarId > 0) {
            updateAvatarImage(profileIcon, avatarId);
        } else {
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
        updateNotificationBadge();

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

        long diffDays = getDiffDays(lastLogin);

        if (lastLogin == 0 || diffDays == 1) {
            currentStreak++;
        } else if (diffDays > 1) {
            currentStreak = 1;
        }

        prefs.edit()
                .putInt("streak", currentStreak)
                .putLong("last_login", System.currentTimeMillis())
                .apply();

        updateStreakDisplay(currentStreak);
    }

    private static long getDiffDays(long lastLogin) {
        Calendar lastLoginCal = Calendar.getInstance();
        lastLoginCal.setTimeInMillis(lastLogin);
        // Set to midnight to normalize
        lastLoginCal.set(Calendar.HOUR_OF_DAY, 0);
        lastLoginCal.set(Calendar.MINUTE, 0);
        lastLoginCal.set(Calendar.SECOND, 0);
        lastLoginCal.set(Calendar.MILLISECOND, 0);

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        long diffDays = (todayCal.getTimeInMillis() - lastLoginCal.getTimeInMillis()) / (24 * 60 * 60 * 1000);
        return diffDays;
    }

//    FOR TESTING
//    private void handleStreak(Context context) {
//        int currentStreak = 1500;
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
