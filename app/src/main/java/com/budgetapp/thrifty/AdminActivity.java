package com.budgetapp.thrifty;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.budgetapp.thrifty.renderers.UserAdapter;
import com.budgetapp.thrifty.model.User;
import com.budgetapp.thrifty.utils.FirestoreManager;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AdminActivity extends AppCompatActivity {
    private UserAdapter adapter;
    private TextView currentPageText;
    private ProgressBar loadingSpinner;
    private TextView totalUsers;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity);

        mFirestore = FirebaseFirestore.getInstance();
        ThemeSync.syncNotificationBarColor(getWindow(), this);

        findViewById(R.id.fab_add_entry).setOnClickListener(v -> {
            showRegisterUserDialog();
        });

        RecyclerView recyclerView = findViewById(R.id.accounts_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadingSpinner = findViewById(R.id.loading_spinner);
        totalUsers = findViewById(R.id.total_users);

        currentPageText = findViewById(R.id.current_page);
        ImageButton nextBtn = findViewById(R.id.next_btn);
        ImageButton prevBtn = findViewById(R.id.previous_btn);

        List<User> userList = new ArrayList<>();
        adapter = new UserAdapter(this, userList, new UserAdapter.OnUserActionListener() {

            @Override
            public void onEdit(User user) {
                showEditUserDialog(user);
            }

            @Override
            public void onDelete(User user) {
                showDeleteDialog(user);
            }
        });

        recyclerView.setAdapter(adapter);

        // Search Bar Filtering
        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updatePageNumberDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Pagination buttons
        nextBtn.setOnClickListener(v -> {
            int nextPage = adapter.getCurrentPage() + 1;
            if (nextPage < adapter.getTotalPages()) {
                adapter.setPage(nextPage);
                updatePageNumberDisplay();
            }
        });

        prevBtn.setOnClickListener(v -> {
            int prevPage = adapter.getCurrentPage() - 1;
            if (prevPage >= 0) {
                adapter.setPage(prevPage);
                updatePageNumberDisplay();
            }
        });

        // Logout
        findViewById(R.id.logout_admin).setOnClickListener(v -> showLogoutDialog());

        try {
            fetchUsersFromFirestore();
        } catch (Exception e) {
            Log.e("AdminActivity", "Error fetching users: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to fetch users: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showLogoutDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.logout_confirmation, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_yes).setOnClickListener(v -> {
            dialog.dismiss();
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(AdminActivity.this, LoginActivity.class));
            finish();
        });

        dialogView.findViewById(R.id.btn_no).setOnClickListener(v -> dialog.dismiss());

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        // ✅ Set dialog width manually
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.85),  // 85% of screen width
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void updatePageNumberDisplay() {
        currentPageText.setText(String.valueOf(adapter.getCurrentPage() + 1));
    }

    @SuppressLint("SetTextI18n")
    private void fetchUsersFromFirestore() {
        loadingSpinner.setVisibility(View.VISIBLE);

        mFirestore.collection("users")
                .whereNotEqualTo("role", "admin")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> filteredList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String uid = document.getId();
                        String email = document.getString("email");
                        String displayName = document.getString("username");
                        if (document.getString("fullname") != null) {
                            displayName = displayName + "|" + document.getString("fullname");
                        }
                        boolean disabled = document.getBoolean("disabled") != null && document.getBoolean("disabled");

                        User user = new User(uid, email, displayName, disabled);
                        filteredList.add(user);
                    }

                    String label = filteredList.size() == 1 ? "user" : "users";
                    adapter.updateData(filteredList);
                    updatePageNumberDisplay();
                    loadingSpinner.setVisibility(View.GONE);
                    totalUsers.setText("Total " + label + ": " + filteredList.size());
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("AdminActivity", "Error fetching users", e);
                });
    }

    private void showDeleteDialog(User user) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.delete_confirmation, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_delete_yes).setOnClickListener(v -> {
            dialog.dismiss();
            deleteUserFromFirestore(user.getUid());
        });

        dialogView.findViewById(R.id.btn_delete_no).setOnClickListener(v -> dialog.dismiss());

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    @SuppressLint("SetTextI18n")
    private void deleteUserFromFirestore(String uid) {
        // Delete user document from Firestore
        mFirestore.collection("users").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Also delete user from Firebase Auth (requires Admin SDK on server)
                    // For now, just update the UI
                    Toast.makeText(this, "User deleted from database", Toast.LENGTH_SHORT).show();
                    adapter.removeUserById(uid);

                    if (adapter.getCurrentPage() >= adapter.getTotalPages()) {
                        adapter.setPage(Math.max(0, adapter.getTotalPages() - 1));
                    }

                    updatePageNumberDisplay();
                    totalUsers.setText("Total users: " + adapter.getTotalUserCount());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("AdminActivity", "Error deleting user", e);
                });
    }

    private void showEditUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.register_user_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.74),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        TextInputLayout firstNameLayout = dialogView.findViewById(R.id.first_name_container);
        TextInputLayout surnameLayout = dialogView.findViewById(R.id.surname_container);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.email_container);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.password_container);
        TextInputLayout confirmPasswordLayout = dialogView.findViewById(R.id.confirm_password_container);
        Button registerBtn = dialogView.findViewById(R.id.register_button);

        EditText firstNameInput = firstNameLayout.getEditText();
        EditText surnameInput = surnameLayout.getEditText();
        EditText emailInput = emailLayout.getEditText();
        EditText passwordInput = passwordLayout.getEditText();
        EditText confirmPasswordInput = confirmPasswordLayout.getEditText();

        // Pre-fill current user info
        String[] parts = user.getDisplayName().split("\\|");
        if (parts.length == 2) {
            assert firstNameInput != null;
            firstNameInput.setText(parts[0]);
            assert surnameInput != null;
            surnameInput.setText(parts[1].replace(parts[0] + " ", ""));
        } else {
            assert firstNameInput != null;
            firstNameInput.setText(user.getDisplayName());
        }
        assert emailInput != null;
        emailInput.setText(user.getEmail());

        registerBtn.setText("Update");

        registerBtn.setOnClickListener(v -> {
            String firstName = firstNameInput.getText().toString().trim();
            assert surnameInput != null;
            String surname = surnameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            assert passwordInput != null;
            String password = passwordInput.getText().toString();
            assert confirmPasswordInput != null;
            String confirmPassword = confirmPasswordInput.getText().toString();
            String displayName = firstName + "|" + firstName + " " + surname;

            if (firstName.isEmpty() || surname.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "First name, surname, and email are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.isEmpty()) {
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Update user in Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("username", firstName);
            updates.put("fullname", firstName + " " + surname);
            updates.put("email", email);

            mFirestore.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Update Firebase Auth profile
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null && firebaseUser.getUid().equals(user.getUid())) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates);

                            if (!email.equals(user.getEmail())) {
                                firebaseUser.updateEmail(email);
                            }

                            if (!password.isEmpty()) {
                                firebaseUser.updatePassword(password);
                            }
                        }

                        Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchUsersFromFirestore();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("AdminActivity", "Error updating user", e);
                    });
        });
    }

    private void showRegisterUserDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.register_user_dialog, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.74),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        TextInputLayout firstNameLayout = dialogView.findViewById(R.id.first_name_container);
        TextInputLayout surnameLayout = dialogView.findViewById(R.id.surname_container);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.email_container);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.password_container);
        TextInputLayout confirmPasswordLayout = dialogView.findViewById(R.id.confirm_password_container);

        EditText firstNameInput = firstNameLayout.getEditText();
        EditText surnameInput = surnameLayout.getEditText();
        EditText emailInput = emailLayout.getEditText();
        EditText passwordInput = passwordLayout.getEditText();
        EditText confirmPasswordInput = confirmPasswordLayout.getEditText();

        Button registerBtn = dialogView.findViewById(R.id.register_button);

        registerBtn.setOnClickListener(v -> {
            assert firstNameInput != null;
            String firstName = firstNameInput.getText().toString().trim();
            assert surnameInput != null;
            String surname = surnameInput.getText().toString().trim();
            assert emailInput != null;
            String email = emailInput.getText().toString().trim();
            assert passwordInput != null;
            String password = passwordInput.getText().toString();
            assert confirmPasswordInput != null;
            String confirmPassword = confirmPasswordInput.getText().toString();

            if (firstName.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = task.getResult().getUser();
                            if (firebaseUser != null) {
                                String fullName = firstName + " " + surname;
                                String displayName = firstName + "|" + fullName;

                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build();

                                firebaseUser.updateProfile(profileUpdates)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                // Save user profile to Firestore
                                                Map<String, Object> userData = new HashMap<>();
                                                userData.put("username", firstName);
                                                userData.put("fullname", fullName);
                                                userData.put("email", email);
                                                userData.put("avatarId", 0);

                                                mFirestore.collection("users").document(firebaseUser.getUid())
                                                        .set(userData)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                                            dialog.dismiss();
                                                            fetchUsersFromFirestore();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(this, "Profile creation failed", Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                Toast.makeText(this, "Profile update failed", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(this, "Registration failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
