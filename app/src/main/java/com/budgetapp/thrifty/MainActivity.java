package com.budgetapp.thrifty;

import com.bumptech.glide.Glide;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import androidx.annotation.Nullable;
import com.budgetapp.thrifty.services.NotificationScheduler;

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
        } else if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        binding.bottomNav.setBackground(null);

        new Handler().postDelayed(this::testPushNotification, 5000);
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

    private void testPushNotification() {
        Log.d("MainActivity", "Setting up local notification scheduler...");

        // Schedule daily notifications
        NotificationScheduler.scheduleDaily(this);

        // Test immediate notification
        testImmediateNotification();
    }

    // ADD this new method to MainActivity:
    private void testImmediateNotification() {
        Log.d("MainActivity", "Testing immediate notification...");

        // Create a test notification
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "transaction_reminders")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Welcome to Thrifty")
                .setContentText("Track. Save. Thrive!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());

        Log.d("MainActivity", "Test notification sent!");
    }

    // ADD this method to MainActivity:
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "transaction_reminders",
                    "Transaction Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for recurring transactions");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
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

        updateBottomNavAvatar();
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

        loadTransactionsAndHandleRecurring();
        loadUserProfileData(user.getUid());
    }

    private void loadTransactionsAndHandleRecurring() {
        FirestoreManager.loadTransactions(transactions -> {
            TransactionsHandler.transactions.clear();
            TransactionsHandler.transactions.addAll(transactions);
            AppLogger.log(this, TAG, "Loaded " + transactions.size() + " transactions");

            dataLoaded = true;

            Intent intent = getIntent();
            boolean isFromAddEntry = intent.getBooleanExtra("from_add_entry", false);

            if (!isFromAddEntry) {
                NotificationsFragment notificationsFragment = new NotificationsFragment();
                TransactionsHandler.checkRecurringTransactions(notificationsFragment);

                // RELOAD transactions after handling recurring
                FirestoreManager.loadTransactions(updatedTransactions -> {
                    TransactionsHandler.transactions.clear();
                    TransactionsHandler.transactions.addAll(updatedTransactions);

                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);

                    if (currentFragment instanceof NotificationsFragment) {
                        replaceFragment(new NotificationsFragment());
                    } else if (currentFragment instanceof HomeFragment) {
                        ((HomeFragment) currentFragment).refreshTransactionList();
                    }

                    refreshAllFragments(); // Optional: keep or remove based on design
                });
            } else {
                intent.removeExtra("from_add_entry");

                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
                if (currentFragment instanceof HomeFragment) {
                    ((HomeFragment) currentFragment).refreshTransactionList();
                }

                refreshAllFragments(); // Optional
            }
        });
    }

    private void loadUserProfileData(String userId) {
        db.collection("users")
                .document(userId)
                .collection("profile")
                .document("info")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        handleProfileDocument(document);
                    } else {
                        // If profile/info doesn't exist, check the root document
                        db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(this::handleProfileDocument);
                    }
                });
    }

    private void handleProfileDocument(com.google.firebase.firestore.DocumentSnapshot document) {
        String username = document.getString("username");
        Long avatarIdLong = document.getLong("avatarId");

        if (username != null) {
            updateGreetingInFragments(username);

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().putString("username", username).apply();
        }

        if (avatarIdLong != null) {
            int avatarId = avatarIdLong.intValue();
            updateAvatarEverywhere(avatarId, null);

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().putInt("avatarId", avatarId).apply();
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
                                updateAvatarEverywhere(avatarId, null);
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
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame_layout);

        // Avoid replacing with the same fragment
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

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
                                updateAvatarEverywhere(avatarId, null);
                            }
                        }
                    });
        }
    }

    public void updateAvatarEverywhere(int avatarId, @Nullable String customAvatarUri) {
        AppLogger.log(this, TAG, "Updating avatar everywhere - avatarId: " + avatarId + ", customAvatarUri: " + customAvatarUri);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Only update if there's actually a change
        int currentAvatarId = prefs.getInt("avatarId", 0);
        String currentCustomUri = prefs.getString("custom_avatar_uri", null);

        boolean hasChanges = false;

        if (avatarId != currentAvatarId) {
            editor.putInt("avatarId", avatarId);
            hasChanges = true;
        }

        if ((customAvatarUri == null && currentCustomUri != null) ||
                (customAvatarUri != null && !customAvatarUri.equals(currentCustomUri))) {
            if (customAvatarUri != null) {
                editor.putString("custom_avatar_uri", customAvatarUri);
            } else {
                editor.remove("custom_avatar_uri");
            }
            hasChanges = true;
        }

        if (hasChanges) {
            editor.apply();

            // Update UI
            updateBottomNavAvatar();

            // Notify fragments
            Bundle result = new Bundle();
            result.putInt("avatarId", avatarId);
            if (customAvatarUri != null) {
                result.putString("custom_avatar_uri", customAvatarUri);
            }
            getSupportFragmentManager().setFragmentResult("profileUpdate", result);
        }
    }

    private void updateBottomNavAvatar() {
        // Get avatar ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int avatarId = prefs.getInt("avatarId", 0);
        String customAvatarUri = prefs.getString("custom_avatar_uri", null);

        // Find the profile icon in the bottom navigation
        View profileItem = binding.bottomNav.findViewById(R.id.ic_profile);
        if (profileItem != null && profileItem instanceof ImageView) {
            updateAvatarImageView((ImageView) profileItem, avatarId, customAvatarUri);
        }
    }

    private void updateAvatarImageView(ImageView imageView, int avatarId, @Nullable String customAvatarUri) {
        if (customAvatarUri != null && !customAvatarUri.isEmpty()) {
            // Load custom avatar from URI
            try {
                Uri uri = Uri.parse(customAvatarUri);
                Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .placeholder(R.drawable.sample_profile)
                        .error(R.drawable.sample_profile)
                        .into(imageView);
            } catch (Exception e) {
                // Fallback to default avatar if URI is invalid
                imageView.setImageResource(R.drawable.sample_profile);
                Log.e(TAG, "Error loading custom avatar", e);
            }
        } else {
            // Load predefined avatar
            int resourceId = getAvatarResourceId(avatarId);
            imageView.setImageResource(resourceId);
        }
    }


    // 4. Add this helper method to MainActivity
    private int getAvatarResourceId(int avatarId) {
        switch (avatarId) {
            case 1: return R.drawable.profile2;
            case 2: return R.drawable.profile3;
            case 3: return R.drawable.profile4;
            case 4: return R.drawable.profile5;
            case 5: return R.drawable.profile6;
            case 6: return R.drawable.profile7;
            case 8: return R.drawable.profile8;
            case 9: return R.drawable.profile9;
            default: return R.drawable.sample_profile;
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

