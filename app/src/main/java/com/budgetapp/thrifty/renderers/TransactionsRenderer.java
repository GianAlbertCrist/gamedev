package com.budgetapp.thrifty.renderers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.budgetapp.thrifty.R;
import com.budgetapp.thrifty.handlers.TransactionsHandler;
import com.budgetapp.thrifty.transaction.Transaction;

public class TransactionsRenderer {

    private Context context;
    private LinearLayout container;

    public TransactionsRenderer(Context context, View view) {
        this.context = context;
        this.container = view.findViewById(R.id.home_transactions); // find the container from the view
    }

    public void initTransactions(Context context) {
        this.context = context;
    }

    public void setUpHome() {
        for (Transaction t : TransactionsHandler.transactions) {
            ConstraintLayout card = new ConstraintLayout(context);
            card.setId(View.generateViewId());
            card.setBackground(ContextCompat.getDrawable(context, R.drawable.transaction_container));
            card.setPadding(24, 24, 24, 24);

            ConstraintLayout.LayoutParams cardParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(20, 24, 20, 24);
            card.setLayoutParams(cardParams);

            // Icon
            ImageView icon = new ImageView(context);
            icon.setId(View.generateViewId());
            icon.setImageResource(t.getIconID());

            ConstraintLayout.LayoutParams iconParams = new ConstraintLayout.LayoutParams(80, 80);
            iconParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            iconParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            iconParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            icon.setLayoutParams(iconParams);

            card.addView(icon);

            // Title
            TextView title = new TextView(context);
            title.setId(View.generateViewId());
            title.setText(t.getCategory());
            title.setTextSize(16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(Color.BLACK);

            ConstraintLayout.LayoutParams titleParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            titleParams.startToEnd = icon.getId();
            titleParams.topToTop = icon.getId();
            titleParams.leftMargin = 16;
            title.setLayoutParams(titleParams);

            card.addView(title);

            // Amount
            TextView amount = new TextView(context);
            amount.setId(View.generateViewId());
            amount.setText(t.getAmount());
            amount.setTextSize(16);
            amount.setTypeface(Typeface.DEFAULT_BOLD);
            amount.setTextColor(Color.parseColor("#2E7D32")); // dark green

            ConstraintLayout.LayoutParams amountParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            amountParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            amountParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            amount.setLayoutParams(amountParams);

            card.addView(amount);

            // Datetime
            TextView datetime = new TextView(context);
            datetime.setId(View.generateViewId());
            datetime.setText(t.getDateAndTime());
            datetime.setTextSize(12);
            datetime.setTextColor(Color.GRAY);

            ConstraintLayout.LayoutParams datetimeParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            datetimeParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            datetimeParams.topToBottom = amount.getId();
            datetimeParams.topMargin = 4;
            datetime.setLayoutParams(datetimeParams);

            card.addView(datetime);

            // Finally add the card to container
            container.addView(card);
        }
    }
}
