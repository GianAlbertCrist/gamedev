package com.budgetapp.thrifty.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.transaction.Transaction;

public class DescriptionDialogFragment extends DialogFragment {

    private static final String ARG_TRANSACTION = "transaction";

    public static DescriptionDialogFragment newInstance(Transaction transaction) {
        DescriptionDialogFragment fragment = new DescriptionDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TRANSACTION, transaction);  // Pass the selected transaction
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = super.onCreateDialog(savedInstanceState);


        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);  // Remove the background

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_description_popup, container, false);


        TextView categoryTextView = view.findViewById(R.id.transaction_detail_category);
        TextView descriptionTextView = view.findViewById(R.id.transaction_detail_description);

        // Get the selected transaction from the arguments
        Transaction transaction = getArguments().getParcelable(ARG_TRANSACTION);

        if (transaction != null) {
            // Set the transaction details to the TextViews
            categoryTextView.setText(transaction.getCategory());
            descriptionTextView.setText(transaction.getDescription());  // Display the description
        }

        return view;
    }
}
