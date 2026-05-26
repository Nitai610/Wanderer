package com.nitai.wanderer;

// --- IMPORTS ---
// Importing Google Maps LatLng class to handle complex geographic coordinates (latitude and longitude)
import com.google.android.gms.maps.model.LatLng;
// Importing standard Java utilities for handling lists, dynamic arrays, and key-value dictionaries
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Walk class represents a single tracking session completed by a user.
 * It contains details about the session and built-in math tools to calculate statistics.
 */
public class Walk {

    // --- VARIABLES (Fields) ---
    public String username; // The unique name of the user who completed this specific walk (used for Leaderboards)
    public String distance; // The distance covered, saved as text (e.g., "5.23 KM")
    public String time;     // The duration of the walk, saved as text (e.g., "45:12" or "01:15:30")
    public String date;     // The calendar date of the walk, saved as text (e.g., "25/05/2026")

    /**
     * NOTE: FIRESTORE TRICK FOR THE MATRICULATION PROJECT (BAGRUT)
     * Cloud Firestore is a "JSON-like" NoSQL database. It cannot natively understand or store complex
     * Google Map objects like 'LatLng'. To fix this, we map the entire path out into a simple list of
     * HashMaps. Each HashMap inside this list acts as a simple key-value dictionary storing two basic numbers:
     * Key "lat" -> Latitude (Double)
     * Key "lng" -> Longitude (Double)
     */
    public List<HashMap<String, Double>> path;

    /**
     * GLOBAL CACHE ARRAYLIST (Static)
     * This master list acts as an in-memory cache. It holds all the walks downloaded from Firestore
     * inside the phone's RAM while the app is active, allowing different screens to read data instantly
     * without making constant, expensive network calls to the cloud.
     */
    public static ArrayList<Walk> walkHistory = new ArrayList<>();

    // =====================================================================
    // --- CONSTRUCTORS ---
    // =====================================================================

    /**
     * --- 1. THE EMPTY CONSTRUCTOR ---
     * MANDATORY FOR CLOUD FIRESTORE. When you request a document from Firestore and ask it to convert
     * the data back into a Java object automatically, Firestore instantiates a blank 'Walk' object using
     * this exact empty constructor first, and then it injects the cloud data into the fields.
     */
    public Walk() {
    }

    /**
     * --- 2. THE CREATION CONSTRUCTOR ---
     * This constructor is manually called by SummaryActivity when a user completely finishes tracking
     * a brand-new live walk and prepares to upload it.
     * * @param username   Name of the active user
     * @param distance   Total distance string (with " KM")
     * @param time       Total elapsed duration string
     * @param date       Current system calendar date
     * @param googlePath The raw, complex track points captured directly from the Google Maps API
     */
    public Walk(String username, String distance, String time, String date, ArrayList<LatLng> googlePath) {
        this.username = username; // Assigns the passed username parameter to this object's instance variable
        this.distance = distance; // Assigns the passed distance parameter to this object's instance variable
        this.time = time;         // Assigns the passed time parameter to this object's instance variable
        this.date = date;         // Assigns the passed date parameter to this object's instance variable

        // INITIALIZATION AND CONVERSION LOGIC:
        this.path = new ArrayList<>(); // Instantiates a fresh, empty list to house the simplified coordinate pairs
        if (googlePath != null) {      // Null-safety check: prevents app crashes if the map route is empty
            for (LatLng point : googlePath) { // Enhanced for-loop: loops through every single complex coordinate in the route

                // Create a lightweight, simple dictionary/map for the current GPS point
                HashMap<String, Double> cord = new HashMap<>();
                cord.put("lat", point.latitude);  // Matches the label "lat" to the numeric latitude double value
                cord.put("lng", point.longitude); // Matches the label "lng" to the numeric longitude double value

                // Append this newly simplified point into our Firestore-compatible list
                this.path.add(cord);
            }
        }
    }

    // =====================================================================
    // --- GETTERS (Data Delivery Methods) ---
    // =====================================================================

    public String getUsername() { return username; }
    public String getDate() { return date; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }

