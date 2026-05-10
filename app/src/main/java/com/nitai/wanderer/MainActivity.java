package com.nitai.wanderer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    ImageButton btnProfile, btnSettings;
    View btnStartWalk, btnJournal, btnStats;
    TextView tvWeeklyDistanceMain, tvDailyStepsMain;

    // NEW: The Bridge to our Kotlin file
    HealthConnectBridge healthBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_main);

        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        tvWeeklyDistanceMain = findViewById(R.id.tvWeeklyDistanceMain);
        tvDailyStepsMain = findViewById(R.id.tvDailyStepsMain); // NEW ID
        btnStartWalk = findViewById(R.id.btnStartWalk);
        btnJournal = findViewById(R.id.btnJournal);
        btnStats = findViewById(R.id.btnStats);

        // Initialize the bridge
        healthBridge = new HealthConnectBridge(this);

        // --- SECURITY CHECK ---
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // --- CLOUD FIRESTORE FETCH (Asynchronous) ---
        String userEmail = currentUser.getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userEmail).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Walk.walkHistory.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Walk downloadedWalk = document.toObject(Walk.class);
                        Walk.walkHistory.add(downloadedWalk);
                    }
                    updateWeeklyDistanceUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to sync data", Toast.LENGTH_SHORT).show();
                });

        // --- BUTTON CLICKS ---
        btnProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        btnJournal.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, JournalActivity.class)));
        btnStats.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StatsActivity.class)));

        // --- GPS HARDWARE CHECK ---
        btnStartWalk.setOnClickListener(v -> {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = false;

            if (locationManager != null) {
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }

            if (isGpsEnabled) {
                startActivity(new Intent(MainActivity.this, TravelActivity.class));
            } else {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("GPS Required")
                        .setMessage("Your phone's GPS is currently turned off. You must enable Location Services to track a walk.")
                        .setPositiveButton("Turn On GPS", (dialog, which) -> {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    // =========================================================================
    // BAGRUT NOTE: Auto-Load Lifecycle Hook
    // We place the Health Connect call inside onResume(). This guarantees that
    // whenever the user looks at the main menu (even coming back from a walk),
    // the app silently asks Google's servers for fresh step data and updates the UI.
    // =========================================================================
    @Override
    protected void onResume() {
        super.onResume();
        updateWeeklyDistanceUI();

        if (tvDailyStepsMain != null) {
            // FIX: Changed StepsCallback to HealthCallback
            healthBridge.readTodaySteps(new HealthConnectBridge.HealthCallback() {
                @Override
                public void onSuccess(long totalSteps) {
                    tvDailyStepsMain.setText(totalSteps + " Steps");
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Fail silently so we don't annoy the user if they didn't grant permission
                    tvDailyStepsMain.setText("Settings > Connect Health");
                }
            });
        }
    }

    private void updateWeeklyDistanceUI() {
        float weeklyTotal = Walk.calculateWeeklyDistance();
        if (tvWeeklyDistanceMain != null) {
            tvWeeklyDistanceMain.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        }
    }
}