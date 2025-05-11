package com.budgetapp.thrifty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.budgetapp.thrifty.databinding.ActivityLoginBinding;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private static final String TAG = "LoginActivity";

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private EditText emailInput;
    private EditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ThemeSync.syncNotificationBarColor(getWindow(), this);
        mAuth = FirebaseAuth.getInstance();

        // Initialize input fields
        emailLayout = findViewById(R.id.enter_email);
        passwordLayout = findViewById(R.id.enter_password);
        emailInput = emailLayout.getEditText();
        passwordInput = passwordLayout.getEditText();
        Button loginButton = binding.loginButton;

        loginButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(emailInput).getText().toString().trim();
            String password = Objects.requireNonNull(passwordInput).getText().toString();

            if (validateInputs(email, password)) {
                signInWithEmailAndPassword(email, password);
            }
        });

        TextView forgotPass = findViewById(R.id.forgot_pass);
        forgotPass.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        setupRegisterRedirect();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, go to MainActivity
            loadUserDataAndProceed(currentUser);
        }
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

        // Reset errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            isValid = false;

        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email address");
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        return isValid;
    }

    private void setupRegisterRedirect() {
        TextView registerRedirect = findViewById(R.id.register_redirect);
        String text = "Don't have an account? Register";
        SpannableString spannableString = new SpannableString(text);

        int startIndex = text.indexOf("Register");
        int endIndex = startIndex + "Register".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
                finish();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };

        ForegroundColorSpan colorSpan = new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_color));
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(colorSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        registerRedirect.setText(spannableString);
        registerRedirect.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void signInWithEmailAndPassword(String email, String password) {
        // Show loading indicator
        Toast.makeText(LoginActivity.this, "Signing in...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            loadUserDataAndProceed(user);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserDataAndProceed(FirebaseUser user) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        String uid = user.getUid();

        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get user data from Firebase
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String fullname = dataSnapshot.child("fullname").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    Long avatarIdLong = dataSnapshot.child("avatarId").getValue(Long.class);
                    int avatarId = avatarIdLong != null ? avatarIdLong.intValue() : 0;

                    // Save to SharedPreferences
                    SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("isLoggedIn", true);

                    if (username != null && !username.isEmpty()) {
                        editor.putString("username", username);
                    } else if (user.getDisplayName() != null) {
                        editor.putString("username", user.getDisplayName());
                    } else {
                        editor.putString("username", "User");
                    }

                    if (fullname != null && !fullname.isEmpty()) {
                        editor.putString("fullname", fullname);
                    }

                    if (email != null && !email.isEmpty()) {
                        editor.putString("email", email);
                    } else {
                        editor.putString("email", user.getEmail());
                    }

                    editor.putInt("avatarId", avatarId);
                    editor.apply();

                    // Proceed to main activity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    // User exists in Auth but not in Database, create a new entry
                    createUserInDatabase(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadUserData:onCancelled", databaseError.toException());
                // Fallback to basic user data
                saveBasicUserDataAndProceed(user);
            }
        });
    }

    private void createUserInDatabase(FirebaseUser user) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        String uid = user.getUid();

        // Extract username from email if display name is not available
        String username = user.getDisplayName();
        if (username == null || username.isEmpty()) {
            String email = user.getEmail();
            username = email != null ? email.substring(0, email.indexOf('@')) : "User";
        }

        // Create user data
        String finalUsername = username;
        mDatabase.child("users").child(uid).setValue(
                        new UserData(finalUsername, finalUsername, user.getEmail(), 0))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save to SharedPreferences
                        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putString("username", finalUsername);
                        editor.putString("fullname", finalUsername);
                        editor.putString("email", user.getEmail());
                        editor.putInt("avatarId", 0);
                        editor.apply();

                        // Proceed to main activity
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Log.w(TAG, "createUserInDatabase:failure", task.getException());
                        saveBasicUserDataAndProceed(user);
                    }
                });
    }

    private void saveBasicUserDataAndProceed(FirebaseUser user) {
        // Extract username from email if display name is not available
        String username = user.getDisplayName();
        if (username == null || username.isEmpty()) {
            String email = user.getEmail();
            username = email != null ? email.substring(0, email.indexOf('@')) : "User";
        }

        // Save to SharedPreferences
        SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
        editor.putString("fullname", username);
        editor.putString("email", user.getEmail());
        editor.putInt("avatarId", 0);
        editor.apply();

        // Proceed to main activity
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    // Simple data class for user information
    private static class UserData {
        public String username;
        public String fullname;
        public String email;
        public int avatarId;

        public UserData() {
            // Default constructor required for Firebase
        }

        public UserData(String username, String fullname, String email, int avatarId) {
            this.username = username;
            this.fullname = fullname;
            this.email = email;
            this.avatarId = avatarId;
        }
    }
}
