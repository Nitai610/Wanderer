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
// EXPLANATION: We import the specific Firebase classes needed to manage user sessions (Auth) and database manipulation (Firestore & WriteBatch).
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class SettingsActivity extends AppCompatActivity {

    // EXPLANATION: Declaring UI elements globally so they can be referenced inside our button click listeners.
    MaterialButton btnLogout, btnClearHistory, btnSettingsBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        // EXPLANATION: This code removes the top battery/time bar and the bottom navigation bar.
        // WindowInsetsControllerCompat is the modern, recommended way to handle system bars in Android.
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // ----------------------

        setContentView(R.layout.activity_settings);

        // Connect UI
        // EXPLANATION: Finding the visual buttons in the XML layout and linking them to our Java variables.
        btnLogout = findViewById(R.id.btnLogout);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        btnSettingsBack = findViewById(R.id.btnSettingsBack);

        // --- 1. GO BACK BUTTON ---
        btnSettingsBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // EXPLANATION: Safely pops this Activity off the stack, returning the user to whatever screen they were on previously.
                finish();
            }
        });

        // --- 3. CLEAR JOURNAL HISTORY BUTTON (UPDATED FOR FIRESTORE) ---
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // EXPLANATION: A critical "Safety Net". Never delete user data permanently without forcing them to confirm their intent via a popup dialog first.
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Clear Journal")
                        .setMessage("Are you sure you want to permanently delete all your saved walks from the cloud?")
                        .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // BAGRUT FIX: Get the Database and the correct User Email!
                                // EXPLANATION: We must verify WHO is pressing this button so we don't accidentally delete another user's database folder.
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                // EXPLANATION: Guard clause. If they somehow aren't logged in, stop immediately.
                                if (user == null || user.getEmail() == null) return;

                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                String userEmail = user.getEmail();

                                // 2. Go to the user's "walks" folder in the cloud
                                // EXPLANATION: Navigate to the correct database path and trigger a background download of all their walks.
                                db.collection("users").document(userEmail).collection("walks")
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {

                                            // BAGRUT NOTE: We use a WriteBatch for massive deletions.
                                            // This tells Firebase to queue up all the deletions and execute them
                                            // in one single network request, saving battery and data!
                                            // EXPLANATION FOR BAGRUT: A WriteBatch is "Atomic". This means if you are deleting 50 walks, and the internet cuts out at walk 25, the Batch cancels the entire operation. It's either 100% success or 0% success, preventing a corrupted or half-deleted database!
                                            WriteBatch batch = db.batch();

                                            // EXPLANATION: Loop through every document the query found.
                                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                                // EXPLANATION: document.getReference() grabs the specific cloud URL for that walk, and batch.delete() adds that URL to the execution queue.
                                                batch.delete(document.getReference());
                                            }

                                            // Execute the mass deletion
                                            // EXPLANATION: .commit() is the trigger that actually sends the bundled package of deletion commands to Google's servers.
                                            batch.commit().addOnSuccessListener(aVoid -> {
                                                // 3. Clear the memory in the phone so the screen updates immediately
                                                // EXPLANATION: If we only deleted it from the cloud, the user would still see their walks on the screen until they restarted the app. Clearing the local RAM list ensures the UI reflects the true database state instantly.
                                                Walk.walkHistory.clear();
                                                Toast.makeText(SettingsActivity.this, "Journal completely cleared", Toast.LENGTH_SHORT).show();
                                            }).addOnFailureListener(e -> {
                                                Toast.makeText(SettingsActivity.this, "Error during mass deletion", Toast.LENGTH_SHORT).show();
                                            });

                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(SettingsActivity.this, "Error finding walks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        })
                        .setNegativeButton("Cancel", null) // EXPLANATION: Passing null means clicking Cancel does nothing but close the box.
                        .show();
            }
        });

        // --- 4. LOG OUT BUTTON (UPDATED FOR FIREBASE AUTH) ---
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // EXPLANATION: Another safety net dialog to prevent accidental misclicks.
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes, Log Out", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // NOTE: This is the critical command! It tells the Google servers to officially
                                // lock the app and erase the temporary security token from the phone.
                                // EXPLANATION: This single line terminates the active user session.
                                FirebaseAuth.getInstance().signOut();

                                // Clear the local memory so the next person who logs in doesn't see your walks
                                // EXPLANATION: CRITICAL SECURITY FEATURE! If you share your phone, the next person logging in shouldn't see your static data lingering in the RAM.
                                Walk.walkHistory.clear();

                                Toast.makeText(SettingsActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                // EXPLANATION FOR BAGRUT: These Intent Flags are vital for a secure logout.
                                // FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK completely wipes the app's back-history.
                                // This means after they are sent to the Login screen, if they press the physical "Back" button on their phone, the app will close instead of glitching them back into the logged-in area!
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