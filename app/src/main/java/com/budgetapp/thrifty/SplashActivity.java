package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DISPLAY_TIME = 800;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar loadingSpinner;
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ThemeSync.syncNotificationBarColor(getWindow(), this);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        loadingSpinner = findViewById(R.id.loading_spinner);
        loadingText = findViewById(R.id.loading_text);

        // Initially hide loading indicators
        loadingSpinner.setVisibility(View.INVISIBLE);
        loadingText.setVisibility(View.INVISIBLE);

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is logged in, show splash for a moment then check auth
            new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthStatus, SPLASH_DISPLAY_TIME);
        } else {
            // User is not logged in, go to FirstActivity immediately after a very brief delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, FirstActivity.class));
                finish();
            }, 500);
        }
    }

    private void checkAuthStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is already logged in, show loading indicators
            loadingSpinner.setVisibility(View.VISIBLE);
            loadingText.setVisibility(View.VISIBLE);
            loadingText.setText("Checking account...");

            // Check for admin role in profile/info document
            db.collection("users")
                    .document(currentUser.getUid())
                    .collection("profile")
                    .document("info")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && "admin".equalsIgnoreCase(documentSnapshot.getString("role"))) {
                            // Admin user
                            startActivity(new Intent(this, AdminActivity.class));
                        } else {
                            // Regular user
                            startActivity(new Intent(this, MainActivity.class));
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        db.collection("users").document(currentUser.getUid())
                                .get()
                                .addOnSuccessListener(rootDoc -> {
                                    if (rootDoc.exists() && "admin".equalsIgnoreCase(rootDoc.getString("role"))) {
                                        // Admin user
                                        startActivity(new Intent(this, AdminActivity.class));
                                    } else {
                                        // Regular user
                                        startActivity(new Intent(this, MainActivity.class));
                                    }
                                    finish();
                                })
                                .addOnFailureListener(error -> {
                                    // If all checks fail, just go to MainActivity as regular user
                                    Log.e(TAG, "Failed to check role: ", error);
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                });
                    });
        } else {
            // No user is logged in, go to FirstActivity immediately
            startActivity(new Intent(this, FirstActivity.class));
            finish();
        }
    }
}
