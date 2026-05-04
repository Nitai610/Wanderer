package com.nitai.wanderer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

public class JournalActivity extends AppCompatActivity {

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
        // ----------------------

        setContentView(R.layout.activity_journal);

        // 1. Connect UI
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnEmptyStartWalk = findViewById(R.id.btnEmptyStartWalk);
        btnJournalBack = findViewById(R.id.btnJournalBack);
        recyclerViewJournal = findViewById(R.id.recyclerViewJournal);

        // Use a standard layout manager (Remember, we fixed the reverse order issue
        // by adding new walks to index 0 in the SummaryActivity instead!)
        recyclerViewJournal.setLayoutManager(new LinearLayoutManager(this));

        // 2. LOGIC: Show the empty box or the list?
        if (Walk.walkHistory.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE); // Show the empty box
            recyclerViewJournal.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE); // Hide the empty box
            recyclerViewJournal.setVisibility(View.VISIBLE);

            // Set up the Adapter with the delete bridge
            adapter = new WalkAdapter(Walk.walkHistory, new WalkAdapter.OnWalkDeleteListener() {
                @Override
                public void onWalkDeleted() {

                    // NOTE: We removed Walk.saveHistory() because we use Firestore now!
                    // This currently removes the walk from the screen perfectly.
                    // (If you want it to permanently delete from the cloud later, let me know!)

                    // If they just deleted the very last walk, show the empty box again
                    if (Walk.walkHistory.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        recyclerViewJournal.setVisibility(View.GONE);
                    }
                }
            });
            recyclerViewJournal.setAdapter(adapter);
        }

        // 3. Start Walk Button Logic (From the Empty State)
        btnEmptyStartWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JournalActivity.this, TravelActivity.class);
                startActivity(intent);
                finish(); // Close the journal so they return to Main Menu later
            }
        });

        // 4. Global Go Back Button Logic (Always at the bottom)
        btnJournalBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes the journal screen safely
            }
        });
    }
}