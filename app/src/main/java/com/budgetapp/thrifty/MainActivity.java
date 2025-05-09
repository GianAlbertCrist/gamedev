package com.budgetapp.thrifty;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
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
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ThemeSync.syncNotificationBarColor(getWindow(), this);
        themeSync();

        replaceFragment(new HomeFragment());
        binding.bottomNav.setBackground(null);

        binding.fabAddEntry.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEntryActivity.class);
            startActivity(intent);
        });

        binding.bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
                } else {
                    return false;
                }
            }
        });

        String navigateTo = getIntent().getStringExtra("navigate_to");
        if (navigateTo != null) {
            if (navigateTo.equals("profile")) {
                navigateToProfileFragment();
            }
        }
    }

    private void navigateToProfileFragment() {
        binding.bottomNav.setSelectedItemId(R.id.ic_profile);
        replaceFragment(new ProfileFragment());
    }

    public void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
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
}
