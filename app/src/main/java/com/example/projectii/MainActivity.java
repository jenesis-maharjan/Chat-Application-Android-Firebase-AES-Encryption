package com.example.projectii;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    RecyclerView mainUserRecyclerView;
    UserAdapter adapter;
    FirebaseDatabase database;
    ArrayList<Users> usersArrayList;
    ImageView imglogout;
    ImageView cumbut, setbut;

    // Reference to current user's online status in Firebase
    DatabaseReference onlineStatusRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        cumbut = findViewById(R.id.camBut);
        setbut = findViewById(R.id.settingBut);

        // ===== Set user online when MainActivity opens =====
        if (auth.getCurrentUser() != null) {
            onlineStatusRef = database.getReference()
                    .child("user")
                    .child(auth.getUid())
                    .child("online");
            onlineStatusRef.setValue(true); // user is now active

            // ===== onDisconnect — Firebase automatically sets user offline =====
            // when the app is killed, crashes, or loses network connection
            // This is more reliable than onPause/onResume because Firebase
            // handles it on the server side even if the app closes abruptly
            onlineStatusRef.onDisconnect().setValue(false);
        }

        // Reference to all users in Firebase
        DatabaseReference reference = database.getReference().child("user");

        // Memory allocation of arraylist
        usersArrayList = new ArrayList<>();

        mainUserRecyclerView = findViewById(R.id.mainUserRecyclerView);
        mainUserRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(MainActivity.this, usersArrayList);
        mainUserRecyclerView.setAdapter(adapter);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // Clear list first to avoid duplicates on every refresh
                usersArrayList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    // Convert each child into a Users object
                    Users users = dataSnapshot.getValue(Users.class);

                    // Skip yourself from the user list
                    if (users.getUserId().equals(auth.getUid())) {
                        continue;
                    }

                    // Add other users to the list
                    usersArrayList.add(users);
                }

                // Notify adapter to refresh RecyclerView
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Logout button
        imglogout = findViewById(R.id.logoutimg);
        imglogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Show logout confirmation dialog
                Dialog dialog = new Dialog(MainActivity.this, R.style.dialogue);
                dialog.setContentView(R.layout.dialog_layout);
                Button no, yes;

                yes = dialog.findViewById(R.id.yesbtn);
                no = dialog.findViewById(R.id.nobtn);

                // If clicked yes sign out and go to login
                yes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ===== Set user offline before logging out =====
                        // Cancel the onDisconnect first so it does not fire twice
                        // then manually set offline right now
                        if (onlineStatusRef != null) {
                            onlineStatusRef.onDisconnect().cancel();
                            onlineStatusRef.setValue(false);
                        }
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(MainActivity.this, login.class);
                        startActivity(intent);
                        finish();
                    }
                });

                // If clicked no dismiss dialog
                no.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        // Settings button
        setbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, setting.class);
                startActivity(intent);
            }
        });

        // Camera button — take photo then open user picker
        cumbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 10);
            }
        });

        // If not logged in redirect to login page
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, login.class);
            startActivity(intent);
        }
    }

    // ===== Set user online when app comes back to foreground =====
    // onResume still sets online so navigating back from settings or
    // other activities correctly marks the user as active again
    @Override
    protected void onResume() {
        super.onResume();
        // User came back to the app — set online and re-register onDisconnect
        if (onlineStatusRef != null && auth.getCurrentUser() != null) {
            onlineStatusRef.setValue(true);
            onlineStatusRef.onDisconnect().setValue(false);
        }
    }

    // ===== onPause removed — replaced by onDisconnect =====
    // Previously onPause set user offline which caused a flicker when
    // navigating between activities (MainActivity → chatwindo would
    // briefly show Offline before chatwindo set Online again).
    // Firebase onDisconnect handles going offline automatically when
    // the app is actually closed or loses connection — no flicker.

    // ===== Handle camera result =====
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {

            // Get captured photo as Bitmap
            Bitmap photo = (Bitmap) data.getExtras().get("data");

            // Convert Bitmap to Base64 string
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Photo = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Open user picker with the photo
            Intent intent = new Intent(MainActivity.this, UserPickerActivity.class);
            intent.putExtra("photoBase64", base64Photo);
            startActivity(intent);
        }
    }
}