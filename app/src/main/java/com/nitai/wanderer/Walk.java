package com.nitai.wanderer;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// NOTE FOR BAGRUT: This is our "Model" class in Object-Oriented Programming (OOP).
// It acts as a blueprint. Every time the user finishes a walk, we create a new 'Walk' object using this blueprint.
public class Walk {

    // ==========================================
    // --- 1. CLASS VARIABLES (PROPERTIES) ---
    // ==========================================
    public String username;
    public String distance;
    public String time;
    public String steps; // NOTE: Stores the steps. We use String so it's easy to read straight from the screen's text.
    public String date;

    // NOTE FOR BAGRUT: Why do we use a List of HashMaps instead of just a List of Google's 'LatLng' objects?
    // ANSWER: Cloud Firestore only knows how to save basic data (Strings, Numbers, HashMaps/Dictionaries).
    // It does NOT know what a Google 'LatLng' object is. So, we must translate Google's complex map points
    // into simple X/Y number coordinates (HashMaps) before sending them to the cloud.
    public List<HashMap<String, Double>> path;

    // NOTE FOR BAGRUT: What does 'static' mean here?
    // ANSWER: 'static' means this list belongs to the app itself, not to any single walk.
    // There is only ONE 'walkHistory' list in the entire phone's memory. All screens share this exact same list.
    public static ArrayList<Walk> walkHistory = new ArrayList<>();

    // ==========================================
    // --- 2. CONSTRUCTORS ---
    // ==========================================

    // NOTE FOR BAGRUT: Why do we have an empty constructor?
    // ANSWER: Cloud Firestore REQUIRES an empty constructor. When the app downloads data from the cloud,
    // Firestore creates a "blank" Walk object first, and then fills in the variables one by one. If you delete this, the app will crash!
    public Walk() {
    }

    // NOTE: This is the constructor we use in SummaryActivity when the user clicks "Save Walk".
    public Walk(String username, String distance, String time, String steps, String date, ArrayList<LatLng> googlePath) {
        this.username = username;
        this.distance = distance;
        this.time = time;
        this.steps = steps;
        this.date = date;

        // NOTE: Here we translate the complex Google Map path into simple HashMaps for the cloud.
        this.path = new ArrayList<>();
        if (googlePath != null) {
            for (LatLng point : googlePath) {
                HashMap<String, Double> cord = new HashMap<>();
                cord.put("lat", point.latitude); // Save Latitude (Y axis)
                cord.put("lng", point.longitude); // Save Longitude (X axis)
                this.path.add(cord);
            }
        }
    }

    // ==========================================
    // --- 3. GETTERS ---
    // ==========================================
    public String getUsername() { return username; }
    public String getDate() { return date; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }
    public String getSteps() { return steps; }

    // NOTE: When JournalActivity wants to draw the map again, we must translate the simple HashMaps BACK into Google 'LatLng' objects.
    public ArrayList<LatLng> getGooglePath() {
        ArrayList<LatLng> googlePath = new ArrayList<>();
        if (path != null) {
            for (HashMap<String, Double> p : path) {
                googlePath.add(new LatLng(p.get("lat"), p.get("lng")));
            }
        }
        return googlePath;
    }

    // ==========================================
    // --- 4. ANALYTICS ENGINE (MATH METHODS) ---
    // ==========================================

    // --- DISTANCE MATH ---
    public static float calculateAllTimeDistance() {
        float total = 0f;
        for (Walk walk : walkHistory) {
            try { // NOTE FOR BAGRUT: We use try/catch to prevent the app from crashing if the text isn't a valid number.
                String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                total += Float.parseFloat(numberOnly);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    public static float calculateMonthlyDistance() {
        float total = 0f;
        // NOTE: Calendar helps us figure out what month we are currently in.
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                // NOTE: We convert the saved "String" date back into a real Java "Date" object so we can compare it.
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

    // --- TIME MATH ---
    // NOTE: It is impossible to do math on "15:30". So we write a helper method to turn it into raw seconds.
    private static int parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.trim().split(":"); // Splits "15:30" into an array -> ["15", "30"]
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]); // Minutes * 60 + Seconds
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]); // Hours * 3600...
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // NOTE: After we add up all the seconds, we must format it back to a beautiful string to show on the screen.
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

    // --- STEPS MATH ---
    // NOTE FOR BAGRUT: "Why do you check if walk.steps != null?"
    // ANSWER: "Backward compatibility! I added the steps feature later in the project. If I download an old walk
    // from my database that doesn't have a 'steps' field, it will be null. Checking for null prevents a NullPointerException crash."

    public static int calculateAllTimeSteps() {
        int total = 0;
        for (Walk walk : walkHistory) {
            try {
                if (walk.steps != null && !walk.steps.isEmpty()) {
                    String numberOnly = walk.steps.replace(" Steps", "").trim();
                    total += Integer.parseInt(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    public static int calculateMonthlySteps() {
        int total = 0;
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
                    if (walk.steps != null && !walk.steps.isEmpty()) {
                        String numberOnly = walk.steps.replace(" Steps", "").trim();
                        total += Integer.parseInt(numberOnly);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    public static int calculateWeeklySteps() {
        int total = 0;
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
                    if (walk.steps != null && !walk.steps.isEmpty()) {
                        String numberOnly = walk.steps.replace(" Steps", "").trim();
                        total += Integer.parseInt(numberOnly);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }
}