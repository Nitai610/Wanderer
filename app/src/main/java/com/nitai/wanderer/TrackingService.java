package com.nitai.wanderer;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class TrackingService extends Service {

    public static ArrayList<LatLng> livePath = new ArrayList<>();
    public static float liveDistanceInMeters = 0f;
    public static int liveSecondsElapsed = 0;
    public static boolean isServiceRunning = false;

    private FusedLocationProviderClient fusedLocationClient;
    // ...
    private LocationCallback locationCallback;
    private Location lastKnownLocation = null;
    private Handler timerHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();
    }
    // We no longer call startForeground() here.
    // It just silently starts doing its job in the backgrounblalblal
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceRunning) {
            isServiceRunning = true;
            livePath.clear();
            liveDistanceInMeters = 0f;
            liveSecondsElapsed = 0;
            lastKnownLocation = null;

            // ---> ADD THIS LINE HERE: <---
            startForegroundWithNotification();

            startLocationUpdates();
            startTimer();
        }
        return START_STICKY; // Tells Android to keep it running
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).setMinUpdateIntervalMillis(2000).build();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    livePath.add(currentLatLng);

                    if (lastKnownLocation != null) {
                        // Simply add whatever distance they moved directly to the total!
                        liveDistanceInMeters += lastKnownLocation.distanceTo(location);
                    }

                    lastKnownLocation = location;
                    sendUpdateBroadcast();
                }
            }
        };
    }

    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    // Add a second unconditionally, every single second.
                    liveSecondsElapsed++;

                    sendUpdateBroadcast();
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }
    private void sendUpdateBroadcast() {
        Intent intent = new Intent("UPDATE_UI_BROADCAST");
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
    // --- THE BAGRUT NOTIFICATION METHOD ---
    private void startForegroundWithNotification() {
        String channelId = "wanderer_tracking_channel";

        // 1. Create a "Channel" (Android 8.0+ requires all notifications to have a channel)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "Walk Tracking",
                    android.app.NotificationManager.IMPORTANCE_LOW // LOW importance so it doesn't beep loudly
            );
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // 2. Build the actual visual notification
        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle("Wanderer is Active")
                .setContentText("Currently tracking your walk in the background...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Standard built-in Android icon
                .build();

        // 3. Command Android to run this service in the foreground with the notification
        // The number '1' is just a random ID for the notification.
        startForeground(1, notification);
    }
}