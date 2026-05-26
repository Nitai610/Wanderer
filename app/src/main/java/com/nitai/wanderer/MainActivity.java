package com.nitai.wanderer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

// BAGRUT NOTE: Firebase imports needed for authentication (verifying the user)
// and Firestore (fetching the database records).
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// EXPLANATION: MainActivity usually serves as the "Dashboard" or "Hub" of an app.
// It inherits from AppCompatActivity, giving it all the standard Android screen behaviors.
public class MainActivity extends AppCompatActivity {

    // Permission ID Tag
    // EXPLANATION: When we ask Android for a permission, it handles the popup and gets back to us later.
    // We pass this unique ID number (1001) so that when Android replies, we know exactly WHICH question it's answering.
    private static final int LOCATION_PERMISSION_CODE = 1001;

    // 1. Declare ALL the UI elements
    // EXPLANATION: Creating empty variables in memory to hold the visual elements from the XML.
    ImageButton btnProfile, btnSettings;
    View btnStartWalk, btnJournal, btnStats;
    TextView tvWeeklyDistanceMain, tvDailyDistanceMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // EXPLANATION: The super call is mandatory. It tells the Android OS to do its fundamental background setup for this screen first.
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        // BAGRUT NOTE: This hides the battery/notification bar at the top and the navigation
        // buttons at the bottom to give the app a modern, edge-to-edge, full-screen design.
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        // EXPLANATION: This inflates the XML design file and actually draws the buttons and text onto the user's physical screen.
        setContentView(R.layout.activity_main);

        // 2. Connect Java to the XML IDs
        // EXPLANATION: We link the empty Java variables to the actual visual components created by setContentView.
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        tvWeeklyDistanceMain = findViewById(R.id.tvWeeklyDistanceMain);
        tvDailyDistanceMain = findViewById(R.id.tvDailyDistanceMain);
        btnStartWalk = findViewById(R.id.btnStartWalk);
        btnJournal = findViewById(R.id.btnJournal);
        btnStats = findViewById(R.id.btnStats);

