package com.nitai.wanderer;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// NEW FIREBASE IMPORTS FOR SECURITY
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ProfileActivity extends AppCompatActivity {

    TextView tvProfileUsername, tvProfileEmail;
    MaterialButton btnProfileBack, btnEditUsername;
    TextView tvProfileTotalWalks, tvProfileTotalDistance; // Added the 2 new ones

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_profile);

        // Connect UI
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileTotalWalks = findViewById(R.id.tvProfileTotalWalks);
        tvProfileTotalDistance = findViewById(R.id.tvProfileTotalDistance);
        btnProfileBack = findViewById(R.id.btnProfileBack);
        btnEditUsername = findViewById(R.id.btnEditUsername);//Cool

        // Load the Profile data immediately
        loadProfileData();

        // --- EDIT USERNAME BUTTON LOGIC ---
        btnEditUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditUsernameDialog();
            }
        });

        // --- GO BACK BUTTON ---
        btnProfileBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // --- HELPER METHOD: LOAD DATA ---
    private void loadProfileData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null && currentUser.getEmail() != null) {
            String fullEmail = currentUser.getEmail();

            // First, check if they have a saved custom Display Name
            String username = currentUser.getDisplayName();

            // If it's blank (because they are a new user), fallback to our email trick!
            if (username == null || username.isEmpty()) {
                username = fullEmail.split("@")[0];
                username = username.substring(0, 1).toUpperCase() + username.substring(1);
            }

            tvProfileUsername.setText(username);
            tvProfileEmail.setText(fullEmail);

            // --- NEW: POPULATE THE MINI STATS ---
            // 1. Total Walks is just the size of the downloaded list
            tvProfileTotalWalks.setText(String.valueOf(Walk.walkHistory.size()));

            // 2. All-Time Distance uses the math engine you already wrote
            float allTimeDistance = Walk.calculateAllTimeDistance();
            tvProfileTotalDistance.setText(String.format(java.util.Locale.US, "%.2f KM", allTimeDistance));
        }
    }

    // --- SECURE DIALOG & FIREBASE UPDATE ---
    private void showEditUsernameDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // 1. Build a custom layout for our Popup Dialog (2 text boxes)
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextInputEditText etNewUsername = new TextInputEditText(this);
        etNewUsername.setHint("Enter New Username");
        layout.addView(etNewUsername);

        TextInputEditText etPassword = new TextInputEditText(this);
        etPassword.setHint("Enter Current Password");
        // Hide the password with little dots for security!
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPassword);

        // 2. Create the Popup Dialog
        new AlertDialog.Builder(this)
                .setTitle("Change Username")
                .setMessage("For your security, please verify your password to change your username.")
                .setView(layout)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newUsername = etNewUsername.getText().toString().trim();
                        String password = etPassword.getText().toString().trim();

                        if (newUsername.isEmpty() || password.isEmpty()) {
                            Toast.makeText(ProfileActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 3. SECURITY CHECK: Authenticate with Firebase using their password
                        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

                        // Tell Firebase to verify this password against their database
                        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                // 4. Password was correct! Now we update the Display Name.
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(newUsername)
                                        .build();

                                currentUser.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ProfileActivity.this, "Username updated!", Toast.LENGTH_SHORT).show();
                                        loadProfileData(); // Refresh the screen with the new name!
                                    }
                                });

                            } else {
                                // Password was wrong
                                Toast.makeText(ProfileActivity.this, "Incorrect password. Try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}