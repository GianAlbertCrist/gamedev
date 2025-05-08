package com.budgetapp.thrifty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class EndSessionFragment extends Fragment implements View.OnTouchListener {

    private LinearLayout endSessionButton;
    private LinearLayout cancelLogoutButton;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_endsession, container, false);

        // Initialize buttons
        endSessionButton = rootView.findViewById(R.id.endsession);
        cancelLogoutButton = rootView.findViewById(R.id.cancelLogout);

        // Set click listeners
        endSessionButton.setOnClickListener(v -> performLogout());
        cancelLogoutButton.setOnClickListener(v -> cancelLogout());

        // Set touch listener on the root view to detect touches outside the container
        rootView.setOnTouchListener(this);

        return rootView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Only handle ACTION_DOWN events (when the user first touches the screen)
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Get the coordinates of the touch event
            float x = event.getX();
            float y = event.getY();

            // Get the dialog container view
            View dialogContainer = (View) rootView.findViewById(R.id.endsession).getParent();

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
        // Here you would typically clear user session data, preferences, etc.
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate back to login screen or close the app
        // For now, just go back to the home screen
        requireActivity().getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Replace with the HomeFragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, new HomeFragment())
                .commit();
    }

    private void cancelLogout() {
        // Just go back to the previous screen
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
