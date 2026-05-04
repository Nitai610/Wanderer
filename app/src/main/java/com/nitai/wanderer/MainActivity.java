package com.nitai.wanderer; // Make sure this matches your new package name!

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    // 1. Declare ALL the UI elements
    ImageButton btnProfile, btnSettings;
    View btnStartWalk, btnJournal, btnStats; // Using "View" here makes it safe whether you used Buttons or MaterialButtons in XML
    TextView tvWeeklyDistanceMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- NEW: TURN ON IMMERSIVE MODE ---
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        // Tells Android to let the user swipe from the edge to temporarily see the battery/buttons
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // Actually hides the top (status) and bottom (navigation) bars!
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        // -----------------------------------

        setContentView(R.layout.activity_main);

        // Load the saved walks from the phone's hard drive!
        // Make sure to import these at the top:
//

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // SECURITY CHECK: If they are not logged in, kick them to the Login Screen!
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // LOAD DATA FROM CLOUD
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("walks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Walk.walkHistory.clear(); // Clear old data

                    // Loop through every walk downloaded from the cloud
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Walk downloadedWalk = document.toObject(Walk.class);
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // Force the screen to update the math totals now that data is loaded
                    float weeklyTotal = Walk.calculateWeeklyDistance();
                    tvWeeklyDistanceMain.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
                });

        // 2. Connect Java to the XML IDs
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        tvWeeklyDistanceMain = findViewById(R.id.tvWeeklyDistanceMain);

        // Note: Make sure these IDs exactly match what you named them in activity_main.xml!
        btnStartWalk = findViewById(R.id.btnStartWalk);
        btnJournal = findViewById(R.id.btnJournal);
        btnStats = findViewById(R.id.btnStats);

        // 3. Profile Button Logic (Log Out)
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 4. Settings Button Logic
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        // 5. Start Walk Button Logic
        btnStartWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TravelActivity.class);
                startActivity(intent);
            }
        });

        // 6. Journal Button Logic
        btnJournal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JournalActivity.class);
                startActivity(intent);
            }
        });

        // 7. Stats Button Logic
        btnStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                startActivity(intent);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Recalculate and update the Main Menu card every time the screen appears
        float weeklyTotal = Walk.calculateWeeklyDistance();
        if (tvWeeklyDistanceMain != null) {
            tvWeeklyDistanceMain.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        }
    }
}