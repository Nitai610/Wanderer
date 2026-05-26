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

// EXPLANATION: A 'Service' is an Android component that can perform long-running operations in the background.
// Unlike an Activity, it does not provide a user interface. This allows Wandrer to keep tracking GPS even if the user puts their phone in their pocket and turns off the screen.
public class TrackingService extends Service {

    // BAGRUT NOTE: These are STATIC variables.
    // Static variables live in the App's memory as long as the app process is alive.
    // This allows us to "pause" and "resume" because the numbers don't disappear
    // even when the Service or Activity is temporarily stopped.
    public static ArrayList<LatLng> livePath = new ArrayList<>();
    public static float liveDistanceInMeters = 0f;
    public static int liveSecondsElapsed = 0;
    public static boolean isServiceRunning = false;

    // EXPLANATION: FusedLocationProviderClient is Google's intelligent GPS engine. Instead of just using the raw GPS chip (which drains battery fast),
    // it "fuses" data from GPS, Wi-Fi, and Cell Towers to get the most accurate location while saving as much battery as possible.
    private FusedLocationProviderClient fusedLocationClient;
    // EXPLANATION: The Callback is the "Listener" that waits for the GPS engine to hand us a new coordinate.
    private LocationCallback locationCallback;
    private Location lastKnownLocation = null;

    // EXPLANATION: A Handler allows us to schedule code to run at a specific time in the future. We use it here to create our 1-second stopwatch timer.
    private Handler timerHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        // EXPLANATION: Waking up the Google Location engine.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // EXPLANATION: Prepping our listener so it's ready before we actually ask for location updates.
        setupLocationCallback();
    }

    @Override
    // EXPLANATION: onStartCommand is triggered every time an Activity calls startService().
    // It is the main entry point where the actual work of the service begins.
    public int onStartCommand(Intent intent, int flags, int startId) {
        // --- THE RESUME FIX ---
        // 1. We extract the "RESUME_WALK" signal sent from TravelActivity.
        boolean isResuming = (intent != null) && intent.getBooleanExtra("RESUME_WALK", false);

        if (!isServiceRunning) {
            isServiceRunning = true;

            // 2. BAGRUT LOGIC: If this is a NEW walk (not resuming), we MUST clear old data.
            // If isResuming is TRUE, we skip this block and the static variables keep their values!
            if (!isResuming) {
                livePath.clear();
                liveDistanceInMeters = 0f;
                liveSecondsElapsed = 0;
            }

            // 3. We always set lastKnownLocation to null when starting/resuming.
            // This prevents a "teleportation" bug where the app calculates the distance
            // between the point where you paused and the point where you resumed.
            lastKnownLocation = null;

            startForegroundWithNotification();
            startLocationUpdates();
            startTimer();
        }
        // EXPLANATION: START_STICKY tells the Android OS: "If you run out of RAM and kill this service, please restart it automatically as soon as you have free memory again."
        return START_STICKY;
    }

    private void startTimer() {
        // EXPLANATION: A Runnable is a block of code designed to be executed by a Thread or Handler.
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    liveSecondsElapsed++;
                    // EXPLANATION: Every single second, we tell the main screen to update the clock UI.
                    sendUpdateBroadcast();
                    // EXPLANATION: The Magic Loop. The Runnable tells the Handler to run *itself* again in exactly 1000 milliseconds (1 second).
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // EXPLANATION: locationResult contains the newest GPS data. Sometimes it returns multiple points at once if the signal was lagging, so we loop through them.
                for (Location location : locationResult.getLocations()) {
                    // EXPLANATION: Convert the raw Location object into a LatLng object so Google Maps can draw it later.
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    livePath.add(currentLatLng);

                    // EXPLANATION: If we have a previous point, we calculate the mathematical distance between point A and point B and add it to the total.
                    if (lastKnownLocation != null) {
                        // EXPLANATION: .distanceTo() is a brilliant built-in Android method that accounts for the curvature of the Earth when calculating the distance between coordinates!
                        liveDistanceInMeters += lastKnownLocation.distanceTo(location);
                    }
                    // EXPLANATION: The current point now becomes the "last known point" for the next time the GPS fires.
                    lastKnownLocation = location;

                    // EXPLANATION: Every time we move, tell the main screen to update the distance UI.
                    sendUpdateBroadcast();
                }
            }
        };
    }

    private void sendUpdateBroadcast() {
        // EXPLANATION: A Broadcast is like a radio tower. The Service broadcasts a signal into the phone saying "UPDATE_UI_BROADCAST".
        // It doesn't care who is listening. Over in TravelActivity, you have a BroadcastReceiver (a radio antenna) listening for this exact frequency to update the text boxes.
        Intent intent = new Intent("UPDATE_UI_BROADCAST");
        // EXPLANATION: setPackage ensures this broadcast is strictly kept inside your app for security, preventing other apps from intercepting it.
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        // EXPLANATION: The LocationRequest is our contract with the GPS engine.
        // PRIORITY_HIGH_ACCURACY forces the phone to use the actual GPS chip (not just Wi-Fi).
        // 3000ms (3 seconds) is the preferred update rate. 2000ms is the absolute fastest we will accept updates to prevent battery drain.
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).setMinUpdateIntervalMillis(2000).build();

        // EXPLANATION: Looper.getMainLooper() ensures the location results are delivered on the main UI thread so they don't crash the app.
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void startForegroundWithNotification() {
        // EXPLANATION: Android 8.0+ strict rule: You cannot track location in the background secretly.
        // You MUST promote the Service to a "Foreground Service" and attach a visible Notification so the user knows the app is actively running.
        String channelId = "wanderer_tracking_channel";

        // EXPLANATION: Modern Android requires Notifications to be assigned to "Channels" so users can manage them in their phone settings.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Walk Tracking", android.app.NotificationManager.IMPORTANCE_LOW);
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle("Wanderer is Active")
                .setContentText("Currently tracking your walk...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();

        // EXPLANATION: Locks the service into the Foreground state using the notification we just built.
        startForeground(1, notification);
    }

    @Override
    // EXPLANATION: onDestroy is called when stopService() is triggered. This is our cleanup phase.
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;

        // EXPLANATION: Crucial Memory Management! If you don't kill the Handler and remove the LocationCallback,
        // they will keep running in the background forever, causing a massive memory leak and battery drain.
        if (timerHandler != null) timerHandler.removeCallbacksAndMessages(null);
        if (fusedLocationClient != null && locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    // EXPLANATION: Services come in two types: "Started" (runs until stopped) and "Bound" (clients attach to it to send direct commands).
    // Because we are a Started service using Broadcasts to communicate, we don't need Binding, so we return null.
    public IBinder onBind(Intent intent) { return null; }
}