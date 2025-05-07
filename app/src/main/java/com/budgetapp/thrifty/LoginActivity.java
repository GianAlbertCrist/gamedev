package com.budgetapp.thrifty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.budgetapp.thrifty.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EditText enterMail = binding.enterMail;
        EditText enterPassword = binding.enterPassw;
        ImageButton loginButton = binding.loginButton;

        loginButton.setOnClickListener(view -> {
            String email = enterMail.getText().toString().trim();
            String password = enterPassword.getText().toString().trim();

            if (email.equals("test@example.com") && password.equals("password123")) {
                SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                preferences.edit()
                        .putBoolean("isLoggedIn", true)
                        .apply();

                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}