package com.nitai.wanderer; // Keep your package name!

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

// --- NEW: Firebase Imports ---
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class StatsActivity extends AppCompatActivity {

    // 1. Declare ALL 6 UI elements
    TextView tvStatsWeekly, tvStatsMonthly, tvStatsAllTime;
    TextView tvStatsWeeklyTime, tvStatsMonthlyTime, tvStatsAllTimeTime;
    MaterialButton btnStatsBack;

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

        setContentView(R.layout.activity_stats);

        // 2. Connect to the XML
        tvStatsWeekly = findViewById(R.id.tvStatsWeekly);
        tvStatsMonthly = findViewById(R.id.tvStatsMonthly);
        tvStatsAllTime = findViewById(R.id.tvStatsAllTime);

        tvStatsWeeklyTime = findViewById(R.id.tvStatsWeeklyTime);
        tvStatsMonthlyTime = findViewById(R.id.tvStatsMonthlyTime);
        tvStatsAllTimeTime = findViewById(R.id.tvStatsAllTimeTime);
        btnStatsBack = findViewById(R.id.btnStatsBack);

        btnStatsBack.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                finish(); // Closes the stats screen
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load the data from Firebase every time the screen is opened
        loadStatsFromFirebase();
    }

    // --- NEW: Firebase Fetching Method ---
    private void loadStatsFromFirebase() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Security Check
        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userEmail = currentUser.getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetching from Cloud Firestore in the background
        db.collection("users").document(userEmail).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 1. Clear the local RAM
                    Walk.walkHistory.clear();

                    // 2. Loop through the downloaded documents
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Walk downloadedWalk = document.toObject(Walk.class);
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // 3. Now that data is downloaded, update the UI!
                    updateStatsUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StatsActivity.this, "Failed to load stats from cloud", Toast.LENGTH_SHORT).show();
                });
    }

    // --- NEW: Helper method to run the math and set the text ---
    private void updateStatsUI() {
        // Grab Distance totals
        float weeklyTotal = Walk.calculateWeeklyDistance();
        float monthlyTotal = Walk.calculateMonthlyDistance();
        float allTimeTotal = Walk.calculateAllTimeDistance();

        // Set Distance text
        tvStatsWeekly.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        tvStatsMonthly.setText(String.format(java.util.Locale.US, "%.2f KM", monthlyTotal));
        tvStatsAllTime.setText(String.format(java.util.Locale.US, "%.2f KM", allTimeTotal));

        // Grab Time totals & Set Time text
        tvStatsWeeklyTime.setText(Walk.calculateWeeklyDuration());
        tvStatsMonthlyTime.setText(Walk.calculateMonthlyDuration());
        tvStatsAllTimeTime.setText(Walk.calculateAllTimeDuration());
    }
}