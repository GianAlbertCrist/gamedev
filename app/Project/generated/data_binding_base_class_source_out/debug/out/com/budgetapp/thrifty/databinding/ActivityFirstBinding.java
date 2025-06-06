// Generated by view binder compiler. Do not edit!
package com.budgetapp.thrifty.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.budgetapp.thrifty.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityFirstBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final ConstraintLayout first;

  @NonNull
  public final ImageView giveMoney;

  @NonNull
  public final Button registerButton;

  @NonNull
  public final Button signInButton;

  @NonNull
  public final Guideline topGuideline;

  @NonNull
  public final ImageView tstImg;

  private ActivityFirstBinding(@NonNull ConstraintLayout rootView, @NonNull ConstraintLayout first,
      @NonNull ImageView giveMoney, @NonNull Button registerButton, @NonNull Button signInButton,
      @NonNull Guideline topGuideline, @NonNull ImageView tstImg) {
    this.rootView = rootView;
    this.first = first;
    this.giveMoney = giveMoney;
    this.registerButton = registerButton;
    this.signInButton = signInButton;
    this.topGuideline = topGuideline;
    this.tstImg = tstImg;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityFirstBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityFirstBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_first, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityFirstBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      ConstraintLayout first = (ConstraintLayout) rootView;

      id = R.id.give_money;
      ImageView giveMoney = ViewBindings.findChildViewById(rootView, id);
      if (giveMoney == null) {
        break missingId;
      }

      id = R.id.register_button;
      Button registerButton = ViewBindings.findChildViewById(rootView, id);
      if (registerButton == null) {
        break missingId;
      }

      id = R.id.sign_in_button;
      Button signInButton = ViewBindings.findChildViewById(rootView, id);
      if (signInButton == null) {
        break missingId;
      }

      id = R.id.topGuideline;
      Guideline topGuideline = ViewBindings.findChildViewById(rootView, id);
      if (topGuideline == null) {
        break missingId;
      }

      id = R.id.tst_img;
      ImageView tstImg = ViewBindings.findChildViewById(rootView, id);
      if (tstImg == null) {
        break missingId;
      }

      return new ActivityFirstBinding((ConstraintLayout) rootView, first, giveMoney, registerButton,
          signInButton, topGuideline, tstImg);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
