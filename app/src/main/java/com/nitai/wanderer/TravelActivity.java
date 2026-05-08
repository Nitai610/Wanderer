package com.nitai.wanderer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
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

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    TextView tvLiveDistance, tvLiveTimer, tvLiveSteps;
    MaterialButton btnStopWalk;
    ImageButton btnCancelWalk;

    // --- HEALTH CONNECT POLLING VARIABLES ---
    HealthConnectBridge healthBridge;
    private long startingSteps = -1; // -1 means we haven't locked in the starting value yet
    private Handler stepPollingHandler = new Handler(Looper.getMainLooper());
    private Runnable stepPollingRunnable;

    // --- RADIO RECEIVER FOR GPS SERVICE ---
    private BroadcastReceiver uiUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateScreenFromService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_travel);

        tvLiveDistance = findViewById(R.id.tvLiveDistance);
        tvLiveTimer = findViewById(R.id.tvLiveTimer);
        tvLiveSteps = findViewById(R.id.tvLiveSteps); // NEW
        btnStopWalk = findViewById(R.id.btnStopWalk);
        btnCancelWalk = findViewById(R.id.btnCancelWalk);

        healthBridge = new HealthConnectBridge(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnStopWalk.setOnClickListener(v -> {
            Intent intent = new Intent(TravelActivity.this, SummaryActivity.class);
            intent.putExtra("FINAL_DISTANCE", tvLiveDistance.getText().toString());
            intent.putExtra("FINAL_TIME", tvLiveTimer.getText().toString());
            intent.putParcelableArrayListExtra("PATH_POINTS", TrackingService.livePath);

            stopTrackingService();
            startActivity(intent);
            finish();
        });

        btnCancelWalk.setOnClickListener(v -> {
            stopTrackingService();
            finish();
        });

        // =========================================================================
        // BAGRUT NOTE: The Background Polling Loop
        // Health Connect is a central database, not a real-time hardware stream.
        // We use a Handler and Runnable to create an infinite loop that asks Google
        // for new steps every 15 seconds. This is battery-efficient while still
        // giving the user "live" feedback during their walk.
        // =========================================================================
        stepPollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (TrackingService.isServiceRunning) {
                    fetchLiveWalkSteps();
                }
                // Post this exact block of code to run again in 15000 milliseconds (15s)
                stepPollingHandler.postDelayed(this, 15000);
            }
        };
    }

    private void fetchLiveWalkSteps() {
        healthBridge.readTodaySteps(new HealthConnectBridge.StepsCallback() {
            @Override
            public void onSuccess(long currentTotalDailySteps) {

                // BAGRUT NOTE: Relative Step Math
                // Health Connect only gives us "Total Steps Today". To find out how many
                // steps were taken *during this walk*, we save the total the moment the walk
                // begins. Then, we subtract the starting line from the live total.
                if (startingSteps == -1) {
                    startingSteps = currentTotalDailySteps;
                }

                long stepsTakenThisWalk = currentTotalDailySteps - startingSteps;

                if (tvLiveSteps != null) {
                    tvLiveSteps.setText(stepsTakenThisWalk + " Steps");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Ignore failures silently so it doesn't interrupt the GPS map view
            }
        });
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        boolean isResuming = getIntent().getBooleanExtra("RESUME_WALK", false);
        serviceIntent.putExtra("RESUME_WALK", isResuming);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateScreenFromService() {
        if (!TrackingService.isServiceRunning) return;

        float distanceInKm = TrackingService.liveDistanceInMeters / 1000f;
        tvLiveDistance.setText(String.format(java.util.Locale.US, "%.2f KM", distanceInKm));

        int hours = TrackingService.liveSecondsElapsed / 3600;
        int minutes = (TrackingService.liveSecondsElapsed % 3600) / 60;
        int seconds = TrackingService.liveSecondsElapsed % 60;

        if (hours > 0) {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));
        }

        if (!TrackingService.livePath.isEmpty() && mMap != null) {
            LatLng latestSpot = TrackingService.livePath.get(TrackingService.livePath.size() - 1);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestSpot, 17f));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (!TrackingService.isServiceRunning) {
            checkPermissionsAndStart();
        }
    }

    private void checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.setPadding(0, 0, 0, 500);
            startTrackingService();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void stopTrackingService() {
        stopService(new Intent(this, TrackingService.class));
        TrackingService.isServiceRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(this, uiUpdateReceiver, new IntentFilter("UPDATE_UI_BROADCAST"), ContextCompat.RECEIVER_NOT_EXPORTED);
        updateScreenFromService();

        // 1. Trigger the step check immediately when the screen opens
        fetchLiveWalkSteps();
        // 2. Start the 15-second loop
        stepPollingHandler.post(stepPollingRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(uiUpdateReceiver);

        // BAGRUT NOTE: We must kill the polling loop when the user minimizes the app.
        // If we don't, the Handler will continue pinging Google forever and drain the battery!
        stepPollingHandler.removeCallbacks(stepPollingRunnable);
    }
}