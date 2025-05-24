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
import com.budgetapp.thrifty.utils.AppLogger;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.budgetapp.thrifty.utils.NetworkUtils;
import com.budgetapp.thrifty.utils.OfflineAccountManager;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        ThemeSync.syncNotificationBarColor(getWindow(), this);

        mAuth = FirebaseAuth.getInstance();

        TextInputLayout firstNameLayout = findViewById(R.id.first_name_container);
        TextInputLayout surnameLayout = findViewById(R.id.surname_container);
        TextInputLayout emailLayout = findViewById(R.id.email_container);
        TextInputLayout passwordLayout = findViewById(R.id.password_container);
        TextInputLayout confirmPasswordLayout = findViewById(R.id.confirm_password_container);

        EditText firstNameInput = firstNameLayout.getEditText();
        EditText surnameInput = surnameLayout.getEditText();
        EditText emailInput = emailLayout.getEditText();
        EditText passwordInput = passwordLayout.getEditText();
        EditText confirmPasswordInput = confirmPasswordLayout.getEditText();

        Button registerButton = findViewById(R.id.register_button);
        TextView loginRedirect = findViewById(R.id.login_redirect);


        registerButton.setOnClickListener(v -> {
            // Retrieve input values
            String firstName = Objects.requireNonNull(firstNameInput).getText().toString().trim();
            String surname = Objects.requireNonNull(surnameInput).getText().toString().trim();
            String email = Objects.requireNonNull(emailInput).getText().toString().trim();
            String password = Objects.requireNonNull(passwordInput).getText().toString();
            String confirmPassword = Objects.requireNonNull(confirmPasswordInput).getText().toString();


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

            if (!NetworkUtils.isOnline(RegistrationActivity.this)) {
                OfflineAccountManager.savePendingAccount(RegistrationActivity.this, email, password);
                Toast.makeText(RegistrationActivity.this, "Account will be created when you're back online.", Toast.LENGTH_SHORT).show();
                return;
            }
            AppLogger.log(this, TAG,"Validation Complete.");
            createAccount(firstName, surname, email, password);
        });

        String text = "Have an account already? Log in";
        SpannableString spannableString = new SpannableString(text);

        int startIndex = text.indexOf("Log in");
        int endIndex = startIndex + "Log in".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
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

        loginRedirect.setText(spannableString);
        loginRedirect.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void createAccount(String firstName, String surname, String email, String password) {
        Toast.makeText(RegistrationActivity.this, "Creating account...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String fullName = firstName + " " + surname;
                            String displayNameWithFullName = firstName + "|" + fullName;

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayNameWithFullName)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            saveUserToFirestore(user, firstName, fullName, 0);
                                            FirestoreManager.saveUserProfile(displayNameWithFullName, email, 0);
                                            mAuth.signOut();
                                            Toast.makeText(RegistrationActivity.this,
                                                    "Registration successful. Please log in.",
                                                    Toast.LENGTH_SHORT).show();
                                            AppLogger.log(this, TAG, "Registration successful.");
                                            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                                            finish();
                                        }
                                    });
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(RegistrationActivity.this,
                                    "An account may already exist with these credentials.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegistrationActivity.this,
                                    "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String username, String fullName, int avatarId) {
        if (user == null) return;

        String email = user.getEmail();

        // Save to Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("fullname", fullName);
        userData.put("email", email);
        userData.put("avatarId", avatarId);
        userData.put("role", "user");

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("Registration", "User profile saved to Firestore");

                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("username", username)
                            .putString("fullname", fullName)
                            .putInt("avatarId", avatarId)
                            .apply();
                })
                .addOnFailureListener(e -> Log.e("Registration", "Error saving user to Firestore", e));
    }
}
