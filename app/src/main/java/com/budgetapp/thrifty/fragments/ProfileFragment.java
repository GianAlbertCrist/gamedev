package com.budgetapp.thrifty.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.budgetapp.thrifty.FirstActivity;
import com.budgetapp.thrifty.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private LinearLayout editProfileButton;
    private LinearLayout securityButton;
    private LinearLayout logoutButton;
    private LinearLayout deleteAccountButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextView usernameText;
    private TextView fullNameText;
    private ImageView profileImage;
    private int currentAvatarId = 0; // Default avatar
    private ValueEventListener userListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize buttons
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        securityButton = view.findViewById(R.id.security_button);
        logoutButton = view.findViewById(R.id.logout_button);
        deleteAccountButton = view.findViewById(R.id.delete_account_button);

        // Initialize text views
        usernameText = view.findViewById(R.id.textView);
        fullNameText = view.findViewById(R.id.textView2);
        profileImage = view.findViewById(R.id.picture_najud);

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
                    }
                });

        // Set click listeners
        editProfileButton.setOnClickListener(v -> navigateToEditProfile());
        securityButton.setOnClickListener(v -> navigateToSecurity());
        logoutButton.setOnClickListener(v -> showLogoutDialog());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the database listener when the fragment is destroyed
        if (userListener != null && mAuth.getCurrentUser() != null) {
            mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).removeEventListener(userListener);
        }
    }

    private void setupUserListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            userListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String username = snapshot.child("username").getValue(String.class);
                        String fullname = snapshot.child("fullname").getValue(String.class);
                        Long avatarIdLong = snapshot.child("avatarId").getValue(Long.class);
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
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "loadUserData:onCancelled", error.toException());
                }
            };

            mDatabase.child("users").child(uid).addValueEventListener(userListener);
        }
    }

    private void loadProfileData() {
        // First try to load from SharedPreferences for immediate display
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);

        String username = prefs.getString("username", "");
        String fullname = prefs.getString("fullname", "");
        int avatarId = prefs.getInt("avatarId", 0);

        if (!username.isEmpty()) {
            usernameText.setText(username);
        }

        if (!fullname.isEmpty()) {
            fullNameText.setText(fullname.toUpperCase());
        }

        if (avatarId > 0) {
            updateProfileImage(avatarId);
            currentAvatarId = avatarId;
        }

        // Then try to get from Firebase for the most up-to-date data
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            mDatabase.child("users").child(uid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    String dbUsername = task.getResult().child("username").getValue(String.class);
                    String dbFullname = task.getResult().child("fullname").getValue(String.class);
                    Long dbAvatarIdLong = task.getResult().child("avatarId").getValue(Long.class);
                    int dbAvatarId = dbAvatarIdLong != null ? dbAvatarIdLong.intValue() : 0;

                    if (dbUsername != null && !dbUsername.isEmpty()) {
                        usernameText.setText(dbUsername);
                        saveProfileData("username", dbUsername);
                    }

                    if (dbFullname != null && !dbFullname.isEmpty()) {
                        fullNameText.setText(dbFullname.toUpperCase());
                        saveProfileData("fullname", dbFullname);
                    }

                    if (dbAvatarId > 0) {
                        updateProfileImage(dbAvatarId);
                        currentAvatarId = dbAvatarId;
                        saveProfileData("avatarId", String.valueOf(dbAvatarId));
                    }
                }
            });
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

    private void showLogoutDialog() {
        // Create the LogoutFragment (EndSessionFragment)
        Fragment logoutFragment = new EndSessionFragment();

        // Get the FragmentManager and start a transaction
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Replace the current fragment with the LogoutFragment and add to back stack
        transaction.replace(R.id.frame_layout, logoutFragment);
        transaction.addToBackStack(null);
        transaction.commit();
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
                mDatabase.child("users").child(uid).removeValue();

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
