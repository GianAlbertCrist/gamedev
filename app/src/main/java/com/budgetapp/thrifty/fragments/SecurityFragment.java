package com.budgetapp.thrifty.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SecurityFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security, container, false);
        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        ConstraintLayout termsAndConditionsRow = view.findViewById(R.id.terms_conditions_row);

        termsAndConditionsRow.setOnClickListener(v -> showTermsAndConditions());

        return view;
    }

    private void showTermsAndConditions() {
        String terms = "THRIFTY Terms and Conditions:\n\n"
                + "1. Thrifty is a free personal budgeting app designed to help individuals track daily income, expenses, and savings.\n"
                + "2. All data is stored locally using SQLite and remotely using Firestore. Real-time notifications may use Firebase Cloud Messaging.\n"
                + "3. Users are responsible for the accuracy of their financial entries.\n"
                + "4. Passwords are securely hashed and stored remotely.\n"
                + "5. No financial advice is provided. Thrifty is a self-help budgeting tool only.\n"
                + "6. The app is online-first. Once you make your account, you can use it offline, but need internet for notifications and updates.\n"
                + "7. By using Thrifty, you agree to these terms. Continued use indicates your acceptance.";

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Terms and Conditions")
                .setMessage(terms)
                .setPositiveButton("OK", (d, which) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
                requireContext().getColor(R.color.primary_color)
        );

        Typeface poppins = ResourcesCompat.getFont(requireContext(), R.font.poppins);
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTypeface(poppins);
    }
}
