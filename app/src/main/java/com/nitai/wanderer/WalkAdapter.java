package com.nitai.wanderer;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class WalkAdapter extends RecyclerView.Adapter<WalkAdapter.WalkViewHolder> {

    private ArrayList<Walk> walks;
    // A bridge used to talk to the JournalActivity when an item is deleted
    private OnWalkDeleteListener deleteListener;

    public interface OnWalkDeleteListener {
        void onWalkDeleted();
    }

    public WalkAdapter(ArrayList<Walk> walks, OnWalkDeleteListener deleteListener) {
        this.walks = walks;
        this.deleteListener = deleteListener;
    }

    // Connects our custom XML design (item_walk.xml) to the adapter
    @NonNull
    @Override
    public WalkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_walk, parent, false);
        return new WalkViewHolder(view);
    }

    // This runs for every single walk in the list to fill it with data
    @Override
    public void onBindViewHolder(@NonNull WalkViewHolder holder, int position) {
        // Grab the correct walk from the list based on the position
        Walk currentWalk = walks.get(position);

        // Put the data onto the screen
        holder.tvWalkDate.setText(currentWalk.getDate());
        String combinedStats = currentWalk.getDistance() + "     " + currentWalk.getTime();
        holder.tvWalkStats.setText(combinedStats);

        // --- CLICK CARD TO OPEN MAP ---
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to open the SummaryActivity
                Intent intent = new Intent(v.getContext(), SummaryActivity.class);
                // Send a message: "I am an old walk, and my position in the list is X"
                intent.putExtra("OLD_WALK_INDEX", holder.getAdapterPosition());
                // Start the activity
                v.getContext().startActivity(intent);
            }
        });

        // --- DELETE BUTTON LOGIC ---
        holder.btnDeleteWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove from the list in memory
                walks.remove(holder.getAdapterPosition());
                // Play the nice shrinking animation
                notifyItemRemoved(holder.getAdapterPosition());
                notifyItemRangeChanged(holder.getAdapterPosition(), walks.size());

                // Tell the JournalActivity to save the new list to the phone
                if (deleteListener != null) {
                    deleteListener.onWalkDeleted();
                }
            }
        });
    }

    // Tells Android how many items to draw
    @Override
    public int getItemCount() {
        return walks.size();
    }

    // Connects the Java variables to the IDs in item_walk.xml
    public static class WalkViewHolder extends RecyclerView.ViewHolder {
        TextView tvWalkDate, tvWalkStats;
        ImageButton btnDeleteWalk;

        public WalkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWalkDate = itemView.findViewById(R.id.tvWalkDate);
            tvWalkStats = itemView.findViewById(R.id.tvWalkStats);
            btnDeleteWalk = itemView.findViewById(R.id.btnDeleteWalk);
        }
    }
}