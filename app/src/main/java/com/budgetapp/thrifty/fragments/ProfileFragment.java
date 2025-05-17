package com.budgetapp.thrifty.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView usernameText, fullnameText, emailDisplay;
    private ImageView userAvatar;
    private LinearLayout editProfileButton, securityButton, logoutButton, deleteAccountButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        usernameText = view.findViewById(R.id.textView);
        fullnameText = view.findViewById(R.id.textView2);
        emailDisplay = view.findViewById(R.id.email_display);
        userAvatar = view.findViewById(R.id.user_avatar);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        securityButton = view.findViewById(R.id.security_button);
        logoutButton = view.findViewById(R.id.logout_button);
        deleteAccountButton = view.findViewById(R.id.delete_account_button);

        // Load user data
        loadUserData();

        // Set up click listeners
        editProfileButton.setOnClickListener(v -> openEditProfileFragment());
        securityButton.setOnClickListener(v -> openSecurityFragment());
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountConfirmation());

        // Set up fragment result listener for profile updates
        getParentFragmentManager().setFragmentResultListener("profileUpdate", this, (requestKey, result) -> {
            String username = result.getString("username");
            String fullname = result.getString("fullname");
            int avatarId = result.getInt("avatarId", 0);

            if (username != null) {
                usernameText.setText(username);
            }
            if (fullname != null) {
                fullnameText.setText(fullname.toUpperCase());
            }
            if (avatarId > 0) {
                updateAvatarImage(userAvatar, avatarId);
            }
        });

        return view;
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // First try to get from SharedPreferences for faster loading
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                    requireActivity().MODE_PRIVATE);
            String username = prefs.getString("username", null);
            String fullname = prefs.getString("fullname", null);
            int avatarId = prefs.getInt("avatarId", 0);
            String userEmail = user.getEmail();
            emailDisplay.setText(userEmail);

            if (username != null) {
                usernameText.setText(username);
            }
            if (fullname != null) {
                fullnameText.setText(fullname.toUpperCase());
            }
            if (avatarId > 0) {
                updateAvatarImage(userAvatar, avatarId);
            }

            DocumentReference userRef = db.collection("users").document(user.getUid());
            userRef.collection("profile").document("info")
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String firestoreUsername = document.getString("username");
                            String firestoreFullname = document.getString("fullname");
                            Long avatarIdLong = document.getLong("avatarId");

                            if (firestoreUsername != null) {
                                usernameText.setText(firestoreUsername);
                                prefs.edit().putString("username", firestoreUsername).apply();
                            }
                            if (firestoreFullname != null) {
                                fullnameText.setText(firestoreFullname.toUpperCase());
                                prefs.edit().putString("fullname", firestoreFullname).apply();
                            }
                            if (avatarIdLong != null) {
                                int newAvatarId = avatarIdLong.intValue();
                                updateAvatarImage(userAvatar, newAvatarId);
                                prefs.edit().putInt("avatarId", newAvatarId).apply();
                            }
                        } else {
                            // If profile/info doesn't exist, check the root document
                            userRef.get()
                                    .addOnSuccessListener(rootDoc -> {
                                        if (rootDoc.exists()) {
                                            String rootUsername = rootDoc.getString("username");
                                            String rootFullname = rootDoc.getString("fullname");
                                            Long rootAvatarId = rootDoc.getLong("avatarId");

                                            if (rootUsername != null) {
                                                usernameText.setText(rootUsername);
                                                prefs.edit().putString("username", rootUsername).apply();
                                            }
                                            if (rootFullname != null) {
                                                fullnameText.setText(rootFullname.toUpperCase());
                                                prefs.edit().putString("fullname", rootFullname).apply();
                                            }
                                            if (rootAvatarId != null) {
                                                updateAvatarImage(userAvatar, rootAvatarId.intValue());
                                                prefs.edit().putInt("avatarId", rootAvatarId.intValue()).apply();
                                            }
                                        }
                                    });
                        }
                    });
        }
    }

    private void updateAvatarImage(ImageView imageView, int avatarId) {
        int resourceId;
        switch (avatarId) {
            case 1: resourceId = R.drawable.profile2; break;
            case 2: resourceId = R.drawable.profile3; break;
            case 3: resourceId = R.drawable.profile4; break;
            case 4: resourceId = R.drawable.profile5; break;
            case 5: resourceId = R.drawable.profile6; break;
            case 6: resourceId = R.drawable.profile7; break;
            case 8: resourceId = R.drawable.profile8; break;
            case 9: resourceId = R.drawable.profile9; break;
            default: resourceId = R.drawable.sample_profile; break;
        }
        imageView.setImageResource(resourceId);
    }

    private void openEditProfileFragment() {
        EditProfileFragment editProfileFragment = new EditProfileFragment();

        Bundle args = new Bundle();
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);
        int avatarId = prefs.getInt("avatarId", 0);
        if (avatarId > 0) {
            args.putInt("avatarId", avatarId);
            editProfileFragment.setArguments(args);
        }

        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, editProfileFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void openSecurityFragment() {
        SecurityFragment securityFragment = new SecurityFragment();
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, securityFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void showLogoutConfirmation() {
        LogoutDialogFragment logoutDialog = new LogoutDialogFragment();
        logoutDialog.show(getParentFragmentManager(), "logout_dialog");
    }

    private void showDeleteAccountConfirmation() {
        DeleteAccountFragment deleteAccountFragment = new DeleteAccountFragment();
        deleteAccountFragment.show(getParentFragmentManager(), "DeleteAccountDialog");
    }
}
