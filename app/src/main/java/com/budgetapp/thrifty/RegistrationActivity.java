package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get references to input containers
        ConstraintLayout firstNameContainer = findViewById(R.id.first_name_container);
        ConstraintLayout surnameContainer = findViewById(R.id.surname_container);
        ConstraintLayout emailContainer = findViewById(R.id.email_container);
        ConstraintLayout passwordContainer = findViewById(R.id.password_container);
        ConstraintLayout confirmPasswordContainer = findViewById(R.id.confirm_password_container);

        // Find TextInputLayout inside each container
        TextInputLayout firstNameLayout = (TextInputLayout) firstNameContainer.getChildAt(0);
        TextInputLayout surnameLayout = (TextInputLayout) surnameContainer.getChildAt(0);
        TextInputLayout emailLayout = (TextInputLayout) emailContainer.getChildAt(0);
        TextInputLayout passwordLayout = (TextInputLayout) passwordContainer.getChildAt(0);
        TextInputLayout confirmPasswordLayout = (TextInputLayout) confirmPasswordContainer.getChildAt(0);

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

                // Create user with Firebase
                createAccount(firstName, surname, email, password);
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

    private void createAccount(String firstName, String surname, String email, String password) {
        // Show loading indicator if you have one

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Hide loading indicator if you have one

                    if (task.isSuccessful()) {
                        // Sign in success, update user profile
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Update user profile with display name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(firstName + " " + surname)
                                .build();

                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        Log.d(TAG, "User profile updated.");
                                    }

                                    // Sign out the user since we want them to log in explicitly
                                    mAuth.signOut();

                                    // Show success message
                                    Toast.makeText(RegistrationActivity.this,
                                            "Registration successful. Please log in.",
                                            Toast.LENGTH_SHORT).show();

                                    // Navigate to LoginActivity
                                    Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                });
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegistrationActivity.this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
