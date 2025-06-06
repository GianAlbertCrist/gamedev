package com.budgetapp.thrifty.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.budgetapp.thrifty.R;

public class SecurityFragment extends Fragment {

    private ImageView backButton;
    private ConstraintLayout termsAndConditionsRow;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security, container, false);

        // Initialize views
        termsAndConditionsRow = view.findViewById(R.id.terms_conditions_row);

        termsAndConditionsRow.setOnClickListener(v -> showTermsAndConditions());

        return view;
    }

    private void showTermsAndConditions() {
        // Here you would typically show the terms and conditions
        // For now, just show a toast message
        Toast.makeText(getContext(), "Terms and Conditions", Toast.LENGTH_SHORT).show();
    }

    private void navigateBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
