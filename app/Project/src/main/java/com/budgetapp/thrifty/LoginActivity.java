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
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Save login state
            SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            preferences.edit()
                    .putBoolean("isLoggedIn", true)
                    .putString("userEmail", user.getEmail())
                    .putString("userName", user.getDisplayName() != null ?
                            user.getDisplayName() : user.getEmail())
                    .apply();

            // Proceed to main activity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
}