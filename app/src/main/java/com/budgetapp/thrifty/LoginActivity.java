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
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.utils.AppLogger;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.budgetapp.thrifty.utils.NetworkUtils;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "LoginActivity";

    private TextInputLayout emailLayout, passwordLayout;
    private EditText emailInput, passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ThemeSync.syncNotificationBarColor(getWindow(), this);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailLayout = findViewById(R.id.enter_email);
        passwordLayout = findViewById(R.id.enter_password);
        emailInput = emailLayout.getEditText();
        passwordInput = passwordLayout.getEditText();
        Button loginButton = binding.loginButton;

        loginButton.setOnClickListener(v -> {

            if (!NetworkUtils.isOnline(this)) {
                Toast.makeText(LoginActivity.this, "Cannot log in, you are offline", Toast.LENGTH_SHORT).show();
                return;
            }

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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
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
        Toast.makeText(LoginActivity.this, "Signing in...", Toast.LENGTH_SHORT).show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserDataToFirestore(user);
                        updateUI(user);
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed: Invalid password or email.",
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            AppLogger.log(this, TAG, "Checking user role...");

            db.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            AppLogger.log(this, TAG, "User role: " + role);

                            if (role != null && role.equalsIgnoreCase("admin")) {

                                Log.d(TAG, "Admin detected. Redirecting to AdminActivity...");
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                finish();
                            } else {

                                AppLogger.log(this, TAG, "Regular user detected. Going to MainActivity...");

                                saveUserDataToSharedPreferences(documentSnapshot);

                                FirestoreManager.loadTransactions(transactions -> {
                                    TransactionsHandler.transactions.clear();
                                    TransactionsHandler.transactions.addAll(transactions);
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                });
                            }
                        } else {
                            AppLogger.log(this, TAG, "User document not found. Treating as regular user.");
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        AppLogger.logError(this, TAG, "Failed to load user profile", e);
                        Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveUserDataToSharedPreferences(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot != null && documentSnapshot.exists()) {
            String username = documentSnapshot.getString("username");
            String fullname = documentSnapshot.getString("fullname");
            Long avatarIdLong = documentSnapshot.getLong("avatarId");
            int avatarId = avatarIdLong != null ? avatarIdLong.intValue() : 0;

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (username != null) editor.putString("username", username);
            if (fullname != null) editor.putString("fullname", fullname);
            editor.putInt("avatarId", avatarId);
            editor.apply();
        }
    }

    private void saveUserDataToFirestore(FirebaseUser user) {
        if (user == null) return;

        String displayName = user.getDisplayName();
        String email = user.getEmail();

        int avatarId = 0;

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (prefs.contains("avatarId")) {
            avatarId = prefs.getInt("avatarId", 0);
        }

        FirestoreManager.saveUserProfile(displayName, email, avatarId);
    }
}
