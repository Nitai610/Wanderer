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

        // EXPLANATION: THIS IS A CRITICAL EXAM CONCEPT!
        // Previously, you used db.collection("users").document(email).collection("walks") to get ONE user's data.
        // .collectionGroup("walks") is a special Firestore query that searches the ENTIRE database and pulls every single document that lives inside ANY folder named "walks", regardless of which user it belongs to!
        db.collectionGroup("walks").get().addOnSuccessListener(queryDocumentSnapshots -> {

            // We now group walks together by Username!
            // EXPLANATION: A HashMap is perfect here. The "Key" is the Username (String), and the "Value" is the total stats for that user (LeaderboardUser object).
            // It allows us to easily check: "Have we seen a walk from Nitai yet?" If yes, add to his total. If no, create a new slot for him.
            HashMap<String, LeaderboardUser> userMap = new HashMap<>();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                // EXPLANATION: Automatically converts the JSON cloud data into your Java Walk object.
                Walk walk = document.toObject(Walk.class);

                // Read the username directly from the saved Walk
                String savedUsername = walk.username;

                // Safety check in case it's a very old walk before we added the username feature
                // EXPLANATION: Excellent defensive programming. If you test the app with old database entries that don't have a username saved, this prevents the app from crashing by assigning a default name.
                if (savedUsername == null || savedUsername.isEmpty()) {
                    savedUsername = "Unknown Explorer";
                }

                // If this is the first time we've seen this user, add them to our Map
                // EXPLANATION: .containsKey() checks if this username already has a "bucket" in our HashMap. If not, we create a fresh LeaderboardUser object for them.
                if (!userMap.containsKey(savedUsername)) {
                    userMap.put(savedUsername, new LeaderboardUser(savedUsername));
                }

                // Add the walk's distance and time to the user's running total
                // EXPLANATION: .get() pulls the user's specific "bucket" out of the HashMap.
                LeaderboardUser user = userMap.get(savedUsername);

                // EXPLANATION: We take the distance/time from this specific walk and add it to their grand total.
                user.addDistance(walk.distance);
                user.addTime(walk.time);
            }

            // EXPLANATION: Now that we have calculated all the totals, we need to move them from the HashMap back into a standard ArrayList so the RecyclerView can understand them.
            userList.clear(); // Empty the old visual list
            userList.addAll(userMap.values()); // Dump all the calculated LeaderboardUser objects into the list

            // EXPLANATION: Sorts the list. For this to work without errors, your 'LeaderboardUser' class MUST implement the 'Comparable' interface to teach Java exactly how to rank the users (e.g., highest distance wins).
            Collections.sort(userList);

            // EXPLANATION: Tells the visual list to redraw itself now that the data has been sorted and calculated.
            adapter.notifyDataSetChanged();

        }).addOnFailureListener(e -> {
            // EXPLANATION: Standard error handling if the device has no internet connection.
            Toast.makeText(LeaderboardActivity.this, "Failed to load leaderboard.", Toast.LENGTH_SHORT).show();
        });
    }
}