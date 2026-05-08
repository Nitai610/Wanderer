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
// NOTE: These Firebase imports are required to talk to the authentication server and cloud database
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class SettingsActivity extends AppCompatActivity {

    // NOTE: Declaring the buttons that exist in our activity_settings.xml design
    MaterialButton btnLogout, btnClearHistory, btnSettingsBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE: Immersive Mode setup.
        // If the examiner asks, this code hides the top battery/clock bar and bottom navigation buttons
        // so the app feels like a modern, full-screen experience. Swiping from the edge brings them back temporarily.
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        // Connects this Java code to the visual XML layout
        setContentView(R.layout.activity_settings);

        // NOTE: View Binding. We link our Java variables to the specific ID tags in the XML file.
        btnLogout = findViewById(R.id.btnLogout);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        btnSettingsBack = findViewById(R.id.btnSettingsBack);

        // --- GO BACK BUTTON LOGIC ---
        btnSettingsBack.setOnClickListener(v -> {
            // 'finish()' destroys this specific screen and drops the user back to whatever screen they came from (MainActivity)
            finish();
        });

        // --- CLEAR HISTORY BUTTON LOGIC ---
        btnClearHistory.setOnClickListener(v -> {
            // NOTE: We use an AlertDialog here as a safety measure.
            // Deleting data is permanent, so we must ask the user for confirmation before executing the cloud command.
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Clear Journal")
                    .setMessage("Are you sure you want to permanently delete all your saved walks from the cloud?")
                    .setPositiveButton("Delete All", (dialog, which) -> {

                        // 1. Get the current logged-in user
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null || user.getEmail() == null) return;

                        // 2. Connect to the Cloud Database
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        String userEmail = user.getEmail();

                        // 3. Locate the user's specific "walks" folder in the cloud
                        db.collection("users").document(userEmail).collection("walks").get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {

                                    // NOTE: We use a 'WriteBatch' here.
                                    // If a user has 100 walks, deleting them one by one is slow and takes 100 server calls.
                                    // A batch bundles all the delete commands together and executes them in one single, fast server call.
                                    WriteBatch batch = db.batch();
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        batch.delete(document.getReference());
                                    }

                                    // 4. Commit the batch and clear the local phone memory
                                    batch.commit().addOnSuccessListener(aVoid -> {
                                        Walk.walkHistory.clear(); // Empties the RAM array so the Journal screen updates instantly
                                        Toast.makeText(SettingsActivity.this, "Journal cleared", Toast.LENGTH_SHORT).show();
                                    });
                                });
                    })
                    .setNegativeButton("Cancel", null) // Does nothing, just dismisses the popup
                    .show();
        });

        // --- LOGOUT BUTTON LOGIC ---
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes, Log Out", (dialog, which) -> {

                        // 1. Tell the Firebase server this device is no longer authenticated
                        FirebaseAuth.getInstance().signOut();

                        // 2. Clear any downloaded walks from RAM for security
                        Walk.walkHistory.clear();
                        Toast.makeText(SettingsActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();

                        // 3. Navigate back to the Login screen
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        // NOTE: These flags are crucial! They clear the "backstack" (the history of opened screens).
                        // This prevents the user from pressing the physical 'Back' button on their phone to bypass the login screen.
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}