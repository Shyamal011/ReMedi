package com.example.remedi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.SetOptions;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
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

        recyclerView = findViewById(R.id.recyclerView);
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

                        Object medsObj = snapshot.get("meds");
                        if (medsObj instanceof List<?>) {
                            for (Object o : (List<?>) medsObj) {
                                if (o instanceof Map<?, ?>) {
                                    Map<String, Object> med = new HashMap<>();
                                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
                                        med.put(String.valueOf(entry.getKey()), entry.getValue());
                                    }
                                    medList.add(med);
                                }
                            }
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

    public void addMed(View view) {
        startActivity(new Intent(Reminders.this, AddMedication.class));
    }

    // RecyclerView Adapter class inside Reminders
    static class MedAdapter extends RecyclerView.Adapter<MedAdapter.MedViewHolder> {
        private final List<Map<String, Object>> meds;

        MedAdapter(List<Map<String, Object>> meds) {
            this.meds = meds;
        }

        @NonNull
        @Override
        public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.each_med, parent, false);
            return new MedViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
            Map<String, Object> med = meds.get(position);
            holder.name.setText((String) med.get("name"));
            holder.time.setText((String) med.get("time"));
            holder.checkbox.setOnCheckedChangeListener(null); // avoid old listeners
            holder.checkbox.setChecked(Boolean.TRUE.equals(med.get("taken")));

            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                med.put("taken", isChecked);

                // 🔹 Only subtract if checked
                if (isChecked) {
                    Object totalObj = med.get("totalQty");
                    Object doseObj = med.get("dosageQty");

                    int totalQty = totalObj instanceof Number ? ((Number) totalObj).intValue() : 0;
                    int dosageQty = doseObj instanceof Number ? ((Number) doseObj).intValue() : 0;

                    if (dosageQty > 0) {
                        totalQty -= dosageQty;
                        if (totalQty < 0) totalQty = 0; // prevent negative stock
                        med.put("totalQty", totalQty);
                    }
                }

                // 🔹 Save back to Firestore
                FirebaseFirestore.getInstance()
                        .collection("reminders")
                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .set(new HashMap<String, Object>() {{
                            put("meds", meds);
                        }}, SetOptions.merge())

                        .addOnFailureListener(e ->
                                Toast.makeText(buttonView.getContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                        );


                notifyItemChanged(holder.getAdapterPosition());
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

