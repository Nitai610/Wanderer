package com.nitai.wanderer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class SummaryActivity extends AppCompatActivity {

    // Declare all UI Elements
    TextView tvFinalDistance, tvFinalTime;
    MaterialButton btnSaveWalk, btnDiscardWalk, btnSummaryBack;

    // Variables to hold the data being displayed
    String finalDistance;
    String finalTime;
    ArrayList<LatLng> walkPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        // Hides status bars for a clean, full-screen look
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // ----------------------

        setContentView(R.layout.activity_summary);

        // Connect UI variables to XML IDs
        tvFinalDistance = findViewById(R.id.tvFinalDistance);
        tvFinalTime = findViewById(R.id.tvFinalTime);
        btnSaveWalk = findViewById(R.id.btnSaveWalk);
        btnDiscardWalk = findViewById(R.id.btnDiscardWalk);
        btnSummaryBack = findViewById(R.id.btnSummaryBack); // The new "Go Back" button

        // --- SMART LOADING LOGIC ---
        // We look inside the "Intent" mailbox to see if the Journal passed us an index number.
        // If nothing was passed, it returns -1.
        int oldWalkIndex = getIntent().getIntExtra("OLD_WALK_INDEX", -1);

        if (oldWalkIndex != -1) {
            // SCENARIO 1: VIEWING AN OLD WALK FROM THE JOURNAL

            // Go into the master list and grab the walk at this specific index
            Walk oldWalk = Walk.walkHistory.get(oldWalkIndex);

            // Extract the data from that old walk
            finalDistance = oldWalk.distance;
            finalTime = oldWalk.time;
            walkPath = oldWalk.path;

            // Hide the Save and Discard buttons, because this walk is already saved
            btnSaveWalk.setVisibility(View.GONE);
            btnDiscardWalk.setVisibility(View.GONE);
            // Show the "Go Back" button instead
            btnSummaryBack.setVisibility(View.VISIBLE);

        } else {
            // SCENARIO 2: JUST FINISHED A BRAND NEW WALK

            // Extract the live data passed directly from the TravelActivity
            finalDistance = getIntent().getStringExtra("FINAL_DISTANCE");
            finalTime = getIntent().getStringExtra("FINAL_TIME");
            walkPath = getIntent().getParcelableArrayListExtra("PATH_POINTS");

            // Ensure the Save and Discard buttons are visible
            btnSaveWalk.setVisibility(View.VISIBLE);
            btnDiscardWalk.setVisibility(View.VISIBLE);
            // Ensure the "Go Back" button remains hidden
            btnSummaryBack.setVisibility(View.GONE);
        }

        // Put the distance and time onto the screen
        tvFinalDistance.setText(finalDistance);
        tvFinalTime.setText(finalTime);

        // --- BOOT UP THE GOOGLE MAP ---
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.summaryMap);

        if (mapFragment != null) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap) {
                    drawRouteOnMap(googleMap); // Calls the drawing function below
                }
            });
        }

        // --- GO BACK BUTTON LOGIC ---
        btnSummaryBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes the summary screen and drops you safely back in the Journal
            }
        });

        // --- DISCARD BUTTON LOGIC ---
        btnDiscardWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SummaryActivity.this, "Walk Discarded", Toast.LENGTH_SHORT).show();
                finish(); // Close without saving
            }
        });

        // --- SAVE BUTTON LOGIC ---
        btnSaveWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get today's date
                String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                // CREATE WALK (Now passing 'walkPath' so the map is saved permanently!)
                Walk completedWalk = new Walk(finalDistance, finalTime, currentDate, walkPath);

                // Add to memory and save to phone
                Walk.walkHistory.add(completedWalk);
                Walk.saveHistory(SummaryActivity.this);

                Toast.makeText(SummaryActivity.this, "Saved to Journal!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    // --- THE DRAWING ENGINE ---
    // Takes the list of coordinates and paints them on the map
    private void drawRouteOnMap(GoogleMap googleMap) {
        if (walkPath != null && !walkPath.isEmpty()) {

            // Setup the line styling
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(walkPath) // Dump all the coordinates onto the map
                    .color(Color.parseColor("#1976D2")) // Blue line
                    .width(12f) // Thick line
                    .geodesic(true); // Curve to match the earth

            // Paint the line
            googleMap.addPolyline(polylineOptions);

            // Move and zoom the camera so the user can immediately see the start of the walk
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(walkPath.get(0), 16f));
        }
    }
}