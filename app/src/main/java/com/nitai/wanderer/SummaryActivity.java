package com.nitai.wanderer; // Make sure this matches your package name!

// --- IMPORT SECTION ---
// These are all the tools Android needs to draw maps, format text, and save data.
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

    // --- VARIABLE DECLARATIONS ---
    TextView tvFinalDistance, tvFinalTime;
    MaterialButton btnSaveWalk, btnDiscardWalk;

    // We create variables to hold the incoming data so the whole file can see them
    String finalDistance;
    String finalTime;
    ArrayList<LatLng> walkPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        // Hides the phone's top and bottom bars so your app looks like a professional full-screen game.
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // ----------------------

        setContentView(R.layout.activity_summary);

        // --- 1. CONNECT UI TO XML ---
        tvFinalDistance = findViewById(R.id.tvFinalDistance);
        tvFinalTime = findViewById(R.id.tvFinalTime);
        btnSaveWalk = findViewById(R.id.btnSaveWalk);
        btnDiscardWalk = findViewById(R.id.btnDiscardWalk);

        // --- 2. CATCH THE INCOMING DATA ---
        // This acts like a mailbox. It opens the "Intent" sent by the TravelActivity
        // and pulls out the distance, time, and the massive list of GPS coordinates.
        finalDistance = getIntent().getStringExtra("FINAL_DISTANCE");
        finalTime = getIntent().getStringExtra("FINAL_TIME");
        walkPath = getIntent().getParcelableArrayListExtra("PATH_POINTS");

        // Put the distance and time onto the screen so the user can see them
        tvFinalDistance.setText(finalDistance);
        tvFinalTime.setText(finalTime);

        // --- 3. BOOT UP THE GOOGLE MAP ---
        // Find the empty map window we built in the XML design and wake it up
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.summaryMap);

        if (mapFragment != null) {
            // This tells Google Maps to start loading in the background.
            // When it is fully loaded and ready, it will trigger the 'onMapReady' method below.
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap) {
                    drawRouteOnMap(googleMap); // Call our custom drawing engine!
                }
            });
        }

        // --- 4. DISCARD BUTTON LOGIC ---
        btnDiscardWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If they click Discard, we don't save anything.
                // We just close this screen, which drops them back to the Main Menu.
                Toast.makeText(SummaryActivity.this, "Walk Discarded", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // --- 5. SAVE BUTTON LOGIC ---
        btnSaveWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. Figure out what today's date is (e.g., "03/05/2026")
                String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                // 2. Create a brand new Walk object with our data
                Walk completedWalk = new Walk(finalDistance, finalTime, currentDate);

                // 3. Add it to our Journal's memory list
                Walk.walkHistory.add(completedWalk);

                // 4. THE MOST IMPORTANT STEP: Save the updated list to the phone's hard drive!
                // Because of this line, if you close the app right now, the walk is still saved forever.
                Walk.saveHistory(SummaryActivity.this);

                // 5. Show a success message
                Toast.makeText(SummaryActivity.this, "Saved to Journal!", Toast.LENGTH_SHORT).show();

                // 6. Close the Summary screen and return to the Main Menu
                finish();
            }
        });
    }

    // --- THE DRAWING ENGINE ---
    // This is the code that actually paints the blue line on the Google Map
    private void drawRouteOnMap(GoogleMap googleMap) {

        // Safety check: Only draw the line if the backpack actually has GPS points inside it!
        if (walkPath != null && !walkPath.isEmpty()) {

            // 1. Set up the Paintbrush
            // A "Polyline" is Google's word for a line drawn between multiple GPS points.
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(walkPath) // Dump all our breadcrumbs onto the map
                    .color(Color.parseColor("#1976D2")) // Paint it with our App's Theme Blue
                    .width(12f) // Make the line nice and thick
                    .geodesic(true); // Makes the line curve slightly to match the curve of the Earth!

            // 2. Draw the line on the map!
            googleMap.addPolyline(polylineOptions);

            // 3. Move the camera
            // We tell the camera to instantly fly to the very first GPS point in our list (walkPath.get(0))
            // The "16f" is the zoom level. (1 is looking at the whole planet, 20 is looking at a single house).
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(walkPath.get(0), 16f));
        }
    }
}