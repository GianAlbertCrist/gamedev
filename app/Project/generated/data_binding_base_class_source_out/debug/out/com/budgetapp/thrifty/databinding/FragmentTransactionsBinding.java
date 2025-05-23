// Generated by view binder compiler. Do not edit!
package com.budgetapp.thrifty.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.budgetapp.thrifty.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentTransactionsBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final ImageView dropdownIcon;

  @NonNull
  public final LinearLayout filterButton;

  @NonNull
  public final TextView todayLabel;

  @NonNull
  public final ConstraintLayout topProfilebar;

  @NonNull
  public final LinearLayout transactionListContainer;

  private FragmentTransactionsBinding(@NonNull ConstraintLayout rootView,
      @NonNull ImageView dropdownIcon, @NonNull LinearLayout filterButton,
      @NonNull TextView todayLabel, @NonNull ConstraintLayout topProfilebar,
      @NonNull LinearLayout transactionListContainer) {
    this.rootView = rootView;
    this.dropdownIcon = dropdownIcon;
    this.filterButton = filterButton;
    this.todayLabel = todayLabel;
    this.topProfilebar = topProfilebar;
    this.transactionListContainer = transactionListContainer;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentTransactionsBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentTransactionsBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_transactions, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentTransactionsBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.dropdown_icon;
      ImageView dropdownIcon = ViewBindings.findChildViewById(rootView, id);
      if (dropdownIcon == null) {
        break missingId;
      }

      id = R.id.filter_button;
      LinearLayout filterButton = ViewBindings.findChildViewById(rootView, id);
      if (filterButton == null) {
        break missingId;
      }

      id = R.id.todayLabel;
      TextView todayLabel = ViewBindings.findChildViewById(rootView, id);
      if (todayLabel == null) {
        break missingId;
      }

      id = R.id.topProfilebar;
      ConstraintLayout topProfilebar = ViewBindings.findChildViewById(rootView, id);
      if (topProfilebar == null) {
        break missingId;
      }

      id = R.id.transactionListContainer;
      LinearLayout transactionListContainer = ViewBindings.findChildViewById(rootView, id);
      if (transactionListContainer == null) {
        break missingId;
      }

      return new FragmentTransactionsBinding((ConstraintLayout) rootView, dropdownIcon,
          filterButton, todayLabel, topProfilebar, transactionListContainer);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
