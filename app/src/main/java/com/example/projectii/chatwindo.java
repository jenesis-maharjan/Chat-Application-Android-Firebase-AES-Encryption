package com.example.projectii;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

// Chat window activity - shows messages between two users
// ===== AES-256-CBC encryption is applied to ALL messages =====
// Text messages, photo messages, and edited messages are all encrypted
// before being saved to Firebase and decrypted when loaded back.
// The key is derived from the shared chat room ID using SHA-256 (see AESHelper).
public class chatwindo extends AppCompatActivity {

    // ===== Variables =====
    String receiverimg, receiverUid, receiverName, SenderUID;
    CircleImageView profile;   // receiver profile image view
    TextView receiverNName;    // receiver name text view
    CardView sendbtn;          // send button
    CardView chatCameraBtn;    // camera button inside chat
    EditText textmsg;          // message input field
    FirebaseAuth firebaseAuth; // Firebase authentication
    FirebaseDatabase database; // Firebase Realtime Database

    // Static variables so the adapter can access sender/receiver images
    public static String senderImg;
    public static String receiverIImg;

    // ===== Track if chat window is open =====
    // Used by UserAdapter to hide badge when user is in chat
    public static boolean isChatOpen = false;

    // Unique chat room IDs for sender and receiver
    String senderRoom, receiverRoom;

    // RecyclerView for displaying messages
    RecyclerView mmessangesAdapter;
    ArrayList<msgModelclass> messagessArrayList; // list of all messages
    messagesAdapter messagesAdapter;

    // Moved to class level so we can access it anywhere in the activity
    LinearLayoutManager linearLayoutManager;

