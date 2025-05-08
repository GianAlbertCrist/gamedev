package com.budgetapp.thrifty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class EditProfileFragment extends Fragment {

    private ImageView profileImage;
    private ImageButton editProfileImageButton;
    private CardView profilePictureSelector;
    private Button cancelAvatarSelection;
    private Button confirmAvatarSelection;
    private Button updateProfileButton;
    private ImageView backButton;

    // Avatar selection
    private ImageView[] avatars = new ImageView[8];
    private int selectedAvatarId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        editProfileImageButton = view.findViewById(R.id.edit_profile_image);
        profilePictureSelector = view.findViewById(R.id.profile_picture_selector);
        cancelAvatarSelection = view.findViewById(R.id.cancel_avatar_selection);
        confirmAvatarSelection = view.findViewById(R.id.confirm_avatar_selection);
        updateProfileButton = view.findViewById(R.id.update_profile_button);
        backButton = view.findViewById(R.id.edit_profile_back);

        // Initialize avatar ImageViews
        avatars[0] = view.findViewById(R.id.avatar_1);
        avatars[1] = view.findViewById(R.id.avatar_2);
        avatars[2] = view.findViewById(R.id.avatar_3);
        avatars[3] = view.findViewById(R.id.avatar_4);
        avatars[4] = view.findViewById(R.id.avatar_5);
        avatars[5] = view.findViewById(R.id.avatar_6);
        avatars[6] = view.findViewById(R.id.avatar_7);
        avatars[7] = view.findViewById(R.id.avatar_8);

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

        return view;
    }

    private void showProfilePictureSelector() {
        profilePictureSelector.setVisibility(View.VISIBLE);
    }

    private void hideProfilePictureSelector() {
        profilePictureSelector.setVisibility(View.GONE);
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

            // Hide the selector
            profilePictureSelector.setVisibility(View.GONE);

            // Reset selection
            selectedAvatarId = -1;
            resetAvatarSelections();
        } else {
            Toast.makeText(getContext(), "Please select an avatar", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfile() {
        // Get the values from the input fields
        EditText usernameInput = getView().findViewById(R.id.username_input);
        EditText fullnameInput = getView().findViewById(R.id.fullname_input);
        EditText emailInput = getView().findViewById(R.id.email_input);

        String username = usernameInput.getText().toString().trim();
        String fullname = fullnameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();

        // Validate inputs
        if (username.isEmpty() || fullname.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Here you would typically save the profile data to your database or preferences
        // For now, just show a success message
        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

        // Navigate back to the profile screen
        navigateBack();
    }

    private void navigateBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
