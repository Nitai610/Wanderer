package com.nitai.wanderer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

// NOTE: We need the Firebase tools to delete cloud data and log out safely!
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class SettingsActivity extends AppCompatActivity {

    MaterialButton btnLogout, btnClearHistory, btnTestNotification, btnSettingsBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // ----------------------

        setContentView(R.layout.activity_settings);

        // Connect UI
        btnLogout = findViewById(R.id.btnLogout);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        btnTestNotification = findViewById(R.id.btnTestNotification);
        btnSettingsBack = findViewById(R.id.btnSettingsBack);

        // --- 1. GO BACK BUTTON ---
        btnSettingsBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // --- 2. TEST NOTIFICATION BUTTON ---
        btnTestNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Ping!")
                        .setMessage("This is a test notification from Wanderer.")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        // --- 3. CLEAR JOURNAL HISTORY BUTTON (UPDATED FOR FIRESTORE) ---
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Clear Journal")
                        .setMessage("Are you sure you want to permanently delete all your saved walks from the cloud?")
                        .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // 1. Get the Database and User ID
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                // 2. Go to the user's "walks" folder in the cloud
                                db.collection("users").document(userId).collection("walks")
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {

                                            // NOTE: Cloud Firestore requires us to loop through and delete files one by one.
                                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                                document.getReference().delete();
                                            }

                                            // 3. Clear the memory in the phone so the screen updates immediately
                                            Walk.walkHistory.clear();

                                            Toast.makeText(SettingsActivity.this, "Journal completely cleared", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(SettingsActivity.this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        // --- 4. LOG OUT BUTTON (UPDATED FOR FIREBASE AUTH) ---
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes, Log Out", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // NOTE: This is the critical command! It tells the Google servers to officially
                                // lock the app and erase the temporary security token from the phone.
                                FirebaseAuth.getInstance().signOut();

                                // Clear the local memory so the next person who logs in doesn't see your walks
                                Walk.walkHistory.clear();

                                Toast.makeText(SettingsActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }
}