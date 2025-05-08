package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentTransaction;

public class ProfileFragment extends Fragment {

    private LinearLayout editProfileButton;
    private LinearLayout securityButton;
    private LinearLayout logoutButton;
    private ImageView profileImage;
    private TextView profileName;
    private TextView profileFullName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        securityButton = view.findViewById(R.id.security_button);
        logoutButton = view.findViewById(R.id.logout_button);
        profileImage = view.findViewById(R.id.picture_najud);
        profileName = view.findViewById(R.id.textView);
        profileFullName = view.findViewById(R.id.textView2);

        // Set click listeners
        editProfileButton.setOnClickListener(v -> navigateToEditProfile());
        securityButton.setOnClickListener(v -> navigateToSecurity());
        logoutButton.setOnClickListener(v -> showLogoutDialog());

        // Listen for profile updates from EditProfileFragment
        getParentFragmentManager().setFragmentResultListener("profileUpdate", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        // Update profile information with data from EditProfileFragment
                        String username = result.getString("username", "");
                        String fullname = result.getString("fullname", "");
                        int avatarId = result.getInt("avatarId", -1);

                        if (!username.isEmpty()) {
                            profileName.setText(username);
                        }

                        if (!fullname.isEmpty()) {
                            profileFullName.setText(fullname.toUpperCase());
                        }

                        // Update avatar if one was selected
                        if (avatarId >= 0) {
                            // In a real app, you would have a way to map avatarId to the correct drawable
                            // For now, we'll just use the sample_profile drawable
                            profileImage.setImageResource(R.drawable.sample_profile);
                        }
                    }
                });

        return view;
    }

    private void navigateToEditProfile() {
        // Create the EditProfileFragment
        Fragment editProfileFragment = new EditProfileFragment();

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
}
