package com.example.remedi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LinkPatient extends AppCompatActivity {

    private EditText patientCodeInput;
    private FirebaseFirestore db;
    private String linkedPatientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_patient);

        patientCodeInput = findViewById(R.id.patientCodeInput);
        db = FirebaseFirestore.getInstance();
    }
    public void Link(View view) {
        String patientCode = patientCodeInput.getText().toString().trim();
        if (patientCode.isEmpty()) {
            Toast.makeText(this, "Enter patient code", Toast.LENGTH_SHORT).show();
            return;
        }
        linkGuardian(patientCode);
    }
    private void linkGuardian(String patientCode) {
        db.collection("users").document(patientCode).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String type = task.getResult().getString("type");
                if (!"patient".equals(type)) {
                    Toast.makeText(this, "Invalid patient code", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, Object> guardianData = new HashMap<>();
                guardianData.put("linkedPatient", patientCode);

                db.collection("guardians").add(guardianData)
                        .addOnSuccessListener(docRef -> {
                            db.collection("users").document(patientCode)
                                    .update("guardianId", docRef.getId());

                            Toast.makeText(this, "Guardian linked successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LinkPatient.this, gHomePage.class);
                            intent.putExtra("linkedPatientId", linkedPatientId);
                            startActivity(intent);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to link guardian: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            } else {
                Toast.makeText(this, "Patient not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
