package com.budgetapp.thrifty.fragments;

import android.content.SharedPreferences;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budgetapp.thrifty.MainActivity;
import com.budgetapp.thrifty.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";
    private ImageView profileImage, profileImageEdit;
    private CardView profileImageEditContainer;
    private TextView profileName, profileFullName;
    private EditText usernameInput, fullnameInput, emailInput;
    private ImageButton editProfileImage;
    private Button updateProfileButton;
    private CardView profilePictureSelector;
    private Button cancelAvatarSelection, confirmAvatarSelection;
    private ImageView[] avatarViews = new ImageView[8];
    private int selectedAvatarId = 0;
    private int currentAvatarId = 0;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private boolean isEditingProfilePicture = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

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
        profileImageEdit = view.findViewById(R.id.profile_image_edit);
        profileImageEditContainer = view.findViewById(R.id.profile_image_edit_container);
        profileName = view.findViewById(R.id.profile_name);
        profileFullName = view.findViewById(R.id.profile_full_name);
        usernameInput = view.findViewById(R.id.username_input);
        fullnameInput = view.findViewById(R.id.fullname_input);
        editProfileImage = view.findViewById(R.id.edit_profile_image);
        updateProfileButton = view.findViewById(R.id.update_profile_button);
        profilePictureSelector = view.findViewById(R.id.profile_picture_selector);
        cancelAvatarSelection = view.findViewById(R.id.cancel_avatar_selection);
        confirmAvatarSelection = view.findViewById(R.id.confirm_avatar_selection);
        emailInput = view.findViewById(R.id.email_input);
        emailInput.setEnabled(false);

        // Initialize avatar views
        avatarViews[0] = view.findViewById(R.id.avatar_1);
        avatarViews[1] = view.findViewById(R.id.avatar_2);
        avatarViews[2] = view.findViewById(R.id.avatar_3);
        avatarViews[3] = view.findViewById(R.id.avatar_4);
        avatarViews[4] = view.findViewById(R.id.avatar_5);
        avatarViews[5] = view.findViewById(R.id.avatar_6);
        avatarViews[6] = view.findViewById(R.id.avatar_7);
        avatarViews[7] = view.findViewById(R.id.avatar_8);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            emailInput.setText(userEmail);

            // First try to get data from Firebase Auth display name
            if (user.getDisplayName() != null) {
                String[] userData = user.getDisplayName().split("\\|");
                String username = userData[0];
                String fullName = userData.length > 1 ? userData[1] : username;

                profileName.setText(username);
                profileFullName.setText(fullName.toUpperCase());
                usernameInput.setText(username);
                fullnameInput.setText(fullName);
            }

            // Then load from Firestore to get the most up-to-date data
            mFirestore.collection("users").document(user.getUid())
                    .collection("profile").document("info")
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String username = document.getString("username");
                            String fullname = document.getString("fullname");
                            Long avatarIdLong = document.getLong("avatarId");

                            if (username != null) {
                                profileName.setText(username);
                                usernameInput.setText(username);
                            }

                            if (fullname != null) {
                                profileFullName.setText(fullname.toUpperCase());
                                fullnameInput.setText(fullname);
                            }

                            if (avatarIdLong != null) {
                                currentAvatarId = avatarIdLong.intValue();
                                updateProfileImage(currentAvatarId);
                            } else {
                                // Get avatar ID from arguments if available
                                Bundle args = getArguments();
                                if (args != null) {
                                    currentAvatarId = args.getInt("avatarId", 0);
                                    updateProfileImage(currentAvatarId);
                                }
                            }
                        } else {
                            // If profile/info doesn't exist, check the root document
                            mFirestore.collection("users").document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(rootDoc -> {
                                        if (rootDoc.exists()) {
                                            Long avatarIdLong = rootDoc.getLong("avatarId");
                                            if (avatarIdLong != null) {
                                                currentAvatarId = avatarIdLong.intValue();
                                                updateProfileImage(currentAvatarId);
                                            }
                                        }
                                    });
                        }
                    });
        }
    }

    private void setupClickListeners() {
        // Debug log to check if this method is being called
        Log.d(TAG, "Setting up click listeners");

        editProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleProfilePictureEditMode();
            }
        });

        cancelAvatarSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profilePictureSelector.setVisibility(View.GONE);
                selectedAvatarId = currentAvatarId;

                // Exit edit mode when canceling
                if (isEditingProfilePicture) {
                    toggleProfilePictureEditMode();
                }
            }
        });

        confirmAvatarSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentAvatarId = selectedAvatarId;
                updateProfileImage(currentAvatarId);
                profilePictureSelector.setVisibility(View.GONE);

                // Exit edit mode when confirming
                if (isEditingProfilePicture) {
                    toggleProfilePictureEditMode();
                }
            }
        });

        // Set up avatar selection listeners
        for (int i = 0; i < avatarViews.length; i++) {
            final int avatarId = i + 1;
            avatarViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedAvatarId = avatarId;
                    highlightSelectedAvatar(avatarId);
                }
            });
        }

        // Debug log to check if the button is null
        if (updateProfileButton == null) {
            Log.e(TAG, "updateProfileButton is null!");
        } else {
            Log.d(TAG, "updateProfileButton found, setting click listener");

            // Set click listener with explicit debug logs
            updateProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Update profile button clicked");
                    updateProfile();
                }
            });

            // Make sure the button is clickable and enabled
            updateProfileButton.setClickable(true);
            updateProfileButton.setEnabled(true);
        }
    }

    private void toggleProfilePictureEditMode() {
        isEditingProfilePicture = !isEditingProfilePicture;

        if (isEditingProfilePicture) {
            // Enter edit mode
            profileImage.setVisibility(View.INVISIBLE);
            profileImageEditContainer.setVisibility(View.VISIBLE);

            // Copy the current image to the edit container
            profileImageEdit.setImageDrawable(profileImage.getDrawable());

            // Show the profile picture selector
            profilePictureSelector.setVisibility(View.VISIBLE);
        } else {
            // Exit edit mode
            profileImage.setVisibility(View.VISIBLE);
            profileImageEditContainer.setVisibility(View.GONE);
            profilePictureSelector.setVisibility(View.GONE);
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
            default: resourceId = R.drawable.sample_profile; break;
        }
        profileImage.setImageResource(resourceId);

        // Also update the edit image if it's visible
        if (profileImageEditContainer.getVisibility() == View.VISIBLE) {
            profileImageEdit.setImageResource(resourceId);
        }
    }

    private void highlightSelectedAvatar(int avatarId) {
        // Hide all highlights first
        for (int i = 1; i <= 8; i++) {
            int highlightId = getResources().getIdentifier("avatar_highlight_" + i, "id", requireActivity().getPackageName());
            View highlightView = getView().findViewById(highlightId);
            if (highlightView != null) {
                highlightView.setVisibility(i == avatarId ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void updateProfile() {
        Log.d(TAG, "updateProfile method called");

        String newUsername = usernameInput.getText().toString().trim();
        String newFullName = fullnameInput.getText().toString().trim();

        if (newUsername.isEmpty() || newFullName.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            Log.d(TAG, "Updating profile for user: " + userId);

            // Update Firebase Auth display name
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername + "|" + newFullName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Firebase Auth profile updated successfully");

                        // Create profile data map
                        Map<String, Object> profileData = new HashMap<>();
                        profileData.put("username", newUsername);
                        profileData.put("fullname", newFullName);
                        profileData.put("avatarId", currentAvatarId);
                        profileData.put("email", user.getEmail());
                        profileData.put("role", "user");

                        // Update Firestore - update only the profile/info document
                        DocumentReference profileRef = mFirestore.collection("users")
                                .document(userId)
                                .collection("profile")
                                .document("info");

                        profileRef.set(profileData, SetOptions.merge())
                                .addOnSuccessListener(aVoid1 -> {
                                    Log.d(TAG, "Firestore profile/info updated successfully");

                                    // Save to SharedPreferences for immediate access
                                    SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                                            requireActivity().MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("username", newUsername);
                                    editor.putString("fullname", newFullName);
                                    editor.putInt("avatarId", currentAvatarId);
                                    editor.apply();

                                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

                                    // Set fragment result to notify ProfileFragment
                                    Bundle result = new Bundle();
                                    result.putString("username", newUsername);
                                    result.putString("fullname", newFullName);
                                    result.putInt("avatarId", currentAvatarId);
                                    getParentFragmentManager().setFragmentResult("profileUpdate", result);

                                    // Refresh MainActivity to update all fragments
                                    if (getActivity() instanceof MainActivity) {
                                        ((MainActivity) getActivity()).refreshAllFragments();
                                        ((MainActivity) getActivity()).updateAvatarEverywhere(currentAvatarId);
                                    }

                                    // Navigate back to ProfileFragment
                                    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                                    fragmentManager.popBackStack();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update Firestore", e);
                                    Toast.makeText(getContext(), "Failed to update profile data", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update Firebase Auth profile", e);
                        Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Double-check that the button is properly set up
        if (updateProfileButton != null) {
            updateProfileButton.setOnClickListener(v -> {
                Log.d(TAG, "Update button clicked from onResume listener");
                updateProfile();
            });
            updateProfileButton.setClickable(true);
            updateProfileButton.setEnabled(true);
        }
    }
}
