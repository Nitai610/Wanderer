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

    // NOTE: Variables for Health Connect
    HealthConnectBridge healthBridge;
    private long startingSteps = -1; // -1 means we haven't read the steps from the sensor yet

    // NOTE FOR BAGRUT: What is a Handler and Runnable?
    // ANSWER: Health Connect doesn't stream data every second (it would kill the battery).
    // Instead, we use a Handler (a timer) and a Runnable (a block of code).
    // We tell the Handler: "Run this code, wait 15 seconds, and then run it again." It's a battery-friendly loop!
    private Handler stepPollingHandler = new Handler(Looper.getMainLooper());
    private Runnable stepPollingRunnable;

    // NOTE: This BroadcastReceiver is like a radio antenna. It listens for signals from TrackingService.java
    private BroadcastReceiver uiUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateScreenFromService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE: Immersive mode (hides top and bottom bars)
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_travel);

        // Connect Java to XML
        tvLiveDistance = findViewById(R.id.tvLiveDistance);
        tvLiveTimer = findViewById(R.id.tvLiveTimer);
        tvLiveSteps = findViewById(R.id.tvLiveSteps);
        btnStopWalk = findViewById(R.id.btnStopWalk);
        btnCancelWalk = findViewById(R.id.btnCancelWalk);

        // Wake up our custom Kotlin Health Connect class
        healthBridge = new HealthConnectBridge(this);

        // Setup the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // ==========================================
        // --- STOP WALK BUTTON (CRASH FIX) ---
        // ==========================================
        btnStopWalk.setOnClickListener(v -> {
            Intent intent = new Intent(TravelActivity.this, SummaryActivity.class);

            // NOTE FOR BAGRUT: Why do we use a Ternary Operator (condition ? true : false) here?
            // ANSWER: It is a safety measure. If the user presses "Stop" a millisecond before the TextViews load,
            // the app would crash with a NullPointerException. This gives it a safe default value ("0.00 KM").
            String dist = tvLiveDistance != null ? tvLiveDistance.getText().toString() : "0.00 KM";
            String time = tvLiveTimer != null ? tvLiveTimer.getText().toString() : "00:00:00";
            String steps = "0";

            if (tvLiveSteps != null) {
                // NOTE: We literally just read the steps from the screen text. We remove the word " Steps" so it's a clean number.
                steps = tvLiveSteps.getText().toString().replace(" Steps", "").trim();
            }

            // Put the clean strings into the Intent backpack
            intent.putExtra("FINAL_DISTANCE", dist);
            intent.putExtra("FINAL_TIME", time);
            intent.putExtra("FINAL_STEPS", steps);

            // NOTE FOR BAGRUT: EXAMINER EXPLANATION (THE CRASH FIX)
            // Examiner: "How do you pass the map route to SummaryActivity?"
            // You: "I DO NOT pass it through the Intent anymore! Android Intents have a strict 1MB size limit.
            // If the user walks a long time, passing hundreds of GPS coordinates through an Intent causes a
            // 'TransactionTooLargeException' crash. Instead, SummaryActivity reads the path directly from
            // the static RAM memory in TrackingService. It's much safer and faster!"

            stopTrackingService();
            startActivity(intent);
            finish();
        });

        btnCancelWalk.setOnClickListener(v -> {
            stopTrackingService();
            finish();
        });

        // NOTE: This is the actual code for the 15-second loop we talked about earlier.
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
                // NOTE FOR BAGRUT: Health Connect only gives us "Total Steps Today".
                // To find out how many steps were taken *during this walk*, we save the total the moment the walk
                // begins (startingSteps). Then, we subtract the starting line from the live total.
                if (startingSteps == -1) {
                    startingSteps = currentTotalDailySteps; // Lock in the starting number
                }
                long stepsTakenThisWalk = currentTotalDailySteps - startingSteps;
                if (tvLiveSteps != null) {
                    tvLiveSteps.setText(stepsTakenThisWalk + " Steps");
                }
            }

            @Override
            public void onFailure(String errorMessage) { }
        });
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        boolean isResuming = getIntent().getBooleanExtra("RESUME_WALK", false);
        serviceIntent.putExtra("RESUME_WALK", isResuming);

        // NOTE: Foreground services behave differently on newer Android versions. We must check the version.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateScreenFromService() {
        if (!TrackingService.isServiceRunning) return;

        // NOTE: The service measures in raw meters. We divide by 1000 to get Kilometers.
        float distanceInKm = TrackingService.liveDistanceInMeters / 1000f;
        tvLiveDistance.setText(String.format(java.util.Locale.US, "%.2f KM", distanceInKm));

        // NOTE: Convert raw seconds back to HH:MM:SS format
        int hours = TrackingService.liveSecondsElapsed / 3600;
        int minutes = (TrackingService.liveSecondsElapsed % 3600) / 60;
        int seconds = TrackingService.liveSecondsElapsed % 60;

        if (hours > 0) {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));
        }

        // NOTE: Animate the map camera to always follow the very last point in the array (the user's current location)
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
            mMap.setPadding(0, 0, 0, 500); // Pushes the Google logo up so it's not hidden behind our UI card
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
        // NOTE: Turn the radio antenna on
        ContextCompat.registerReceiver(this, uiUpdateReceiver, new IntentFilter("UPDATE_UI_BROADCAST"), ContextCompat.RECEIVER_NOT_EXPORTED);
        updateScreenFromService();
        fetchLiveWalkSteps();
        stepPollingHandler.post(stepPollingRunnable); // Start the 15-second loop
    }

    @Override
    protected void onPause() {
        super.onPause();
        // NOTE: Turn the radio antenna off
        unregisterReceiver(uiUpdateReceiver);
        // NOTE FOR BAGRUT: We MUST remove the callbacks here! If we don't, the 15-second loop will continue
        // running in the background forever even if the app is minimized, draining the user's battery!
        stepPollingHandler.removeCallbacks(stepPollingRunnable);
    }
}