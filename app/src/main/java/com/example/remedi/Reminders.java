package com.example.remedi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reminders extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedAdapter adapter;
    private List<Map<String, Object>> medList;

    private FirebaseFirestore db;
    private String patientUID;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        recyclerView = findViewById(R.id.recyclerView); // make sure your XML has this
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        medList = new ArrayList<>();
        adapter = new MedAdapter(medList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        patientUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadMedsRealtime();
    }

    private void loadMedsRealtime() {
        listenerRegistration = db.collection("reminders")
                .document(patientUID)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(Reminders.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        medList.clear();
                        List<Map<String, Object>> meds = (List<Map<String, Object>>) snapshot.get("meds");
                        if (meds != null) {
                            medList.addAll(meds);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    // RecyclerView Adapter
    static class MedAdapter extends RecyclerView.Adapter<MedAdapter.MedViewHolder> {

        private final List<Map<String, Object>> meds;

        MedAdapter(List<Map<String, Object>> meds) {
            this.meds = meds;
        }

        @NonNull
        @Override
        public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view, parent, false);
            return new MedViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
            Map<String, Object> med = meds.get(position);
            holder.name.setText((String) med.get("name"));
            holder.time.setText((String) med.get("time"));
            holder.checkbox.setChecked(Boolean.TRUE.equals(med.get("taken")));

            // Update Firestore when checkbox is toggled
            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                FirebaseFirestore.getInstance()
                        .collection("reminders")
                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .update("meds", meds) // overwrite meds list with updated "taken" values
                        .addOnSuccessListener(aVoid -> {
                            med.put("taken", isChecked);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(buttonView.getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                        });
            });
        }

        @Override
        public int getItemCount() {
            return meds.size();
        }

        static class MedViewHolder extends RecyclerView.ViewHolder {
            TextView name, time;
            CheckBox checkbox;

            MedViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.medName);
                time = itemView.findViewById(R.id.medTime);
                checkbox = itemView.findViewById(R.id.medCheckbox);
            }
        }
    }
}
