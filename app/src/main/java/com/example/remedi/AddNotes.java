package com.example.remedi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddNotes extends AppCompatActivity {

    private EditText editTitle, editContent;
    private Button buttonAdd;
    private FirebaseFirestore db;
    private String userUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_notes);

        editTitle = findViewById(R.id.editTextTitle);
        editContent = findViewById(R.id.editTextNote);
        buttonAdd = findViewById(R.id.buttonAddNote);

        db = FirebaseFirestore.getInstance();
        userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        buttonAdd.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("content", content);
        note.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users")
                .document(userUID)
                .collection("notes")
                .add(note)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                );
    }
}
