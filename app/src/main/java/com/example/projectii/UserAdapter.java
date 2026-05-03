package com.example.projectii;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

// Adapter class for displaying users in a RecyclerView (user list in main activity)
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.viewholder> {

    MainActivity mainActivity;           // Reference to MainActivity for context and navigation
    ArrayList<Users> usersArrayList;     // List of user objects to display

    // Constructor initializes adapter with activity context and user list
    public UserAdapter(MainActivity mainActivity, ArrayList<Users> usersArrayList) {
        this.mainActivity = mainActivity;
        this.usersArrayList = usersArrayList;
    }

    // Called when RecyclerView needs a new ViewHolder
    @NonNull
    @Override
    public UserAdapter.viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the user_item layout for each row
        View view = LayoutInflater.from(mainActivity).inflate(R.layout.user_item, parent, false);
        return new viewholder(view);
    }

    // Binds data to the ViewHolder at the specified position
    @Override
    public void onBindViewHolder(@NonNull UserAdapter.viewholder holder, int position) {

        // Get the user object for this position
        Users users = usersArrayList.get(position);
        holder.username.setText(users.getUsername());  // Set username

        // ===== Show Online / Offline status text automatically =====
        // If user is online show "Online" text and green dot
        // If user is offline show their custom status text or "Offline" if status is empty
        if (users.isOnline()) {
            holder.userstatus.setText("Online");          // Display "Online" text
            holder.onlineDot.setVisibility(View.VISIBLE); // Show green dot indicator
        } else {
            holder.userstatus.setText("Offline");         // Display "Offline" text
            holder.onlineDot.setVisibility(View.GONE);    // Hide green dot
        }

        // ===== Load profile image from Base64 =====
        // Check if profile picture exists or is valid
        if (users.getProfilepic() == null
                || users.getProfilepic().isEmpty()
                || users.getProfilepic().equals("default")) {
            // Use default profile image if no custom image exists
            holder.userimg.setImageResource(R.drawable.profile);
        } else {
            try {
                // Decode Base64 string to byte array

                // ===== X = ∑ Bi × 2^18-6i happens here =====
                // Decoding reconstructs X from Base64 characters
                // then splits back into original bytes
                byte[] decodedBytes = Base64.decode(users.getProfilepic(), Base64.DEFAULT);
                // Convert byte array to Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                // Set the bitmap to the CircleImageView
                holder.userimg.setImageBitmap(bitmap);
            } catch (Exception e) {
                // Fallback to default image if decoding fails
                holder.userimg.setImageResource(R.drawable.profile);
            }
        }

        // ===== Unread message badge =====
        String currentUid = FirebaseAuth.getInstance().getUid();  // Get current user's ID
        String senderRoom = currentUid + users.getUserId();       // Create unique chat room ID

        // Listen for unread count in this specific chat room
        FirebaseDatabase.getInstance().getReference()
                .child("chats")           // Navigate to "chats" node in Firebase
                .child(senderRoom)        // Access specific chat room
                .child("messages")        // Access messages sub-node
                .addValueEventListener(new ValueEventListener() {  // Listen for real-time updates
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = 0;

                        // Count messages from other user that are not yet read
                        for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                            String senderId = msgSnapshot.child("senderid").getValue(String.class);
                            Boolean isRead = msgSnapshot.child("isRead").getValue(Boolean.class);

                            // Message is from other user and not yet marked as read
                            if (senderId != null
                                    && !senderId.equals(currentUid)      // Message not from current user
                                    && (isRead == null || !isRead)) {    // Message is unread
                                unreadCount++;
                            }
                        }

                        // ===== FIX: Do not show badge when chat window is open =====
                        if (unreadCount > 0 && !chatwindo.isChatOpen) {  // Don't show if chat is active
                            // Show badge with the unread count
                            holder.unreadBadge.setVisibility(View.VISIBLE);
                            holder.unreadBadge.setText(String.valueOf(unreadCount));

                            // Make username bold to indicate unread messages
                            holder.username.setTypeface(null, Typeface.BOLD);
                        } else {
                            // Hide badge when no unread messages OR chat window is open
                            holder.unreadBadge.setVisibility(View.GONE);

                            // Reset username to normal font weight
                            holder.username.setTypeface(null, Typeface.NORMAL);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle errors silently (can add logging here if needed)
                    }
                });

        // ===== When clicked on user redirect to chat window =====
        // and mark all messages as read
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Mark all messages in this chat as read before opening
                FirebaseDatabase.getInstance().getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .addListenerForSingleValueEvent(new ValueEventListener() {  // Single read, not real-time
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                                    String senderId = msgSnapshot.child("senderid").getValue(String.class);
                                    // Only mark messages from the other user as read (not current user's own messages)
                                    if (senderId != null && !senderId.equals(currentUid)) {
                                        msgSnapshot.getRef().child("isRead").setValue(true);  // Update to read
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle errors silently
                            }
                        });

                // Open chat window activity
                Intent intent = new Intent(mainActivity, chatwindo.class);
                intent.putExtra("nameeee", users.getUsername());      // Pass username
                intent.putExtra("reciverImg", users.getProfilepic()); // Pass profile picture
                intent.putExtra("uid", users.getUserId());            // Pass user ID
                mainActivity.startActivity(intent);                   // Start chat activity
            }
        });
    }

    // Returns the total number of items in the data set
    @Override
    public int getItemCount() {
        return usersArrayList.size();
    }

    // ViewHolder class holds references to views for efficient recycling
    public class viewholder extends RecyclerView.ViewHolder {
        CircleImageView userimg;      // Circular profile image view
        TextView username;            // Username text view
        TextView userstatus;          // Status text view (Online/Offline)
        TextView unreadBadge;         // Red circle badge showing unread message count
        View onlineDot;               // Green dot shown when user is online

        // Constructor initializes all view references
        public viewholder(@NonNull View itemView) {
            super(itemView);
            userimg = itemView.findViewById(R.id.userimg);
            username = itemView.findViewById(R.id.username);
            userstatus = itemView.findViewById(R.id.userstatus);
            unreadBadge = itemView.findViewById(R.id.unreadBadge);
            onlineDot = itemView.findViewById(R.id.onlineDot);
        }
    }
}