package com.budgetapp.thrifty.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.budgetapp.thrifty.FirstActivity;
import com.budgetapp.thrifty.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Objects;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private TextView usernameText;
    private TextView fullNameText;
    private ImageView profileImage;
    private int currentAvatarId = 0; // Default avatar
    private ListenerRegistration userListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Initialize buttons
        LinearLayout editProfileButton = view.findViewById(R.id.edit_profile_button);
        LinearLayout securityButton = view.findViewById(R.id.security_button);
        LinearLayout logoutButton = view.findViewById(R.id.logout_button);
        LinearLayout deleteAccountButton = view.findViewById(R.id.delete_account_button);

        // Initialize text views
        usernameText = view.findViewById(R.id.textView);
        fullNameText = view.findViewById(R.id.textView2);
        profileImage = view.findViewById(R.id.user_avatar);

        // Load saved profile data
        loadProfileData();

        // Set up real-time listener for user data changes
        setupUserListener();

        // Set up fragment result listener for profile updates
        getParentFragmentManager().setFragmentResultListener("profileUpdate", this,
                (requestKey, result) -> {
                    if (result.containsKey("username")) {
                        String username = result.getString("username");
                        usernameText.setText(username);
                    }

                    if (result.containsKey("fullname")) {
                        String fullname = result.getString("fullname");
                        fullNameText.setText(fullname.toUpperCase());
                    }

                    if (result.containsKey("avatarId")) {
                        int avatarId = result.getInt("avatarId");
                        updateProfileImage(avatarId);
                        currentAvatarId = avatarId;

                        // Save to SharedPreferences
                        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                                requireActivity().MODE_PRIVATE);
                        prefs.edit().putInt("avatarId", avatarId).apply();
                    }
                });

        // Set click listeners
        editProfileButton.setOnClickListener(v -> navigateToEditProfile());
        securityButton.setOnClickListener(v -> navigateToSecurity());
        logoutButton.setOnClickListener(v -> showCustomLogoutDialog());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the database listener when the fragment is destroyed
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void setupUserListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            DocumentReference userRef = mFirestore.collection("users").document(uid);
            userListener = userRef.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.w(TAG, "loadUserData:onCancelled", error);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    String username = snapshot.getString("username");
                    String fullname = snapshot.getString("fullname");
                    Long avatarIdLong = snapshot.getLong("avatarId");
                    int avatarId = avatarIdLong != null ? avatarIdLong.intValue() : 0;

                    if (username != null && !username.isEmpty()) {
                        usernameText.setText(username);
                        saveProfileData("username", username);
                    }

                    if (fullname != null && !fullname.isEmpty()) {
                        fullNameText.setText(fullname.toUpperCase());
                        saveProfileData("fullname", fullname);
                    }

                    if (avatarId > 0) {
                        updateProfileImage(avatarId);
                        currentAvatarId = avatarId;
                        saveProfileData("avatarId", String.valueOf(avatarId));
                    }
                }
            });
        }
    }

    private void loadProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            // Get the display name
            String displayName = user.getDisplayName();

            // Safely split the display name and extract values
            String username = "";
            String fullName = "";

            if (displayName.contains("|")) {
                String[] userData = displayName.split("\\|");
                if (userData.length >= 2) {
                    username = userData[0];
                    fullName = userData[1];
                } else {
                    // Fallback if format is incorrect
                    username = displayName;
                    fullName = displayName;
                }
            } else {
                // Fallback if no separator found
                username = displayName;
                fullName = displayName;
            }

            usernameText.setText(username);
            fullNameText.setText(fullName.toUpperCase());

            // Check SharedPreferences first for the most up-to-date avatar
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                    requireActivity().MODE_PRIVATE);
            int savedAvatarId = prefs.getInt("avatarId", 0);

            if (savedAvatarId > 0) {
                updateProfileImage(savedAvatarId);
                currentAvatarId = savedAvatarId;
            } else {
                // Default profile image if no saved avatar
                profileImage.setImageResource(R.drawable.sample_profile);

                // Get avatar ID from arguments if available
                Bundle args = getArguments();
                if (args != null) {
                    int avatarId = args.getInt("avatarId", 0);
                    if (avatarId > 0) {
                        updateProfileImage(avatarId);
                        currentAvatarId = avatarId;
                    }
                }
            }
        }
    }

    private void saveProfileData(String key, String value) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (key.equals("avatarId")) {
            editor.putInt(key, Integer.parseInt(value));
        } else {
            editor.putString(key, value);
        }

        editor.apply();
    }

    private void updateProfileImage(int avatarId) {
        currentAvatarId = avatarId;

        // Set the profile image based on the avatar ID
        int resourceId;
        switch (avatarId) {
            case 1:
                resourceId = R.drawable.profile2;
                break;
            case 2:
                resourceId = R.drawable.profile3;
                break;
            case 3:
                resourceId = R.drawable.profile4;
                break;
            case 4:
                resourceId = R.drawable.profile5;
                break;
            case 5:
                resourceId = R.drawable.profile6;
                break;
            case 6:
                resourceId = R.drawable.profile7;
                break;
            default:
                resourceId = R.drawable.sample_profile;
                break;
        }

        profileImage.setImageResource(resourceId);
    }

    private void navigateToEditProfile() {
        // Create the EditProfileFragment
        EditProfileFragment editProfileFragment = new EditProfileFragment();

        // Pass current profile data to the edit fragment
        Bundle args = new Bundle();
        args.putString("username", usernameText.getText().toString());
        args.putString("fullname", fullNameText.getText().toString());
        args.putInt("avatarId", currentAvatarId);
        editProfileFragment.setArguments(args);

        // Get the FragmentManager and start a transaction
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Replace the current fragment with the EditProfileFragment and add to back stack
        transaction.replace(R.id.frame_layout, editProfileFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void navigateToSecurity() {
        // Create the SecurityFragment
        Fragment securityFragment = new SecurityFragment();

        // Get the FragmentManager and start a transaction
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Replace the current fragment with the SecurityFragment and add to back stack
        transaction.replace(R.id.frame_layout, securityFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showCustomLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.logout_confirmation, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        Button btnYes = dialogView.findViewById(R.id.btn_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_no);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
    }

    private void performLogout() {
        Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show();

        FirebaseAuth.getInstance().signOut();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(requireActivity(), FirstActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        }, 1500);
    }

    private void showDeleteAccountDialog() {
        // Create a confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                // Delete user data from Firebase Database
                String uid = user.getUid();
                mFirestore.collection("users").document(uid).delete();

                // Delete user from Firebase Auth
                user.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Clear SharedPreferences
                        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                                requireActivity().MODE_PRIVATE);
                        prefs.edit().clear().apply();

                        Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();

                        // Navigate to login screen
                        Intent intent = new Intent(requireActivity(), FirstActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete account: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }
}
