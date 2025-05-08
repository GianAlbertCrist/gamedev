package com.budgetapp.thrifty;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class FirstActivity extends AppCompatActivity {

    private ImageView signInButton, registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);


        signInButton = findViewById(R.id.sign_in_button);
        registerButton = findViewById(R.id.register_button);

        signInButton.setOnClickListener(v -> {
            Intent intent = new Intent(FirstActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(FirstActivity.this, RegistrationActivity.class);
            startActivity(intent);
        });
    }
}