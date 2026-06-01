package com.nitai.wanderer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

// EXPLANATION: 'implements OnMapReadyCallback' is an interface contract. It forces this class to include the 'onMapReady' method,
// which Google Maps will automatically trigger the exact moment the map finishes downloading and is ready to be used.
public class TravelActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    TextView tvLiveDistance, tvLiveTimer;
    MaterialButton btnStopWalk;

    // --- THE RADIO RECEIVER (OBSERVER PATTERN) ---
    // EXPLANATION: If TrackingService is the radio tower broadcasting updates, this BroadcastReceiver is the antenna on your app's screen.
    private BroadcastReceiver uiUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // EXPLANATION: Every time the service shouts "UPDATE!", this antenna catches it and triggers the screen to redraw the math.
            updateScreenFromService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel); // This line draws the screen

        // 1. Check if the device is running Android 13 (Tiramisu) or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

            // 2. Check if the user has ALREADY granted us this permission in the past
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                // 3. If not, pop up the system dialog asking the user for permission
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Immersive Mode
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());


        tvLiveDistance = findViewById(R.id.tvLiveDistance);
        tvLiveTimer = findViewById(R.id.tvLiveTimer);
        btnStopWalk = findViewById(R.id.btnStopWalk);

        // EXPLANATION: Finds the map box in your XML and starts downloading the Google Maps tiles asynchronously (in the background) so the screen doesn't freeze.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnStopWalk.setOnClickListener(v -> {
            // Passing final data to SummaryActivity
            Intent intent = new Intent(TravelActivity.this, SummaryActivity.class);

            // EXPLANATION: We pull the final text directly off the screen and pack it into the Intent to send to the Summary screen.
            intent.putExtra("FINAL_DISTANCE", tvLiveDistance.getText().toString());
            intent.putExtra("FINAL_TIME", tvLiveTimer.getText().toString());
            // EXPLANATION: Sending our massive list of GPS coordinates to the next screen so it can draw the final blue line.
            intent.putParcelableArrayListExtra("PATH_POINTS", TrackingService.livePath);

            stopTrackingService(); // Stop the service while user reviews summary
            startActivity(intent);
            finish();
        });

    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);

        // BAGRUT LOGIC: We check if TravelActivity itself was opened with a "Resume" flag.
        // We then forward that flag to the Background Service.
        // EXPLANATION: The "false" here is a fallback. If the intent doesn't have a "RESUME_WALK" tag, we assume it's a brand new walk.
        boolean isResuming = getIntent().getBooleanExtra("RESUME_WALK", false);
        serviceIntent.putExtra("RESUME_WALK", isResuming);

        // EXPLANATION: Android 8.0 (Oreo) introduced strict background limits. If we just use startService(), Android will kill our GPS tracker after 1 minute to save battery.
        // startForegroundService() tells Android: "This is crucial, keep it alive, and I promise to show a notification to the user so they know."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateScreenFromService() {
        // EXPLANATION: Guard clause. Don't try to update math if the service isn't even running.
        if (!TrackingService.isServiceRunning) return;

        // 1. Update Distance
        // EXPLANATION: The service calculates in raw meters. We divide by 1000 to get Kilometers.
        float distanceInKm = TrackingService.liveDistanceInMeters / 1000f;
        tvLiveDistance.setText(String.format(java.util.Locale.US, "%.2f KM", distanceInKm));

        // 2. Update Timer (Supports HH:MM:SS)
        // EXPLANATION: Classic modulo arithmetic used in computer science!
        // 3600 seconds in an hour. % gives us the remainder. So % 60 gives us just the leftover seconds that don't fit into a full minute.
        int hours = TrackingService.liveSecondsElapsed / 3600;
        int minutes = (TrackingService.liveSecondsElapsed % 3600) / 60;
        int seconds = TrackingService.liveSecondsElapsed % 60;

        if (hours > 0) {
            // EXPLANATION: "%02d" means: "Format this as an integer. If it's only 1 digit long, add a leading zero." (e.g., "5" becomes "05").
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            tvLiveTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));
        }

        // 3. Update Camera
        if (!TrackingService.livePath.isEmpty() && mMap != null) {
            // EXPLANATION: Grabs the very last item added to the array (size - 1) which is your current physical location.
            LatLng latestSpot = TrackingService.livePath.get(TrackingService.livePath.size() - 1);
            // EXPLANATION: .animateCamera physically pans the map smoothly to keep your character centered on the screen as you walk.
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestSpot, 17f));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // EXPLANATION: Saves the downloaded map into our global variable so we can use it later in the updateScreenFromService method.
        mMap = googleMap;
        if (!TrackingService.isServiceRunning) {
            checkPermissionsAndStart();
        }
    }

    private void checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // EXPLANATION: This turns on the native "Blue Dot" that shows the user's orientation/facing direction on the map.
            mMap.setMyLocationEnabled(true);
            // EXPLANATION: Adds a 500px invisible margin to the bottom of the map. This pushes the Google logo and the "Center Map" button up so they aren't hidden behind your UI buttons.
            mMap.setPadding(0, 0, 0, 500);
            startTrackingService();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void stopTrackingService() {
        // EXPLANATION: Sends a kill signal to the Android OS to destroy our background service.
        stopService(new Intent(this, TrackingService.class));
        TrackingService.isServiceRunning = false;
    }

    // --- LIFECYCLE MANAGEMENT (CRITICAL FOR PERFORMANCE) ---

    @Override
    protected void onResume() {
        super.onResume();
        // EXPLANATION: When the screen becomes visible, we "plug in" the radio antenna.
        // We tell Android: "Listen for the specific frequency 'UPDATE_UI_BROADCAST'."
        // RECEIVER_NOT_EXPORTED is a modern Android 13+ security feature that prevents other apps on the phone from injecting fake broadcasts into your app.
        ContextCompat.registerReceiver(this, uiUpdateReceiver, new IntentFilter("UPDATE_UI_BROADCAST"), ContextCompat.RECEIVER_NOT_EXPORTED);

        // EXPLANATION: Force a manual refresh immediately just in case a second ticked by while the screen was loading.
        updateScreenFromService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // EXPLANATION: This is vital! When the user turns off their screen or switches apps, we UNPLUG the radio antenna.
        // If we didn't do this, the app would crash with a "Leaked IntentReceiver" error, because the background service would be trying to update a screen that is currently asleep.
        unregisterReceiver(uiUpdateReceiver);
    }
}