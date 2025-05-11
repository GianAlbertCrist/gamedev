package com.budgetapp.thrifty.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.budgetapp.thrifty.FirstActivity;
import com.budgetapp.thrifty.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EndSessionFragment extends Fragment implements View.OnTouchListener {

    private Button endSessionButton;
    private Button cancelButton;
    private View rootView;
    private CardView dialogContainer;
    private FirebaseAuth mAuth;
    private TextView profileName, profileFullName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_endsession, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews();

        // Set click listeners
        setupClickListeners();

        // Load user profile data
        loadUserProfile();

        return rootView;
    }

    private void initializeViews() {
        endSessionButton = rootView.findViewById(R.id.yes_end_session);
        cancelButton = rootView.findViewById(R.id.cancel_button);
        dialogContainer = rootView.findViewById(R.id.dialog_container);
        profileName = rootView.findViewById(R.id.profile_name);
        profileFullName = rootView.findViewById(R.id.profile_full_name);
    }

    private void setupClickListeners() {
        endSessionButton.setOnClickListener(v -> performLogout());
        cancelButton.setOnClickListener(v -> cancelLogout());
        rootView.setOnTouchListener(this);
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Only handle ACTION_DOWN events (when the user first touches the screen)
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Get the coordinates of the touch event
            float x = event.getX();
            float y = event.getY();

            // Check if the touch is outside the dialog container
            if (dialogContainer != null && !isPointInsideView(x, y, dialogContainer)) {
                cancelLogout();
                return true; // Consume the touch event
            }
        }
        return false; // Let other touch events pass through
    }

    private boolean isPointInsideView(float x, float y, View view) {
        // Get the absolute coordinates of the view
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        // Convert touch coordinates to be relative to the screen
        int[] rootLocation = new int[2];
        rootView.getLocationOnScreen(rootLocation);
        float screenX = x + rootLocation[0];
        float screenY = y + rootLocation[1];

        // Check if the point is inside the view's bounds
        return (screenX >= location[0] &&
                screenX <= location[0] + view.getWidth() &&
                screenY >= location[1] &&
                screenY <= location[1] + view.getHeight());
    }

    private void performLogout() {
        if (getActivity() == null) return;

        try {
            // Sign out from Firebase Auth
            mAuth.signOut();

            // Navigate to FirstActivity immediately after sign out
            Intent intent = new Intent(getActivity(), FirstActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finish current activity
            requireActivity().finish();

        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error during logout: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cancelLogout() {
        // Just go back to the previous screen
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}