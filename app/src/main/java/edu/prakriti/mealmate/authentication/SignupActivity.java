package edu.prakriti.mealmate.authentication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.prakriti.mealmate.CustomProgressDialog;
import edu.prakriti.mealmate.R;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText etEmail, etChoosePassword, etConfirmPassword;
    private MaterialButton btnSignUp;
    private MaterialTextView tvSignIn;
    private CustomProgressDialog progressDialog; // Custom Progress Dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new CustomProgressDialog(this); // Initialize Progress Dialog

        // Initialize Views
        etEmail = findViewById(R.id.etEmail);
        etChoosePassword = findViewById(R.id.etChoosePassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);

        // Sign Up Button Click
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser(v);
            }
        });

        // Already Have an Account? Redirect to LoginActivity
        tvSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etChoosePassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate Fields
        if (TextUtils.isEmpty(email)) {
            showSnackbar(view, "Please enter your email");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            showSnackbar(view, "Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showSnackbar(view, "Passwords do not match");
            return;
        }

        // Show Progress Dialog
        progressDialog.show();

        // Firebase Signup
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Signup Success
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Send verification email
                            sendVerificationEmail(user, view);
                        }
                    } else {
                        // Hide Progress Dialog
                        progressDialog.dismiss();
                        // Signup Failed
                        showSnackbar(view, "Signup Failed: " + task.getException().getMessage());
                    }
                });
    }

    /**
     * Sends an email verification to the newly registered user
     * @param user Current FirebaseUser
     * @param view View for showing Snackbar messages
     */
    private void sendVerificationEmail(FirebaseUser user, View view) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    // Hide Progress Dialog
                    progressDialog.dismiss();
                    
                    if (task.isSuccessful()) {
                        // Save user ID for later use
                        saveUserId(user.getUid());
                        
                        // Show success message
                        showSnackbar(view, "Registration successful! Verification email sent to " + user.getEmail());
                        
                        // Redirect to login screen after a short delay
                        view.postDelayed(() -> {
                            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                            intent.putExtra("VERIFICATION_PENDING", true);
                            intent.putExtra("EMAIL", user.getEmail());
                            startActivity(intent);
                            finish();
                        }, 2000);
                    } else {
                        showSnackbar(view, "Failed to send verification email. Please try again.");
                    }
                });
    }

    private void saveUserId(String userId) {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("USER_ID", userId);
        editor.apply();
    }

    private void showSnackbar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }
}
