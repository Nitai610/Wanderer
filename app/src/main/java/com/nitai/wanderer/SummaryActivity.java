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
    // EXPLANATION: LatLng is a Google Maps object that holds two double variables: Latitude and Longitude.
    // This ArrayList acts as the "breadcrumb trail" of the entire walk.
    ArrayList<LatLng> walkPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE: Immersive Mode hides system bars to focus on the map
        // EXPLANATION: This removes the top battery bar and bottom navigation buttons, giving the Google Map a clean, edge-to-edge look.
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_summary);

        // --- CONNECTING JAVA TO XML ---
        tvFinalDistance = findViewById(R.id.tvFinalDistance);
        tvFinalTime = findViewById(R.id.tvFinalTime);
        btnSaveWalk = findViewById(R.id.btnSaveWalk);
        btnDiscardWalk = findViewById(R.id.btnDiscardWalk);
        btnContinueWalk = findViewById(R.id.btnContinueWalk);
        btnSummaryBack = findViewById(R.id.btnSummaryBack);
        layoutActiveButtons = findViewById(R.id.layoutActiveButtons);

        // --- INTENT HANDLING (BAGRUT TRICK) ---
        // We use one screen for two purposes: viewing old history or finishing a new walk.
        // EXPLANATION: getIntExtra looks into the Intent envelope for a specific number.
        // The '-1' is the "Default Value". If the previous screen didn't send an "OLD_WALK_INDEX" (meaning this is a brand new walk), Android will give us -1 instead of crashing.
        int oldWalkIndex = getIntent().getIntExtra("OLD_WALK_INDEX", -1);

        if (oldWalkIndex != -1) {
            // SCENARIO 1: Viewing an old walk from JournalActivity
            // EXPLANATION: We extract the exact walk from our local static RAM list using the index number.
            Walk oldWalk = Walk.walkHistory.get(oldWalkIndex);
            finalDistance = oldWalk.distance;
            finalTime = oldWalk.time;
            walkPath = oldWalk.getGooglePath();

            // EXPLANATION: Dynamically changing the UI. If it's an old walk, you can't "Save" or "Discard" it again, so we hide that whole row of buttons and show a simple "Back" button.
            layoutActiveButtons.setVisibility(View.GONE); // Hide Save/Discard/Continue
            btnSummaryBack.setVisibility(View.VISIBLE); // Show Back button
        } else {
            // SCENARIO 2: Just finished tracking a live walk
            // EXPLANATION: Extracting the data directly passed from the TravelActivity Intent.
            finalDistance = getIntent().getStringExtra("FINAL_DISTANCE");
            finalTime = getIntent().getStringExtra("FINAL_TIME");
            // EXPLANATION: getParcelableArrayListExtra is a special command. Standard objects (like a list of LatLng coordinates) can't easily be passed between screens.
            // "Parcelable" means Android shreds the object into primitive bytes, passes it, and rebuilds it on this screen.
            walkPath = getIntent().getParcelableArrayListExtra("PATH_POINTS");

            layoutActiveButtons.setVisibility(View.VISIBLE);
            btnSummaryBack.setVisibility(View.GONE);
        }

        // EXPLANATION: Setting the UI text regardless of whether it came from Scenario 1 or Scenario 2.
        tvFinalDistance.setText(finalDistance);
        tvFinalTime.setText(finalTime);

        // --- GOOGLE MAPS SETUP ---
        // EXPLANATION: Fragments are like "mini-activities" embedded inside a layout. SupportMapFragment is a Google-provided fragment that handles all the heavy lifting of downloading map tiles.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.summaryMap);
        if (mapFragment != null) {
            // EXPLANATION: .getMapAsync() tells Android: "Start loading the map in the background. When it's fully ready and drawn, trigger my drawRouteOnMap function."
            mapFragment.getMapAsync(googleMap -> drawRouteOnMap(googleMap));
        }

        // --- BUTTON: RESUME WALKING ---
        btnContinueWalk.setOnClickListener(v -> {
            // NOTE: Flawless Resume. We pass a flag to tell TravelActivity NOT to reset the timer
            // EXPLANATION: Passing a boolean extra lets the destination screen know it needs to behave differently (resume rather than start fresh).
            Intent intent = new Intent(SummaryActivity.this, TravelActivity.class);
            intent.putExtra("RESUME_WALK", true);
            startActivity(intent);
            finish();
        });

        // --- BUTTON: DISCARD WALK (DIALOG REQUIREMENT) ---
        btnDiscardWalk.setOnClickListener(v -> {
            // BAGRUT REQUIREMENT: Using an AlertDialog to prevent accidental data loss
            new AlertDialog.Builder(SummaryActivity.this)
                    .setTitle("Discard Walk?")
                    .setMessage("Are you sure you want to throw away this walk? This action cannot be undone.")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        resetTrackingData(); // Wipe static variables
                        Toast.makeText(SummaryActivity.this, "Walk Discarded", Toast.LENGTH_SHORT).show();

                        // CLEAR_TOP ensures we don't build a stack of open activities
                        // EXPLANATION: This is a massive memory-management win. FLAG_ACTIVITY_CLEAR_TOP looks for MainActivity in the background.
                        // Instead of opening a *second* copy of MainActivity, it destroys everything currently sitting on top of it and brings the original MainActivity to the front.
                        Intent homeIntent = new Intent(SummaryActivity.this, MainActivity.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(homeIntent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show(); // EXPLANATION: Mandatory command to physically draw the popup on the screen.
        });

        // --- BUTTON: SAVE TO CLOUD ---
        btnSaveWalk.setOnClickListener(v -> saveWalkToFirestore());

        btnSummaryBack.setOnClickListener(v -> finish());
    }
    private void saveWalkToFirestore() {
        // EXPLANATION: UX (User Experience) best practice. Disabling the button instantly stops the user from impatient double-clicking, which would upload the exact same walk twice to the database.
        btnSaveWalk.setEnabled(false); // Prevent multiple clicks/saves
        btnSaveWalk.setText("SAVING...");

        // 1. Keep the standard date for your 'Walk' object (User sees this)
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        // 2. Create a UNIQUE, Firestore-safe Document ID (Date + Time)
        // We use hyphens and underscores. NO SLASHES allowed in Firestore Document IDs!
        // EXPLANATION: In Firestore, slashes ("/") are used to define the file path (Collection / Document / Collection).
        // If a document ID contains a slash, Firestore gets confused and thinks you are trying to create a new subcollection. This avoids that crash.
        String documentId = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // BAGRUT TRICK: If display name is missing, use email prefix
        String currentUsername = user.getDisplayName();
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = user.getEmail().split("@")[0];
        }

        // EXPLANATION: Packing all the finalized variables into our custom OOP class.
        Walk completedWalk = new Walk(currentUsername, finalDistance, finalTime, currentDate, walkPath);

        // 3. Swap .add() for .document(documentId).set()
        // EXPLANATION: .add() forces Firebase to generate a random gibberish ID (like "aX8bY...").
        // Using .document(documentId).set() lets US decide the exact name of the file being saved, keeping the database extremely organized and easy to read.
        FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                .collection("walks").document(documentId).set(completedWalk)
                .addOnSuccessListener(aVoid -> { // Note: ref becomes aVoid when using .set()
                    Walk.walkHistory.add(0, completedWalk); // Add to top of local list
                    resetTrackingData(); // Clear live counters
                    finish(); // EXPLANATION: Closes the Summary screen, dropping the user cleanly back to wherever they came from.
                })
                .addOnFailureListener(e -> {
                    // Good practice: re-enable the button if the upload fails
                    // EXPLANATION: If the internet drops, we must unlock the button so they can try saving again once their connection returns.
                    btnSaveWalk.setEnabled(true);
                    btnSaveWalk.setText("SAVE");
                    Toast.makeText(SummaryActivity.this, "Error saving to cloud", Toast.LENGTH_SHORT).show();
                });
    }

    private void drawRouteOnMap(GoogleMap googleMap) {
        // EXPLANATION: Guard clause. Checks if there is actually GPS data to draw.
        if (walkPath != null && !walkPath.isEmpty()) {
            // Draw a line connecting all GPS points
            // EXPLANATION: A Polyline is simply a series of connected dots. We feed it our ArrayList, pick a color, and set the line thickness (width).
            PolylineOptions line = new PolylineOptions().addAll(walkPath).color(Color.BLUE).width(12f);
            googleMap.addPolyline(line);

            // EXPLANATION: Instantly teleports the camera directly over the very first GPS coordinate of the walk (index 0), setting the zoom level to 16 (city street level).
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(walkPath.get(0), 16f));
        }
    }

    private void resetTrackingData() {
        // We must clear static variables so the next walk starts at 0
        // EXPLANATION: Because these variables are 'static' in the TrackingService, they survive even when activities close.
        // If we don't wipe them to 0 here, the user's *next* walk will start with the distance and time of this current walk!
        TrackingService.liveDistanceInMeters = 0f;
        TrackingService.liveSecondsElapsed = 0;
        if (TrackingService.livePath != null) TrackingService.livePath.clear();
    }
}