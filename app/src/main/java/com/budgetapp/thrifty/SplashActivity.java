package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.budgetapp.thrifty.utils.AppLogger;
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
            // Show loading while checking role
            loadingSpinner.setVisibility(View.VISIBLE);
            loadingText.setVisibility(View.VISIBLE);
            loadingText.setText("Checking account...");

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && "admin".equalsIgnoreCase(documentSnapshot.getString("role"))) {
                            startActivity(new Intent(this, AdminActivity.class));
                        } else {
                            startActivity(new Intent(this, MainActivity.class));
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        AppLogger.logError(this, TAG, "Failed to check root role, falling back to profile/info", e);

                        db.collection("users")
                                .document(currentUser.getUid())
                                .collection("profile")
                                .document("info")
                                .get()
                                .addOnSuccessListener(profileDoc -> {
                                    if (profileDoc.exists() && "admin".equalsIgnoreCase(profileDoc.getString("role"))) {
                                        startActivity(new Intent(this, AdminActivity.class));
                                    } else {
                                        startActivity(new Intent(this, MainActivity.class));
                                    }
                                    finish();
                                })
                                .addOnFailureListener(error -> {
                                    AppLogger.logError(this, TAG, "Failed to check fallback profile role", error);
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                });
                    });
        } else {
            startActivity(new Intent(this, FirstActivity.class));
            finish();
        }
    }
}