        // --- SECURITY & DATA LOADING ---
        // EXPLANATION: Retrieves the active session manager.
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // EXPLANATION: Retrieves the specific user currently logged in (contains their email, UID, etc.).
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // BAGRUT NOTE: Security Check (Defensive Programming).
        // If the user somehow bypassed the login screen, or their session expired,
        // we immediately kick them back to LoginActivity to protect the app from crashing.
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // EXPLANATION: Destroys MainActivity completely so they can't press 'Back' to bypass the login screen.
            return; // Stop the rest of onCreate from running
        }

        String userEmail = currentUser.getEmail();
        // EXPLANATION: Initializes the connection to the Firestore Database engine.
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // BAGRUT NOTE: Asynchronous Cloud Fetching.
        // We use `.get().addOnSuccessListener()` so the app's UI doesn't freeze while waiting
        // for Google's servers to respond. It runs in the background and triggers when ready.
        // EXPLANATION: The path here navigates deep into the database: Collection(users) -> Document(Specific Email) -> Collection(walks).
        db.collection("users").document(userEmail).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 1. Clear the local RAM so we don't accidentally duplicate data if it reloads
                    // EXPLANATION: Without this, every time you open MainActivity, it would append the same walks to the list again, doubling your stats!
                    Walk.walkHistory.clear();

                    // 2. Loop through every single document downloaded from the cloud
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // BAGRUT NOTE: 'toObject' is a powerful Firebase feature. It automatically
                        // maps the raw JSON data from the cloud directly into our custom Java 'Walk' class!
                        Walk downloadedWalk = document.toObject(Walk.class);
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // 3. Force the screen to update the math totals now that the data has arrived
                    updateWeeklyDistanceUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to sync data", Toast.LENGTH_SHORT).show();
                });

        // --- BUTTON CLICKS ---
        // EXPLANATION: Lambda expressions (v ->) handle user taps, using Intents to navigate to different screens.
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // --- THE TWO-STEP LOCATION CHECK ---
        btnStartWalk.setOnClickListener(v -> {

            // BAGRUT NOTE: Two-Step Verification (Hardware + Software).
            // Step 1: Hardware Check. We use LocationManager to check the physical
            // hardware status of the GPS antenna.
            // EXPLANATION: getSystemService allows us to access core Android OS hardware managers.
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = false;

            // EXPLANATION: Always check if managers are null to prevent NullPointerExceptions.
            if (locationManager != null) {
                // EXPLANATION: Asks the OS: "Is the physical GPS chip currently turned on by the user?"
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }

            if (!isGpsEnabled) {
                // GPS is off! Block the user from crashing the map and show a helpful dialog.
                // EXPLANATION: AlertDialog is a built-in UI tool for critical popups that require user attention.
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("GPS Required")
                        .setMessage("Your phone's GPS is currently turned off. You must enable Location Services to track a walk.")
                        .setPositiveButton("Turn On GPS", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // BAGRUT NOTE: System Intents.
                                // Instead of making the user dig through their phone manually,
                                // this Intent teleports them directly to the native Android Location Settings!
                                // EXPLANATION: This is an "Implicit Intent". We aren't opening our own screen, we are asking Android to open a specific system menu.
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel", null) // EXPLANATION: Passing 'null' means it will just dismiss the dialog and do nothing else.
                        .show();
            } else {
                // Step 2: Software Check. GPS is physically on, but did the user grant us permission to use it?
                // EXPLANATION: Android 6.0+ requires apps to ask for "Dangerous Permissions" (like location, camera) at runtime, not just during install.
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Both Hardware AND Software checks passed!
                    startTravelActivity();
                } else {
                    // Permission is missing. Ask the user for it.
                    // EXPLANATION: This triggers the standard Android system popup saying "Allow Wanderer to access this device's location?".
                    // We pass LOCATION_PERMISSION_CODE so we can recognize the answer later.
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_CODE);
                }
            }
        });

        btnJournal.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, JournalActivity.class);
            startActivity(intent);
        });

        btnStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });
    }

    // Helper method to actually launch the Travel/Map screen
    // EXPLANATION: Isolating this into a helper method prevents us from writing the exact same Intent code twice.
    private void startTravelActivity() {
        Intent intent = new Intent(MainActivity.this, TravelActivity.class);
        startActivity(intent);
    }

    // BAGRUT NOTE: Asynchronous Callbacks.
    // The permission dialog is an Android system popup. This built-in method automatically triggers
    // the exact moment the user clicks "Allow" or "Deny", letting us react accordingly.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // EXPLANATION: We must call the super method so the OS can do its own internal cleanup regarding permissions.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EXPLANATION: Check if this reply belongs to the Location question we asked earlier (using our ID 1001).
        if (requestCode == LOCATION_PERMISSION_CODE) {
            // Check if the array has results and the first result is GRANTED
            // EXPLANATION: grantResults is an array because you can ask for multiple permissions at once. Since we only asked for one, we check index [0].
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User clicked "Allow"!
                startTravelActivity();
            } else {
                // User clicked "Deny".
                // EXPLANATION: We fail gracefully. The app doesn't crash, we just tell the user why nothing happened.
                Toast.makeText(this, "Location permission is required to track your walk.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // BAGRUT NOTE: Lifecycle Management.
    // onResume() is a built-in Android hook that runs EVERY TIME this screen becomes visible again.
    // By putting the update logic here, the dashboard immediately refreshes with new stats
    // the moment the user returns from finishing a walk in TravelActivity.
    @Override
    protected void onResume() {
        // EXPLANATION: The Activity Lifecycle goes: onCreate -> onStart -> onResume.
        // When you come back from another screen, it skips onCreate and jumps straight to onResume.
        super.onResume();
        updateWeeklyDistanceUI();
    }

    // BAGRUT NOTE: Helper method to keep our code clean and avoid repeating the same lines
    private void updateWeeklyDistanceUI() {
        // 1. Calculate the totals using our Walk.java math engine
        float weeklyTotal = Walk.calculateWeeklyDistance();
        float dailyTotal = Walk.calculateDailyDistance();

        // 2. Set the text for the Weekly TV
        if (tvWeeklyDistanceMain != null) {
            // EXPLANATION: String.format handles the text layout. "%.2f" means "take the floating-point number, and format it to show exactly 2 decimal places".
            // Locale.US ensures that the decimal point is a dot (.) and not a comma (,) to prevent crashes in Hebrew/European phones.
            tvWeeklyDistanceMain.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        }

        // 3. Set the text for the Daily TV
        if (tvDailyDistanceMain != null) {
            tvDailyDistanceMain.setText(String.format(java.util.Locale.US, "%.2f KM", dailyTotal));
        }
    }
}