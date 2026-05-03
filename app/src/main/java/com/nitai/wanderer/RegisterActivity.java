package com.nitai.wanderer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class RegisterActivity extends AppCompatActivity {

    // 1. Declare UI Elements (Matching your new XML IDs)
    MaterialButton btnBack, btnRegisterSubmit;
    TextView tvLoginLink;
    TextInputEditText etRegisterUsername, etRegisterPassword, etConfirmPassword;

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

        // CRITICAL: Set the layout BEFORE finding any views
        setContentView(R.layout.activity_register);

        // 2. Connect Java variables to XML IDs
        btnBack = findViewById(R.id.btnBack);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // 3. Floating Go Back Button Logic
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Returns to the Login screen
                finish();
            }
        });

        // 4. "Log In" Link Logic
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Also returns to the Login screen
                finish();
            }
        });

        // 5. Register Button Logic (Bagrut 5-Unit Requirement: Input Validation)
        btnRegisterSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etRegisterUsername.getText().toString().trim();
                String password = etRegisterPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                // Validation: Did they leave anything blank?
                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validation: Do the passwords match?
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Success Message (Ready for Firebase Integration later)
                Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                // Move to the Dashboard (MainActivity) to test the app flow
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);

                // finish() prevents the user from going back to the Register screen after logging in
                finish();
            }
        });
    }
}