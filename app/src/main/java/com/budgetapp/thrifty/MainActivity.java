package com.budgetapp.thrifty;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import com.budgetapp.thrifty.databinding.ActivityMainBinding;
import com.budgetapp.thrifty.fragments.HomeFragment;
import com.budgetapp.thrifty.fragments.ProfileFragment;
import com.budgetapp.thrifty.fragments.ReportsFragment;
import com.budgetapp.thrifty.fragments.TransactionsFragment;
import com.budgetapp.thrifty.fragments.NotificationsFragment;
import com.budgetapp.thrifty.utils.AppLogger;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.budgetapp.thrifty.model.Notification;
import com.budgetapp.thrifty.transaction.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private ListenerRegistration profileListener;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    ActivityMainBinding binding;
    private boolean dataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ThemeSync.syncNotificationBarColor(getWindow(), this);
        appBttmBarColorAjuster();

        // Setup initial UI without waiting for data
        setupBottomNavigation();

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Initialize FCM
        initializeFCM();

        // Determine which fragment to show
        String navigateTo = getIntent().getStringExtra("navigate_to");
        String forceNavigateTo = getIntent().getStringExtra("force_navigate_to");
        String transactionId = getIntent().getStringExtra("transaction_id");

        if (forceNavigateTo != null && forceNavigateTo.equals("notifications")) {
            navigateToNotificationFragment();
        } else if (navigateTo != null && navigateTo.equals("profile")) {
            navigateToProfileFragment();
        } else if (navigateTo != null && navigateTo.equals("transactions") && transactionId != null) {
            navigateToTransactionDetails(transactionId);
        } else {
            replaceFragment(new HomeFragment());
        }

        binding.bottomNav.setBackground(null);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void initializeFCM() {
        // Subscribe to topics for general notifications
        FirebaseMessaging.getInstance().subscribeToTopic("transaction_reminders")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        AppLogger.log(this, TAG, "Subscribed to transaction_reminders topic");
                    } else {
                        AppLogger.logError(this, TAG, "Failed to subscribe to transaction_reminders topic", task.getException());
                    }
                });

        // Save FCM token to Firestore
        FirestoreManager.saveFCMToken();
    }

    private void navigateToTransactionDetails(String transactionId) {
        // Navigate to transactions fragment
        binding.bottomNav.setSelectedItemId(R.id.ic_transactions);

        // Create and set up the transactions fragment with the transaction ID
        TransactionsFragment fragment = new TransactionsFragment();
        Bundle args = new Bundle();
        args.putString("transaction_id", transactionId);
        fragment.setArguments(args);

        replaceFragment(fragment);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppLogger.log(this, TAG, "onStart called - attaching Firestore listeners");
        attachFirestoreListeners();
        loadUserData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppLogger.log(this, TAG, "onStop called - detaching Firestore listeners");
        detachFirestoreListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppLogger.log(this, TAG, "onResume called");
        // Ensure data is loaded when app is brought to foreground
        if (!dataLoaded) {
            loadUserData();
        }

        // Update avatar in bottom navigation
        updateBottomNavAvatar();
    }

    private void checkRecurringTransactions() {
        // Create a temporary NotificationsFragment if one doesn't exist
        NotificationsFragment notificationsFragment = new NotificationsFragment();

        // Check recurring transactions and generate notifications
        TransactionsHandler.checkRecurringTransactions(notificationsFragment);

        // If notifications were added, update the UI
        if (!notificationsFragment.getNotificationList().isEmpty()) {
            AppLogger.log(this, TAG, "Found " + notificationsFragment.getNotificationList().size() + " notifications");

            // Save notifications to Firestore
            for (Notification notification : notificationsFragment.getNotificationList()) {
                // Find the transaction ID for this notification
                for (Transaction transaction : TransactionsHandler.transactions) {
                    if (transaction.getRecurring().equals(notification.getRecurring()) &&
                            notification.getDescription().contains(transaction.getDescription())) {
                        FirestoreManager.saveNotification(transaction);
                        break;
                    }
                }
            }

            // If we're currently on the notifications fragment, refresh it
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            if (currentFragment instanceof NotificationsFragment) {
                replaceFragment(new NotificationsFragment());
            }
        }
    }

    private void attachFirestoreListeners() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Listen for profile changes in the profile/info document
            profileListener = db.collection("users")
                    .document(userId)
                    .collection("profile")
                    .document("info")
                    .addSnapshotListener((document, error) -> {
                        if (error != null) {
                            AppLogger.logError(this, TAG, "Error listening to profile changes", error);
                            return;
                        }

                        if (document != null && document.exists()) {
                            String username = document.getString("username");
                            Long avatarIdLong = document.getLong("avatarId");

                            updateGreetingInFragments(username);

                            if (avatarIdLong != null) {
                                int avatarId = avatarIdLong.intValue();
                                updateAvatarEverywhere(avatarId);
                            }

                            AppLogger.log(this, TAG, "Profile data updated: username = " + username);
                        }
                    });
        }
    }

    private void detachFirestoreListeners() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            AppLogger.log(this, TAG, "No user logged in, redirecting to login");
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        AppLogger.log(this, TAG, "Loading user data for: " + user.getUid());

        // Load transactions data
        FirestoreManager.loadTransactions(transactions -> {
            TransactionsHandler.transactions.clear();
            TransactionsHandler.transactions.addAll(transactions);
            AppLogger.log(this, TAG, "Loaded " + transactions.size() + " transactions");

            dataLoaded = true;

            // ✅ Now that data is loaded, trigger recurring check
            NotificationsFragment notificationsFragment = new NotificationsFragment();
            TransactionsHandler.checkRecurringTransactions(notificationsFragment);

            // Save notifications if generated
            if (!notificationsFragment.getNotificationList().isEmpty()) {
                for (Notification notification : notificationsFragment.getNotificationList()) {
                    for (Transaction transaction : TransactionsHandler.transactions) {
                        if (transaction.getRecurring().equals(notification.getRecurring()) &&
                                notification.getDescription().contains(transaction.getDescription())) {
                            FirestoreManager.saveNotification(transaction);
                            break;
                        }
                    }
                }

                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
                if (currentFragment instanceof NotificationsFragment) {
                    replaceFragment(new NotificationsFragment());
                }
            }

            refreshAllFragments();
        });

        // Load user profile data
        db.collection("users")
                .document(user.getUid())
                .collection("profile")
                .document("info")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        Long avatarIdLong = document.getLong("avatarId");

                        if (username != null) {
                            updateGreetingInFragments(username);

                            // Save to SharedPreferences
                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            prefs.edit().putString("username", username).apply();
                        }

                        if (avatarIdLong != null) {
                            int avatarId = avatarIdLong.intValue();
                            updateAvatarEverywhere(avatarId);

                            // Save to SharedPreferences
                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            prefs.edit().putInt("avatarId", avatarId).apply();
                        }
                    } else {
                        // If profile/info doesn't exist, check the root document
                        db.collection("users").document(user.getUid())
                                .get()
                                .addOnSuccessListener(rootDoc -> {
                                    if (rootDoc.exists()) {
                                        String username = rootDoc.getString("username");
                                        Long avatarIdLong = rootDoc.getLong("avatarId");

                                        if (username != null) {
                                            updateGreetingInFragments(username);
                                        }

                                        if (avatarIdLong != null) {
                                            int avatarId = avatarIdLong.intValue();
                                            updateAvatarEverywhere(avatarId);
                                        }
                                    }
                                });
                    }
                });
    }

    private void setupBottomNavigation() {
        binding.fabAddEntry.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEntryActivity.class);
            startActivity(intent);
        });

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.ic_home) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (id == R.id.ic_transactions) {
                replaceFragment(new TransactionsFragment());
                return true;
            } else if (id == R.id.ic_reports) {
                replaceFragment(new ReportsFragment());
                return true;
            } else if (id == R.id.ic_profile) {
                replaceFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void navigateToProfileFragment() {
        binding.bottomNav.setSelectedItemId(R.id.ic_profile);
        replaceFragment(new ProfileFragment());
    }

    private void navigateToNotificationFragment() {
        replaceFragment(new NotificationsFragment());
    }

    // Replace current fragment with the new fragment
    public void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        fragmentTransaction.replace(R.id.frame_layout, fragment, tag);
        fragmentTransaction.commitNow();
    }

    public void appBttmBarColorAjuster() {
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        ColorStateList iconTintDay = ContextCompat.getColorStateList(this, R.color.bottom_nav_icon_selector_day);
        ColorStateList iconTintNight = ContextCompat.getColorStateList(this, R.color.bottom_nav_icon_selector_night);

        if (isDarkMode) {
            binding.bottomNav.setItemIconTintList(iconTintNight);
        } else {
            binding.bottomNav.setItemIconTintList(iconTintDay);
        }
    }

    private void updateGreetingInFragments(String username) {
        FragmentManager fm = getSupportFragmentManager();
        for (Fragment fragment : fm.getFragments()) {
            if (fragment instanceof HomeFragment && fragment.getView() != null) {
                TextView userGreet = fragment.getView().findViewById(R.id.user_greet);
                if (userGreet != null && username != null && !username.isEmpty()) {
                    userGreet.setText("Hello, " + username + "!");
                }
            }
        }
    }

    public void refreshAllFragments() {
        // Get current visible fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);

        if (currentFragment != null) {
            // Refresh the current fragment by replacing it with a new instance
            String className = currentFragment.getClass().getSimpleName();
            AppLogger.log(this, TAG, "Refreshing current fragment: " + className);

            if (className.equals(HomeFragment.class.getSimpleName())) {
                replaceFragment(new HomeFragment());
            } else if (className.equals(TransactionsFragment.class.getSimpleName())) {
                replaceFragment(new TransactionsFragment());
            } else if (className.equals(ReportsFragment.class.getSimpleName())) {
                replaceFragment(new ReportsFragment());
            } else if (className.equals(ProfileFragment.class.getSimpleName())) {
                replaceFragment(new ProfileFragment());
            } else if (className.equals(NotificationsFragment.class.getSimpleName())) {
                replaceFragment(new NotificationsFragment());
            }
        }

        // Also update any user-related UI
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users")
                    .document(userId)
                    .collection("profile")
                    .document("info")
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String username = document.getString("username");
                            Long avatarIdLong = document.getLong("avatarId");

                            if (username != null) {
                                updateGreetingInFragments(username);
                            }

                            if (avatarIdLong != null) {
                                int avatarId = avatarIdLong.intValue();
                                updateAvatarEverywhere(avatarId);
                            }
                        }
                    });
        }
    }

    public void updateAvatarEverywhere(int avatarId) {
        // Update SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putInt("avatarId", avatarId).apply();

        AppLogger.log(this, TAG, "Updating avatar everywhere to: " + avatarId);

        // Update bottom navigation avatar
        updateBottomNavAvatar();

        // Update current fragment if it's HomeFragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
        if (currentFragment instanceof HomeFragment) {
            View fragmentView = currentFragment.getView();
            if (fragmentView != null) {
                ImageView profileIcon = fragmentView.findViewById(R.id.ic_profile);
                if (profileIcon != null) {
                    updateAvatarImage(profileIcon, avatarId);
                }
            }
        }
    }

    private void updateBottomNavAvatar() {
        // Get avatar ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int avatarId = prefs.getInt("avatarId", 0);

        // Find the profile icon in the bottom navigation
        View profileItem = binding.bottomNav.findViewById(R.id.ic_profile);
        if (profileItem != null && profileItem instanceof ImageView) {
            updateAvatarImage((ImageView) profileItem, avatarId);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLogger.log(this, TAG, "Notification permission granted");
            } else {
                AppLogger.log(this, TAG, "Notification permission denied");
            }
        }
    }
}
