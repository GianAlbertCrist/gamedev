package com.budgetapp.thrifty.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.budgetapp.thrifty.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private ImageView profileImage;
    private ImageButton editProfileImageButton;
    private CardView profilePictureSelector;
    private Button cancelAvatarSelection;
    private Button confirmAvatarSelection;
    private Button updateProfileButton;
    private ImageView backButton;
    private EditText usernameInput;
    private EditText fullnameInput;
    private EditText emailInput;

    // Avatar selection
    private ImageView[] avatars = new ImageView[8];
    private int selectedAvatarId = -1;
    private int currentAvatarId = 0; // Default avatar

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Store original values to check if they've changed
    private String originalUsername = "";
    private String originalFullname = "";
    private String originalEmail = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        editProfileImageButton = view.findViewById(R.id.edit_profile_image);
        profilePictureSelector = view.findViewById(R.id.profile_picture_selector);
        cancelAvatarSelection = view.findViewById(R.id.cancel_avatar_selection);
        confirmAvatarSelection = view.findViewById(R.id.confirm_avatar_selection);
        updateProfileButton = view.findViewById(R.id.update_profile_button);
        backButton = view.findViewById(R.id.edit_profile_back);
        usernameInput = view.findViewById(R.id.username_input);
        fullnameInput = view.findViewById(R.id.fullname_input);
        emailInput = view.findViewById(R.id.email_input);

        // Initialize avatar ImageViews
        avatars[0] = view.findViewById(R.id.avatar_1);
        avatars[1] = view.findViewById(R.id.avatar_2);
        avatars[2] = view.findViewById(R.id.avatar_3);
        avatars[3] = view.findViewById(R.id.avatar_4);
        avatars[4] = view.findViewById(R.id.avatar_5);
        avatars[5] = view.findViewById(R.id.avatar_6);
        avatars[6] = view.findViewById(R.id.avatar_7);
        avatars[7] = view.findViewById(R.id.avatar_8);

        // First try to get data from arguments
        Bundle args = getArguments();
        if (args != null) {
            String username = args.getString("username", "");
            String fullname = args.getString("fullname", "");
            currentAvatarId = args.getInt("avatarId", 0);

            if (!username.isEmpty()) {
                usernameInput.setText(username);
                originalUsername = username;
            }

            if (!fullname.isEmpty()) {
                fullnameInput.setText(fullname);
                originalFullname = fullname;
            }

            if (currentAvatarId > 0) {
                updateProfileImage(currentAvatarId);
            }
        }

        // Then try to get from SharedPreferences
        if (originalUsername.isEmpty() || originalFullname.isEmpty()) {
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                    requireActivity().MODE_PRIVATE);

            String username = prefs.getString("username", "");
            String fullname = prefs.getString("fullname", "");
            int avatarId = prefs.getInt("avatarId", 0);

            if (!username.isEmpty() && originalUsername.isEmpty()) {
                usernameInput.setText(username);
                originalUsername = username;
            }

            if (!fullname.isEmpty() && originalFullname.isEmpty()) {
                fullnameInput.setText(fullname);
                originalFullname = fullname;
            }

            if (avatarId > 0 && currentAvatarId == 0) {
                currentAvatarId = avatarId;
                updateProfileImage(avatarId);
            }
        }

        // Finally try to get from Firebase Auth
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            if (email != null && !email.isEmpty()) {
                emailInput.setText(email);
                originalEmail = email;
            }

            // If we still don't have a username, try to get from Firebase Auth
            if (originalUsername.isEmpty() && user.getDisplayName() != null) {
                usernameInput.setText(user.getDisplayName());
                originalUsername = user.getDisplayName();
            }
        }

        // Set click listeners
        editProfileImageButton.setOnClickListener(v -> showProfilePictureSelector());
        cancelAvatarSelection.setOnClickListener(v -> hideProfilePictureSelector());
        confirmAvatarSelection.setOnClickListener(v -> confirmAvatarAndHideSelector());
        updateProfileButton.setOnClickListener(v -> updateProfile());
        backButton.setOnClickListener(v -> navigateBack());

        // Set click listeners for avatars
        for (int i = 0; i < avatars.length; i++) {
            final int avatarIndex = i;
            avatars[i].setOnClickListener(v -> selectAvatar(avatarIndex));
        }

        // Setup touch listener to hide keyboard when clicking outside EditText
        setupTouchListener(view);

        return view;
    }

    private void setupTouchListener(View view) {
        // Set up touch listener for non-text box views to hide keyboard
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideSoftKeyboard();
                return false;
            });
        }

        // If a layout container, iterate over children and seed recursion
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupTouchListener(innerView);
            }
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void updateProfileImage(int avatarId) {
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

    private void showProfilePictureSelector() {
        // Show the profile picture selector with animation
        profilePictureSelector.setVisibility(View.VISIBLE);
        profilePictureSelector.setAlpha(0f);
        profilePictureSelector.animate()
                .alpha(1f)
                .setDuration(300)
                .start();

        // Pre-select the current avatar if any
        if (currentAvatarId >= 0 && currentAvatarId < avatars.length) {
            selectAvatar(currentAvatarId);
        }
    }

    private void hideProfilePictureSelector() {
        // Hide the profile picture selector with animation
        profilePictureSelector.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> profilePictureSelector.setVisibility(View.GONE))
                .start();

        // Reset selection
        selectedAvatarId = -1;
        resetAvatarSelections();
    }

    private void selectAvatar(int index) {
        // Reset all avatars to default state
        resetAvatarSelections();

        // Highlight the selected avatar
        avatars[index].setBackgroundResource(R.drawable.rounded_background);
        avatars[index].setBackgroundTintList(getResources().getColorStateList(R.color.primary_color));

        // Store the selected avatar index
        selectedAvatarId = index;
    }

    private void resetAvatarSelections() {
        for (ImageView avatar : avatars) {
            avatar.setBackgroundResource(R.drawable.rounded_background);
            avatar.setBackgroundTintList(null);
        }
    }

    private void confirmAvatarAndHideSelector() {
        if (selectedAvatarId != -1) {
            // Update the profile image with the selected avatar
            profileImage.setImageDrawable(avatars[selectedAvatarId].getDrawable());

            // Store the current avatar ID
            currentAvatarId = selectedAvatarId;

            // Hide the selector with animation
            hideProfilePictureSelector();

            // Show confirmation toast
            Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Please select a profile picture", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfile() {
        // Hide keyboard
        hideSoftKeyboard();

        // Get the values from the input fields
        String username = usernameInput.getText().toString().trim();
        String fullname = fullnameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();

        // Check if any changes were made
        boolean usernameChanged = !username.equals(originalUsername) && !username.isEmpty();
        boolean fullnameChanged = !fullname.equals(originalFullname) && !fullname.isEmpty();
        boolean emailChanged = !email.equals(originalEmail) && !email.isEmpty();
        boolean avatarChanged = selectedAvatarId != -1;

        // If nothing changed, just go back
        if (!usernameChanged && !fullnameChanged && !emailChanged && !avatarChanged) {
            Toast.makeText(getContext(), "No changes detected", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        // Show loading indicator
        Toast.makeText(getContext(), "Updating profile...", Toast.LENGTH_SHORT).show();

        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Create a map for database updates
            Map<String, Object> updates = new HashMap<>();

            // Only update fields that have changed
            if (usernameChanged) {
                updates.put("username", username);
            }

            if (fullnameChanged) {
                updates.put("fullname", fullname);
            }

            if (avatarChanged || currentAvatarId > 0) {
                updates.put("avatarId", currentAvatarId);
            }

            // Update Firebase Auth display name if username changed
            if (usernameChanged) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build();

                user.updateProfile(profileUpdates);
            }

            // Update Firebase Database
            String uid = user.getUid();
            if (!updates.isEmpty()) {
                mDatabase.child("users").child(uid).updateChildren(updates);
            }

            // Update SharedPreferences
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                    requireActivity().MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            if (usernameChanged) {
                editor.putString("username", username);
            }

            if (fullnameChanged) {
                editor.putString("fullname", fullname);
            }

            if (avatarChanged || currentAvatarId > 0) {
                editor.putInt("avatarId", currentAvatarId);
            }

            editor.apply();

            // Create a bundle to pass data back to ProfileFragment
            Bundle result = new Bundle();
            if (usernameChanged) {
                result.putString("username", username);
            }

            if (fullnameChanged) {
                result.putString("fullname", fullname);
            }

            if (avatarChanged || currentAvatarId > 0) {
                result.putInt("avatarId", currentAvatarId);
            }

            // Set the result to be retrieved by ProfileFragment
            getParentFragmentManager().setFragmentResult("profileUpdate", result);

            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

            // Navigate back to the profile screen
            navigateBack();
        } else {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
