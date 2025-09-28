package com.example.remedi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class gNotes extends AppCompatActivity {

    private NotesAdapter adapter;
    private List<String> titles;
    private List<String> noteIds;
    private FirebaseFirestore db;
    private String userUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gnotes);

        ListView listView = findViewById(R.id.listView);
        Button addNoteButton = findViewById(R.id.add_button);

        titles = new ArrayList<>();
        noteIds = new ArrayList<>();

        adapter = new NotesAdapter(this, titles, noteIds);
        listView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadNotes();

        addNoteButton.setOnClickListener(v ->
                startActivity(new Intent(gNotes.this, AddNotes.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        db.collection("users")
                .document(userUID)
                .collection("notes")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    titles.clear();
                    noteIds.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        titles.add(doc.getString("title"));
                        noteIds.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notes", Toast.LENGTH_SHORT).show());
    }
}
