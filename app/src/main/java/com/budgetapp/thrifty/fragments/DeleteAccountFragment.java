package com.budgetapp.thrifty.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.budgetapp.thrifty.FirstActivity;
import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.utils.ThemeSync;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteAccountFragment extends DialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.FullScreenDialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delete_account, container, false);
        ThemeSync.syncNotificationBarColor(getActivity().getWindow(), this.getContext());

        Button btnYesDelete = view.findViewById(R.id.yes_delete_account);
        Button btnCancel = view.findViewById(R.id.cancel_button);

        btnYesDelete.setOnClickListener(v -> showPasswordPrompt());
        btnCancel.setOnClickListener(v -> dismiss());

        view.setOnClickListener(v -> dismiss());
        view.findViewById(R.id.dialog_container).setOnClickListener(v -> {
            // consume click
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

    private void showPasswordPrompt() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_prompt, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        AlertDialog passwordDialog = builder.create();
        passwordDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        passwordDialog.show();

        Button confirmBtn = dialogView.findViewById(R.id.confirm_password_btn);
        Button cancelBtn = dialogView.findViewById(R.id.cancel_password_btn);
        EditText passwordInput = dialogView.findViewById(R.id.password_field);

        confirmBtn.setOnClickListener(view -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                passwordInput.setError("Password is required");
                return;
            }

            passwordDialog.dismiss();
            performAccountDeletion(password);
        });

        cancelBtn.setOnClickListener(view -> passwordDialog.dismiss());
    }

    private void performAccountDeletion(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (user == null) return;

        String uid = user.getUid();
        DocumentReference userDoc = db.collection("users").document(uid);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    deleteAllDocumentsInSubcollection(userDoc.collection("profile"));
                    deleteAllDocumentsInSubcollection(userDoc.collection("notifications"));
                    deleteAllDocumentsInSubcollection(userDoc.collection("tokens"));
                    deleteAllDocumentsInSubcollection(userDoc.collection("transactions"));

                    // Wait briefly to ensure deletions
                    new Handler().postDelayed(() -> {
                        userDoc.delete()
                                .addOnSuccessListener(aVoid -> {
                                    user.delete().addOnCompleteListener(deleteTask -> {
                                        if (deleteTask.isSuccessful()) {
                                            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", requireActivity().MODE_PRIVATE);
                                            prefs.edit().clear().apply();

                                            Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                            if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                                                Intent intent = new Intent(getActivity(), FirstActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                getActivity().finish();
                                            }
                                        } else {
                                            Toast.makeText(requireContext(), "Auth deletion failed: " + deleteTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), "User data deletion failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }, 1000); // Delay to allow subcollection deletion to start
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void deleteAllDocumentsInSubcollection(CollectionReference subcollection) {
        subcollection.get().addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                doc.getReference().delete();
            }
        });
    }
}