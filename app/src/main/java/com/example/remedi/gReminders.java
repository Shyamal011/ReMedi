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

public class gReminders extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedAdapter adapter;
    private List<Map<String, Object>> medList;

    private FirebaseFirestore db;
    private String patientUID;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greminders);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        medList = new ArrayList<>();
        // MedAdapter is now a NON-STATIC inner class
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
                        Toast.makeText(gReminders.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
        startActivity(new Intent(gReminders.this, AddMedication.class));
    }

    /**
     * UTILITY METHOD to safely parse quantity values from the Firestore map.
     * This handles String (from AddMedication) and various Number types (from Firestore).
     */
    private int getQuantityValue(Map<String, Object> med, String key) {
        Object value = med.get(key);
        if (value == null) return 0;

        // Handles common Firebase number types (Long, Integer, Double)
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        // Handles String type (as saved by your current AddMedication.java)
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                // Log.e("Reminders", "Error parsing quantity string for key " + key + ": " + value, e);
                return 0;
            }
        }

        return 0;
    }

    // RecyclerView Adapter class inside Reminders (NON-STATIC)
    class MedAdapter extends RecyclerView.Adapter<MedAdapter.MedViewHolder> {
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
        public void onBindViewHolder(@NonNull MedAdapter.MedViewHolder holder, int position) {
            Map<String, Object> med = meds.get(position);
            holder.name.setText((String) med.get("name"));
            holder.time.setText((String) med.get("time"));

            // Set Checkbox State
            holder.checkbox.setOnCheckedChangeListener(null);
            boolean isTaken = Boolean.TRUE.equals(med.get("taken"));
            holder.checkbox.setChecked(isTaken);
            holder.checkbox.setEnabled(!isTaken); // Disable if already taken

            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Only process the stock change if checking the box (taking medication)
                if (isChecked) {

                    // --- FIX: Use safe retrieval method for quantities ---
                    int totalQty = getQuantityValue(med, "totalQty");
                    int dosageQty = getQuantityValue(med, "dosageQty");

                    if (dosageQty > 0) {
                        totalQty -= dosageQty;
                        if (totalQty < 0) {
                            totalQty = 0; // prevent negative stock
                            Toast.makeText(buttonView.getContext(), "Warning: " + med.get("name") + " inventory is now empty!", Toast.LENGTH_SHORT).show();
                        }

                        // Update the map item with the new totalQty (as an Integer/Long for better consistency)
                        med.put("totalQty", totalQty);
                    }


                    // 🔹 Mark as taken
                    med.put("taken", true);


                    // 🔹 Save back to Firestore
                    FirebaseFirestore.getInstance()
                            .collection("reminders")
                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .set(new HashMap<String, Object>() {{
                                put("meds", meds);
                            }}, SetOptions.merge())

                            .addOnFailureListener(e ->
                                    Toast.makeText(buttonView.getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );

                    // Disable the checkbox instantly after checking it.
                    holder.checkbox.setEnabled(false);
                }
            });
        }

        @Override
        public int getItemCount() {
            return meds.size();
        }

        // MedViewHolder class inside MedAdapter (NON-STATIC)
        class MedViewHolder extends RecyclerView.ViewHolder {
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