package com.example.projectii;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserPickerActivity extends AppCompatActivity {

    RecyclerView userPickerRecyclerView;
    EditText searchUser;
    ArrayList<Users> allUsers;       // all users from Firebase
    ArrayList<Users> filteredUsers;  // users after search filter
    UserPickerAdapter adapter;
    FirebaseAuth auth;
    String base64Photo;              // Base64 photo string passed from MainActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_picker);

        getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();

        // Get the Base64 photo string passed from MainActivity
        base64Photo = getIntent().getStringExtra("photoBase64");

        userPickerRecyclerView = findViewById(R.id.userPickerRecyclerView);
        searchUser = findViewById(R.id.searchUser);

        allUsers = new ArrayList<>();
        filteredUsers = new ArrayList<>();

        userPickerRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Adapter — when user is clicked, go to their chat window with photo ready to send
        adapter = new UserPickerAdapter(this, filteredUsers, user -> {
            Intent intent = new Intent(UserPickerActivity.this, chatwindo.class);
            intent.putExtra("nameeee", user.getUsername());
            intent.putExtra("reciverImg", user.getProfilepic());
            intent.putExtra("uid", user.getUserId());
            intent.putExtra("photoToSend", base64Photo); // pass photo to chat window
            startActivity(intent);
            finish();
        });

        userPickerRecyclerView.setAdapter(adapter);

        // ===== Load all users from Firebase except yourself =====
        FirebaseDatabase.getInstance().getReference().child("user")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allUsers.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Users user = dataSnapshot.getValue(Users.class);
                            // Skip yourself
                            if (user.getUserId().equals(auth.getUid())) continue;
                            allUsers.add(user);
                        }
                        // Show all users initially
                        filteredUsers.clear();
                        filteredUsers.addAll(allUsers);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });

        // ===== Search filter =====
        searchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter users by name as user types
                filteredUsers.clear();
                String query = s.toString().toLowerCase().trim();
                if (query.isEmpty()) {
                    filteredUsers.addAll(allUsers);
                } else {
                    for (Users user : allUsers) {
                        if (user.getUsername().toLowerCase().contains(query)) {
                            filteredUsers.add(user);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

    }
}