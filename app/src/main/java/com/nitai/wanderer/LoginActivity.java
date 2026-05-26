package com.nitai.wanderer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
// This imports the Firebase Authentication library
import com.google.firebase.auth.FirebaseAuth;

// EXPLANATION: LoginActivity is the gateway to your app. It handles verifying returning users.
public class LoginActivity extends AppCompatActivity {

    // EXPLANATION: Declaring the UI variables globally so they can be accessed anywhere within this class.
    MaterialButton btnLogin, btnBack;
    TextView tvRegisterLink;
    TextInputEditText etUsername, etPassword;

    // NOTE: Declaring the Firebase Authentication engine variable again for this screen.
    // EXPLANATION: This creates an empty reference variable that will soon hold the active Firebase session manager.
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE: Immersive Mode (same as before)
        // EXPLANATION: This hides the Android system UI (battery bar, time, navigation buttons) to give the app a modern, full-screen look.
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        // EXPLANATION: Connects this Java logic file to the visual layout file (activity_login.xml).
        setContentView(R.layout.activity_login);

        // NOTE: Connecting Java variables to XML elements
        // EXPLANATION: findViewById searches the XML layout for the specific ID and links it to our Java variable so we can control it via code.
        btnLogin = findViewById(R.id.btnLogin);
        btnBack = findViewById(R.id.btnBack);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);

        // NOTE: Waking up the Firebase engine for the Login screen
        // EXPLANATION: .getInstance() uses the Singleton pattern. It fetches the one, single active instance of the Firebase engine running in your app.
        mAuth = FirebaseAuth.getInstance();

        btnBack.setOnClickListener(v -> {
            // EXPLANATION: An Intent is a formal request to the Android OS to transition from one screen to another.
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            // EXPLANATION: finish() destroys this specific screen from the device's RAM, ensuring a clean back-stack.
            finish();
        });

        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // --- FIREBASE LOGIN LOGIC ---
        // EXPLANATION: A Lambda expression (v ->) that defines exactly what happens when the login button is clicked.
        btnLogin.setOnClickListener(v -> {

            // NOTE: Grab what the user typed and remove accidental spaces
            // EXPLANATION: .trim() is a crucial sanitization method. It removes any invisible leading or trailing spaces the user might have accidentally typed.
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // NOTE: Validation - Don't bother the Firebase server if the boxes are empty
            // EXPLANATION: Local Validation. This acts as a gatekeeper, preventing the app from wasting battery and time making a network request if the inputs are obviously invalid.
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return; // Stop the code here. EXPLANATION: 'return' acts as a hard stop, preventing the rest of the method from executing.
            }

            // NOTE: The magic login command! We ask Firebase to check if this email/password combination exists in our database.
            // EXPLANATION: This initiates an asynchronous (background) network call to Google's servers.
            mAuth.signInWithEmailAndPassword(email, password)
                    // NOTE: Wait patiently for the server to check the database...
                    // EXPLANATION: 'this' ensures the listener is tied to the lifecycle of this specific screen. 'task' holds the final result of the internet request.
                    .addOnCompleteListener(this, task -> {

                        // NOTE: Did the email and password match perfectly?
                        // EXPLANATION: task.isSuccessful() returns true only if the server confirmed the credentials match an existing account.
                        if (task.isSuccessful()) {
                            // NOTE: Success! The user is now authenticated.
                            Toast.makeText(LoginActivity.this, "Welcome Back!", Toast.LENGTH_SHORT).show();

                            // Move them to the main dashboard
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);

                            // Destroy the login screen so pressing the phone's back arrow doesn't log them out
                            // EXPLANATION: This is excellent UX (User Experience) design. Once logged in, the login screen should cease to exist in the back history.
                            finish();
                        } else {
                            // NOTE: Failed! They either typed the wrong password, or the account doesn't exist.
                            // We use a generic error message here so hackers don't know if they guessed a correct email or not.
                            // EXPLANATION: This is a widely used cyber-security best practice. It prevents "Email Enumeration", where a hacker could figure out who uses your app based on specific error messages.
                            Toast.makeText(LoginActivity.this, "Login Failed. Check spellnigs.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}