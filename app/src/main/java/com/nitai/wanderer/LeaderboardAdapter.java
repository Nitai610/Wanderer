package com.nitai.wanderer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

// EXPLANATION: The Adapter is the engine of the RecyclerView. Its job is to take raw data (your ArrayList)
// and translate it into visual rows on the screen, recycling the views as the user scrolls to save memory.
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    // EXPLANATION: This is the data source. It holds the pre-sorted list of users that was calculated in LeaderboardActivity.
    private ArrayList<LeaderboardUser> userList;

    // EXPLANATION: The Constructor. When the LeaderboardActivity creates this Adapter, it passes the data list into this variable so the Adapter knows what to display.
    public LeaderboardAdapter(ArrayList<LeaderboardUser> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    // EXPLANATION: onCreateViewHolder is responsible for creating a brand new, empty visual "card" or "row".
    // It only runs a few times (just enough to fill the screen), after which the RecyclerView starts recycling these cards.
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // EXPLANATION: LayoutInflater reads your XML design file (item_leaderboard) and "inflates" it into a real, physical Java View object.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    // EXPLANATION: onBindViewHolder is where the data meets the UI. It takes an existing card (holder) and fills it with the data for a specific row (position).
    // This method runs constantly as the user scrolls up and down.
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        // EXPLANATION: Grabs the exact user object for the row we are currently drawing. (e.g., if position is 0, we grab the 1st place user).
        LeaderboardUser user = userList.get(position);

        // EXPLANATION: 'position' starts at 0, but ranks start at 1. So we add 1 to the position to display "#1", "#2", etc.
        holder.tvRank.setText("#" + (position + 1));
        holder.tvLeaderboardUsername.setText(user.username);
        // EXPLANATION: Formats the floating-point distance to strictly show two decimal places (e.g., "15.50 KM").
        holder.tvLeaderboardDistance.setText(String.format(java.util.Locale.US, "%.2f KM", user.totalDistance));
        holder.tvLeaderboardTime.setText(user.getFormattedTime());

        // Visual Polish: Color the Top 3 players!
        // EXPLANATION: This is excellent UI design. By checking the row's 'position', we can dynamically change the text color.
        // Color.parseColor() allows you to use standard HEX color codes right inside your Java code.
        if (position == 0) {
            holder.tvRank.setTextColor(Color.parseColor("#FFD700")); // Gold
        } else if (position == 1) {
            holder.tvRank.setTextColor(Color.parseColor("#C0C0C0")); // Silver
        } else if (position == 2) {
            holder.tvRank.setTextColor(Color.parseColor("#CD7F32")); // Bronze
        } else {
            holder.tvRank.setTextColor(Color.parseColor("#9E9E9E")); // Gray for everyone else
        }
    }

    @Override
    // EXPLANATION: This tells the RecyclerView exactly how many rows it needs to draw. Without this, the list would be blank.
    public int getItemCount() {
        return userList.size();
    }

    // EXPLANATION: The ViewHolder acts as a memory cache.
    // Finding UI elements by their ID (findViewById) is a slow process for the phone.
    // The ViewHolder finds them exactly ONCE when the row is created and saves them in memory, making scrolling incredibly smooth.
    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvLeaderboardUsername, tvLeaderboardDistance, tvLeaderboardTime;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            // EXPLANATION: Linking the Java variables to the specific IDs inside 'item_leaderboard.xml'.
            tvRank = itemView.findViewById(R.id.tvRank);
            tvLeaderboardUsername = itemView.findViewById(R.id.tvLeaderboardUsername);
            tvLeaderboardDistance = itemView.findViewById(R.id.tvLeaderboardDistance);
            tvLeaderboardTime = itemView.findViewById(R.id.tvLeaderboardTime);
        }
    }
}