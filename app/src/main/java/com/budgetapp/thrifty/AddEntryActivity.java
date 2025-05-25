package com.budgetapp.thrifty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.budgetapp.thrifty.transaction.AddExpenseFragment;
import com.budgetapp.thrifty.transaction.AddIncomeFragment;
import com.budgetapp.thrifty.utils.AppLogger;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.budgetapp.thrifty.utils.KeyboardBehavior;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AddEntryActivity extends AppCompatActivity {
    private static final String TAG = "AddEntryActivity";
    private TabLayout tabLayout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView userGreet;
    private ImageView profileIcon;
    private TextView notificationBadge;
    private ListenerRegistration notificationsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ThemeSync.syncNotificationBarColor(getWindow(), this);

        // Set window soft input mode to adjust resize
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.activity_add_entry);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userGreet = findViewById(R.id.user_greet);
        profileIcon = findViewById(R.id.ic_profile);
        notificationBadge = findViewById(R.id.notification_badge);

        // Load initial fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new AddIncomeFragment())
                .commit();

        View rootView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // Apply padding for both IME and navigation bar
            int totalBottomPadding = Math.max(imeHeight, navigationBarHeight);
            v.setPadding(0, 0, 0, totalBottomPadding);

            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize TabLayout
        tabLayout = findViewById(R.id.tabLayout);
        loadUserData();
        loadNotificationCount();

        ImageButton smallThrifty = findViewById(R.id.small_thrifty);
        smallThrifty.setOnClickListener(view -> {
            finish();
        });

        // Profile button
        ImageButton profileButton = findViewById(R.id.ic_profile);
        profileButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to", "profile");
            startActivity(intent);
            finish();
        });

        // notification button
        ImageButton notificationsButton = findViewById(R.id.ic_notifications);
        notificationsButton.setOnClickListener(view -> {
            try {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("force_navigate_to", "notifications");
                startActivity(intent);
                finish();
            } catch (Exception e) {
                AppLogger.logError(this, TAG,"Error starting NotificationsActivity", e);
            }
        });

        getSupportFragmentManager().setFragmentResultListener("notificationsViewed", this, (requestKey, result) -> {
            boolean notificationsViewed = result.getBoolean("notificationsViewed", false);
            if (notificationsViewed) {
                // Refresh the notification badge count
                loadNotificationCount();
            }
        });

        // Setup touch outside to dismiss keyboard
        setupTouchOutsideToDismissKeyboard();

        //tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment selectedFragment;
                if (tab.getPosition() == 0) {
                    selectedFragment = new AddIncomeFragment();
                } else {
                    selectedFragment = new AddExpenseFragment();
                }

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();

                int color = tab.getPosition() == 0 ? R.color.primary_color : R.color.red;
                tabLayout.setSelectedTabIndicatorColor(getColor(color));
                tabLayout.setTabTextColors(
                        getColor(tab.getPosition() == 0 ? R.color.red : R.color.primary_color),
                        getColor(color)
                );

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadNotificationCount() {
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        FirestoreManager.getDueNotificationCount(count -> {
            runOnUiThread(() -> {
                if (notificationBadge != null) {
                    if (count > 0) {
                        notificationBadge.setText(String.valueOf(count));
                        notificationBadge.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Showing notification badge with count: " + count);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                        Log.d(TAG, "Hiding notification badge - no due notifications");
                    }
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        loadNotificationCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationsListener != null) {
            notificationsListener.remove();
        }
    }

    private void loadUserData() {
        // Load user name and avatar
        loadUserName();
        loadUserAvatar();
    }

    private void loadUserName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // First try to get the name from SharedPreferences for faster loading
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String cachedUsername = prefs.getString("username", null);

            if (cachedUsername != null) {
                userGreet.setText("Hello, " + cachedUsername + "!");
            } else {
                // If not in SharedPreferences, try to get from Firebase Auth display name
                String displayName = user.getDisplayName();
                if (displayName != null && displayName.contains("|")) {
                    String username = displayName.split("\\|")[0];
                    userGreet.setText("Hello, " + username + "!");
                } else {
                    // As a last resort, fetch from Firestore
                    db.collection("users").document(user.getUid())
                            .collection("profile").document("info")
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                    String name = task.getResult().getString("username");
                                    userGreet.setText(name != null ?
                                            "Hello, " + name + "!" :
                                            "Hello, User!");

                                    // Cache the username for future use
                                    if (name != null) {
                                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                                .edit()
                                                .putString("username", name)
                                                .apply();
                                    }
                                } else {
                                    // If profile/info doesn't exist, check the root document
                                    db.collection("users").document(user.getUid())
                                            .get()
                                            .addOnCompleteListener(rootTask -> {
                                                if (rootTask.isSuccessful() && rootTask.getResult() != null && rootTask.getResult().exists()) {
                                                    String name = rootTask.getResult().getString("username");
                                                    userGreet.setText(name != null ?
                                                            "Hello, " + name + "!" :
                                                            "Hello, User!");
                                                } else {
                                                    userGreet.setText("Hello, User!");
                                                    Log.e(TAG, "Error loading user", task.getException());
                                                }
                                            });
                                }
                            });
                }
            }
        } else {
            userGreet.setText("Hello, Guest!");
        }
    }

    private void loadUserAvatar() {
        // First try to get avatar from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int avatarId = prefs.getInt("avatarId", 0);

        if (avatarId > 0) {
            updateAvatarImage(profileIcon, avatarId);
        } else {
            // If not in SharedPreferences, fetch from Firestore
            FirebaseUser user = auth.getCurrentUser();
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

    private void updateAvatarImage(ImageView imageView, int avatarId) {
        int resourceId;
        switch (avatarId) {
            case 1: resourceId = R.drawable.profile2; break;
            case 2: resourceId = R.drawable.profile3; break;
            case 3: resourceId = R.drawable.profile4; break;
            case 4: resourceId = R.drawable.profile5; break;
            case 5: resourceId = R.drawable.profile6; break;
            case 6: resourceId = R.drawable.profile7; break;
            case 8: resourceId = R.drawable.profile8; break;
            case 9: resourceId = R.drawable.profile9; break;
            default: resourceId = R.drawable.sample_profile; break;
        }
        imageView.setImageResource(resourceId);
    }

    private void setupTouchOutsideToDismissKeyboard() {
        // Get the main layout
        View mainLayout = findViewById(R.id.main);

        // Set up touch listener for non-text box views to hide keyboard
        if (mainLayout != null) {
            mainLayout.setOnClickListener(v -> {
                // Hide keyboard when clicking outside of text fields
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    currentFocus.clearFocus();
                    KeyboardBehavior.hideKeyboard(this, currentFocus);
                }
            });
        }

        // Make sure buttons don't trigger the keyboard hiding when clicked
        Button confirmButton = findViewById(R.id.confirm_button);
        Button cancelButton = findViewById(R.id.cancel_button);

        // Handle confirm button click
        // This will be handled by the fragment
        if (confirmButton != null) {
            confirmButton.setOnClickListener(v -> {
            });
        }
        // Handle cancel button click
        // This will be handled by the fragment
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
            });
        }
    }
}
