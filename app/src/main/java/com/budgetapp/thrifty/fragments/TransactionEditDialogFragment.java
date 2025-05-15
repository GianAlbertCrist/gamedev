package com.budgetapp.thrifty.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.transaction.AddIncomeFragment;
import com.budgetapp.thrifty.transaction.AddExpenseFragment;
import com.budgetapp.thrifty.transaction.Transaction;
import com.budgetapp.thrifty.utils.FirestoreManager;
import android.view.Window;

public class TransactionEditDialogFragment extends DialogFragment {
    private Runnable onDismissListener;

    public void setOnDismissListener(Runnable listener) {
        this.onDismissListener = listener;
    }

    public static TransactionEditDialogFragment newInstance(Transaction transaction) {
        TransactionEditDialogFragment dialog = new TransactionEditDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable("transactionToEdit", transaction);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_transaction_edit, container, false);

        Transaction transaction = null;
        if (getArguments() != null) {
            transaction = getArguments().getParcelable("transactionToEdit");
        }

        if (transaction == null) {
            dismiss();
            return inflater.inflate(R.layout.dialog_transaction_edit, container, false);
        }

        Fragment fragment = "Income".equalsIgnoreCase(transaction.getType())
                ? new AddIncomeFragment()
                : new AddExpenseFragment();

        fragment.setArguments(getArguments());

        try {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.edit_fragment_container, fragment)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
            dismiss();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) onDismissListener.run();
    }

    public void updateTransaction(Transaction transaction) {
        FirestoreManager.updateTransaction(transaction);
        if (onDismissListener != null) {
            onDismissListener.run();
        }
        dismiss();
    }
}
