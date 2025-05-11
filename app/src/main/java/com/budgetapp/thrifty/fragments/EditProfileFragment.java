package com.budgetapp.thrifty.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.budgetapp.thrifty.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView profileName, profileFullName;
    private EditText usernameInput, fullnameInput;
    private ImageButton editProfileImage;
    private Button updateProfileButton;
    private CardView profilePictureSelector;
    private Button cancelAvatarSelection, confirmAvatarSelection;
    private ImageView[] avatarViews = new ImageView[8];
    private int selectedAvatarId = 0;
    private int currentAvatarId = 0;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initializeViews(view);

        // Load current user data
        loadUserData();

        // Set up click listeners
        setupClickListeners();

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
        profilePictureSelector = view.findViewById(R.id.profile_picture_selector);
        cancelAvatarSelection = view.findViewById(R.id.cancel_avatar_selection);
        confirmAvatarSelection = view.findViewById(R.id.confirm_avatar_selection);

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
        if (user != null && user.getDisplayName() != null) {
            String[] userData = user.getDisplayName().split("\\|");
            String username = userData[0];
            String fullName = userData[1];

            // Set the text in TextViews and EditTexts
            profileName.setText(username);
            profileFullName.setText(fullName.toUpperCase());
            usernameInput.setText(username);
            fullnameInput.setText(fullName);

            // Get avatar ID from arguments if available
            Bundle args = getArguments();
            if (args != null) {
                currentAvatarId = args.getInt("avatarId", 0);
                updateProfileImage(currentAvatarId);
            }
        }
    }

    private void setupClickListeners() {
        editProfileImage.setOnClickListener(v -> {
            profilePictureSelector.setVisibility(View.VISIBLE);
        });

        cancelAvatarSelection.setOnClickListener(v -> {
            profilePictureSelector.setVisibility(View.GONE);
            selectedAvatarId = currentAvatarId;
        });

        confirmAvatarSelection.setOnClickListener(v -> {
            currentAvatarId = selectedAvatarId;
            updateProfileImage(currentAvatarId);
            profilePictureSelector.setVisibility(View.GONE);
        });

        // Set up avatar selection listeners
        for (int i = 0; i < avatarViews.length; i++) {
            final int avatarId = i + 1;
            avatarViews[i].setOnClickListener(v -> {
                selectedAvatarId = avatarId;
                highlightSelectedAvatar(avatarId);
            });
        }

        updateProfileButton.setOnClickListener(v -> updateProfile());
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
    }

    private void highlightSelectedAvatar(int avatarId) {
        for (int i = 0; i < avatarViews.length; i++) {
            avatarViews[i].setBackgroundResource(i == avatarId - 1 ?
                    R.drawable.selected_avatar_background : R.drawable.rounded_background);
        }
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

            // Update the data in Firebase Database
            Map<String, Object> updates = new HashMap<>();
            updates.put("username", newUsername);
            updates.put("fullname", newFullName);
            updates.put("avatarId", currentAvatarId);

            mDatabase.child("users").child(userId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Send result back to ProfileFragment
                        Bundle result = new Bundle();
                        result.putString("username", newUsername);
                        result.putString("fullname", newFullName);
                        result.putInt("avatarId", currentAvatarId);
                        getParentFragmentManager().setFragmentResult("profileUpdate", result);

                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
        }
    }
}