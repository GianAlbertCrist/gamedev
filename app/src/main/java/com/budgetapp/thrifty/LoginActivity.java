package com.budgetapp.thrifty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.budgetapp.thrifty.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize views
        EditText enterMail = binding.enterMail;
        EditText enterPassword = binding.enterPassw;
        ImageButton loginButton = binding.loginButton;
        TextView registerText = binding.registerNo;

        loginButton.setOnClickListener(view -> {
            String email = enterMail.getText().toString().trim();
            String password = enterPassword.getText().toString().trim();

            if (validateInput(email, password)) {
                authenticateUser(email, password);
            }
        });

        registerText.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            binding.enterMail.setError("Email cannot be empty");
            return false;
        }

        if (password.isEmpty()) {
            binding.enterPassw.setError("Password cannot be empty");
            return false;
        }

        return true;
    }

    private void authenticateUser(String email, String password) {
        // Replace with your actual authentication logic
        if (email.equals("test@example.com") && password.equals("password123")) {
            // Save login state
            SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            preferences.edit()
                    .putBoolean("isLoggedIn", true)
                    .putString("userEmail", email)
                    .apply();

            // Proceed to main activity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(LoginActivity.this,
                    "Invalid email or password",
                    Toast.LENGTH_SHORT).show();
        }
    }
}