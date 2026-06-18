package com.nitai.wanderer;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
// EXPLANATION: HashMap is a highly efficient Java data structure that stores items in "Key-Value" pairs (like a dictionary).
import java.util.HashMap;

public class LeaderboardActivity extends AppCompatActivity {

    // EXPLANATION: Standard UI variables for the scrolling list and back button.
    RecyclerView rvLeaderboard;
    MaterialButton btnLeaderboardBack;
    LeaderboardAdapter adapter;

    // EXPLANATION: This ArrayList will hold the final, sorted list of users that the RecyclerView actually displays on the screen.
    ArrayList<LeaderboardUser> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive Mode
        // EXPLANATION: Hides the system UI (battery, time, navigation bar) to keep the visual design consistent across all screens in the app.
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_leaderboard);

        btnLeaderboardBack = findViewById(R.id.btnLeaderboardBack);
        rvLeaderboard = findViewById(R.id.rvLeaderboard);

        // EXPLANATION: Tells the RecyclerView to stack the user cards vertically, one on top of the other.
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));

        // Connect the empty list to the screen for now
        // EXPLANATION: We attach the adapter immediately with an empty list. This prevents the RecyclerView from crashing before the internet data arrives.
        adapter = new LeaderboardAdapter(userList);
        rvLeaderboard.setAdapter(adapter);

        // Fetch Data from the Cloud
        // EXPLANATION: Triggers the network request to download the scores right when the screen opens.
        fetchLeaderboardData();

        btnLeaderboardBack.setOnClickListener(v -> finish()); // EXPLANATION: Closes the leaderboard and returns to the profile.
    }
    private void fetchLeaderboardData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Capture the current live system month and year for filtering
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH); // 0 = January, 11 = December
        int currentYear = now.get(java.util.Calendar.YEAR);

        // Formatter to read the string dates stored in your Walk objects
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

        db.collectionGroup("walks").get().addOnSuccessListener(queryDocumentSnapshots -> {

            HashMap<String, LeaderboardUser> userMap = new HashMap<>();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Walk walk = document.toObject(Walk.class);

                // --- NEW: MONTHLY FILTER LOGIC ---
                // If the walk doesn't have a date or fails the month check, skip it!
                if (walk.date == null || walk.date.isEmpty()) {
                    continue;
                }

                try {
                    java.util.Date walkDate = sdf.parse(walk.date);
                    java.util.Calendar walkCal = java.util.Calendar.getInstance();
                    walkCal.setTime(walkDate);

                    // If the walk's month or year doesn't match this current month, skip it
                    if (walkCal.get(java.util.Calendar.MONTH) != currentMonth ||
                            walkCal.get(java.util.Calendar.YEAR) != currentYear) {
                        continue; // Moves instantly to the next walk in the loop
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue; // Skip if the date format is corrupted
                }
                // ----------------------------------

                // Read the username safely
                String savedUsername = walk.username;
                if (savedUsername == null || savedUsername.isEmpty()) {
                    savedUsername = "Unknown Explorer";
                }

                // Grouping and adding stats (Same as before)
                if (!userMap.containsKey(savedUsername)) {
                    userMap.put(savedUsername, new LeaderboardUser(savedUsername));
                }

                LeaderboardUser user = userMap.get(savedUsername);
                user.addDistance(walk.distance);
                user.addTime(walk.time);
            }

            // Update UI list
            userList.clear();
            userList.addAll(userMap.values());

            // Sorts via your Comparable interface (highest distance, tie-broken by fastest time)
            Collections.sort(userList);
            adapter.notifyDataSetChanged();

        }).addOnFailureListener(e -> {
            Toast.makeText(LeaderboardActivity.this, "Failed to load leaderboard.", Toast.LENGTH_SHORT).show();
        });
    }
}