package com.example.remedi;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class IndividualNotePage extends AppCompatActivity {

    private TextView titleView, contentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_note_page);

        titleView = findViewById(R.id.editTextTitle);
        contentView = findViewById(R.id.editTextNote);

        String noteId = getIntent().getStringExtra("NOTE_ID");
        String userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (noteId != null) {
            db.collection("users")
                    .document(userUID)
                    .collection("notes")
                    .document(noteId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            titleView.setText(doc.getString("title"));
                            contentView.setText(doc.getString("content"));
                        } else {
                            Toast.makeText(this, "Note not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load note", Toast.LENGTH_SHORT).show()
                    );
        }
    }
}