    /**
     * REVERSE CONVERSION METHOD
     * When a user opens their history log and clicks on an old walk, the app needs to draw it on a map view.
     * This method automatically translates our simple Firestore numbers BACK into complex Google Map LatLng structures.
     * * @return An ArrayList containing proper LatLng coordinate points that the Google Maps API can natively draw
     */
    public ArrayList<LatLng> getGooglePath() {
        ArrayList<LatLng> googlePath = new ArrayList<>(); // Create a fresh list to hold the output objects
        if (path != null) { // Safety check to ensure there is an existing path to extract
            for (HashMap<String, Double> p : path) { // Loop through each simplified database dictionary point

                // Reconstruct a formal Google Map coordinate using the numeric values fetched by their specific string keys
                googlePath.add(new LatLng(p.get("lat"), p.get("lng")));
            }
        }
        return googlePath; // Returns the finalized, map-ready list
    }

    // =====================================================================
    // --- ANALYTICS ENGINE (The Math Logic) ---
    // =====================================================================

    /**
     * CALCULATE ALL-TIME DISTANCE
     * Iterates through the entire history array and sums every single recorded kilometer.
     * * @return Sum total of all distances tracked across all time as a numeric float
     */
    public static float calculateAllTimeDistance() {
        float total = 0f; // Initialize our running sum at zero
        for (Walk walk : walkHistory) { // Loop through every single walk stored in the global cache array
            try {
                // STRING PARSING TRICK:
                // 1. Removes the text unit " KM" so only characters remain
                // 2. Replaces commas with periods (e.g., "5,2" -> "5.2") to match international float formats
                // 3. .trim() deletes hidden blank spaces at the edges
                String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();

                // Converts the cleaned string text into an actual math-ready float and adds it to our total sum
                total += Float.parseFloat(numberOnly);
            } catch (Exception e) {
                e.printStackTrace(); // Catch-block: If a single string is corrupted, print the error details but DO NOT crash the app
            }
        }
        return total; // Returns the global calculated sum
    }

    /**
     * CALCULATE MONTHLY DISTANCE
     * Filters the history log and sums up distances achieved only within the current calendar month.
     */
    public static float calculateMonthlyDistance() {
        float total = 0f;
        java.util.Calendar now = java.util.Calendar.getInstance(); // Captures the exact live system time and date right now
        int currentMonth = now.get(java.util.Calendar.MONTH);       // Extracts the current numerical month (0 = January, 11 = December)
        int currentYear = now.get(java.util.Calendar.YEAR);         // Extracts the current numerical calendar year (e.g., 2026)

        // Formatter pattern instructing Java how to read our string dates (dd=day, MM=month, yyyy=year)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                // Parse the text date string from the database back into a standard Java Date Object
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate); // Load the parsed walk date into a Calendar utility instance for structural sorting