    // Reference to current user's online status in Firebase
    DatabaseReference onlineStatusRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatwindo);

        // Adjust layout when keyboard opens so messages stay visible
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Hide the default action bar
        getSupportActionBar().hide();

        // ===== Initialize Firebase =====
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // ===== Set user online when chat window opens =====
        onlineStatusRef = database.getReference()
                .child("user")
                .child(firebaseAuth.getUid())
                .child("online");
        onlineStatusRef.setValue(true); // user is now active in chat

        // ===== onDisconnect — Firebase automatically sets user offline =====
        // when the app is killed, crashes, or loses network connection
        onlineStatusRef.onDisconnect().setValue(false);

        // ===== Get data sent from previous activity =====
        receiverName = getIntent().getStringExtra("nameeee");
        receiverimg = getIntent().getStringExtra("reciverImg");
        receiverUid = getIntent().getStringExtra("uid");

        // ===== Initialize UI components =====
        sendbtn = findViewById(R.id.sendbtnn);
        chatCameraBtn = findViewById(R.id.chatCameraBtn);
        textmsg = findViewById(R.id.textmsg);
        profile = findViewById(R.id.profileimgg);
        receiverNName = findViewById(R.id.recivername);
        mmessangesAdapter = findViewById(R.id.msgadpter);

        // ===== Setup RecyclerView =====
        messagessArrayList = new ArrayList<>();

        // setStackFromEnd(true) makes the list layout start from the bottom
        // so new messages appear at the bottom like WhatsApp/Messenger
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        mmessangesAdapter.setLayoutManager(linearLayoutManager);

        // ===== Store the current logged-in user's UID =====
        SenderUID = firebaseAuth.getUid();

        // Combine sender and receiver UIDs to create unique chat rooms
        senderRoom = SenderUID + receiverUid;   // e.g., "user1user2"
        receiverRoom = receiverUid + SenderUID;  // e.g., "user2user1"

        // ===== Attach adapter and pass chat rooms =====
        messagesAdapter = new messagesAdapter(chatwindo.this, messagessArrayList, senderRoom, receiverRoom);
        mmessangesAdapter.setAdapter(messagesAdapter);

        // ===== Display Receiver's Name =====
        if (receiverName != null && !receiverName.isEmpty()) {
            receiverNName.setText(receiverName);
        } else {
            receiverNName.setText("Unknown User");
        }

        // ===== Load Receiver's Profile Picture from Base64 =====
        if (receiverimg != null && !receiverimg.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(receiverimg, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                profile.setImageBitmap(bitmap);
            } catch (Exception e) {
                profile.setImageResource(R.drawable.profile);
            }
        } else {
            profile.setImageResource(R.drawable.profile);
        }

        // ===== Get sender's profile image from Firebase =====
        DatabaseReference reference = database.getReference()
                .child("user")
                .child(firebaseAuth.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderImg = snapshot.child("profilepic").exists()
                        ? snapshot.child("profilepic").getValue(String.class)
                        : "";

                receiverIImg = receiverimg;

                if (messagesAdapter != null) {
                    messagesAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // ===== Listen for messages in real time and DECRYPT each one =====
        // Every message stored in Firebase is AES-256-CBC encrypted.
        // We decrypt the message field here before passing it to the adapter.
        DatabaseReference chatreference = database.getReference()
                .child("chats")
                .child(senderRoom)
                .child("messages");

        chatreference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                messagessArrayList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    msgModelclass messages = dataSnapshot.getValue(msgModelclass.class);
                    messages.setMessageId(dataSnapshot.getKey());

                    // ===== DECRYPT the message before adding to list =====
                    // Photo messages store Base64 image data — we decrypt the
                    // Base64 string itself (not the image bytes).
                    // Text messages store encrypted text — we decrypt to plain text.
                    if (messages.getMessage() != null
                            && !messages.getMessage().isEmpty()
                            && messages.getMessage().contains(":")) {
                        // Only try to decrypt if it looks like our "IV:CipherText" format
                        try {
                            String decrypted = AESHelper.decrypt(
                                    messages.getMessage(),
                                    senderRoom,
                                    receiverRoom
                            );
                            messages.setMessage(decrypted);
                        } catch (Exception e) {
                            // If decryption fails (e.g., legacy unencrypted message),
                            // leave the message as-is so old chats still show
                        }
                    }

                    messagessArrayList.add(messages);

                    // ===== Mark messages as read ONLY when chat window is open =====
                    String senderId = messages.getSenderid();
                    Boolean isRead = dataSnapshot.child("isRead").getValue(Boolean.class);

                    if (isChatOpen) {
                        if (senderId != null
                                && !senderId.equals(SenderUID)
                                && (isRead == null || !isRead)) {

                            dataSnapshot.getRef().child("isRead").setValue(true);

                            database.getReference()
                                    .child("chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .child(messages.getMessageId())
                                    .child("isRead")
                                    .setValue(true);
                        }
                    }
                }

                messagesAdapter.notifyDataSetChanged();

                if (messagessArrayList.size() > 0) {
                    mmessangesAdapter.scrollToPosition(messagessArrayList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // ===== Send Button Click Listener =====
        sendbtn.setOnClickListener(v -> {

            String message = textmsg.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(chatwindo.this, "Enter the Message First", Toast.LENGTH_SHORT).show();
                return;
            }

            textmsg.setText("");

            // ===== ENCRYPT the message before saving to Firebase =====
            // C = E_K(P) using AES-256-CBC with a random IV per message.
            // Key K is derived from the shared chat room ID via SHA-256.
            String encryptedMessage;
            try {
                encryptedMessage = AESHelper.encrypt(message, senderRoom, receiverRoom);
            } catch (Exception e) {
                Toast.makeText(chatwindo.this,
                        "Encryption failed. Message not sent.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create message object with the ENCRYPTED text
            msgModelclass msg = new msgModelclass(encryptedMessage, SenderUID, new Date().getTime());

            // ===== Generate same messageId for both rooms =====
            String messageId = database.getReference().push().getKey();
            msg.setMessageId(messageId);

            // Save encrypted message to senderRoom
            database.getReference().child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(messageId)
                    .setValue(msg);

            // Save encrypted message to receiverRoom
            database.getReference().child("chats")
                    .child(receiverRoom)
                    .child("messages")
                    .child(messageId)
                    .setValue(msg);
        });

        // ===== Camera button inside chat =====
        chatCameraBtn.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, 20);
        });

        // ===== Check if a photo was passed from UserPickerActivity =====
        String photoToSend = getIntent().getStringExtra("photoToSend");
        if (photoToSend != null && !photoToSend.isEmpty()) {
            mmessangesAdapter.postDelayed(() ->
                    showViewOnceDialog(photoToSend), 500);
        }

        // ===== Scroll to last message whenever keyboard opens =====
        mmessangesAdapter.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mmessangesAdapter.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (messagessArrayList.size() > 0) {
                                mmessangesAdapter.scrollToPosition(messagessArrayList.size() - 1);
                            }
                        }
                    }, 100);
                }
            }
        });
    }

    // ===== Set isChatOpen true when user is on this screen =====
    @Override
    protected void onResume() {
        super.onResume();
        isChatOpen = true;

        if (onlineStatusRef != null && firebaseAuth.getCurrentUser() != null) {
            onlineStatusRef.setValue(true);
            onlineStatusRef.onDisconnect().setValue(false);
        }
    }

    // ===== Set isChatOpen false when user leaves this screen =====
    @Override
    protected void onPause() {
        super.onPause();
        isChatOpen = false;
    }

    // ===== Handle camera result =====
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 20 && resultCode == RESULT_OK && data != null) {

            Bitmap photo = (Bitmap) data.getExtras().get("data");

            // Convert Bitmap → Base64 string
            // X = B1×2^16 + B2×2^8 + B3 — bytes merged into stream
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);

            // Bi = floor(X / 2^18-6i) mod 64 — 24-bit blocks split into 4×6-bit chars
            String base64Photo = Base64.encodeToString(
                    byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

            showViewOnceDialog(base64Photo);
        }
    }

    // ===== Dialog — ask user to send as View Once or Normal =====
    private void showViewOnceDialog(String base64Photo) {
        new AlertDialog.Builder(this)
                .setTitle("Send Photo")
                .setMessage("How do you want to send this photo?")
                .setPositiveButton("View Once", (dialog, which) ->
                        sendPhotoMessage(base64Photo, true))
                .setNegativeButton("Normal", (dialog, which) ->
                        sendPhotoMessage(base64Photo, false))
                .setCancelable(false)
                .show();
    }

    // ===== Send a photo message — Base64 image data is AES encrypted =====
    // Even photo data (Base64 string) is encrypted before storing in Firebase.
    // The receiver decrypts the Base64 string, then decodes it back to a Bitmap.
    private void sendPhotoMessage(String base64Photo, boolean isViewOnce) {

        // ===== ENCRYPT the Base64 photo string before saving =====
        String encryptedPhoto;
        try {
            encryptedPhoto = AESHelper.encrypt(base64Photo, senderRoom, receiverRoom);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Photo encryption failed. Not sent.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create photo message object with the ENCRYPTED Base64 string
        msgModelclass photoMessage = new msgModelclass(
                encryptedPhoto,  // AES-encrypted Base64 image stored as message string
                SenderUID,
                new Date().getTime(),
                true,            // isPhoto = true
                isViewOnce       // isViewOnce
        );

        String messageId = database.getReference().push().getKey();
        photoMessage.setMessageId(messageId);

        // Save to senderRoom
        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .child(messageId)
                .setValue(photoMessage);

        // Save to receiverRoom
        database.getReference().child("chats")
                .child(receiverRoom)
                .child("messages")
                .child(messageId)
                .setValue(photoMessage);
    }

    // ===== Helper: Edit an existing message (call from adapter long-press) =====
    // Encrypts the new text and overwrites the message field in both rooms.
    public void editMessage(String messageId, String newText) {

        String encryptedNew;
        try {
            encryptedNew = AESHelper.encrypt(newText, senderRoom, receiverRoom);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Encryption failed. Edit not saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update senderRoom
        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .child(messageId)
                .child("message")
                .setValue(encryptedNew);

        // Update receiverRoom so both sides show the edited text
        database.getReference().child("chats")
                .child(receiverRoom)
                .child("messages")
                .child(messageId)
                .child("message")
                .setValue(encryptedNew);
    }

    // ===== Helper: Delete a message from both rooms (call from adapter long-press) =====
    public void deleteMessage(String messageId) {

        // Remove from senderRoom
        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .child(messageId)
                .removeValue();

        // Remove from receiverRoom
        database.getReference().child("chats")
                .child(receiverRoom)
                .child("messages")
                .child(messageId)
                .removeValue();
    }
}