package com.example.projectii;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class setting extends AppCompatActivity {

    // ===== UI Components =====
    CircleImageView setprofile; // profile image — user can click to change
    EditText setname;           // name input field
    EditText setstatus;         // status input field
    Button donebut;             // save button

    // ===== Firebase =====
    FirebaseAuth auth;
    DatabaseReference reference;

    // ===== User credentials already stored in Firebase =====
    // We need these to rebuild the Users object when saving
    String email, password;

    // ===== Image variables =====
    String currentBase64Image = null; // existing Base64 image from Firebase
    // null means no image saved yet
    Uri selectedImageUri = null;      // URI of newly selected image from gallery
    // null means user did not pick a new image

    // ===== Original name =====
    // Always pre-filled in the name field just like status
    // If user clears the field and saves — restore original name silently
    String originalName = "";

    // ===== Flag to prevent fields being overwritten after user edits =====
    // Without this addValueEventListener fires again after save and
    // overwrites whatever the user typed back to the original values
    boolean dataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Hide default action bar for clean UI
        getSupportActionBar().hide();

        // ===== Initialize Firebase Auth =====
        auth = FirebaseAuth.getInstance();

        // Reference to current logged in user's node in Firebase
        reference = FirebaseDatabase.getInstance()
                .getReference()
                .child("user")
                .child(auth.getUid());

        // ===== Bind XML views =====
        setprofile = findViewById(R.id.settingprofile);
        setname = findViewById(R.id.settingname);
        setstatus = findViewById(R.id.settingstatus);
        donebut = findViewById(R.id.donebutt);

        // ===== Fetch existing user data from Firebase =====
        // Using addValueEventListener instead of addListenerForSingleValueEvent
        // because addListenerForSingleValueEvent sometimes fires before data is ready
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // ===== Only load data once =====
                // Without this flag every time user saves Firebase fires this
                // listener again and overwrites the fields back to original values
                if (dataLoaded) return;
                dataLoaded = true;

                // ===== Convert entire snapshot to Users object =====
                // More reliable than reading each child separately
                Users currentUser = snapshot.getValue(Users.class);

                if (currentUser == null) {
                    Toast.makeText(setting.this, "Could not load profile", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // ===== Store credentials for rebuilding user on save =====
                email = currentUser.getMail();
                password = currentUser.getPassword();

                // ===== Store original name =====
                // Pre-fill name field just like status field
                originalName = currentUser.getUsername() != null ? currentUser.getUsername() : "";

                // ===== Pre-fill name and status fields =====
                // User sees their current name and status already filled in
                setname.setText(originalName);
                setstatus.setText(currentUser.getStatus() != null ? currentUser.getStatus() : "");

                // ===== Load existing profile image from Base64 =====
                String profilepic = currentUser.getProfilepic();
                if (profilepic != null && !profilepic.isEmpty() && !profilepic.equals("default")) {
                    currentBase64Image = profilepic; // store current image string
                    try {
                        // Decode Base64 string back to Bitmap
                        byte[] decodedBytes = Base64.decode(profilepic, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        setprofile.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        // If decoding fails show default
                        setprofile.setImageResource(R.drawable.profile);
                    }
                } else {
                    // No image saved — show default profile drawable
                    setprofile.setImageResource(R.drawable.profile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Called if Firebase read fails
                Toast.makeText(setting.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });

        // ===== Profile image click — open gallery to pick new image =====
        setprofile.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), 10);
        });

        // ===== Save button clicked =====
        donebut.setOnClickListener(v -> {

            // Get current values from input fields
            String name = setname.getText().toString().trim();
            String status = setstatus.getText().toString().trim();

            // ===== Name handling =====
            // If user cleared the name field restore original name silently
            if (name.isEmpty()) {
                name = originalName;
                setname.setText(originalName);
            }

            // ===== Image handling =====
            // If user picked a new image convert it to Base64
            // If not keep the existing Base64 image
            String finalBase64Image = currentBase64Image != null ? currentBase64Image : "";
            if (selectedImageUri != null) {
                finalBase64Image = convertImageToBase64(selectedImageUri);
            }

            // ===== Store name and image in final variables for use inside listener =====
            final String finalName = name;
            final String finalImage = finalBase64Image;

            // ===== Preserve online status when saving =====
            // We read current online value first so it is not overwritten to false
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    // Get current online status from Firebase
                    Boolean currentOnline = snapshot.child("online").getValue(Boolean.class);
                    boolean isOnline = currentOnline != null && currentOnline;

                    // ===== Create updated user object =====
                    Users user = new Users(
                            auth.getUid(),
                            finalName,   // updated or original name
                            email,       // unchanged email
                            password,    // unchanged password
                            finalImage,  // new or existing Base64 profile image
                            status       // updated status
                    );

                    // ===== Preserve online status so it is not reset to false =====
                    user.setOnline(isOnline);

                    // ===== Save updated data back to Firebase =====
                    reference.setValue(user).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(setting.this, "Profile Updated", Toast.LENGTH_SHORT).show();

                            // Go back to main screen after saving
                            startActivity(new Intent(setting.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(setting.this, "Update Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(setting.this, "Update Failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ===== Handle image selection result from gallery =====
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (data != null) {
                // Store selected image URI
                selectedImageUri = data.getData();

                // Show selected image immediately in the circle view
                setprofile.setImageURI(selectedImageUri);
            }
        }
    }

    // ===== Convert selected image URI to Base64 string =====
    // Compresses and resizes image to keep Firebase database size small
    private String convertImageToBase64(Uri imageUri) {
        try {
            // Open the image as an input stream
            InputStream inputStream = getContentResolver().openInputStream(imageUri);

            // Decode stream into Bitmap
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize to max 200x200 pixels to keep database size small
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true);

            // Compress to JPEG at 50% quality to reduce size further
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);

            // Convert compressed bytes to Base64 string
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (Exception e) {
            // If something goes wrong keep the existing image unchanged
            Toast.makeText(this, "Could not process image", Toast.LENGTH_SHORT).show();
            return currentBase64Image != null ? currentBase64Image : "";
        }
    }
}