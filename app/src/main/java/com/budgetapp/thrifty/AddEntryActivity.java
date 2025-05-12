package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.budgetapp.thrifty.fragments.NotificationsFragment;
import com.budgetapp.thrifty.fragments.ProfileFragment;
import com.budgetapp.thrifty.transaction.AddExpenseFragment;
import com.budgetapp.thrifty.transaction.AddIncomeFragment;
import com.budgetapp.thrifty.utils.KeyboardBehavior;
import com.google.android.material.tabs.TabLayout;

public class AddEntryActivity extends AppCompatActivity {
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Set window soft input mode to adjust resize
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.activity_add_entry);

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

        ImageButton smallThrifty = findViewById(R.id.small_thrifty);
        smallThrifty.setOnClickListener(view -> {
            finish();
        });

        ImageButton notificationsIcon = findViewById(R.id.ic_notifications);
        notificationsIcon.setOnClickListener(view -> {
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
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("force_navigate_to", "notifications");
            startActivity(intent);
            finish();
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
