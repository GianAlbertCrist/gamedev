package com.budgetapp.thrifty.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.budgetapp.thrifty.R;

public class AvatarSelectionDialogFragment extends DialogFragment {

    public interface OnAvatarSelectedListener {
        void onAvatarSelected(int avatarId, @Nullable Uri customAvatarUri);
    }

    private static final int PICK_IMAGE_REQUEST_CODE = 1001;
    private ImageView avatarCustom;
    private Uri selectedCustomAvatarUri = null;

    private OnAvatarSelectedListener listener;
    private int selectedAvatarId = 0;
    private ImageView[] avatarViews = new ImageView[9];

    public void setOnAvatarSelectedListener(OnAvatarSelectedListener listener) {
        this.listener = listener;
    }

    public static AvatarSelectionDialogFragment newInstance(int currentAvatarId) {
        AvatarSelectionDialogFragment fragment = new AvatarSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putInt("currentAvatarId", currentAvatarId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.avatar_popup_dialog, container, false);

        selectedAvatarId = getArguments() != null ? getArguments().getInt("currentAvatarId", 0) : 0;

        // Initialize avatar views
        avatarViews[0] = view.findViewById(R.id.avatar_1);
        avatarViews[1] = view.findViewById(R.id.avatar_2);
        avatarViews[2] = view.findViewById(R.id.avatar_3);
        avatarViews[3] = view.findViewById(R.id.avatar_4);
        avatarViews[4] = view.findViewById(R.id.avatar_5);
        avatarViews[5] = view.findViewById(R.id.avatar_6);
        avatarViews[6] = view.findViewById(R.id.avatar_7);
        avatarViews[7] = view.findViewById(R.id.avatar_8);
        avatarViews[8] = view.findViewById(R.id.avatar_9);

        for (int i = 0; i < avatarViews.length; i++) {
            final int avatarId = i + 1;
            avatarViews[i].setOnClickListener(v -> {
                selectedAvatarId = avatarId;
                selectedCustomAvatarUri = null; // Clear custom selection
                highlightSelected(view, avatarId);
            });
        }

        // Custom avatar (camera icon) view
        avatarCustom = view.findViewById(R.id.avatar_custom);
        if (avatarCustom != null) {
            avatarCustom.setOnClickListener(v -> {
                selectedAvatarId = 0; // Clear default avatar selection
                highlightSelected(view, 0);
                openGalleryForImage();  // This opens gallery picker
            });
        }

        view.findViewById(R.id.cancel_avatar_selection).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.confirm_avatar_selection).setOnClickListener(v -> {
            if (listener != null) {
                listener.onAvatarSelected(selectedAvatarId, selectedCustomAvatarUri);
            }
            dismiss();
        });

        highlightSelected(view, selectedAvatarId);
        return view;
    }

    private void highlightSelected(View view, int avatarId) {
        for (int i = 1; i <= 9; i++) {
            int highlightId = getResources().getIdentifier("avatar_highlight_" + i, "id", requireContext().getPackageName());
            View highlightView = view.findViewById(highlightId);
            if (highlightView != null) {
                highlightView.setVisibility(i == avatarId ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void openGalleryForImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                selectedCustomAvatarUri = selectedImageUri;
                selectedAvatarId = 0; // Clear default selection

                if (avatarCustom != null) {
                    avatarCustom.setImageURI(selectedCustomAvatarUri);
                }
            }
        }
    }
}
