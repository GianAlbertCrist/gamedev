package com.budgetapp.thrifty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.budgetapp.thrifty.account.Account;
import com.budgetapp.thrifty.handlers.AccountsHandler;

import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Get references to input fields
        TextInputLayout firstNameLayout = findViewById(R.id.first_name_container);
        TextInputLayout surnameLayout = findViewById(R.id.surname_container);
        TextInputLayout emailLayout = findViewById(R.id.email_container);
        TextInputLayout passwordLayout = findViewById(R.id.password_container);
        TextInputLayout confirmPasswordLayout = findViewById(R.id.confirm_password_container);

        // Get the TextInputEditText from each layout
        TextInputEditText firstNameInput = (TextInputEditText) firstNameLayout.getEditText();
        TextInputEditText surnameInput = (TextInputEditText) surnameLayout.getEditText();
        TextInputEditText emailInput = (TextInputEditText) emailLayout.getEditText();
        TextInputEditText passwordInput = (TextInputEditText) passwordLayout.getEditText();
        TextInputEditText confirmPasswordInput = (TextInputEditText) confirmPasswordLayout.getEditText();

        ImageButton registerButton = findViewById(R.id.register_button);
        TextView loginRedirect = findViewById(R.id.login_redirect);

        // Set click listener for the register button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve input values
                String firstName = Objects.requireNonNull(firstNameInput).getText().toString().trim();
                String surname = Objects.requireNonNull(surnameInput).getText().toString().trim();
                String email = Objects.requireNonNull(emailInput).getText().toString().trim();
                String password = Objects.requireNonNull(passwordInput).getText().toString();
                String confirmPassword = Objects.requireNonNull(confirmPasswordInput).getText().toString();

                // Validate inputs
                if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(surname) ||
                        TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ||
                        TextUtils.isEmpty(confirmPassword)) {
                    Toast.makeText(RegistrationActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(RegistrationActivity.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegistrationActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(RegistrationActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if email already exists
                if (AccountsHandler.emailExists(email)) {
                    Toast.makeText(RegistrationActivity.this, "Email already registered", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create and save the new account
                Account newAccount = new Account(firstName, surname, email, password);
                AccountsHandler.addAccount(newAccount);

                // Save login state
                SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                preferences.edit()
                        .putBoolean("isLoggedIn", true)
                        .putString("userEmail", email)
                        .putString("userName", firstName)
                        .apply();

                // Show success message
                Toast.makeText(RegistrationActivity.this, "Registration successful. Please log in.", Toast.LENGTH_SHORT).show();

                // Navigate to LoginActivity
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Create a SpannableString for the login redirect
        String text = "Have an account already? Log in";
        SpannableString spannableString = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        };

        int startIndex = text.indexOf("Log in");
        int endIndex = startIndex + "Log in".length();
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        loginRedirect.setText(spannableString);
        loginRedirect.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
