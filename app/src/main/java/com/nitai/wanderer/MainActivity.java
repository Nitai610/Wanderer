package com.nitai.wanderer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

// NOTE: Firebase imports needed for authentication and database fetching
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    // 1. Declare ALL the UI elements
    ImageButton btnProfile, btnSettings;
    View btnStartWalk, btnJournal, btnStats;
    TextView tvWeeklyDistanceMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_main);

        // 2. Connect Java to the XML IDs
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        tvWeeklyDistanceMain = findViewById(R.id.tvWeeklyDistanceMain);
        btnStartWalk = findViewById(R.id.btnStartWalk);
        btnJournal = findViewById(R.id.btnJournal);
        btnStats = findViewById(R.id.btnStats);

        // --- SECURITY & DATA LOADING ---
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // BAGRUT NOTE: Security Check. If the user somehow bypassed the login screen,
        // or their session expired, we immediately kick them back to LoginActivity.
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return; // Stop the rest of onCreate from running
        }

        // BAGRUT NOTE: Async Cloud Fetching. As soon as the app opens, we ask Firebase
        // to send us the user's entire walk history. We use getEmail() to find their specific folder.
        String userEmail = currentUser.getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userEmail).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 1. Clear the local RAM so we don't accidentally duplicate data
                    Walk.walkHistory.clear();

                    // 2. Loop through every single document downloaded from the cloud
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Firebase magically converts the JSON data back into our Java 'Walk' object!
                        Walk downloadedWalk = document.toObject(Walk.class);
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // 3. Force the screen to update the math totals now that data is loaded
                    updateWeeklyDistanceUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to sync data", Toast.LENGTH_SHORT).show();
                });

        // --- BUTTON CLICKS ---
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnStartWalk.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TravelActivity.class);
            startActivity(intent);
        });

        btnJournal.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, JournalActivity.class);
            startActivity(intent);
        });

        btnStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });
    }

    // BAGRUT NOTE: Lifecycle Management.
    // onResume() is called not just when the app opens, but EVERY TIME the user comes back
    // to this screen (for example, after finishing a walk in TravelActivity).
    @Override
    protected void onResume() {
        super.onResume();
        // Recalculate and update the Main Menu card every time the screen appears
        updateWeeklyDistanceUI();
    }

    // Helper method to keep our code clean and avoid repeating ourselves
    private void updateWeeklyDistanceUI() {
        float weeklyTotal = Walk.calculateWeeklyDistance();
        if (tvWeeklyDistanceMain != null) {
            tvWeeklyDistanceMain.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        }
    }
}