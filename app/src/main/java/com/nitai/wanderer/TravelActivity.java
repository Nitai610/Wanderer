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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import androidx.core.content.ContextCompat;

public class TravelActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int PERMISSION_REQUEST_CODE = 100;

    ImageButton btnCancelWalk;
    MaterialButton btnStopWalk;
    TextView tvLiveDistance, tvLiveTimer;

    // --- THE RADIO RECEIVER ---
    // This listens for the "UPDATE_UI" message sent by our background service
    private BroadcastReceiver uiUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateScreenFromService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IMMERSIVE MODE
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_travel);

        btnCancelWalk = findViewById(R.id.btnCancelWalk);
        btnStopWalk = findViewById(R.id.btnStopWalk);
        tvLiveDistance = findViewById(R.id.tvLiveDistance);
        tvLiveTimer = findViewById(R.id.tvLiveTimer);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // CANCEL WALK
        btnCancelWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrackingService();
                finish();
            }
        });

        // STOP WALK AND GO TO SUMMARY
        btnStopWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String finalDistance = tvLiveDistance.getText().toString();
                String finalTime = tvLiveTimer.getText().toString();

                Intent intent = new Intent(TravelActivity.this, SummaryActivity.class);
                intent.putExtra("FINAL_DISTANCE", finalDistance);
                intent.putExtra("FINAL_TIME", finalTime);
                // Grab the final map data directly from the Service's memory
                intent.putParcelableArrayListExtra("PATH_POINTS", TrackingService.livePath);

                stopTrackingService();
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkPermissionsAndStart();
    }

    private void checkPermissionsAndStart() {
        // Modern Android requires notification permissions alongside GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        } else {
            mMap.setMyLocationEnabled(true);
            // Start the background engine!
            startTrackingService();
        }
    }

    // --- SERVICE CONTROLS ---
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent); // Required for Android 8.0+
        } else {
            startService(serviceIntent);
        }
    }

    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        stopService(serviceIntent);
    }

    // --- UPDATE THE UI ---
    // Grabs the live numbers from the background service and updates the Text/Map
    private void updateScreenFromService() {
        // 1. Update Distance
        float distanceInKm = TrackingService.liveDistanceInMeters / 1000f;
        tvLiveDistance.setText(String.format(java.util.Locale.US, "%.2f KM", distanceInKm));

        // 2. Update Timer
        int minutes = TrackingService.liveSecondsElapsed / 60;
        int seconds = TrackingService.liveSecondsElapsed % 60;
        tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));

        // 3. Update Map Camera
        if (!TrackingService.livePath.isEmpty() && mMap != null) {
            LatLng latestSpot = TrackingService.livePath.get(TrackingService.livePath.size() - 1);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestSpot, 17f));
        }
    }

    // Turn the radio receiver on when the screen is open
    @Override
    protected void onResume() {
        super.onResume();
        // FIX: Safely register the receiver and block outside apps from listening
        ContextCompat.registerReceiver(
                this,
                uiUpdateReceiver,
                new IntentFilter("UPDATE_UI_BROADCAST"),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        updateScreenFromService(); // Catch up on any math that happened while screen was off
    }

    // Turn the radio receiver off when screen is closed (Service keeps running though!)
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(uiUpdateReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermissionsAndStart();
        }
    }
}