package com.budgetapp.thrifty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

public class SecurityFragment extends Fragment implements View.OnTouchListener {

    private ImageView securityClick;
    private ImageView backButton;
    private View rootView;
    private ConstraintLayout contentContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_security, container, false);

        // Initialize views
        securityClick = rootView.findViewById(R.id.securityclick);
        contentContainer = rootView.findViewById(R.id.security_container);

        // Set click listeners
        securityClick.setOnClickListener(v -> showTermsAndConditions());

        // Find the back button if it exists in the layout
        backButton = rootView.findViewById(R.id.Profile_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> navigateBack());
        }

        // Set touch listener on the root view to detect touches outside the container
        rootView.setOnTouchListener(this);

        return rootView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Only handle ACTION_DOWN events (when the user first touches the screen)
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if the touch is outside the content container
            if (contentContainer != null && !isTouchInsideView(event, contentContainer)) {
                navigateBack();
                return true; // Consume the touch event
            }
        }
        return false; // Let other touch events pass through
    }

    private boolean isTouchInsideView(MotionEvent event, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        // Check if the touch point is inside the view's bounds
        return (event.getRawX() >= x &&
                event.getRawX() <= (x + view.getWidth()) &&
                event.getRawY() >= y &&
                event.getRawY() <= (y + view.getHeight()));
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
