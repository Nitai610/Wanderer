package com.nitai.wanderer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class TrackingService extends Service {

    // --- GLOBAL TRACKING VARIABLES ---
    // We make these 'public static' so the TravelActivity can easily read them to update the screen
    public static ArrayList<LatLng> livePath = new ArrayList<>();
    public static float liveDistanceInMeters = 0f;
    public static int liveSecondsElapsed = 0;
    public static boolean isServiceRunning = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastKnownLocation = null;
    private Handler timerHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();
    }

    // This runs the moment TravelActivity tells the service to start
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceRunning) {
            isServiceRunning = true;
            // Clear old data in case of a new walk
            livePath.clear();
            liveDistanceInMeters = 0f;
            liveSecondsElapsed = 0;
            lastKnownLocation = null;

            // 1. Start the sticky notification so Android doesn't kill us
            startForeground(1, createNotification());

            // 2. Start the GPS and Timer
            startLocationUpdates();
            startTimer();
        }
        return START_STICKY; // Tells Android to restart the service if it crashes
    }

    // --- THE STICKY NOTIFICATION ---
    private Notification createNotification() {
        String channelId = "walk_tracker_channel";
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        // Modern Android requires a "Channel" for notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Walk Tracker", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        // If the user taps the notification, open TravelActivity
        Intent notificationIntent = new Intent(this, TravelActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Wanderer is tracking your walk")
                .setContentText("Keep up the good pace!")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Makes it sticky (cannot be swiped away)
                .build();
    }

    // --- GPS ENGINE ---
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    livePath.add(currentLatLng); // Add to the backpack

                    if (lastKnownLocation != null) {
                        liveDistanceInMeters += lastKnownLocation.distanceTo(location);
                    }
                    lastKnownLocation = location;

                    // Send a radio broadcast to TravelActivity saying "Hey, the screen needs to update!"
                    sendBroadcast(new Intent("UPDATE_UI_BROADCAST"));
                }
            }
        };
    }

    // --- TIMER ENGINE ---
    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    liveSecondsElapsed++;
                    sendBroadcast(new Intent("UPDATE_UI_BROADCAST")); // Update screen every second
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    // --- CLEANUP ---
    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}