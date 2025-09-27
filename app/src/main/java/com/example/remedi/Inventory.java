package com.example.remedi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class Inventory extends AppCompatActivity {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<Map<String, Object>> inventoryList;

    private FirebaseFirestore db;
    private String userUID;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        recyclerView = findViewById(R.id.recyclerViewInventory); // Make sure this ID exists
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        inventoryList = new ArrayList<>();
        adapter = new InventoryAdapter(inventoryList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadInventoryRealtime();
    }

    private void loadInventoryRealtime() {
        listenerRegistration = db.collection("reminders")
                .document(userUID)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot != null && snapshot.exists()) {
                        inventoryList.clear();
                        Object medsObj = snapshot.get("meds");
                        if (medsObj instanceof List<?>) {
                            for (Object o : (List<?>) medsObj) {
                                if (o instanceof Map<?, ?>) {
                                    Map<String, Object> med = new HashMap<>();
                                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
                                        med.put(String.valueOf(entry.getKey()), entry.getValue());
                                    }
                                    inventoryList.add(med);
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

    // RecyclerView Adapter
    static class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {
        private final List<Map<String, Object>> items;

        InventoryAdapter(List<Map<String, Object>> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.each_stock, parent, false);
            return new InventoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);
            holder.name.setText((String) item.get("name"));

            Object totalObj = item.get("totalQty");

            int totalQty = 0;
            try {
                totalQty = Integer.parseInt(String.valueOf(totalObj));
            } catch (NumberFormatException e) {
                totalQty = 0;
            }

            holder.quantity.setText(totalQty + " left");
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class InventoryViewHolder extends RecyclerView.ViewHolder {
            TextView name, quantity;

            InventoryViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.medication_name);
                quantity = itemView.findViewById(R.id.doses_left);
            }
        }
    }
    public void addMed(View view) {
        startActivity(new Intent(Inventory.this, AddMedication.class));
    }
}
