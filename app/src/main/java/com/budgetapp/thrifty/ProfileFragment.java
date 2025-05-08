package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class ProfileFragment extends Fragment {

    private LinearLayout editProfileButton;
    private LinearLayout securityButton;
    private LinearLayout logoutButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize buttons
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        securityButton = view.findViewById(R.id.security_button);
        logoutButton = view.findViewById(R.id.logout_button);

        // Set click listeners
        editProfileButton.setOnClickListener(v -> navigateToEditProfile());
        securityButton.setOnClickListener(v -> navigateToSecurity());
        logoutButton.setOnClickListener(v -> showLogoutDialog());

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
