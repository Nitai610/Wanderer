package com.nitai.wanderer;

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
    // EXPLANATION: Grouping related UI elements together keeps your declarations organized and easy to read.
    TextView tvStatsWeekly, tvStatsMonthly, tvStatsAllTime;
    TextView tvStatsWeeklyTime, tvStatsMonthlyTime, tvStatsAllTimeTime;
    MaterialButton btnStatsBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- TURN ON IMMERSIVE MODE ---
        // EXPLANATION: This manipulates the Android system windows to hide the top battery bar and bottom navigation buttons, matching the modern, full-screen UI of the rest of your app.
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // -----------------------------------

        setContentView(R.layout.activity_stats);

        // 2. Connect to the XML
        // EXPLANATION: Linking our empty Java variables to the actual visual components drawn on the screen.
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
                // EXPLANATION: finish() destroys this specific screen from the phone's memory, gracefully returning the user to the dashboard.
                finish(); // Closes the stats screen
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load the data from Firebase every time the screen is opened
        // EXPLANATION: Placing the fetch command inside onResume() is a brilliant lifecycle choice.
        // If the user leaves the app open, goes to the background, and then returns to the Stats screen, this ensures the numbers are always 100% up to date without needing to close and reopen the app.
        loadStatsFromFirebase();
    }

    // --- NEW: Firebase Fetching Method ---
    private void loadStatsFromFirebase() {
        // EXPLANATION: We must first check WHO is logged in so we know WHICH folder to open in the database.
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Security Check
        // EXPLANATION: Defensive programming. If the session expired or broke, we display an error and kick them out of this screen rather than letting the app crash trying to find a null user's data.
        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userEmail = currentUser.getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetching from Cloud Firestore in the background
        // EXPLANATION: We navigate to the specific user's subcollection. Using .get() initiates an asynchronous download of every document inside that folder.
        db.collection("users").document(userEmail).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 1. Clear the local RAM
                    // EXPLANATION: Crucial step! Because onResume() can run multiple times, if we don't clear the list first, we will keep adding duplicate copies of the same walks to our math engine, falsely inflating the stats.
                    Walk.walkHistory.clear();

                    // 2. Loop through the downloaded documents
                    // EXPLANATION: We open the "box" of results (queryDocumentSnapshots) and go through the JSON files one by one.
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // EXPLANATION: Firestore's magic conversion. It translates the cloud JSON fields directly into our custom Walk.java object properties.
                        Walk downloadedWalk = document.toObject(Walk.class);
                        // EXPLANATION: Saving the converted object into our phone's temporary RAM list.
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // 3. Now that data is downloaded, update the UI!
                    // EXPLANATION: We only call the math/UI update AFTER the success listener confirms the data has fully arrived from Google's servers.
                    updateStatsUI();
                })
                .addOnFailureListener(e -> {
                    // EXPLANATION: Graceful error handling. If the user has no internet, we tell them why the screen is blank instead of just freezing.
                    Toast.makeText(StatsActivity.this, "Failed to load stats from cloud", Toast.LENGTH_SHORT).show();
                });
    }

    // --- NEW: Helper method to run the math and set the text ---
    private void updateStatsUI() {
        // Grab Distance totals
        // EXPLANATION: This is excellent Object-Oriented design. Instead of writing messy date-parsing math directly in the Activity, you are calling clean, static helper methods from your Walk class.
        float weeklyTotal = Walk.calculateWeeklyDistance();
        float monthlyTotal = Walk.calculateMonthlyDistance();
        float allTimeTotal = Walk.calculateAllTimeDistance();

        // Set Distance text
        // EXPLANATION: String.format with Locale.US ensures the distance displays as "12.50 KM" with a dot, rather than a comma, which prevents formatting crashes on Hebrew/European keyboards.
        tvStatsWeekly.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        tvStatsMonthly.setText(String.format(java.util.Locale.US, "%.2f KM", monthlyTotal));
        tvStatsAllTime.setText(String.format(java.util.Locale.US, "%.2f KM", allTimeTotal));

        // Grab Time totals & Set Time text
        // EXPLANATION: The duration methods return fully formatted Strings (like "01:30:45"), so we don't need String.format here, we just pass the result directly to the TextView.
        tvStatsWeeklyTime.setText(Walk.calculateWeeklyDuration());
        tvStatsMonthlyTime.setText(Walk.calculateMonthlyDuration());
        tvStatsAllTimeTime.setText(Walk.calculateAllTimeDuration());
    }
}