package com.nitai.wanderer;

// EXPLANATION: 'import' brings in pre-written code from Android and Google so you don't have to write it from scratch.
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
// This imports the Firebase Authentication library we just added!
import com.google.firebase.auth.FirebaseAuth;

// EXPLANATION: 'public class' defines your screen. 'extends AppCompatActivity' means this class inherits all the basic abilities of a standard Android screen.
public class RegisterActivity extends AppCompatActivity {

    // EXPLANATION: Declaring variables (creating the empty boxes).
    // These are 'global' variables for this screen, meaning any part of this class can use them.
    MaterialButton btnBack, btnRegisterSubmit;
    TextView tvLoginLink;
    TextInputEditText etRegisterUsername, etRegisterPassword, etConfirmPassword;

    // NOTE: This creates a variable to hold the Firebase Authentication engine.
    // Think of mAuth as your app's personal security guard that talks to the cloud.
    private FirebaseAuth mAuth;

    // EXPLANATION: onCreate is the very first method that runs when this screen is opened.
    // It is the "setup" phase of your screen.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // EXPLANATION: super.onCreate tells the Android OS to do its default background setup for this screen first.
        super.onCreate(savedInstanceState);

        // NOTE: Immersive Mode - This hides the battery and notification bars at the top of the phone
        // to make your app look like a full-screen modern application.
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        // EXPLANATION: setContentView links this Java file to your visual XML design file.
        // It actually draws the buttons and text boxes on the screen.
        setContentView(R.layout.activity_register);

        // NOTE: Connecting our Java variables to the visual elements in the XML design.
        // EXPLANATION: R.id is a map of all your XML elements. We are finding the specific ID and putting it into our Java variable.
        btnBack = findViewById(R.id.btnBack);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // NOTE: This wakes up the Firebase security guard so it is ready to accept commands.
        // EXPLANATION: .getInstance() is a Singleton. It fetches the one, active, running copy of the Firebase engine for your app.
        mAuth = FirebaseAuth.getInstance();

        // NOTE: Simple navigation buttons. 'finish()' simply closes this screen and goes back.
        // EXPLANATION: 'v ->' is a Lambda expression. It is a modern, short way to say "When this view is clicked, do the following:"
        btnBack.setOnClickListener(v -> finish());
        tvLoginLink.setOnClickListener(v -> finish());

        // --- FIREBASE REGISTRATION LOGIC ---
        btnRegisterSubmit.setOnClickListener(v -> {

            // NOTE: .trim() is very important! It cuts off any accidental spaces
            // the user might have typed at the beginning or end of their email/password.
            // EXPLANATION: .getText() grabs the raw text, .toString() converts it to a standard Java String.
            String email = etRegisterUsername.getText().toString().trim();
            String password = etRegisterPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // NOTE: Bagrut Validation 1 - Did they leave any box completely empty?
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                // EXPLANATION: A Toast is a small pop-up message. 'LENGTH_SHORT' means it shows for about 2 seconds.
                Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return; // Stops the code here so it doesn't try to send blank info to Firebase
            }

            // NOTE: Bagrut Validation 2 - Did they make a typo in their password?
            if (!password.equals(confirmPassword)) {
                Toast.makeText(RegisterActivity.this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            // NOTE: Bagrut Validation 3 - Firebase requires passwords to be at least 6 characters long!
            if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // NOTE: This is the actual magic command. We hand the email and password to the Firebase engine.
            // It sends it to Google's servers in the background.
            mAuth.createUserWithEmailAndPassword(email, password)
                    // NOTE: This "Listener" waits patiently for Google's servers to reply.
                    // EXPLANATION: 'this' (the Context) tells Firebase this request belongs to this specific screen, preventing crashes if the screen closes early.
                    // EXPLANATION: 'task' is the box containing the final result that comes back from the internet.
                    .addOnCompleteListener(this, task -> {

                        // NOTE: 'task.isSuccessful()' asks Firebase: "Did it work?"
                        if (task.isSuccessful()) {
                            // NOTE: If yes, the user is now registered and logged in simultaneously!
                            Toast.makeText(RegisterActivity.this, "Account created!", Toast.LENGTH_SHORT).show();

                            // Move them directly to the main menu
                            // EXPLANATION: Intent is the "ticket" to change screens.
                            // Parameter 1: Where we are coming from (RegisterActivity.this)
                            // Parameter 2: Where we are going to (MainActivity.class)
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);

                            // EXPLANATION: startActivity actually executes the Intent and opens the new screen.
                            startActivity(intent);

                            // Close the register screen so they can't press the back button to return here
                            // EXPLANATION: Destroys this Activity from the phone's memory.
                            finish();
                        } else {
                            // NOTE: If it failed (e.g., they typed an invalid email format, or the email is already taken),
                            // Firebase sends back an error message. 'task.getException().getMessage()' grabs that exact error text to show the user.
                            // EXPLANATION: getException() extracts the exact reason Google rejected the registration.
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}