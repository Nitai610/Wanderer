package com.nitai.wanderer;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class Walk {
    public String distance;
    public String time;
    public String date;

    public static ArrayList<Walk> walkHistory = new ArrayList<>();
    public Walk(String distance, String time, String date) {
        this.distance = distance;
        this.time = time;
        this.date = date;
    }

    // --- NEW: PERMANENT STORAGE POWERS ---

    // 1. Converts the list to text and saves it to the phone
    public static void saveHistory(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("WandererPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(walkHistory);
        editor.putString("WalkList", json);
        editor.apply();
    }

    // 2. Reads the text from the phone and turns it back into a list
    public static void loadHistory(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("WandererPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("WalkList", null);

        // If there is saved data, load it! If not, make a blank list.
        if (json != null) {
            Type type = new TypeToken<ArrayList<Walk>>() {}.getType();
            walkHistory = gson.fromJson(json, type);
        } else {
            walkHistory = new ArrayList<>();
        }
    }// --- NEW: ANALYTICS ENGINE ---

    // 1. All-Time Total
    public static float calculateAllTimeDistance() {
        float total = 0f;
        for (Walk walk : walkHistory) {
            try {
                // Strip the " KM" away and convert "3.20" to a real number
                String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                total += Float.parseFloat(numberOnly);
            } catch (Exception e) {
                e.printStackTrace(); // Failsafe in case a string is weird
            }
        }
        return total;
    }

    // 2. This Month's Total
    public static float calculateMonthlyDistance() {
        float total = 0f;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                if (walkCal.get(java.util.Calendar.MONTH) == currentMonth && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                    total += Float.parseFloat(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    // 3. This Week's Total
    public static float calculateWeeklyDistance() {
        float total = 0f;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentWeek = now.get(java.util.Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                if (walkCal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                    total += Float.parseFloat(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }
    // --- NEW: TIME ANALYTICS ENGINE ---

    // Helper 1: Turns "15:30" or "01:15:30" into total raw seconds for math
    private static int parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]); // MM:SS
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]); // HH:MM:SS
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // Helper 2: Turns raw seconds back into a beautiful "00:00:00" string
    private static String formatSecondsToTimeString(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    // 1. All-Time Total Time
    public static String calculateAllTimeDuration() {
        int totalSeconds = 0;
        for (Walk walk : walkHistory) {
            totalSeconds += parseTimeToSeconds(walk.time);
        }
        return formatSecondsToTimeString(totalSeconds);
    }

    // 2. This Month's Total Time
    public static String calculateMonthlyDuration() {
        int totalSeconds = 0;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                if (walkCal.get(java.util.Calendar.MONTH) == currentMonth && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    totalSeconds += parseTimeToSeconds(walk.time);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return formatSecondsToTimeString(totalSeconds);
    }

    // 3. This Week's Total Time
    public static String calculateWeeklyDuration() {
        int totalSeconds = 0;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentWeek = now.get(java.util.Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                if (walkCal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    totalSeconds += parseTimeToSeconds(walk.time);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return formatSecondsToTimeString(totalSeconds);
    }
    public String getDate() {
        return date; // or whatever your variable is named
    }

    public String getDistance() {
        return distance;
    }

    public String getTime() {
        return time;
    }
}