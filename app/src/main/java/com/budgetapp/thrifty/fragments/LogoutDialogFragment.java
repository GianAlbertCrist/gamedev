package com.budgetapp.thrifty.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.budgetapp.thrifty.FirstActivity;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;

public class LogoutDialogFragment extends DialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.FullScreenDialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.logout_confirmation, container, false);
        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        Button btnYes = view.findViewById(R.id.btn_yes);
        Button btnNo = view.findViewById(R.id.btn_no);

        btnYes.setOnClickListener(v -> {
            performLogout();
            dismiss();
        });

        btnNo.setOnClickListener(v -> dismiss());

        // Allow dismissing by clicking outside the card
        view.setOnClickListener(v -> dismiss());

        // Prevent clicks on the card from dismissing the dialog
        view.findViewById(R.id.dialog_container).setOnClickListener(v -> {
            // Do nothing, just consume the click
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();

        // Clear shared preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs",
                requireActivity().MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to FirstActivity
        Intent intent = new Intent(getActivity(), FirstActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
