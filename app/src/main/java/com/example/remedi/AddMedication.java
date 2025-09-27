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

import java.util.ArrayList;
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

        medNameInput = findViewById(R.id.medNameInput);
        totalQtyInput = findViewById(R.id.totalQtyInput);
        dosageQtyInput = findViewById(R.id.dosageQtyInput);
        timeInput = findViewById(R.id.timeInput);
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

        db.collection("reminders")
                .document(patientUID)
                .update("meds", FieldValue.arrayUnion(med))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medication added", Toast.LENGTH_SHORT).show();
                    finish(); // go back to previous page
                })
                .addOnFailureListener(e -> {
                    // Document doesn't exist, create a new one
                    ArrayList<Map<String, Object>> medArray = new ArrayList<>();
                    medArray.add(med);

                    Map<String, Object> newDoc = new HashMap<>();
                    newDoc.put("meds", medArray);

                    db.collection("reminders")
                            .document(patientUID)
                            .set(newDoc)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Medication added", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(err ->
                                    Toast.makeText(this, "Error: " + err.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                });


    }
}
