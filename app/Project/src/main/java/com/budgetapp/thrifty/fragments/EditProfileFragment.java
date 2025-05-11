package com.budgetapp.thrifty.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.budgetapp.thrifty.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditProfileFragment extends Fragment {

    private EditText usernameInput;
    private EditText fullnameInput;
    private ImageView profileImage;
    private ImageButton editProfileImageButton;
    private Button updateProfileButton;
    private View profilePictureSelector;
    private ImageView[] avatarViews;
    private Button confirmAvatarButton;
    private Button cancelAvatarButton;
    private int selectedAvatarId = 0;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        usernameInput = view.findViewById(R.id.username_input);
        fullnameInput = view.findViewById(R.id.fullname_input);
        profileImage = view.findViewById(R.id.profile_image);
        editProfileImageButton = view.findViewById(R.id.edit_profile_image);
        updateProfileButton = view.findViewById(R.id.update_profile_button);
        profilePictureSelector = view.findViewById(R.id.profile_picture_selector);

        // Initialize avatar selection views
        avatarViews = new ImageView[]{
            view.findViewById(R.id.avatar_1),
            view.findViewById(R.id.avatar_2),
            view.findViewById(R.id.avatar_3),
            view.findViewById(R.id.avatar_4),
            view.findViewById(R.id.avatar_5),
            view.findViewById(R.id.avatar_6),
            view.findViewById(R.id.avatar_7),
            view.findViewById(R.id.avatar_8)
        };

        confirmAvatarButton = view.findViewById(R.id.confirm_avatar_selection);
        cancelAvatarButton = view.findViewById(R.id.cancel_avatar_selection);

        // Load current profile data
        Bundle args = getArguments();
        if (args != null) {
            usernameInput.setText(args.getString("username", ""));
            fullnameInput.setText(args.getString("fullname", ""));
            selectedAvatarId = args.getInt("avatarId", 0);
            updateProfileImage(selectedAvatarId);
        }

        // Set click listeners
        editProfileImageButton.setOnClickListener(v -> showAvatarSelector());
        updateProfileButton.setOnClickListener(v -> updateProfile());
        confirmAvatarButton.setOnClickListener(v -> confirmAvatarSelection());
        cancelAvatarButton.setOnClickListener(v -> hideAvatarSelector());

        // Set avatar click listeners
        for (int i = 0; i < avatarViews.length; i++) {
            final int avatarId = i + 1;
            avatarViews[i].setOnClickListener(v -> selectAvatar(avatarId));
        }

        return view;
    }

    private void showAvatarSelector() {
        profilePictureSelector.setVisibility(View.VISIBLE);
    }

    private void hideAvatarSelector() {
        profilePictureSelector.setVisibility(View.GONE);
    }

    private void selectAvatar(int avatarId) {
        selectedAvatarId = avatarId;
        // Highlight selected avatar if needed
    }

    private void confirmAvatarSelection() {
        updateProfileImage(selectedAvatarId);
        hideAvatarSelector();
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
            default: resourceId = R.drawable.sample_profile;
        }
        profileImage.setImageResource(resourceId);
    }

    private void updateProfile() {
        String username = usernameInput.getText().toString().trim();
        String fullname = fullnameInput.getText().toString().trim();

        if (username.isEmpty() || fullname.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Update display name with format: "username|fullname"
            String displayNameWithFullName = username + "|" + fullname;
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayNameWithFullName)
                .build();

            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Update database
                    mDatabase.child("users").child(user.getUid()).child("username").setValue(username);
                    mDatabase.child("users").child(user.getUid()).child("fullname").setValue(fullname);
                    mDatabase.child("users").child(user.getUid()).child("avatarId").setValue(selectedAvatarId);

                    // Update shared preferences
                    saveToSharedPreferences(username, fullname);

                    // Notify parent fragment of the update
                    Bundle result = new Bundle();
                    result.putString("username", username);
                    result.putString("fullname", fullname);
                    result.putInt("avatarId", selectedAvatarId);
                    getParentFragmentManager().setFragmentResult("profileUpdate", result);

                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveToSharedPreferences(String username, String fullName) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
            requireActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", username);
        editor.putString("fullname", fullName);
        editor.putInt("avatarId", selectedAvatarId);
        editor.apply();
    }
}