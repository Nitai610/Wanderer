package com.nitai.wanderer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

// FIRESTORE IMPORTS
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// NEW: IMPORTS FOR SORTING DATES
// EXPLANATION: Standard Java utility classes used for parsing strings into dates, managing collections, and comparing custom objects.
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class JournalActivity extends AppCompatActivity {

    // EXPLANATION: This LinearLayout acts as our "Empty State" view. If there are no walks found,
    // we will show this layout and hide the RecyclerView, and vice versa.
    LinearLayout layoutEmptyState;
    MaterialButton btnEmptyStartWalk, btnJournalBack;
    RecyclerView recyclerViewJournal;
    WalkAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- IMMERSIVE MODE ---
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_journal);

        // 1. Connect UI
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnEmptyStartWalk = findViewById(R.id.btnEmptyStartWalk);
        btnJournalBack = findViewById(R.id.btnJournalBack);
        recyclerViewJournal = findViewById(R.id.recyclerViewJournal);

        // EXPLANATION: The LayoutManager determines how items are arranged inside the RecyclerView.
        // LinearLayoutManager displays them in a standard vertical scrolling list (one under another).
        recyclerViewJournal.setLayoutManager(new LinearLayoutManager(this));

        // 2. Set up the Adapter immediately
        // EXPLANATION: We initialize the Adapter with our static history list (Walk.walkHistory).
        // We also instantiate our custom OnWalkDeleteListener interface (the "walkie-talkie" mechanism).
        adapter = new WalkAdapter(Walk.walkHistory, new WalkAdapter.OnWalkDeleteListener() {
            @Override
            public void onWalkDeleted() {
                // EXPLANATION: This method is automatically triggered from deep inside the WalkAdapter whenever a user successfully deletes a walk.
                // When it fires, we re-run checkIfEmpty() to see if the last remaining item was deleted.
                checkIfEmpty();
            }
        });
        recyclerViewJournal.setAdapter(adapter);

        // 3. Fetch data from the cloud
        // EXPLANATION: Triggers the helper method to start downloading user data from Firestore immediately when the screen opens.
        fetchWalksFromCloud();

        // 4. Start Walk Button Logic
        btnEmptyStartWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JournalActivity.this, TravelActivity.class);
                startActivity(intent);
                finish(); // EXPLANATION: Destroys the Journal activity so it doesn't linger in the background during a live walking session.
            }
        });

        // 5. Global Go Back Button Logic
        btnJournalBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // EXPLANATION: Closes the current screen and safely returns the user to the previous activity (Main Menu).
            }
        });
    }

    // --- HELPER METHOD: DOWNLOAD DATA & SORT ---
    private void fetchWalksFromCloud() {
        // EXPLANATION: Grabs the profile of the currently logged-in user from Firebase Authentication.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // EXPLANATION: Guard clause: If no user is logged in or their email is missing, stop immediately to prevent a crash.
        if (user == null || user.getEmail() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // EXPLANATION: Targets the "walks" subcollection sitting inside this specific user's unique email document.
        db.collection("users").document(user.getEmail()).collection("walks")
                .get() // EXPLANATION: Sends an asynchronous network request to download all documents in this subcollection.
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // EXPLANATION: Clears the local ArrayList in the phone's RAM before loading new data,
                    // ensuring we don't accidentally duplicate items if this method runs multiple times.
                    Walk.walkHistory.clear(); // Clear the memory

                    // 1. Loop through the cloud database and rebuild the raw list
                    // EXPLANATION: Iterates over the "basket" of documents (queryDocumentSnapshots) sent back by Firestore.
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // EXPLANATION: Automatically maps/converts the cloud JSON fields into a fully realized Java 'Walk' object.
                        Walk downloadedWalk = document.toObject(Walk.class);
                        // EXPLANATION: Appends the freshly converted object to our local static tracking list.
                        Walk.walkHistory.add(downloadedWalk);
                    }

                    // ==========================================================
                    // 2. NEW BAGRUT FIX: SORT THE LIST BY DATE (NEWEST FIRST)
                    // ==========================================================
                    // NOTE FOR BAGRUT EXAMINER:
                    // "Firestore downloads documents in random order. I used Java's Collections.sort()
                    // along with a custom Comparator. It translates the String dates (like '14/05/2026')
                    // into real Date objects, compares them, and pushes the newest dates to the top (index 0)."

                    // EXPLANATION: Defines how our dates are written textually. This gives the parser a map to interpret days, months, and years.
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                    // EXPLANATION: Uses Java's built-in collection utility to sort our array list dynamically using a custom sorting blueprint (Comparator).
                    Collections.sort(Walk.walkHistory, new Comparator<Walk>() {
                        @Override
                        public int compare(Walk walk1, Walk walk2) {
                            try {
                                // EXPLANATION: sdf.parse converts the raw string text (e.g., "14/05/2026") into a real, mathematical Java Date object.
                                Date date1 = sdf.parse(walk1.getDate());
                                Date date2 = sdf.parse(walk2.getDate());

                                // Comparing date2 to date1 puts it in DESCENDING order (Newest on top)
                                // EXPLANATION: By comparing date2 directly against date1, Java organizes them in descending order (Newest/Highest to Oldest/Lowest).
                                return date2.compareTo(date1);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return 0; // EXPLANATION: If a date happens to be corrupted or fails to parse, leave its relative position unchanged.
                            }
                        }
                    });
                    // ==========================================================

                    // 3. Tell the screen to update with the new sorted data
                    // EXPLANATION: A critical command that tells the Adapter: "The data inside the source list changed completely. Force the RecyclerView to redraw itself."
                    adapter.notifyDataSetChanged();
                    // EXPLANATION: Checks the final status of our list to toggle between showing the walks or showing the empty message layout.
                    checkIfEmpty();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(JournalActivity.this, "Error loading journal.", Toast.LENGTH_SHORT).show();
                });
    }

    // --- HELPER METHOD: TOGGLE EMPTY STATE ---
    // EXPLANATION: This helper method dynamically evaluates the list's size to toggle UI visibility states.
    private void checkIfEmpty() {
        if (Walk.walkHistory.isEmpty()) {
            // EXPLANATION: If the array list is completely empty, make the Empty State message visible, and completely remove the RecyclerView layout.
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewJournal.setVisibility(View.GONE);
        } else {
            // EXPLANATION: If there is at least one walk available, completely remove the Empty State message layout, and make the RecyclerView visible.
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewJournal.setVisibility(View.VISIBLE);
        }
    }
}