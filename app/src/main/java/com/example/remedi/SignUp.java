package com.example.remedi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText signEmail, signPass;
    private Button signButton;
    private TextView loginRedirect;
    private String userRole;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        userRole = getIntent().getStringExtra("role");
        if (userRole == null) {
            userRole = "patient";
        }
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        signEmail = findViewById(R.id.signEmail);
        signPass = findViewById(R.id.signPass);
        signButton = findViewById(R.id.signButton);
        loginRedirect = findViewById(R.id.loginRedirect);
        signButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = signEmail.getText().toString().trim();
                String pass = signPass.getText().toString().trim();
                if(user.isEmpty()) {
                    signEmail.setError("Email cannot be empty");
                }
                if(pass.isEmpty()) {
                    signPass.setError("Please enter password");
                }
                else {
                    auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                String uid = auth.getCurrentUser().getUid();
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("email", user);
                                userData.put("type", "patient");
                                userData.put("guardianId", null);
                                db.collection("users").document(uid)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(SignUp.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignUp.this, HomePage.class));
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(SignUp.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }

                        }
                    });
                }
            }
        });
        loginRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignUp.this, Login.class));
            }
        });
    }
}