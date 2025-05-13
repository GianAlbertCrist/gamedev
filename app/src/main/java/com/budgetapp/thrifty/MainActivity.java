package com.budgetapp.thrifty;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.budgetapp.thrifty.databinding.ActivityMainBinding;
import com.budgetapp.thrifty.fragments.HomeFragment;
import com.budgetapp.thrifty.fragments.ProfileFragment;
import com.budgetapp.thrifty.fragments.ReportsFragment;
import com.budgetapp.thrifty.fragments.TransactionsFragment;
import com.budgetapp.thrifty.fragments.NotificationsFragment;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
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
        themeSync();

        // Setup initial UI without waiting for data
        setupBottomNavigation();

        // Determine which fragment to show
        String navigateTo = getIntent().getStringExtra("navigate_to");
        String forceNavigateTo = getIntent().getStringExtra("force_navigate_to");

        if (forceNavigateTo != null && forceNavigateTo.equals("notifications")) {
            navigateToNotificationFragment();
        } else if (navigateTo != null && navigateTo.equals("profile")) {
            navigateToProfileFragment();
        } else {
            replaceFragment(new HomeFragment());
        }

        binding.bottomNav.setBackground(null);

        NotificationsFragment notificationsFragment = (NotificationsFragment) getSupportFragmentManager()
                .findFragmentByTag(NotificationsFragment.class.getSimpleName());
        if (notificationsFragment != null) {
            TransactionsHandler.checkRecurringTransactions(notificationsFragment);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called - attaching Firestore listeners");
        attachFirestoreListeners();
        loadUserData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called - detaching Firestore listeners");
        detachFirestoreListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Ensure data is loaded when app is brought to foreground
        if (!dataLoaded) {
            loadUserData();
        }
    }

    private void attachFirestoreListeners() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Listen for profile changes
            profileListener = db.collection("users")
                    .document(userId)
                    .addSnapshotListener((document, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error listening to profile changes", error);
                            return;
                        }

                        if (document != null && document.exists()) {
                            String username = document.getString("username");
                            updateGreetingInFragments(username);
                            Log.d(TAG, "Profile data updated: username = " + username);
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
            Log.w(TAG, "No user logged in, redirecting to login");
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "Loading user data for: " + user.getUid());

        // Load transactions data
        FirestoreManager.loadTransactions(transactions -> {
            TransactionsHandler.transactions.clear();
            TransactionsHandler.transactions.addAll(transactions);
            Log.d(TAG, "Loaded " + transactions.size() + " transactions");

            dataLoaded = true;

            // Refresh fragments with new data
            refreshAllFragments();
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

    public void themeSync() {
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
            Log.d(TAG, "Refreshing current fragment: " + className);

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
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String username = document.getString("username");
                            updateGreetingInFragments(username);
                        }
                    });
        }
    }
}