                // FILTER CONDITION: Check if the walk's month and year exactly match our current active month and year
                if (walkCal.get(java.util.Calendar.MONTH) == currentMonth && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    // Extract, clean, parse, and accumulate the float distance value
                    String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                    total += Float.parseFloat(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    /**
     * CALCULATE WEEKLY DISTANCE
     * Filters the cache log and sums up distances achieved only within the current rolling calendar week.
     */
    public static float calculateWeeklyDistance() {
        float total = 0f;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentWeek = now.get(java.util.Calendar.WEEK_OF_YEAR); // Extracts the specific week index number out of the 52 weeks in a year
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                // FILTER CONDITION: Check if the walk matches the current week index and the current year
                if (walkCal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                    total += Float.parseFloat(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }

    /**
     * PRIVATE HELPER METHOD: PARSE TIME TO SECONDS
     * Doing calculations on colon-separated text strings (like "15:30") is extremely difficult.
     * This utility helper slices the text string up and boils it down into total raw integer seconds.
     * * @param timeStr A time text input string (e.g. "MM:SS" or "HH:MM:SS")
     * @return The exact equivalent duration expressed entirely as a single integer of total raw seconds
     */
    private static int parseTimeToSeconds(String timeStr) {
        try {
            // Slices the string wherever a colon exists. Example: "15:30" becomes an array containing: ["15", "30"]
            String[] parts = timeStr.trim().split(":");

            if (parts.length == 2) {
                // Format is MM:SS (Minutes and Seconds)
                // Convert minutes text to integer, multiply by 60 to get seconds, then add the remaining seconds integer
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                // Format is HH:MM:SS (Hours, Minutes, and Seconds)
                // Hours * 3600 + Minutes * 60 + Seconds
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0; // Returns 0 if the string is empty or unparseable
    }

    /**
     * PRIVATE HELPER METHOD: FORMAT SECONDS TO TIME STRING
     * The reverse math tool. After adding up hundreds of thousands of total raw seconds,
     * this tool converts those raw numbers back into readable user interface text layouts.
     * * @param totalSeconds Accumulation of raw mathematical seconds
     * @return A styled text string (e.g., "02:15:40" or "05:12")
     */
    private static String formatSecondsToTimeString(int totalSeconds) {
        int hours = totalSeconds / 3600;            // Extracts whole hours (3600 seconds per hour)
        int minutes = (totalSeconds % 3600) / 60;   // Takes remaining seconds, extracts whole minutes (60 seconds per minute)
        int seconds = totalSeconds % 60;            // The ultimate remainder represents leftover seconds

        if (hours > 0) {
            // If the time spans past an hour, print full HH:MM:SS format
            // %02d forces numerical integers to always display at least two digits, adding placeholder zeros if needed (e.g., "5" -> "05")
            return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            // If shorter than an hour, keep it cleaner using basic MM:SS format
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * CALCULATE ALL-TIME DURATION
     * Sums up every single second spent walking across all history entries and formats the result.
     */
    public static String calculateAllTimeDuration() {
        int totalSeconds = 0; // Initialize total second counter
        for (Walk walk : walkHistory) {
            // Passes each text string into our parsing helper, and adds the returned raw integer value to the total counter
            totalSeconds += parseTimeToSeconds(walk.time);
        }
        // Passes the absolute sum into our styling helper to produce UI text
        return formatSecondsToTimeString(totalSeconds);
    }

    /**
     * CALCULATE MONTHLY DURATION
     * Sums up duration seconds tracked exclusively during the current calendar month.
     */
    public static String calculateMonthlyDuration() {
        int totalSeconds = 0;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentMonth = now.get(java.util.Calendar.MONTH);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                // Date Filter validation check
                if (walkCal.get(java.util.Calendar.MONTH) == currentMonth && walkCal.get(java.util.Calendar.YEAR) == currentYear) {
                    totalSeconds += parseTimeToSeconds(walk.time); // Accumulate matching seconds
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return formatSecondsToTimeString(totalSeconds);
    }

    /**
     * CALCULATE WEEKLY DURATION
     * Sums up duration seconds tracked exclusively during the current active week.
     */
    public static String calculateWeeklyDuration() {
        int totalSeconds = 0;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentWeek = now.get(java.util.Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

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

    /**
     * CALCULATE DAILY DISTANCE
     * Filters the history logs to aggregate kilometers accumulated purely on the present day.
     */
    public static float calculateDailyDistance() {
        float total = 0f;
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentDay = now.get(java.util.Calendar.DAY_OF_YEAR); // Returns the day index out of 365 days in a year (e.g., Feb 1st = day 32)
        int currentYear = now.get(java.util.Calendar.YEAR);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

        for (Walk walk : walkHistory) {
            try {
                java.util.Date walkDate = sdf.parse(walk.date);
                java.util.Calendar walkCal = java.util.Calendar.getInstance();
                walkCal.setTime(walkDate);

                // Check if the walk happened on the exact same numerical day of the year AND year match
                if (walkCal.get(java.util.Calendar.DAY_OF_YEAR) == currentDay &&
                        walkCal.get(java.util.Calendar.YEAR) == currentYear) {

                    // Process, clean, convert, and sum the distance data
                    String numberOnly = walk.distance.replace(" KM", "").replace(",", ".").trim();
                    total += Float.parseFloat(numberOnly);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return total;
    }
}