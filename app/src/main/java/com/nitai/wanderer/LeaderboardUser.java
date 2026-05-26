package com.nitai.wanderer;

// 'Comparable' allows Java to automatically sort a list of these users!
// EXPLANATION: By implementing the 'Comparable' interface, you are making a promise to Java:
// "I will provide a custom compareTo() method inside this class so you know exactly how to rank these objects."
public class LeaderboardUser implements Comparable<LeaderboardUser> {

    // EXPLANATION: The core properties (fields) of your object.
    public String username;
    public float totalDistance;
    public int totalTimeSeconds;

    // EXPLANATION: The Constructor. When you create a 'new LeaderboardUser("Nitai")',
    // it sets the name and initializes the math variables to zero so they are ready to be added to.
    public LeaderboardUser(String username) {
        this.username = username;
        this.totalDistance = 0f;
        this.totalTimeSeconds = 0;
    }

    // Helper method to add distance from a string (like "3.50 KM")
    public void addDistance(String distanceStr) {
        // EXPLANATION: A 'try-catch' block is defensive programming. If the database has corrupted text that isn't a number, it will fail silently instead of crashing the whole app.
        try {
            // EXPLANATION: String Manipulation.
            // 1. .replace(" KM", "") strips the text away leaving just the number.
            // 2. .replace(",", ".") is a brilliant localization fix! Some Hebrew/European keyboards save decimals with a comma. Java math requires a dot.
            // 3. .trim() removes invisible spaces.
            String numberOnly = distanceStr.replace(" KM", "").replace(",", ".").trim();

            // EXPLANATION: Float.parseFloat() translates the raw text string into a mathematical floating-point number, then adds it (+=) to the user's running total.
            this.totalDistance += Float.parseFloat(numberOnly);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Helper method to add time from a string (like "15:30")
    public void addTime(String timeStr) {
        try {
            // EXPLANATION: .split(":") acts like a knife, cutting the string into an array of smaller strings wherever there is a colon.
            String[] parts = timeStr.trim().split(":");

            // EXPLANATION: If the array length is 2, the format was MM:SS (Minutes and Seconds).
            if (parts.length == 2) {
                // EXPLANATION: Integer.parseInt() turns the text into a math number. We multiply minutes by 60 to convert everything to pure seconds.
                this.totalTimeSeconds += (Integer.parseInt(parts[0]) * 60) + Integer.parseInt(parts[1]);

                // EXPLANATION: If the array length is 3, the format was HH:MM:SS (Hours, Minutes, and Seconds).
            } else if (parts.length == 3) {
                // EXPLANATION: We multiply hours by 3600 (60 * 60) to convert them to seconds, minutes by 60, and add the leftover seconds.
                this.totalTimeSeconds += (Integer.parseInt(parts[0]) * 3600) + (Integer.parseInt(parts[1]) * 60) + Integer.parseInt(parts[2]);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Formats the raw seconds back into a pretty "00:00:00" string for the screen
    public String getFormattedTime() {
        // EXPLANATION: Modulo arithmetic. Total seconds divided by 3600 gives the hours.
        // The remainder (%) is used to find the leftover minutes, and the remainder of THAT gives the leftover seconds.
        int hours = totalTimeSeconds / 3600;
        int minutes = (totalTimeSeconds % 3600) / 60;
        int seconds = totalTimeSeconds % 60;

        // EXPLANATION: %02d is a formatting rule that guarantees the number will always take up at least 2 spaces, adding a leading zero if necessary (e.g., "5" becomes "05").
        if (hours > 0) {
            return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    // BAGRUT REQUIREMENT: This tells Java HOW to sort the users.
    // We sort by distance. If distance is equal, we sort by who was faster!
    @Override
    public int compareTo(LeaderboardUser other) {
        // EXPLANATION: Primary Sorting Condition (Distance).
        if (this.totalDistance != other.totalDistance) {
            // Sort distance descending (highest first)
            // EXPLANATION: By placing 'other' before 'this', you are telling Java to sort in DESCENDING order (highest distance goes to the top/index 0).
            return Float.compare(other.totalDistance, this.totalDistance);
        } else {
            // Secondary Sorting Condition (The Tie-Breaker).
            // If distances are tied, sort time ascending (lowest/fastest first)
            // EXPLANATION: By placing 'this' before 'other', you are telling Java to sort in ASCENDING order.
            // This means if two people walked exactly 10 KM, the person who did it in LESS time gets the higher rank!
            return Integer.compare(this.totalTimeSeconds, other.totalTimeSeconds);
        }
    }
}