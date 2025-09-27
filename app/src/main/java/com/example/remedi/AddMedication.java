package com.example.remedi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddMedication extends AppCompatActivity {

    private EditText medNameInput, totalQtyInput, dosageQtyInput, timeInput;
    private Button addButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        medNameInput = findViewById(R.id.editTextText);
        totalQtyInput = findViewById(R.id.editTextNumber);
        dosageQtyInput = findViewById(R.id.editTextNumber1);
        timeInput = findViewById(R.id.editTextTime);
        addButton = findViewById(R.id.button3);

        db = FirebaseFirestore.getInstance();

        addButton.setOnClickListener(v -> saveMedication());
    }

    private void saveMedication() {
        String name = medNameInput.getText().toString().trim();
        String totalQty = totalQtyInput.getText().toString().trim();
        String dosageQty = dosageQtyInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();

        if (name.isEmpty()) {
            medNameInput.setError("Enter medication name");
            return;
        }

        String patientUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> med = new HashMap<>();
        med.put("name", name);
        med.put("totalQty", totalQty);
        med.put("dosageQty", dosageQty);
        med.put("time", time);
        med.put("taken", false);

        db.collection("reminders").document(patientUID)
                .set(
                        new HashMap<String, Object>() {{
                            put("meds", FieldValue.arrayUnion(med));
                        }},
                        com.google.firebase.firestore.SetOptions.merge()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medication added", Toast.LENGTH_SHORT).show();
                    finish(); // go back to Reminders page
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
