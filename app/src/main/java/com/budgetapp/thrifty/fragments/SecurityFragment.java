package com.budgetapp.thrifty.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import com.budgetapp.thrifty.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SecurityFragment extends Fragment {
    private ConstraintLayout termsAndConditionsRow;
    private TextView profileName, profileFullName;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security, container, false);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        termsAndConditionsRow = view.findViewById(R.id.terms_conditions_row);
        profileName = view.findViewById(R.id.profile_name);
        profileFullName = view.findViewById(R.id.profile_full_name);

        // Set up click listeners
        termsAndConditionsRow.setOnClickListener(v -> showTermsAndConditions());

        // Load user profile data
        loadUserProfile();

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            String[] userData = user.getDisplayName().split("\\|");
            String username = userData[0];
            String fullName = userData.length > 1 ? userData[1] : username;

            profileName.setText(username);
            profileFullName.setText(fullName.toUpperCase());
        }
    }

    private void showTermsAndConditions() {
        // Here you would typically show the terms and conditions
        // For now, just show a toast message
        Toast.makeText(getContext(), "Terms and Conditions", Toast.LENGTH_SHORT).show();
    }
}
