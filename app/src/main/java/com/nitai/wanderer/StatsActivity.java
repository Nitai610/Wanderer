package com.nitai.wanderer;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

public class StatsActivity extends AppCompatActivity {

    // 1. Declare ALL 9 UI elements
    TextView tvStatsWeekly, tvStatsMonthly, tvStatsAllTime;
    TextView tvStatsWeeklyTime, tvStatsMonthlyTime, tvStatsAllTimeTime;
    TextView tvStatsWeeklySteps, tvStatsMonthlySteps, tvStatsAllTimeSteps;
    MaterialButton btnStatsBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE FOR BAGRUT: Immersive Mode!
        // This code hides the status bar (clock/battery) and navigation bar.
        // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE means the user can swipe from the edge to temporarily bring them back.
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_stats);

        // 2. View Binding: Connect Java variables to the ID tags in XML
        tvStatsWeekly = findViewById(R.id.tvStatsWeekly);
        tvStatsMonthly = findViewById(R.id.tvStatsMonthly);
        tvStatsAllTime = findViewById(R.id.tvStatsAllTime);

        tvStatsWeeklyTime = findViewById(R.id.tvStatsWeeklyTime);
        tvStatsMonthlyTime = findViewById(R.id.tvStatsMonthlyTime);
        tvStatsAllTimeTime = findViewById(R.id.tvStatsAllTimeTime);

        tvStatsWeeklySteps = findViewById(R.id.tvStatsWeeklySteps);
        tvStatsMonthlySteps = findViewById(R.id.tvStatsMonthlySteps);
        tvStatsAllTimeSteps = findViewById(R.id.tvStatsAllTimeSteps);

        btnStatsBack = findViewById(R.id.btnStatsBack);

        btnStatsBack.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                finish(); // Closes the Activity
            }
        });
    }

    // NOTE FOR BAGRUT: THE ANDROID LIFECYCLE (Important!)
    // Examiner: "Why did you put the math inside onResume() instead of onCreate()?"
    // Answer: "onCreate() only runs once when the screen is first built. If the user minimizes the app,
    // goes for a walk, and comes back, onCreate() won't run again, so the stats would be stale.
    // onResume() is triggered EVERY SINGLE TIME the screen comes into focus. Putting the math here
    // guarantees that the user always sees the absolute newest, fresh data."
    @Override
    protected void onResume() {
        super.onResume();

        // 3. Grab Distance totals from Walk.java & Set Text
        float weeklyTotal = Walk.calculateWeeklyDistance();
        float monthlyTotal = Walk.calculateMonthlyDistance();
        float allTimeTotal = Walk.calculateAllTimeDistance();

        // NOTE: String.format with Locale.US ensures the distance uses a period instead of a comma (e.g., 5.50 KM, not 5,50 KM)
        tvStatsWeekly.setText(String.format(java.util.Locale.US, "%.2f KM", weeklyTotal));
        tvStatsMonthly.setText(String.format(java.util.Locale.US, "%.2f KM", monthlyTotal));
        tvStatsAllTime.setText(String.format(java.util.Locale.US, "%.2f KM", allTimeTotal));

        // 4. Grab Time totals & Set Text
        tvStatsWeeklyTime.setText(Walk.calculateWeeklyDuration());
        tvStatsMonthlyTime.setText(Walk.calculateMonthlyDuration());
        tvStatsAllTimeTime.setText(Walk.calculateAllTimeDuration());

        // 5. Grab Step Totals & Set Text
        // NOTE: Walk.calculateWeeklySteps() returns an integer (int). We cannot set an 'int' directly to a TextView.
        // We MUST use String.valueOf() to convert the math number into a String text so Android can display it.
        tvStatsWeeklySteps.setText(String.valueOf(Walk.calculateWeeklySteps()));
        tvStatsMonthlySteps.setText(String.valueOf(Walk.calculateMonthlySteps()));
        tvStatsAllTimeSteps.setText(String.valueOf(Walk.calculateAllTimeSteps()));
    }
}