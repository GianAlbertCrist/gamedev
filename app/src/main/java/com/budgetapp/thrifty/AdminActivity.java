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
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {
    private UserAdapter adapter;
    private TextView currentPageText;
    private ProgressBar loadingSpinner;
    private TextView totalUsers;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity);

        ThemeSync.syncNotificationBarColor(getWindow(), this);

        findViewById(R.id.fab_add_entry).setOnClickListener(v -> {showRegisterUserDialog();});

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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updatePageNumberDisplay();
            }
            @Override public void afterTextChanged(Editable s) {}
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
            fetchUsersFromServer(userList);
        } catch (Exception e) {
            Log.e("AdminActivity", "Error fetching users: " + e.getMessage(), e);
            runOnUiThread(() ->
                    Toast.makeText(this, "Failed to fetch users: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
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
                (int)(getResources().getDisplayMetrics().widthPixels * 0.85),  // 85% of screen width
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void updatePageNumberDisplay() {
        currentPageText.setText(String.valueOf(adapter.getCurrentPage() + 1));
    }

    @SuppressLint("SetTextI18n")
    private void fetchUsersFromServer(List<User> userList) {
        runOnUiThread(() -> loadingSpinner.setVisibility(View.VISIBLE));

        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.1.35:3000/api/users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();

                if (responseCode != 200) {
                    throw new Exception("Unexpected HTTP code: " + responseCode);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONArray array = new JSONArray(result.toString());
                List<User> filteredList = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String role = obj.optString("role", "");
                    if (!"admin".equalsIgnoreCase(role)) {
                        User user = new User(
                                obj.getString("uid"),
                                obj.getString("email"),
                                obj.optString("displayName", ""),
                                obj.optBoolean("disabled", false)
                        );
                        filteredList.add(user);
                    }
                }

                String label = filteredList.size() == 1 ? "user" : "users";

                runOnUiThread(() -> {
                    adapter.updateData(filteredList);
                    updatePageNumberDisplay();
                    loadingSpinner.setVisibility(View.GONE);
                    totalUsers.setText("Total " + label + ": " + filteredList.size());
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showDeleteDialog(User user) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.delete_confirmation, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_delete_yes).setOnClickListener(v -> {
            dialog.dismiss();
            deleteUserFromServer(user.getUid());
        });

        dialogView.findViewById(R.id.btn_delete_no).setOnClickListener(v -> dialog.dismiss());

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private void deleteUserFromServer(String uid) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.1.35:3000/api/user/" + uid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                        adapter.removeUserById(uid);

                        // ✅ Adjust pagination if needed
                        if (adapter.getCurrentPage() >= adapter.getTotalPages()) {
                            adapter.setPage(Math.max(0, adapter.getTotalPages() - 1));
                        }

                        // ✅ Update UI
                        updatePageNumberDisplay();
                        totalUsers.setText("Total users: " + adapter.getTotalUserCount());
                    });

                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
            firstNameInput.setText(parts[0]);
            surnameInput.setText(parts[1].replace(parts[0] + " ", ""));
        } else {
            firstNameInput.setText(user.getDisplayName());
        }
        emailInput.setText(user.getEmail());

        registerBtn.setText("Update");

        registerBtn.setOnClickListener(v -> {
            String firstName = firstNameInput.getText().toString().trim();
            String surname = surnameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
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

            new Thread(() -> {
                try {
                    URL url = new URL("http://192.168.1.35:3000/api/user/" + user.getUid());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject payload = new JSONObject();
                    payload.put("displayName", displayName);
                    if (!email.equals(user.getEmail())) {
                        payload.put("email", email);
                    }
                    if (!password.isEmpty()) {
                        payload.put("password", password);
                    }

                    conn.getOutputStream().write(payload.toString().getBytes());

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            fetchUsersFromServer(new ArrayList<>()); // refresh UI
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Failed to update user", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private void showRegisterUserDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.register_user_dialog, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.74),
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
            String firstName = firstNameInput.getText().toString().trim();
            String surname = surnameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
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

            // ✅ Register the user via Firebase Auth
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = task.getResult().getUser();
                            if (firebaseUser != null) {
                                String fullName = firstName + " " + surname;
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(firstName + "|" + fullName)
                                        .build();
                                firebaseUser.updateProfile(profileUpdates)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                                fetchUsersFromServer(new ArrayList<>());
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

}