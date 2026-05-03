package com.example.projectii;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class login extends AppCompatActivity {
    TextView logsignup;
    Button button;
    EditText email, password;

    FirebaseAuth auth;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    android.app.ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance();

        // ===== AUTO LOGIN CHECK =====
        // If user is already logged in, go directly to MainActivity
        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(login.this, MainActivity.class);
            startActivity(intent);
            finish(); // Prevents back button from returning to login
            return; // Stop further execution of onCreate
        }
        setContentView(R.layout.activity_login);
        //hold message like loading.... after logged in
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait....");
        progressDialog.setCancelable(false);
        getSupportActionBar().hide();


        // Link UI elements from XML
        button = findViewById(R.id.logbutton);
        email=findViewById(R.id.editTextLogEmail);
        password=findViewById(R.id.editTextLogPassword);
        logsignup = findViewById(R.id.logsignup);

        logsignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(login.this,registration.class);
                startActivity(intent);
                finish();
            }
        });

        //when login button is clicked
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String Email = email.getText().toString();
                String pass = password.getText().toString();

                //validation code
                if ((TextUtils.isEmpty(Email))){
                    progressDialog.dismiss();
                    Toast.makeText(login.this, "Enter The Email", Toast.LENGTH_SHORT).show();
                }else if (TextUtils.isEmpty(pass)){
                    progressDialog.dismiss();
                    Toast.makeText(login.this, "Enter The Password", Toast.LENGTH_SHORT).show();
                }else if(!Email.matches(emailPattern)){
                    progressDialog.dismiss();
                    email.setError("Invalid Email Address");
                }else if(password.length()<6){
                    progressDialog.dismiss();
                    password.setError("More than 6 characters");
                    Toast.makeText(login.this, "Password Needs To Be Longer Than 6 Characters", Toast.LENGTH_SHORT).show();
                }else{
                    // Firebase login attempt with email and password
                    auth.signInWithEmailAndPassword(Email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // Check if login was successful
                            if(task.isSuccessful()){
                                progressDialog.show(); //shows loading when login is successful
                                try{
                                    //navigate to mainactivity
                                    Intent intent =new Intent(login.this , MainActivity.class);
                                    startActivity(intent);
                                    finish(); //if not finish then it will redirect to login screen when backed
                                }catch(Exception e){
                                    Toast.makeText(login.this , e.getMessage(), Toast.LENGTH_SHORT).show();
                                }

                            }else{
                                Toast.makeText(login.this , task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }

            }
        });



    }
}