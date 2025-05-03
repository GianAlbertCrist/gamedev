package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

public class AddEntryActivity extends AppCompatActivity {
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize TabLayout
        tabLayout = findViewById(R.id.tabLayout);

        // Small Thrifty (Home) button
        ImageButton smallThrifty = findViewById(R.id.small_thrifty);
        smallThrifty.setOnClickListener(view -> {
            // This will close the current activity and return to MainActivity
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

        //tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int color = tab.getPosition() == 0 ?
                        R.color.primary_color : R.color.red;
                tabLayout.setSelectedTabIndicatorColor(getColor(color));
                tabLayout.setTabTextColors(
                        getColor(tab.getPosition() == 0 ?
                                R.color.red : R.color.primary_color),
                        getColor(color)
                );
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
}