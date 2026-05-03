package com.nitai.wanderer; // Make sure this matches your package name!

// --- IMPORT SECTION ---
// These are all the tools Android needs to make this screen work.
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class TravelActivity extends AppCompatActivity implements OnMapReadyCallback {

    // --- VARIABLE DECLARATIONS ---

    // 1. Map & GPS Tools
    private GoogleMap mMap; // The actual Google Map
    private FusedLocationProviderClient fusedLocationClient; // Google's tool for getting your GPS location
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100; // A secret code we use to ask for GPS permission

    // 2. UI Elements (What the user sees and clicks)
    ImageButton btnCancelWalk;
    MaterialButton btnStopWalk;
    TextView tvLiveDistance, tvLiveTimer;

    // 3. The Breadcrumb Backpack!
    // This is an infinitely expanding list that holds every single GPS coordinate you step on.
    // We will pack this up and send it to the Summary screen later to draw the blue line.
    ArrayList<LatLng> pathPoints = new ArrayList<>();

    // 4. Tracking Variables (Math stuff)
    private LocationCallback locationCallback; // The "listener" that waits for GPS updates
    private Location lastKnownLocation = null; // Remembers where you were 3 seconds ago to calculate distance
    private float totalDistanceInMeters = 0f; // The running total of your distance

    // 5. Timer Variables
    private int secondsElapsed = 0; // Total seconds walked
    private Handler timerHandler = new Handler(); // A background worker that ticks every second
    private boolean isTracking = false; // A simple switch to turn the timer and GPS on/off

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        // This block hides the phone's battery bar and back buttons so the map takes up 100% of the screen.
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // ----------------------

        // Connect this Java file to your XML design
        setContentView(R.layout.activity_travel);

        // --- CONNECT UI TO XML ---
        btnCancelWalk = findViewById(R.id.btnCancelWalk);
        btnStopWalk = findViewById(R.id.btnStopWalk);
        tvLiveDistance = findViewById(R.id.tvLiveDistance);
        tvLiveTimer = findViewById(R.id.tvLiveTimer);

        // Wake up the GPS tool
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Wake up the Map window and tell it to get ready
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // When the map is ready, it triggers onMapReady() below
        }

        // Build the engine that handles new GPS locations (but don't start it yet)
        setupLocationCallback();

        // --- BUTTON: CANCEL WALK ---
        btnCancelWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTracking(); // Turn off the GPS so it doesn't drain the battery
                finish(); // Destroy this screen and go back
            }
        });

        // --- BUTTON: STOP WALK ---
        btnStopWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTracking(); // Stops the GPS and Timer immediately

                // Grab the final math numbers directly off the screen
                String finalDistance = tvLiveDistance.getText().toString();
                String finalTime = tvLiveTimer.getText().toString();

                // Create the "Intent" ticket to go to the Summary Screen
                Intent intent = new Intent(TravelActivity.this, SummaryActivity.class);

                // Pack the simple text data into the Intent
                intent.putExtra("FINAL_DISTANCE", finalDistance);
                intent.putExtra("FINAL_TIME", finalTime);

                // Pack the massive list of breadcrumbs!
                // 'putParcelableArrayListExtra' safely packs our complex list of coordinates into the Intent.
                intent.putParcelableArrayListExtra("PATH_POINTS", pathPoints);

                // Start the Summary screen and destroy this Travel screen
                startActivity(intent);
                finish();
            }
        });
    }

    // --- MAP IS READY ---
    // This runs automatically exactly when Google Maps finishes loading on the screen.
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Check if the user has already given us permission to use their GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation(); // We have permission, start tracking!
        } else {
            // We don't have permission, so ask the user with a popup box
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // --- START TRACKING ---
    // Turns on the blue dot, starts the GPS pings, and starts the clock.
    private void enableUserLocation() {
        // A safety check just to be 100% sure we have permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true); // Turns on the blue "You are here" dot

        // 1. Tell Google Maps how often we want updates (High Accuracy, ping every ~3 seconds)
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        // 2. Start the live GPS tracking engine
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // 3. Flip the switch and start the timer!
        isTracking = true;
        startTimer();
    }

    // --- THE GPS ENGINE ---
    // This is the brain of the walk. It runs every time the phone gets a new GPS ping.
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return; // Failsafe if GPS glitches

                for (Location location : locationResult.getLocations()) {
                    // 1. Get the exact coordinate of this ping
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // 2. Move the camera to smoothly follow the user
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));

                    // 3. SAVING THE BREADCRUMB
                    // Drop this exact coordinate into our backpack so we can draw the map later!
                    pathPoints.add(currentLatLng);

                    // 4. Calculate distance
                    if (lastKnownLocation != null) {
                        // Check how far we moved since the last ping (3 seconds ago)
                        float distanceWalked = lastKnownLocation.distanceTo(location);
                        totalDistanceInMeters += distanceWalked; // Add it to the total

                        // Math: Convert meters to Kilometers (e.g., 1500m -> 1.5km)
                        float distanceInKm = totalDistanceInMeters / 1000f;

                        // Update the big text box on the screen. (Locale.US forces it to use a dot instead of a comma for decimals)
                        tvLiveDistance.setText(String.format(java.util.Locale.US, "%.2f KM", distanceInKm));
                    }

                    // Save this current spot to memory, so next time it pings, it has something to compare against
                    lastKnownLocation = location;
                }
            }
        };
    }

    // --- THE TIMER ENGINE ---
    // A loop that counts seconds and updates the clock on the screen
    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isTracking) { // Only count if we haven't hit STOP
                    secondsElapsed++; // Add 1 second

                    // Math to figure out minutes and seconds
                    int minutes = secondsElapsed / 60;
                    int seconds = secondsElapsed % 60;

                    // Format as "00:00" and put it on the screen
                    tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));

                    // Tell this block of code to run itself again in exactly 1000 milliseconds (1 second)
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000); // Start the very first tick after 1000 milliseconds
    }

    // --- SHUTDOWN SEQUENCE ---
    // Extremely important: Stops the GPS from pinging after the screen closes so we don't kill the battery
    private void stopTracking() {
        isTracking = false; // Turns off the timer loop
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback); // Tells Google to stop sending GPS pings
        }
    }

    // --- SAFETY NET ---
    // If the user forcibly closes the app by swiping it away, this guarantees the GPS turns off
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTracking();
    }

    // --- PERMISSION RESULT ---
    // If the user clicks "Allow" on the GPS popup box, this catches their answer and starts the walk
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation(); // They clicked Yes! Start tracking.
            }
        }
    }
}