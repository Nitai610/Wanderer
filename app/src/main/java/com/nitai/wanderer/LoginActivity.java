package com.nitai.wanderer; // Make sure this matches your package name!

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class LoginActivity extends AppCompatActivity {

    // 1. Declare UI Elements
    MaterialButton btnLogin;
    MaterialButton btnBack;
    TextView tvRegisterLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- TURN ON IMMERSIVE MODE ---
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // -----------------------------------

        // CRITICAL FIX: You MUST build the screen BEFORE finding any buttons!
        setContentView(R.layout.activity_login);

        // 2. Connect Java variables to XML IDs
        btnLogin = findViewById(R.id.btnLogin);
        btnBack = findViewById(R.id.btnBack);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        // 3. Login Button Logic (Go to the Dashboard)
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Later, we will add Firebase checks here to make sure the password is correct!

                // For now, bypass the check and go straight to the Dashboard
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);

                // finish() destroys the login screen so pressing the phone's "Back" arrow doesn't log them out
                finish();
            }
        });

        // 4. Go Back Button Logic (Go specifically to the Menu Screen)
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tell Android to open your Menu screen
                // NOTE: If your menu screen is named something else (like MenuActivity.class), change it here!
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);

                // Close the login screen so it doesn't sit in the background taking up memory
                finish();
            }
        });

        // 5. Register Link Logic (Go to Register Screen)
        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}