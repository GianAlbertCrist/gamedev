package com.budgetapp.thrifty;

import android.media.Image;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;


public class TransactionsFragment extends Fragment {

    private TextView todayLabel;
    private ImageView dropdownIcon;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        todayLabel = view.findViewById(R.id.todayLabel);
        ImageView dropdownIcon = view.findViewById(R.id.dropdown_icon);

        dropdownIcon.setOnClickListener(v -> showPopupMenu(v));

        return view;
    }

    private void toggleDropdown(View view) {

        if (dropdownIcon.getTag() == null || dropdownIcon.getTag().equals("next")) {
            dropdownIcon.setImageResource(R.drawable.ic_down);
            dropdownIcon.setTag("dropdown");
            showPopupMenu(view);
        } else {
            dropdownIcon.setImageResource(R.drawable.ic_next);
            dropdownIcon.setTag("next");
        }
    }

    private void showPopupMenu(View view) {

        PopupMenu popupMenu = new PopupMenu(getContext(), view);


        popupMenu.getMenu().add(0, 1, 0, "Today");
        popupMenu.getMenu().add(0, 2, 0, "Days");
        popupMenu.getMenu().add(0, 3, 0, "Weeks");
        popupMenu.getMenu().add(0, 4, 0, "Months");


        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    todayLabel.setText("Today");
                    return true;
                case 2:
                    todayLabel.setText("Days");
                    return true;
                case 3:
                    todayLabel.setText("Weeks");
                    return true;
                case 4:
                    todayLabel.setText("Months");
                    return true;
                default:
                    return false;
            }
        });


        popupMenu.show();
    }
}