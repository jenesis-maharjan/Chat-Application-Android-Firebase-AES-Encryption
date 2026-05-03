package com.example.projectii;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class registration extends AppCompatActivity {

    TextView loginbut;
    EditText rg_username, rg_email, rg_password, rg_repassword;
    Button rg_signup;
    CircleImageView rg_profileImg;
    FirebaseAuth auth;
    Uri imageURI;    // local reference to image selected by user
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    FirebaseDatabase database;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If user already logged in, skip registration and go to MainActivity
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(registration.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        // Loading dialog shown during account creation
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Establishing your account");
        progressDialog.setCancelable(false);

        // Hide default action bar
        getSupportActionBar().hide();

        // Initialize Firebase database
        database = FirebaseDatabase.getInstance();

        // Link UI elements
        loginbut = findViewById(R.id.loginbut);
        rg_username = findViewById(R.id.rgusername);
        rg_email = findViewById(R.id.rgemail);
        rg_password = findViewById(R.id.rgpassword);
        rg_repassword = findViewById(R.id.rgrepassword);
        rg_profileImg = findViewById(R.id.profilerg0);
        rg_signup = findViewById(R.id.signupbutton);

        // When clicked on login in registration
        loginbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(registration.this, login.class);
                startActivity(intent);
                finish();
            }
        });

        // ===== Signup Button =====
        rg_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String namee = rg_username.getText().toString();
                String emaill = rg_email.getText().toString();
                String Password = rg_password.getText().toString();
                String cPassword = rg_repassword.getText().toString();
                String status = "";

                // Validation checks
                if (TextUtils.isEmpty(namee) || TextUtils.isEmpty(emaill) ||
                        TextUtils.isEmpty(Password) || TextUtils.isEmpty(cPassword)) {
                    Toast.makeText(registration.this, "Please Enter Valid Information", Toast.LENGTH_SHORT).show();
                } else if (!emaill.matches(emailPattern)) {
                    rg_email.setError("Type A Valid Email Here");
                } else if (Password.length() < 6) {
                    rg_password.setError("Password Must Be 6 Characters Or More");
                } else if (!Password.equals(cPassword)) {
                    rg_password.setError("The Password Doesn't Match");
                } else {

                    // Show loading dialog
                    progressDialog.show();

                    // ===== Convert image to Base64 if user selected one =====
                    // If no image selected, use empty string — default profile will show
                    String base64Image = "";
                    if (imageURI != null) {
                        base64Image = convertImageToBase64(imageURI); //method used
                    }

                    // Store base64Image in final variable to use inside listener
                    final String finalBase64Image = base64Image;

                    // Create user in Firebase Auth
                    auth.createUserWithEmailAndPassword(emaill, Password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                String id = task.getResult().getUser().getUid();

                                // User's info saved in Realtime Database
                                DatabaseReference reference = database.getReference().child("user").child(id);

                                // Save user data — profile pic is Base64 string or empty
                                Users users = new Users(id, namee, emaill, Password, finalBase64Image, status);
                                reference.setValue(users).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        progressDialog.dismiss();
                                        if (task.isSuccessful()) {
                                            // Success — go to MainActivity
                                            Intent intent = new Intent(registration.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // Database save failed — delete Auth user so same email can be used again
                                            auth.getCurrentUser().delete();
                                            Toast.makeText(registration.this, "Error saving user data. Please try again.", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });

                            } else {
                                // Firebase Auth user creation failed
                                progressDialog.dismiss();
                                Toast.makeText(registration.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        // Profile image picker
        rg_profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 10);
            }
        });
    }

    // ===== Convert selected image URI to Base64 string =====
    // This lets us save the image directly in Firebase Realtime Database
    // as a text string — no Firebase Storage needed
    private String convertImageToBase64(Uri imageUri) {
        try {
            // Open the image as a stream
            InputStream inputStream = getContentResolver().openInputStream(imageUri);

            // Decode into bitmap and compress to reduce size
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize bitmap to max 200x200 to keep database size small
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true);

            // Compress to JPEG with 50% quality to reduce size further
            // ===== X = B1×2^16 + B2×2^8 + B3 happens here =====
            // All 3 photo bytes are merged into one stream
            // by ByteArrayOutputStream before encoding

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);

            // Convert to Base64 string
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (Exception e) {
            // If something goes wrong return empty string — default profile will show
            Toast.makeText(this, "Could not process image, using default profile", Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    // Handle image selection result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (data != null) {
                imageURI = data.getData();
                rg_profileImg.setImageURI(imageURI); // show selected image locally
            }
        }
    }
}