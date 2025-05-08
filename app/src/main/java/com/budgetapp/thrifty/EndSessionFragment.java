package com.budgetapp.thrifty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class EndSessionFragment extends Fragment implements View.OnTouchListener {

    private Button yesEndSessionButton;
    private Button cancelButton;
    private View rootView;
    private CardView dialogContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_endsession, container, false);

        // Initialize buttons
        yesEndSessionButton = rootView.findViewById(R.id.yes_end_session);
        cancelButton = rootView.findViewById(R.id.cancel_button);
        dialogContainer = rootView.findViewById(R.id.dialog_container);

        // Set click listeners
        yesEndSessionButton.setOnClickListener(v -> performLogout());
        cancelButton.setOnClickListener(v -> cancelLogout());

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

            // Check if the touch is outside the dialog container
            if (dialogContainer != null && !isPointInsideView(x, y, dialogContainer)) {
                // Check if the touch is in the middle section (not header or footer)
                int[] rootLocation = new int[2];
                rootView.getLocationOnScreen(rootLocation);
                float screenY = y + rootLocation[1];

                // Get header and footer heights (approximate)
                int headerHeight = rootView.findViewById(R.id.topEndSessionBar).getHeight();
                int footerHeight = 150; // Approximate height of the bottom navigation
                int screenHeight = rootView.getHeight();

                // Check if touch is in the middle section
                if (screenY > headerHeight && screenY < (screenHeight - footerHeight)) {
                    cancelLogout();
                    return true; // Consume the touch event
                }
            }
        }
        return false; // Let other touch events pass through
    }

    private boolean isPointInsideView(float x, float y, View view) {
        // Get the absolute coordinates of the view
        int[] location = new int[2];
        view.getLocationInWindow(location);

        // Check if the point is inside the view's bounds
        return (x >= location[0] &&
                x <= location[0] + view.getWidth() &&
                y >= location[1] &&
                y <= location[1] + view.getHeight());
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
