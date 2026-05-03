package com.nitai.wanderer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {

    MaterialButton btnLogout, btnClearHistory, btnSettingsBack, btnTestNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IMMERSIVE MODE
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_settings);

        btnLogout = findViewById(R.id.btnLogout);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        btnSettingsBack = findViewById(R.id.btnSettingsBack);
        btnTestNotification = findViewById(R.id.btnTestNotification); // Connect new button

        // --- TEST NOTIFICATION BUTTON LOGIC ---
        btnTestNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If on Android 13+, check if we have permission to send pop-ups
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(SettingsActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                } else {
                    // Permission granted, fire the notification!
                    sendTestNotification();
                }
            }
        });

        // --- GO BACK BUTTON ---
        btnSettingsBack.setOnClickListener(v -> finish());

        // --- CLEAR JOURNAL HISTORY BUTTON ---
        btnClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Clear Journal")
                    .setMessage("Are you sure you want to delete all your saved walks? This action cannot be undone.")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        Walk.walkHistory.clear();
                        Walk.saveHistory(SettingsActivity.this);
                        Toast.makeText(SettingsActivity.this, "Journal completely cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // --- LOG OUT BUTTON ---
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out of your account?")
                    .setPositiveButton("Yes, Log Out", (dialog, which) -> {
                        Toast.makeText(SettingsActivity.this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    // --- THE NOTIFICATION BUILDER ---
    private void sendTestNotification() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        // Setup the modern Android "Channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("test_channel", "Test Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the pop-up
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "test_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard info icon
                .setContentTitle("Test Successful! \uD83D\uDE80") // Rocket emoji
                .setContentText("Your notifications are working perfectly.")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // This makes it drop down from the top of the screen
                .setAutoCancel(true); // Goes away when you swipe it

        // Fire it!
        notificationManager.notify(99, builder.build());
        Toast.makeText(this, "Notification sent!", Toast.LENGTH_SHORT).show();
    }
}