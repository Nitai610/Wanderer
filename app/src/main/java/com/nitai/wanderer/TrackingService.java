package com.nitai.wanderer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceRunning) {
            isServiceRunning = true;
            // Clear old data in case of a new walk
            livePath.clear();
            liveDistanceInMeters = 0f;
            liveSecondsElapsed = 0;
            lastKnownLocation = null;

            // Safe Foreground Service start for Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(this, 1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(1, createNotification());
            }

            // Start the GPS and Timer
            startLocationUpdates();
            startTimer();
        }
        return START_STICKY;
    }

    // --- THE STICKY NOTIFICATION ---
    private Notification createNotification() {
        String channelId = "walk_tracker_channel";
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Walk Tracker", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, TravelActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Wanderer is tracking your walk")
                .setContentText("Keep up the good pace!")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
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
                    livePath.add(currentLatLng);

                    if (lastKnownLocation != null) {
                        liveDistanceInMeters += lastKnownLocation.distanceTo(location);
                    }
                    lastKnownLocation = location;

                    // Send the explicit, safe radio broadcast
                    sendUpdateBroadcast();
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

                    // Send the explicit, safe radio broadcast
                    sendUpdateBroadcast();

                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    // --- THE FIX: EXPLICIT BROADCASTER ---
    // This helper attaches your app's specific package name to the message so Android 14 doesn't block it.
    private void sendUpdateBroadcast() {
        Intent intent = new Intent("UPDATE_UI_BROADCAST");
        intent.setPackage(getPackageName()); // <--- The magic delivery address
        sendBroadcast(intent);
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