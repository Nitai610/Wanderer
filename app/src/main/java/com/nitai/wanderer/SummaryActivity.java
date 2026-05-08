package com.nitai.wanderer;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SummaryActivity extends AppCompatActivity {

    // --- UI ELEMENTS ---
    TextView tvFinalDistance, tvFinalTime;
    MaterialButton btnSaveWalk, btnDiscardWalk, btnSummaryBack, btnContinueWalk;
    LinearLayout layoutActiveButtons;

    // --- DATA VARIABLES ---
    String finalDistance;
    String finalTime;
    String finalSteps;
    ArrayList<LatLng> walkPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE: Immersive Mode hides system bars to focus on the map
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_summary);

        tvFinalDistance = findViewById(R.id.tvFinalDistance);
        tvFinalTime = findViewById(R.id.tvFinalTime);
        btnSaveWalk = findViewById(R.id.btnSaveWalk);
        btnDiscardWalk = findViewById(R.id.btnDiscardWalk);
        btnContinueWalk = findViewById(R.id.btnContinueWalk);
        btnSummaryBack = findViewById(R.id.btnSummaryBack);
        layoutActiveButtons = findViewById(R.id.layoutActiveButtons);

        // NOTE FOR BAGRUT: Why do we check an Intent for an index?
        // ANSWER: Because we recycle this one screen for TWO different purposes to write less code!
        // Purpose 1: Viewing an old walk from the Journal list.
        // Purpose 2: Just finished walking and deciding to save or discard.
        int oldWalkIndex = getIntent().getIntExtra("OLD_WALK_INDEX", -1);

        if (oldWalkIndex != -1) {
            // ==========================================
            // SCENARIO 1: Viewing an old walk
            // ==========================================
            Walk oldWalk = Walk.walkHistory.get(oldWalkIndex); // Grab the walk from RAM
            finalDistance = oldWalk.distance;
            finalTime = oldWalk.time;

            // NOTE FOR BAGRUT: We check if steps are null. If we download a very old walk that was created
            // before we wrote the steps feature, 'oldWalk.steps' will be null. If we try to show null on screen, it crashes.
            finalSteps = (oldWalk.steps != null) ? oldWalk.steps : "0";

            walkPath = oldWalk.getGooglePath(); // Translates the HashMaps back into Google LatLng

            layoutActiveButtons.setVisibility(View.GONE); // Hide Save/Discard/Continue buttons
            btnSummaryBack.setVisibility(View.VISIBLE); // Show standard Back button
        } else {
            // ==========================================
            // SCENARIO 2: Just finished a live walk
            // ==========================================
            finalDistance = getIntent().getStringExtra("FINAL_DISTANCE");
            finalTime = getIntent().getStringExtra("FINAL_TIME");
            finalSteps = getIntent().getStringExtra("FINAL_STEPS");

            // NOTE FOR BAGRUT: EXAMINER EXPLANATION (THE CRASH FIX)
            // Instead of pulling the GPS path from the Intent (which causes TransactionTooLargeException crashes),
            // we copy the path directly from the TrackingService's static RAM memory into a new local ArrayList.
            walkPath = new ArrayList<>(TrackingService.livePath);

            layoutActiveButtons.setVisibility(View.VISIBLE); // Show Save/Discard/Continue buttons
            btnSummaryBack.setVisibility(View.GONE); // Hide standard Back button
        }

        // Put the data onto the screen
        tvFinalDistance.setText(finalDistance);
        tvFinalTime.setText(finalTime);

        // Trigger Google Maps to load
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.summaryMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> drawRouteOnMap(googleMap));
        }

        // NOTE: Flawless Resume feature. We send a boolean flag "RESUME_WALK".
        // TrackingService sees this flag and skips resetting the distance to 0!
        btnContinueWalk.setOnClickListener(v -> {
            Intent intent = new Intent(SummaryActivity.this, TravelActivity.class);
            intent.putExtra("RESUME_WALK", true);
            startActivity(intent);
            finish();
        });

        // NOTE: Discard uses an AlertDialog to prevent accidental data loss.
        btnDiscardWalk.setOnClickListener(v -> {
            new AlertDialog.Builder(SummaryActivity.this)
                    .setTitle("Discard Walk?")
                    .setMessage("Are you sure you want to throw away this walk? This action cannot be undone.")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        resetTrackingData(); // Wipe static variables so the next walk starts at 0
                        Toast.makeText(SummaryActivity.this, "Walk Discarded", Toast.LENGTH_SHORT).show();

                        // NOTE: FLAG_ACTIVITY_CLEAR_TOP ensures we don't build a massive stack of open activities.
                        // It destroys all activities between here and MainActivity.
                        Intent homeIntent = new Intent(SummaryActivity.this, MainActivity.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(homeIntent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnSaveWalk.setOnClickListener(v -> saveWalkToFirestore());

        btnSummaryBack.setOnClickListener(v -> finish());
    }

    private void saveWalkToFirestore() {
        btnSaveWalk.setEnabled(false); // NOTE: Prevent multiple clicks/saves while waiting for the server
        btnSaveWalk.setText("SAVING...");

        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // NOTE: Bagrut Trick - What if the user didn't register a custom username?
        // We split their email address at the "@" symbol. (e.g., nitai@gmail.com becomes "nitai")
        String currentUsername = user.getDisplayName();
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = user.getEmail().split("@")[0];
        }

        // NOTE: We use the constructor from Walk.java to bundle all this data into one object.
        Walk completedWalk = new Walk(currentUsername, finalDistance, finalTime, finalSteps, currentDate, walkPath);

        // Send the Walk object to the Cloud Database
        FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                .collection("walks").add(completedWalk)
                .addOnSuccessListener(ref -> {
                    // Success! Add it to the top of our local RAM list so the Journal page updates instantly.
                    Walk.walkHistory.add(0, completedWalk);
                    resetTrackingData(); // Clear live counters
                    finish(); // Close the summary screen
                });
    }

    private void drawRouteOnMap(GoogleMap googleMap) {
        if (walkPath != null && !walkPath.isEmpty()) {
            // NOTE: A Polyline is a line that connects dots. We feed it our array of LatLng points to draw the path.
            PolylineOptions line = new PolylineOptions().addAll(walkPath).color(Color.BLUE).width(12f);
            googleMap.addPolyline(line);
            // Move the camera to the first point of the walk
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(walkPath.get(0), 16f));
        }
    }

    private void resetTrackingData() {
        // NOTE: We must clear the static variables so the next walk starts exactly at 0
        TrackingService.liveDistanceInMeters = 0f;
        TrackingService.liveSecondsElapsed = 0;
        if (TrackingService.livePath != null) TrackingService.livePath.clear();
    }
}