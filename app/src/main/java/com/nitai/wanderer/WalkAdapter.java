package com.nitai.wanderer; // Make sure this matches your package name!

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class WalkAdapter extends RecyclerView.Adapter<WalkAdapter.WalkViewHolder> {

    // 1. The list of walks we want to show
    private ArrayList<Walk> walks;

    // 2. The bridge that tells JournalActivity when a walk is deleted
    private OnWalkDeleteListener deleteListener;

    // --- THE INTERFACE (The Bridge) ---
    public interface OnWalkDeleteListener {
        void onWalkDeleted();
    }

    // --- THE CONSTRUCTOR ---
    public WalkAdapter(ArrayList<Walk> walks, OnWalkDeleteListener deleteListener) {
        this.walks = walks;
        this.deleteListener = deleteListener;
    }

    // --- STEP 1: CREATE THE EMPTY CARD ---
    @NonNull
    @Override
    public WalkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // This line grabs our beautiful new XML design (item_walk.xml) and inflates it into a real object
        // NOTE: If your XML file is named something else (like walk_item.xml), change it here!
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_walk, parent, false);
        return new WalkViewHolder(view);
    }

    // --- STEP 2: FILL THE CARD WITH DATA ---
    @Override
    public void onBindViewHolder(@NonNull WalkViewHolder holder, int position) {
        // Find out exactly which walk we are looking at in the list
        Walk currentWalk = walks.get(position);

        // 1. Set the Title (The Date)
        // Note: If your Walk class uses public variables instead of getters,
        // change this to currentWalk.date, currentWalk.distance, etc.
        holder.tvWalkDate.setText(currentWalk.getDate());

        // 2. Format the Stats (Distance • Time)
        // We combine the distance and time into one clean string with a dot in the middle
        String combinedStats = currentWalk.getDistance() + "  •  " + currentWalk.getTime();
        holder.tvWalkStats.setText(combinedStats);

        // 3. The Delete Button Logic
        holder.btnDeleteWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove the walk from our list
                walks.remove(holder.getAdapterPosition());

                // Tell the RecyclerView to play the shrinking animation to remove the card
                notifyItemRemoved(holder.getAdapterPosition());
                notifyItemRangeChanged(holder.getAdapterPosition(), walks.size());

                // Send a message across the bridge to JournalActivity so it can save the updated list to the phone!
                if (deleteListener != null) {
                    deleteListener.onWalkDeleted();
                }
            }
        });
    }

    // --- STEP 3: HOW MANY CARDS? ---
    @Override
    public int getItemCount() {
        return walks.size();
    }

    // --- THE VIEWHOLDER (Connects Java to the XML IDs) ---
    public static class WalkViewHolder extends RecyclerView.ViewHolder {

        TextView tvWalkDate, tvWalkStats;
        ImageButton btnDeleteWalk;

        public WalkViewHolder(@NonNull View itemView) {
            super(itemView);

            // These IDs must match exactly what we put in item_walk.xml!
            tvWalkDate = itemView.findViewById(R.id.tvWalkDate);
            tvWalkStats = itemView.findViewById(R.id.tvWalkStats);
            btnDeleteWalk = itemView.findViewById(R.id.btnDeleteWalk);
        }
    }
}