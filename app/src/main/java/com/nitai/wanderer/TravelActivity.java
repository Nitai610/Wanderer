package com.nitai.wanderer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

public class TravelActivity extends AppCompatActivity implements OnMapReadyCallback {

    // --- VARIABLES ---
    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // UI Elements
    ImageButton btnCancelWalk;
    MaterialButton btnStopWalk;
    TextView tvLiveDistance, tvLiveTimer;

    // --- THE RADIO RECEIVER ---
    // This listens for the "UPDATE_UI_BROADCAST" message sent by the TrackingService every second
    private BroadcastReceiver uiUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateScreenFromService(); // Refresh the numbers and the map camera
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        // Hides the phone's battery bar and navigation buttons for a full-screen map
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_travel);

        // Connect variables to XML IDs
        btnCancelWalk = findViewById(R.id.btnCancelWalk);
        btnStopWalk = findViewById(R.id.btnStopWalk);
        tvLiveDistance = findViewById(R.id.tvLiveDistance);
        tvLiveTimer = findViewById(R.id.tvLiveTimer);

        // Make sure the bottom button is set as a red STOP button immediately
        btnStopWalk.setText("STOP WALK");
        btnStopWalk.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
        btnStopWalk.setIconResource(android.R.drawable.ic_media_pause);

        // Wake up the Map Fragment and get it ready
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // Triggers onMapReady() when finished loading
        }

        // --- CANCEL BUTTON LOGIC (Top Left 'X') ---
        btnCancelWalk.setOnClickListener(v -> {
            stopTrackingService(); // Kill the background engine
            finish(); // Close the screen without saving
        });

        // --- STOP WALK BUTTON LOGIC ---
        btnStopWalk.setOnClickListener(v -> {
            // 1. Grab the final numbers straight from the screen
            String finalDistance = tvLiveDistance.getText().toString();
            String finalTime = tvLiveTimer.getText().toString();

            // 2. Pack the numbers AND the map path into an Intent
            Intent intent = new Intent(TravelActivity.this, SummaryActivity.class);
            intent.putExtra("FINAL_DISTANCE", finalDistance);
            intent.putExtra("FINAL_TIME", finalTime);
            intent.putParcelableArrayListExtra("PATH_POINTS", TrackingService.livePath);

            // 3. Stop the engine and go to the Summary screen
            stopTrackingService();
            startActivity(intent);
            finish();
        });
    }

    // --- MAP INITIALIZATION ---
    // This runs the exact second Google Maps finishes loading on the screen
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // AUTO-START LOGIC:
        if (!TrackingService.isServiceRunning) {
            // If the service isn't running yet, ask for permissions and start the walk!
            checkPermissionsAndStart();
        } else {
            // If the service IS running (e.g., you turned the screen off and back on),
            // just make sure the blue dot is visible again.
            enableBlueDotSafely();
        }
    }

    // --- PERMISSIONS LOGIC ---
    private void checkPermissionsAndStart() {
        // Check if we are missing GPS or Notification permissions
        boolean missingLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean missingNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;

        if (missingLocation || missingNotifications) {
            // We are missing permissions, ask the user!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            // We already have permissions! Start the walk instantly.
            executeStartWalk();
        }
    }

    // Catches the user's answer when they click "Allow" or "Deny" on the permission popup
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                executeStartWalk(); // They clicked Allow, start the walk!
            } else {
                Toast.makeText(this, "Location permission is required to track your walk.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- STARTING THE WALK ---
    private void executeStartWalk() {
        enableBlueDotSafely(); // Turn on the blue dot
        startTrackingService(); // Start the background GPS engine
    }

    // --- THE BLUE DOT FIX ---
    private void enableBlueDotSafely() {
        // Double-check permissions just to be safe
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && mMap != null) {

            // 1. Actually turn the Blue Dot ON
            mMap.setMyLocationEnabled(true);

            // 2. FIX THE OVERLAP ISSUE
            // This adds 500 pixels of "invisible padding" to the bottom of the map.
            // It forces the camera to center higher up, so the blue dot isn't hiding under your white stats card!
            mMap.setPadding(0, 0, 0, 500);
        }
    }

    // --- SERVICE CONTROLS ---
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent); // Android 8.0+ requires this
        } else {
            startService(serviceIntent);
        }
    }

    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        stopService(serviceIntent);
        TrackingService.isServiceRunning = false;
    }

    // --- UPDATE THE UI ---
    // This is called every 1 second by the radio receiver
    private void updateScreenFromService() {
        if (!TrackingService.isServiceRunning) return;

        // 1. Update the Distance Text
        float distanceInKm = TrackingService.liveDistanceInMeters / 1000f;
        tvLiveDistance.setText(String.format(java.util.Locale.US, "%.2f KM", distanceInKm));

        // 2. Update the Timer Text
        int minutes = TrackingService.liveSecondsElapsed / 60;
        int seconds = TrackingService.liveSecondsElapsed % 60;
        tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));

        // 3. Move the Camera
        if (!TrackingService.livePath.isEmpty() && mMap != null) {
            LatLng latestSpot = TrackingService.livePath.get(TrackingService.livePath.size() - 1);
            // Gently pan the camera to follow the user's latest location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestSpot, 17f));
        }
    }

    // --- LIFECYCLE METHODS ---
    @Override
    protected void onResume() {
        super.onResume();
        // Turn the radio receiver ON when the screen is visible.
        // The ContextCompat.RECEIVER_NOT_EXPORTED tag is the Android 14 security fix to stop crashes!
        ContextCompat.registerReceiver(this, uiUpdateReceiver, new IntentFilter("UPDATE_UI_BROADCAST"), ContextCompat.RECEIVER_NOT_EXPORTED);
        updateScreenFromService(); // Catch up on any math that happened while the phone was asleep
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Turn the radio receiver OFF when the screen closes (saves battery).
        // Note: The background service keeps running, we just stop updating the screen.
        unregisterReceiver(uiUpdateReceiver);
    }
}