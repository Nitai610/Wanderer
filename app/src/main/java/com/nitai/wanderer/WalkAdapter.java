package com.nitai.wanderer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

// NOTE: We must import Firebase tools so the Adapter can talk to the cloud database
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

// EXPLANATION: The 'Adapter' is the bridge between your raw data (the ArrayList) and the visual screen (the RecyclerView).
// It takes your data, builds visual "cards" for each item, and recycles them as the user scrolls to save phone memory.
public class WalkAdapter extends RecyclerView.Adapter<WalkAdapter.WalkViewHolder> {

    // EXPLANATION: This is the raw data source. A dynamic list that holds all the 'Walk' objects downloaded from Firebase.
    private ArrayList<Walk> walks;

    // NOTE: This listener is like a walkie-talkie back to JournalActivity.
    // It tells JournalActivity "Hey, I deleted something, check if the list is empty now!"
    private OnWalkDeleteListener deleteListener;

    // EXPLANATION: An 'interface' creates a custom rulebook or contract.
    // It allows this Adapter to send a signal back to the main screen without the Adapter needing to know how the main screen works.
    public interface OnWalkDeleteListener {
        void onWalkDeleted();
    }

    // EXPLANATION: The Constructor. When JournalActivity creates this Adapter, it must pass in the list of walks and the "walkie-talkie" listener.
    public WalkAdapter(ArrayList<Walk> walks, OnWalkDeleteListener deleteListener) {
        this.walks = walks;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    // EXPLANATION: onCreateViewHolder is called when the RecyclerView needs a brand new visual "card" (before any data is put into it).
    public WalkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // EXPLANATION: 'LayoutInflater' takes your XML design file (item_walk) and translates it into real Java View objects.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_walk, parent, false);
        return new WalkViewHolder(view); // Hands the empty visual card to the ViewHolder to be saved.
    }

    @Override
    // EXPLANATION: onBindViewHolder is where the data meets the visual design.
    // It takes an empty card (the 'holder') and fills it with data for a specific row ('position').
    public void onBindViewHolder(@NonNull WalkViewHolder holder, int position) {
        // EXPLANATION: Grabs the exact Walk object from the list based on which row we are currently drawing.
        Walk currentWalk = walks.get(position);

        // Put the text data onto the screen
        holder.tvWalkDate.setText(currentWalk.getDate().substring(0, 10));
        String combinedStats = currentWalk.getDistance() + "     " + currentWalk.getTime();
        holder.tvWalkStats.setText(combinedStats);

        // --- CLICK CARD TO OPEN MAP ---
        // EXPLANATION: 'holder.itemView' refers to the entire row/card itself. If they tap anywhere on the card, this triggers.
        holder.itemView.setOnClickListener(v -> {
            // EXPLANATION: We create an Intent to change screens. Notice we use 'v.getContext()' because we aren't inside an Activity directly.
            Intent intent = new Intent(v.getContext(), SummaryActivity.class);

            // EXPLANATION: .putExtra() is like stuffing a note inside the Intent envelope.
            // We are passing the index number of the walk so the next screen knows which walk to load.
            intent.putExtra("OLD_WALK_INDEX", holder.getAdapterPosition());
            v.getContext().startActivity(intent);
        });

        // --- DELETE BUTTON LOGIC WITH DIALOG ---
        holder.btnDeleteWalk.setOnClickListener(v -> {
            // We need 'Context' to show popups on the screen
            // EXPLANATION: Context is required because popups need to know which screen they should draw themselves over.
            Context context = v.getContext();

            // BAGRUT REQUIREMENT: Using an AlertDialog to prevent accidental data loss.
            // EXPLANATION: AlertDialog is a built-in Android tool to create quick pop-up confirmation boxes.
            new AlertDialog.Builder(context)
                    .setTitle("Delete Walk")
                    .setMessage("Are you sure you want to permanently delete this walk from your journal?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Only if they click "Delete" do we run the cloud deletion process
                            // EXPLANATION: getAdapterPosition() dynamically checks exactly which row the user clicked.
                            deleteWalkPermanently(holder.getAdapterPosition(), context);
                        }
                    })
                    .setNegativeButton("Cancel", null) // Does nothing, just closes the box
                    .show(); // EXPLANATION: Don't forget .show()! Without it, the dialog is built but remains invisible.
        });
    }

    // --- HELPER METHOD: DELETE FROM CLOUD AND MEMORY ---
    private void deleteWalkPermanently(int position, Context context) {
        // Safety check to ensure the position is still valid
        // EXPLANATION: If the user double-clicks delete really fast, the position might become invalid. This prevents a crash.
        if (position == RecyclerView.NO_POSITION) return;

        Walk walkToDelete = walks.get(position);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // EXPLANATION: We dynamically grab the currently logged-in user's email directly from the Auth engine.
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // 1. FIND THE WALK IN THE CLOUD
        // We ask Firebase to find the exact walk that matches this date, time, and distance.
        // EXPLANATION: This is a Firestore Query. We navigate to the user's specific subcollection and use '.whereEqualTo' to filter the database.
        db.collection("users").document(userEmail).collection("walks")
                .whereEqualTo("date", walkToDelete.getDate())
                .whereEqualTo("time", walkToDelete.getTime())
                .whereEqualTo("distance", walkToDelete.getDistance())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // 2. DELETE IT FROM THE CLOUD SERVER
                    // EXPLANATION: Even though we expect only 1 result, Firestore queries always return a list (snapshot). We loop through it and delete.
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }

                    // 3. REMOVE IT FROM THE PHONE'S MEMORY (RAM)
                    // We must update the local ArrayList so the item visually disappears.
                    // EXPLANATION: If you don't remove it from the local list, the app will crash when you tell the Adapter to update, because the visual list and data list sizes won't match.
                    walks.remove(position);

                    // Play the shrinking animation and update the list positions
                    // EXPLANATION: These two commands tell Android to play the built-in swipe/shrink animation and recalculate the row numbers for all the items below the deleted one.
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, walks.size());

                    // 4. TELL JOURNAL ACTIVITY TO REFRESH
                    // This triggers checkIfEmpty() in JournalActivity
                    // EXPLANATION: This uses the interface we defined at the top. It fires a signal back to the main Activity.
                    if (deleteListener != null) {
                        deleteListener.onWalkDeleted();
                    }

                    Toast.makeText(context, "Walk deleted from cloud", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    // EXPLANATION: The RecyclerView calls this constantly to know exactly how many rows it needs to draw.
    public int getItemCount() {
        return walks.size();
    }

    // EXPLANATION: The ViewHolder is like a small cache. Every time a row is created, `findViewById` searches the XML.
    // Doing that search thousands of times while scrolling causes lag. The ViewHolder finds the elements ONCE and saves them in memory for fast access.
    public static class WalkViewHolder extends RecyclerView.ViewHolder {
        TextView tvWalkDate, tvWalkStats;
        ImageButton btnDeleteWalk;

        public WalkViewHolder(@NonNull View itemView) {
            super(itemView);
            // EXPLANATION: Linking the Java variables to the XML design IDs, just like you do in an Activity's onCreate.
            tvWalkDate = itemView.findViewById(R.id.tvWalkDate);
            tvWalkStats = itemView.findViewById(R.id.tvWalkStats);
            btnDeleteWalk = itemView.findViewById(R.id.btnDeleteWalk);
        }
    }
}