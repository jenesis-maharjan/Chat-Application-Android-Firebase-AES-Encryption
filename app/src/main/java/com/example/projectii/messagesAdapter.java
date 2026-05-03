package com.example.projectii;

import static com.example.projectii.chatwindo.receiverIImg;
import static com.example.projectii.chatwindo.senderImg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

// Adapter class to show chat messages in RecyclerView
// Handles both sender and receiver message layouts
// Supports text messages, normal photos, and view once photos
public class messagesAdapter extends RecyclerView.Adapter {

    Context context;
    ArrayList<msgModelclass> messagesAdapterArrayList; // list of all chat messages

    int ITEM_SEND = 1;   // constant for sender layout (right side)
    int ITEM_RECIVE = 2; // constant for receiver layout (left side)

    // Chat room IDs passed from chatwindo activity
    String senderRoom, receiverRoom;

    // ===== Constructor =====
    public messagesAdapter(Context context,
                           ArrayList<msgModelclass> messagesAdapterArrayList,
                           String senderRoom, String receiverRoom) {
        this.context = context;
        this.messagesAdapterArrayList = messagesAdapterArrayList;
        this.senderRoom = senderRoom;
        this.receiverRoom = receiverRoom;
    }

    // ===== Inflate correct layout based on message type =====
    // Sender gets right side layout, receiver gets left side layout
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        // Inflate sender layout for messages sent by current user
        if (viewType == ITEM_SEND) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.sender_layout, parent, false);
            return new senderViewHolder(view);
        }
        // Inflate receiver layout for messages received from other user
        else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.reciver_layout, parent, false);
            return new receiverViewHolder(view);
        }
    }

    // ===== Bind message data to the correct layout =====
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {

        // Get the message at this position
        msgModelclass message = messagesAdapterArrayList.get(position);

        // ===== SENDER MESSAGE LAYOUT =====
        if (holder instanceof senderViewHolder) {
            senderViewHolder viewHolder = (senderViewHolder) holder;

            // Load sender profile image from Base64 string stored in Firebase
            loadBase64Image(senderImg, viewHolder.cirleImageView);

            if (message.isPhoto()) {
                // ===== Photo message =====
                // Hide text bubble and show photo view
                viewHolder.msgtxt.setVisibility(View.GONE);
                viewHolder.photoMsg.setVisibility(View.VISIBLE);
                viewHolder.viewOnceLabel.setVisibility(View.GONE);

                if (message.isViewOnce()) {
                    // View once photo — sender only sees camera icon and label
                    // The actual photo is hidden from sender side like WhatsApp
                    viewHolder.photoMsg.setImageResource(R.drawable.photocamera);
                    viewHolder.viewOnceLabel.setVisibility(View.VISIBLE);
                    viewHolder.viewOnceLabel.setText("📷 View Once");

                } else {
                    // Normal photo — decode Base64 and show as thumbnail
                    try {
                        byte[] decodedBytes = Base64.decode(message.getMessage(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        viewHolder.photoMsg.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        // If decoding fails show camera icon as fallback
                        viewHolder.photoMsg.setImageResource(R.drawable.photocamera);
                    }

                    // Click thumbnail to open photo in full screen
                    viewHolder.photoMsg.setOnClickListener(v ->
                            showFullScreenPhoto(message.getMessage()));

                    // Long press on photo thumbnail — show delete dialog
                    viewHolder.photoMsg.setOnLongClickListener(v -> {
                        showDeleteOnlyDialog(message);
                        return true;
                    });
                }

                // Long press on entire item — show delete dialog
                // Photos can be deleted but not edited
                viewHolder.itemView.setOnLongClickListener(v -> {
                    showDeleteOnlyDialog(message);
                    return true;
                });

            } else {
                // ===== Text message =====
                // Hide bubble if message is empty or null
                // Prevents blank gray bubble appearing when message is empty
                if (message.getMessage() == null || message.getMessage().isEmpty()) {
                    viewHolder.msgtxt.setVisibility(View.GONE);
                } else {
                    viewHolder.msgtxt.setVisibility(View.VISIBLE);
                    viewHolder.msgtxt.setText(message.getMessage());
                }

                // Hide all photo related views for text messages
                viewHolder.photoMsg.setVisibility(View.GONE);
                viewHolder.viewOnceLabel.setVisibility(View.GONE);

                // Long press on text message — show edit or delete dialog
                viewHolder.itemView.setOnLongClickListener(v -> {
                    showEditDeleteDialog(message);
                    return true;
                });
            }
        }

        // ===== RECEIVER MESSAGE LAYOUT =====
        else {
            receiverViewHolder viewHolder = (receiverViewHolder) holder;

            // Load receiver profile image from Base64 string stored in Firebase
            loadBase64Image(receiverIImg, viewHolder.cirleImageView);

            if (message.isPhoto()) {
                // ===== Photo message =====
                // Hide text bubble for photo messages
                viewHolder.msgtxt.setVisibility(View.GONE);

                if (message.isViewOnce()) {

                    if (message.isViewed()) {
                        // Photo already viewed — hide photo and show opened label
                        // Photo cannot be viewed again after isViewed is true
                        viewHolder.photoMsg.setVisibility(View.GONE);
                        viewHolder.viewOnceLabel.setVisibility(View.GONE);
                        viewHolder.viewedLabel.setVisibility(View.VISIBLE);
                        viewHolder.viewedLabel.setText("📷 Opened");

                    } else {
                        // Photo not yet viewed — show camera icon and tap to view label
                        // Do NOT show actual photo before receiver views it
                        viewHolder.photoMsg.setVisibility(View.VISIBLE);
                        viewHolder.photoMsg.setImageResource(R.drawable.photocamera);
                        viewHolder.viewedLabel.setVisibility(View.GONE);
                        viewHolder.viewOnceLabel.setVisibility(View.VISIBLE);
                        viewHolder.viewOnceLabel.setText("📷 Tap to view");

                        // When receiver taps — open full screen then mark as viewed
                        viewHolder.photoMsg.setOnClickListener(v -> {

                            // Show full screen photo first
                            showFullScreenPhoto(message.getMessage());

                            // Mark as viewed in senderRoom so sender sees it too
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats").child(senderRoom)
                                    .child("messages").child(message.getMessageId())
                                    .child("viewed").setValue(true);

                            // Mark as viewed in receiverRoom
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats").child(receiverRoom)
                                    .child("messages").child(message.getMessageId())
                                    .child("viewed").setValue(true);
                        });

                        // Long press on view once photo — show delete dialog
                        viewHolder.photoMsg.setOnLongClickListener(v -> {
                            showDeleteOnlyDialog(message);
                            return true;
                        });
                    }

                } else {
                    // Normal photo — show thumbnail directly
                    viewHolder.photoMsg.setVisibility(View.VISIBLE);
                    viewHolder.viewOnceLabel.setVisibility(View.GONE);
                    viewHolder.viewedLabel.setVisibility(View.GONE);

                    // Decode Base64 and show photo thumbnail
                    try {
                        byte[] decodedBytes = Base64.decode(message.getMessage(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        viewHolder.photoMsg.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        // If decoding fails show camera icon as fallback
                        viewHolder.photoMsg.setImageResource(R.drawable.photocamera);
                    }

                    // Click thumbnail to open photo in full screen
                    viewHolder.photoMsg.setOnClickListener(v ->
                            showFullScreenPhoto(message.getMessage()));

                    // Long press on normal photo thumbnail — show delete dialog
                    viewHolder.photoMsg.setOnLongClickListener(v -> {
                        showDeleteOnlyDialog(message);
                        return true;
                    });
                }

                // Long press on entire item — show delete dialog
                // Photos can be deleted but not edited
                viewHolder.itemView.setOnLongClickListener(v -> {
                    showDeleteOnlyDialog(message);
                    return true;
                });

            } else {
                // ===== Text message =====
                // Hide bubble if message is empty or null
                // Prevents blank gray bubble appearing on receiver side
                // when message is temporarily empty during Firebase sync
                if (message.getMessage() == null || message.getMessage().isEmpty()) {
                    viewHolder.msgtxt.setVisibility(View.GONE);
                } else {
                    viewHolder.msgtxt.setVisibility(View.VISIBLE);
                    viewHolder.msgtxt.setText(message.getMessage());
                }

                // Hide all photo related views for text messages
                viewHolder.photoMsg.setVisibility(View.GONE);
                viewHolder.viewOnceLabel.setVisibility(View.GONE);
                viewHolder.viewedLabel.setVisibility(View.GONE);
            }
        }
    }

    // ===== Open photo in full screen dialog =====
    // Decodes Base64 image and shows it covering the entire screen
    // Tap anywhere on the photo to close
    private void showFullScreenPhoto(String base64Image) {
        try {
            // Decode Base64 string back to Bitmap
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            // Create full screen dialog covering entire screen
            Dialog dialog = new Dialog(context,
                    android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_fullscreen_photo);

            // Set decoded photo in the full screen image view
            ImageView fullPhoto = dialog.findViewById(R.id.fullScreenPhoto);
            fullPhoto.setImageBitmap(bitmap);

            // Tap anywhere on photo to close the dialog
            fullPhoto.setOnClickListener(v -> dialog.dismiss());

            dialog.show();

        } catch (Exception e) {
            // If decoding fails do nothing — dialog will not open
        }
    }

    // ===== Decode Base64 string and display as profile image =====
    // Used for both sender and receiver profile pictures in chat bubbles
    // Shows default profile drawable if Base64 string is empty or null
    private void loadBase64Image(String base64String, CircleImageView imageView) {
        if (base64String == null || base64String.isEmpty()) {
            // No image saved — show default profile drawable
            imageView.setImageResource(R.drawable.profile);
        } else {
            try {
                // Decode Base64 string back to byte array
                byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

                // Convert byte array to Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                // Set decoded Bitmap to the CircleImageView
                imageView.setImageBitmap(bitmap);

            } catch (Exception e) {
                // If decoding fails show default profile drawable
                imageView.setImageResource(R.drawable.profile);
            }
        }
    }

    // ===== Show Delete only dialog for photo messages =====
    // Photos cannot be edited — only deleted
    // Shows on long press of any photo message
    private void showDeleteOnlyDialog(msgModelclass message) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete photo from both chat rooms
                    deleteMessage(message);
                })
                .setNegativeButton("Cancel", null) // cancel does nothing
                .show();
    }

    // ===== Show Edit and Delete options on long press =====
    // Only shown for text messages
    private void showEditDeleteDialog(msgModelclass message) {
        String[] options = {"Edit Message", "Delete Message"};
        new AlertDialog.Builder(context)
                .setTitle("Select Option")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showEditDialog(message); // user chose edit
                    else deleteMessage(message);             // user chose delete
                })
                .show();
    }

    // ===== Edit message dialog =====
    // Opens dialog with EditText prefilled with current message
    // Updates message in both senderRoom and receiverRoom in Firebase
    private void showEditDialog(msgModelclass message) {

        // Create EditText and prefill with current message text
        EditText editText = new EditText(context);
        editText.setText(message.getMessage());

        new AlertDialog.Builder(context)
                .setTitle("Edit Message")
                .setView(editText)
                .setPositiveButton("OK", (dialog, which) -> {

                    String newMsg = editText.getText().toString().trim();
                    if (newMsg.isEmpty()) return; // do nothing if empty

                    FirebaseDatabase database = FirebaseDatabase.getInstance();

                    // Update message text in senderRoom
                    database.getReference()
                            .child("chats").child(senderRoom)
                            .child("messages").child(message.getMessageId())
                            .child("message").setValue(newMsg);

                    // Update message text in receiverRoom
                    // so receiver also sees the edited version
                    database.getReference()
                            .child("chats").child(receiverRoom)
                            .child("messages").child(message.getMessageId())
                            .child("message").setValue(newMsg);
                })
                .setNegativeButton("Cancel", null) // cancel does nothing
                .show();
    }

    // ===== Delete message from both chat rooms =====
    // Removes message permanently from senderRoom and receiverRoom in Firebase
    private void deleteMessage(msgModelclass message) {

        // Safety check — do nothing if messageId is null
        if (message.getMessageId() == null) return;

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Remove message from senderRoom
        database.getReference()
                .child("chats").child(senderRoom)
                .child("messages").child(message.getMessageId())
                .removeValue();

        // Remove message from receiverRoom
        database.getReference()
                .child("chats").child(receiverRoom)
                .child("messages").child(message.getMessageId())
                .removeValue();
    }

    // ===== Return total number of messages in the list =====
    @Override
    public int getItemCount() {
        return messagesAdapterArrayList.size();
    }

    // ===== Determine if message is sent or received =====
    // Compares message senderid with current logged in user UID
    // Returns ITEM_SEND for sender layout, ITEM_RECIVE for receiver layout
    @Override
    public int getItemViewType(int position) {
        if (FirebaseAuth.getInstance().getUid()
                .equals(messagesAdapterArrayList.get(position).getSenderid())) {
            return ITEM_SEND;   // current user sent this message
        } else {
            return ITEM_RECIVE; // other user sent this message
        }
    }

    // ===== SENDER VIEW HOLDER =====
    // Holds references to all views in sender_layout.xml
    class senderViewHolder extends RecyclerView.ViewHolder {
        CircleImageView cirleImageView; // sender profile image
        TextView msgtxt;                // sender text message bubble
        ImageView photoMsg;             // sender photo message thumbnail
        TextView viewOnceLabel;         // view once label shown on photo

        public senderViewHolder(@NonNull View itemView) {
            super(itemView);
            cirleImageView = itemView.findViewById(R.id.profilerggg);
            msgtxt = itemView.findViewById(R.id.msgsendertyp);
            photoMsg = itemView.findViewById(R.id.senderPhotoMsg);
            viewOnceLabel = itemView.findViewById(R.id.senderViewOnceLabel);
        }
    }

    // ===== RECEIVER VIEW HOLDER =====
    // Holds references to all views in reciver_layout.xml
    class receiverViewHolder extends RecyclerView.ViewHolder {
        CircleImageView cirleImageView; // receiver profile image
        TextView msgtxt;                // receiver text message bubble
        ImageView photoMsg;             // receiver photo message thumbnail
        TextView viewOnceLabel;         // tap to view label for view once photos
        TextView viewedLabel;           // opened label shown after view once viewed

        public receiverViewHolder(@NonNull View itemView) {
            super(itemView);
            cirleImageView = itemView.findViewById(R.id.pro);
            msgtxt = itemView.findViewById(R.id.recivertextset);
            photoMsg = itemView.findViewById(R.id.receiverPhotoMsg);
            viewOnceLabel = itemView.findViewById(R.id.receiverViewOnceLabel);
            viewedLabel = itemView.findViewById(R.id.receiverViewedLabel);
        }
    }
}