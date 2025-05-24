package com.budgetapp.thrifty.fragments;


import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.bumptech.glide.Glide;
import com.budgetapp.thrifty.MainActivity;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment implements AvatarSelectionDialogFragment.OnAvatarSelectedListener {

    private static final String TAG = "EditProfileFragment";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_CUSTOM_AVATAR_URI = "custom_avatar_uri";
    private ImageView profileImage;
    private TextView profileName, profileFullName, emailDisplay;
    private EditText usernameInput, fullnameInput;
    private ImageButton editProfileImage;
    private Button updateProfileButton;
    private int currentAvatarId = 0;
    private Uri customAvatarUri = null;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews(view);

        // Load current user data
        loadUserData();

        // Set up click listeners
        setupClickListeners();

        // Fix for keyboard pushing up the navigation bar
        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        return view;
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileFullName = view.findViewById(R.id.profile_full_name);
        usernameInput = view.findViewById(R.id.username_input);
        fullnameInput = view.findViewById(R.id.fullname_input);
        editProfileImage = view.findViewById(R.id.edit_profile_image);
        updateProfileButton = view.findViewById(R.id.update_profile_button);
        emailDisplay = view.findViewById(R.id.email_display);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            emailDisplay.setText(userEmail);

            // Load from SharedPreferences first
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            String savedUsername = prefs.getString("username", null);
            String savedFullname = prefs.getString("fullname", null);
            int savedAvatarId = prefs.getInt("avatarId", 0);
            String savedCustomUri = prefs.getString(KEY_CUSTOM_AVATAR_URI, null);

            if (savedUsername != null) {
                profileName.setText(savedUsername);
                usernameInput.setText(savedUsername);
            }
            if (savedFullname != null) {
                profileFullName.setText(savedFullname.toUpperCase());
                fullnameInput.setText(savedFullname);
            }

            currentAvatarId = savedAvatarId;
            if (savedCustomUri != null) {
                customAvatarUri = Uri.parse(savedCustomUri);
            }

            // Then load from Firestore and update if different
            mFirestore.collection("users").document(user.getUid())
                    .collection("profile").document("info")
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String username = document.getString("username");
                            String fullname = document.getString("fullname");
                            Long avatarIdLong = document.getLong("avatarId");
                            String firestoreCustomUri = document.getString("customAvatarUri"); // Fixed field name

                            if (username != null) {
                                profileName.setText(username);
                                usernameInput.setText(username);
                            }

                            if (fullname != null) {
                                profileFullName.setText(fullname.toUpperCase());
                                fullnameInput.setText(fullname);
                            }

                            if (firestoreCustomUri != null && !firestoreCustomUri.isEmpty()) {
                                customAvatarUri = Uri.parse(firestoreCustomUri);
                                currentAvatarId = 0;
                            } else if (avatarIdLong != null) {
                                currentAvatarId = avatarIdLong.intValue();
                                customAvatarUri = null;
                            }

                            loadSavedAvatar();
                        } else {
                            loadSavedAvatar();
                        }
                    });
        }
    }

    private void loadSavedAvatar() {

        if (customAvatarUri != null) {
            Glide.with(this)
                    .load(customAvatarUri)
                    .circleCrop()
                    .placeholder(R.drawable.sample_profile)
                    .error(R.drawable.sample_profile)
                    .into(profileImage);
            return;
        }
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_CUSTOM_AVATAR_URI, null);
        if (uriString != null) {
            customAvatarUri = Uri.parse(uriString);
            Glide.with(this)
                    .load(customAvatarUri)
                    .circleCrop()
                    .into(profileImage);
        } else {
            customAvatarUri = null;
            updateProfileImage(currentAvatarId);
        }
    }

        private void setupClickListeners() {
        // Debug log to check if this method is being called
        Log.d(TAG, "Setting up click listeners");

            editProfileImage.setOnClickListener(v -> {
                AvatarSelectionDialogFragment dialog = AvatarSelectionDialogFragment.newInstance(currentAvatarId);
                dialog.setOnAvatarSelectedListener(this);
                dialog.show(getParentFragmentManager(), "AvatarSelectionDialog");
            });

            if (updateProfileButton != null) {
                updateProfileButton.setOnClickListener(v -> updateProfile());
                updateProfileButton.setClickable(true);
                updateProfileButton.setEnabled(true);
            }
        }


    private void updateProfileImage(int avatarId) {
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
        profileImage.setImageResource(resourceId);
    }


    private void updateProfile() {
        String newUsername = usernameInput.getText().toString().trim();
        String newFullName = fullnameInput.getText().toString().trim();

        if (newUsername.isEmpty() || newFullName.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername + "|" + newFullName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> profileData = new HashMap<>();
                        profileData.put("username", newUsername);
                        profileData.put("fullname", newFullName);
//                        profileData.put("avatarId", currentAvatarId);

                        if (customAvatarUri != null) {
                            profileData.put("avatarId", 0); // Set to 0 for custom avatar
                            profileData.put("customAvatarUri", customAvatarUri.toString());
                        } else {
                            profileData.put("avatarId", currentAvatarId);
                            profileData.put("customAvatarUri", null);
                        }

                        profileData.put("email", user.getEmail());
                        profileData.put("role", "user");

                        DocumentReference userRef = mFirestore.collection("users").document(userId);
                        DocumentReference profileRef = userRef.collection("profile").document("info");

                        mFirestore.runBatch(batch -> {
                            batch.set(userRef, profileData, SetOptions.merge());
                            batch.set(profileRef, profileData, SetOptions.merge());
                        }).addOnSuccessListener(aVoid1 -> {
                            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("username", newUsername);
                            editor.putString("fullname", newFullName);
//                            editor.putInt("avatarId", currentAvatarId);

                            if (customAvatarUri != null) {
                                editor.putInt("avatarId", 0); // Set to 0 for custom avatar
                                editor.putString(KEY_CUSTOM_AVATAR_URI, customAvatarUri.toString());
                            } else {
                                editor.putInt("avatarId", currentAvatarId);
                                editor.remove(KEY_CUSTOM_AVATAR_URI);
                            }

                            editor.apply();

                            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

                            Bundle result = new Bundle();
                            result.putString("username", newUsername);
                            result.putString("fullname", newFullName);
//                            result.putInt("avatarId", currentAvatarId);

                            if (customAvatarUri != null) {
                                result.putInt("avatarId", 0);
                                result.putString("custom_avatar_uri", customAvatarUri.toString());
                            } else {
                                result.putInt("avatarId", currentAvatarId);
                            }

                            getParentFragmentManager().setFragmentResult("profileUpdate", result);

                            if (getActivity() instanceof MainActivity) {
                                if (customAvatarUri != null) {
                                    ((MainActivity) getActivity()).updateAvatarEverywhere(0, customAvatarUri.toString());
                                } else {
                                    ((MainActivity) getActivity()).updateAvatarEverywhere(currentAvatarId, null);
                                }
                            }

                            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                            fragmentManager.popBackStack();

                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update profile data", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    @Override
    public void onAvatarSelected(int avatarId, @Nullable Uri selectedCustomAvatarUri) {
        if (selectedCustomAvatarUri != null) {
            customAvatarUri = selectedCustomAvatarUri;
            currentAvatarId = 0;

            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_CUSTOM_AVATAR_URI, customAvatarUri.toString());
            editor.putInt("avatarId", 0);
            editor.apply();

            Glide.with(this)
                    .load(customAvatarUri)
                    .circleCrop()
                    .placeholder(R.drawable.sample_profile)
                    .error(R.drawable.sample_profile)
                    .into(profileImage);

        } else {
            customAvatarUri = null;
            currentAvatarId = avatarId;

            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_CUSTOM_AVATAR_URI);
            editor.putInt("avatarId", currentAvatarId);
            editor.apply();

            // Update the profile image view
            updateProfileImage(currentAvatarId);
        }
    }
}
