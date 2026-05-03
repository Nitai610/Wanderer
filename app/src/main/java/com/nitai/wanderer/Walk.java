package com.nitai.wanderer;

import android.content.Context;
import android.content.SharedPreferences;

// Imports needed for the map coordinates and saving data
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Walk {
    // --- VARIABLES ---
    public String distance;
    public String time;
    public String date;
    // NEW: The "backpack" that holds every GPS coordinate from the walk
    public ArrayList<LatLng> path;

    // The master list that holds all saved walks in the app's memory
    public static ArrayList<Walk> walkHistory = new ArrayList<>();

    // --- CONSTRUCTOR ---
    // This is called whenever you create a brand new Walk object.
    // We updated it to require the GPS path.
    public Walk(String distance, String time, String date, ArrayList<LatLng> path) {
        this.distance = distance;
        this.time = time;
        this.date = date;
        this.path = path;
    }

    // --- GETTERS ---
    // Used by the Adapter to pull data out of the Walk object
    public String getDate() { return date; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }
    public ArrayList<LatLng> getPath() { return path; }

    // --- PERMANENT STORAGE POWERS ---

    // 1. Save to Phone
    public static void saveHistory(Context context) {
        // Open the phone's hidden local storage ("SharedPreferences")
        SharedPreferences sharedPreferences = context.getSharedPreferences("WandererPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Gson translates our entire list of Walks (including the complex GPS points) into one long text string
        Gson gson = new Gson();
        String json = gson.toJson(walkHistory);

        // Save the text string and apply the changes
        editor.putString("WalkList", json);
        editor.apply();
    }

    // 2. Load from Phone
    public static void loadHistory(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("WandererPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        // Grab the saved text string (returns 'null' if nothing is saved yet)
        String json = sharedPreferences.getString("WalkList", null);

        if (json != null) {
            // Tell Gson exactly what kind of data it needs to translate the text back into
            Type type = new TypeToken<ArrayList<Walk>>() {}.getType();
            walkHistory = gson.fromJson(json, type);
        } else {
            // If the app is opened for the very first time, create a blank list
            walkHistory = new ArrayList<>();
        }
    }

    // --- ANALYTICS ENGINE (Your existing math code) ---
    // These functions calculate the totals for your Stats screen.

    public static float calculateAllTimeDistance() {
        float total = 0f;
        for (Walk walk : walkHistory) {
            try {
                // Remove " KM" and convert the text (like "3.50") into a real math number
                String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                total += Float.parseFloat(numberOnly);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

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
                // Only add to the total if the walk's month and year match right now
                if (walkCal.get(java.util.Calendar.MONTH) == currentMonth && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                    total += Float.parseFloat(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

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

    // --- TIME ANALYTICS ENGINE (Your existing math code) ---

    // Helper: Turns "15:30" (MM:SS) into raw seconds for easier math
    private static int parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // Helper: Turns raw seconds back into a beautiful "00:00:00" format
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

    public static String calculateAllTimeDuration() {
        int totalSeconds = 0;
        for (Walk walk : walkHistory) {
            totalSeconds += parseTimeToSeconds(walk.time);
        }
        return formatSecondsToTimeString(totalSeconds);
    }

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
}