package edu.prakriti.mealmate.authentication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

import edu.prakriti.mealmate.CustomProgressDialog;
import edu.prakriti.mealmate.home.DashboardActivity;
import edu.prakriti.mealmate.home.ProfileActivity;
import edu.prakriti.mealmate.R;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText etEmail, etPassword, etCaptcha;
    private MaterialButton btnSignIn;
    private MaterialTextView forgotPassword;
    private CustomProgressDialog progressDialog;
    private MaterialTextView tvSignUp;
    private ImageView captchaImage;
    private ImageButton refreshCaptcha;
    
    // Captcha handling
    private String captchaText;
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int CAPTCHA_LENGTH = 6;
    private static final String TAG = "LoginActivity";
    
    // ReCAPTCHA site key from Google console 
    // For demo purposes, using a testing key. Ideally, this should be stored securely.
    private static final String RECAPTCHA_SITE_KEY = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth & Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressDialog = new CustomProgressDialog(this);

        // Initialize Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etCaptcha = findViewById(R.id.etCaptcha);
        btnSignIn = findViewById(R.id.btnSignIn);
        forgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
        captchaImage = findViewById(R.id.captchaImage);
        refreshCaptcha = findViewById(R.id.refreshCaptcha);

        // Generate and display captcha
        generateCaptcha();
        
        // Set refresh captcha button listener
        refreshCaptcha.setOnClickListener(v -> generateCaptcha());

        // Check if coming from signup with verification pending
        if (getIntent().getBooleanExtra("VERIFICATION_PENDING", false)) {
            String email = getIntent().getStringExtra("EMAIL");
            showVerificationAlert(email);
        }

        // Sign In Button Click
        btnSignIn.setOnClickListener(v -> loginUser(v));
        forgotPassword.setOnClickListener(v -> redirectToForgetPassword(v));
        
        // Sign Up Button Click
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }
    
    /**
     * Generates a random captcha text and displays it on the image view
     */
    private void generateCaptcha() {
        // Generate random captcha text
        captchaText = randomCaptchaText(CAPTCHA_LENGTH);
        
        // Create captcha image
        Bitmap captchaBitmap = createCaptchaImage(captchaText, captchaImage.getWidth(), captchaImage.getHeight());
        
        // Set the image
        captchaImage.setImageBitmap(captchaBitmap);
        
        // Clear the input field
        if (etCaptcha != null) {
            etCaptcha.setText("");
        }
    }
    
    /**
     * Generates a random captcha text of specified length
     */
    private String randomCaptchaText(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CAPTCHA_CHARS.length());
            sb.append(CAPTCHA_CHARS.charAt(index));
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a bitmap with the captcha text
     */
    private Bitmap createCaptchaImage(String text, int width, int height) {
        // If width or height is not valid, use default values
        if (width <= 0) width = 300;
        if (height <= 0) height = 80;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Draw background
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.rgb(245, 245, 245));
        canvas.drawRect(0, 0, width, height, backgroundPaint);
        
        // Draw noise (random lines)
        Paint noisePaint = new Paint();
        noisePaint.setAntiAlias(true);
        noisePaint.setStyle(Paint.Style.STROKE);
        noisePaint.setStrokeWidth(2);
        
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            noisePaint.setColor(Color.rgb(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            canvas.drawLine(
                    random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height),
                    noisePaint);
        }
        
        // Draw text
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(height * 0.5f);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        // Calculate text position
        float textWidth = textPaint.measureText(text);
        float startX = (width - textWidth) / 2;
        float startY = (height + textPaint.getTextSize()) / 2;
        
        // Draw each character with random color and slight rotation
        for (int i = 0; i < text.length(); i++) {
            textPaint.setColor(Color.rgb(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            
            canvas.save();
            float x = startX + textPaint.measureText(text.substring(0, i));
            float y = startY + random.nextInt(20) - 10;
            
            // Rotate the canvas slightly for each character
            canvas.rotate(random.nextInt(30) - 15, x, y);
            
            canvas.drawText(String.valueOf(text.charAt(i)), x, y, textPaint);
            canvas.restore();
        }
        
        return bitmap;
    }
    
    /**
     * Validates the captcha input against the generated captcha
     */
    private boolean validateCaptcha() {
        String userInput = etCaptcha.getText().toString().trim();
        
        if (TextUtils.isEmpty(userInput)) {
            etCaptcha.setError(getString(R.string.captcha_required));
            showSnackbar(findViewById(android.R.id.content), getString(R.string.captcha_required));
            return false;
        }
        
        if (!userInput.equals(captchaText)) {
            etCaptcha.setError(getString(R.string.captcha_invalid));
            showSnackbar(findViewById(android.R.id.content), getString(R.string.captcha_invalid));
            generateCaptcha(); // Generate a new captcha
            return false;
        }
        
        return true;
    }
    
    /**
     * Alternative implementation using Google's reCAPTCHA
     */
    private void verifyWithRecaptcha() {
        SafetyNet.getClient(this).verifyWithRecaptcha(RECAPTCHA_SITE_KEY)
                .addOnSuccessListener(this, response -> {
                    // Verification successful, proceed with login
                    String userResponseToken = response.getTokenResult();
                    if (!userResponseToken.isEmpty()) {
                        // Here you would typically send this token to your server for verification
                        // For this demo, we'll just proceed with login
                        Log.d(TAG, "reCAPTCHA verification successful");
                        proceedWithLogin();
                    }
                })
                .addOnFailureListener(this, e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        Log.d(TAG, "Error: " + apiException.getStatusCode());
                    } else {
                        Log.d(TAG, "Error: " + e.getMessage());
                    }
                    showSnackbar(findViewById(android.R.id.content), "reCAPTCHA verification failed. Please try again.");
                });
    }

    private void redirectToForgetPassword(View view) {
        goToActivity(ForgotPasswordActivity.class);
    }

    private void loginUser(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate Fields
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your email");
            showSnackbar(view, "Please enter your email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter your password");
            showSnackbar(view, "Please enter your password");
            return;
        }
        
        // Validate captcha
        if (!validateCaptcha()) {
            return;
        }

        // Show progress dialog
        progressDialog.show();

        // Sign In with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Get User ID
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if email is verified
                            if (user.isEmailVerified()) {
                                String userId = user.getUid();
                                saveUserId(userId); // Save USER_ID to SharedPreferences
                                fetchUserData(view, userId); // Fetch user profile data from Firestore
                            } else {
                                progressDialog.dismiss();
                                showVerificationAlert(user.getEmail());
                            }
                        }
                    } else {
                        // Show error message
                        progressDialog.dismiss();
                        showSnackbar(view, "Login Failed: " + task.getException().getMessage());
                        // Generate new captcha after failed login attempt
                        generateCaptcha();
                    }
                });
    }
    
    /**
     * Proceed with login after successful captcha verification
     */
    private void proceedWithLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // Show progress dialog
        progressDialog.show();

        // Sign In with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Get User ID
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if email is verified
                            if (user.isEmailVerified()) {
                                String userId = user.getUid();
                                saveUserId(userId); // Save USER_ID to SharedPreferences
                                fetchUserData(findViewById(android.R.id.content), userId); // Fetch user profile data from Firestore
                            } else {
                                progressDialog.dismiss();
                                showVerificationAlert(user.getEmail());
                            }
                        }
                    } else {
                        // Show error message
                        progressDialog.dismiss();
                        showSnackbar(findViewById(android.R.id.content), "Login Failed: " + task.getException().getMessage());
                        // Generate new captcha after failed login attempt
                        generateCaptcha();
                    }
                });
    }

    /**
     * Shows an alert dialog informing the user that email verification is required
     * and provides an option to resend the verification email
     *
     * @param email The user's email address
     */
    private void showVerificationAlert(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Verification Required");
        builder.setMessage("We've sent a verification email to " + email + ". Please verify your email to continue.");
        builder.setPositiveButton("OK", null);
        builder.setNeutralButton("Resend Email", (dialog, which) -> resendVerificationEmail(email));
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * Resends the verification email to the user
     * 
     * @param email The user's email address
     */
    private void resendVerificationEmail(String email) {
        progressDialog.show();
        
        // First sign in with email to get the user
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, etPassword.getText().toString())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        user.sendEmailVerification()
                            .addOnCompleteListener(verificationTask -> {
                                progressDialog.dismiss();
                                if (verificationTask.isSuccessful()) {
                                    showSnackbar(findViewById(android.R.id.content), 
                                        "Verification email resent successfully");
                                } else {
                                    showSnackbar(findViewById(android.R.id.content), 
                                        "Failed to resend verification email: " + 
                                        verificationTask.getException().getMessage());
                                }
                            });
                    } else {
                        progressDialog.dismiss();
                        showSnackbar(findViewById(android.R.id.content), "Error resending verification email");
                    }
                } else {
                    progressDialog.dismiss();
                    // Don't show the actual error which might reveal password info
                    showSnackbar(findViewById(android.R.id.content), 
                        "Please enter your password correctly to resend the verification email");
                }
            });
    }

    private void fetchUserData(View view, String userId) {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressDialog.dismiss();
                    if (documentSnapshot.exists()) {
                        // Extract user details
                        String name = documentSnapshot.getString("name");
                        String mobile = documentSnapshot.getString("mobile");
                        String dob = documentSnapshot.getString("dob");
                        String gender = documentSnapshot.getString("gender");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        // Save user details in SharedPreferences
                        saveUserDetails(name, mobile, dob, gender, photoUrl);

                        // Always redirect to ProfileActivity first
                        goToActivity(ProfileActivity.class);
                    } else {
                        // If no user data found, go to ProfileActivity
                        showSnackbar(view, "Profile data not found. Please complete your profile.");
                        goToActivity(ProfileActivity.class);
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    showSnackbar(view, "Error fetching user data: " + e.getMessage());
                    // Even on failure, redirect to ProfileActivity
                    goToActivity(ProfileActivity.class);
                });
    }

    private void saveUserId(String userId) {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("USER_ID", userId);
        editor.apply();
    }

    private void saveUserDetails(String name, String mobile, String dob, String gender, String photoUrl) {
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("USER_NAME", name);
        editor.putString("USER_MOBILE", mobile);
        editor.putString("USER_DOB", dob);
        editor.putString("USER_GENDER", gender);
        editor.putString("USER_PHOTO", photoUrl);
        editor.apply();
    }

    private void goToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(LoginActivity.this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showSnackbar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }
}
