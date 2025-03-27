package edu.prakriti.mealmate.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import edu.prakriti.mealmate.R;
import edu.prakriti.mealmate.authentication.LoginActivity;
import edu.prakriti.mealmate.authentication.SignupActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Buttons
        MaterialButton signUpButton = findViewById(R.id.signUpButton);
        MaterialButton loginButton = findViewById(R.id.loginButton);

        // Redirect to SignupActivity
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        // Redirect to LoginActivity
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
