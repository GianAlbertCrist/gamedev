package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirstActivity extends AppCompatActivity {

    private Button registerButton, signInButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "FirstActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in before setting up UI
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "User already logged in, checking admin status");
            // User already logged in, go directly to MainActivity/AdminActivity
            checkAdminStatusAndRedirect(currentUser);
            return; // Skip UI setup
        }

        // Only set up UI if not already logged in
        setContentView(R.layout.activity_first);
        ThemeSync.syncNotificationBarColor(getWindow(), this);

        // Find views using correct type (Button)
        signInButton = findViewById(R.id.sign_in_button);
        registerButton = findViewById(R.id.register_button);

        signInButton.setOnClickListener(v -> {
            Intent intent = new Intent(FirstActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(FirstActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // onStart no longer needs to check authentication status
        // as we do it in onCreate before setting up the UI
    }

    private void checkAdminStatusAndRedirect(FirebaseUser currentUser) {
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
                    // If we can't check the role, fall back to checking the root document
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
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                                Toast.makeText(this, "Failed to check role.", Toast.LENGTH_SHORT).show();
                            });
                });
    }
}
