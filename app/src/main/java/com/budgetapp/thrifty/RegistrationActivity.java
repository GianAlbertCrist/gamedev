package com.budgetapp.thrifty;

                    import android.content.Intent;
                    import android.os.Bundle;
                    import android.text.SpannableString;
                    import android.text.Spanned;
                    import android.text.TextUtils;
                    import android.text.method.LinkMovementMethod;
                    import android.text.style.ClickableSpan;
                    import android.view.View;
                    import android.widget.EditText;
                    import android.widget.ImageButton;
                    import android.widget.Toast;

                    import androidx.appcompat.app.AppCompatActivity;

                    public class RegistrationActivity extends AppCompatActivity {

                        @Override
                        protected void onCreate(Bundle savedInstanceState) {
                            super.onCreate(savedInstanceState);
                            setContentView(R.layout.activity_registration);

                            // Get references to input fields and button
                            EditText firstNameInput = findViewById(R.id.first_name_input);
                            EditText surnameInput = findViewById(R.id.surname_input);
                            EditText emailInput = findViewById(R.id.email_input);
                            EditText passwordInput = findViewById(R.id.password_input);
                            EditText confirmPasswordInput = findViewById(R.id.confirm_password_input);
                            ImageButton registerButton = findViewById(R.id.register_button);

                            // Set click listener for the register button
                            registerButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Retrieve input values
                                    String firstName = firstNameInput.getText().toString().trim();
                                    String surname = surnameInput.getText().toString().trim();
                                    String email = emailInput.getText().toString().trim();
                                    String password = passwordInput.getText().toString();
                                    String confirmPassword = confirmPasswordInput.getText().toString();

                                    // Validate inputs
                                    if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(surname) ||
                                            TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ||
                                            TextUtils.isEmpty(confirmPassword)) {
                                        Toast.makeText(RegistrationActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                        Toast.makeText(RegistrationActivity.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    if (!password.equals(confirmPassword)) {
                                        Toast.makeText(RegistrationActivity.this, "Password do not match", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    if (password.length() < 6) {
                                        Toast.makeText(RegistrationActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    // Proceed with registration (e.g., send data to server or save locally)
                                    Toast.makeText(RegistrationActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                }
                            });
                            // Create a SpannableString for the text
                            String text = "Have an account already? Log in";
                            SpannableString spannableString = new SpannableString(text);

                            // Set a ClickableSpan for "Log in"
                            ClickableSpan clickableSpan = new ClickableSpan() {
                                @Override
                                public void onClick(View widget) {
                                    // Redirect to login activity
//                                    Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
//                                    startActivity(intent);
                                }
                            };

                            // Apply the ClickableSpan to "Log in"
                            int startIndex = text.indexOf("Log in");
                            int endIndex = startIndex + "Log in".length();
                            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            // Set the SpannableString to the TextView
//                            loginRedirect.setText(spannableString);
//                            loginRedirect.setMovementMethod(LinkMovementMethod.getInstance());
                        }
                        }
