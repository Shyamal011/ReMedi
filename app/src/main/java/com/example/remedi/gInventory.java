package com.example.remedi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class gInventory extends AppCompatActivity {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<Map<String, Object>> inventoryList;

    private FirebaseFirestore db;
    private String userUID;
    private ListenerRegistration listenerRegistration;

    // Notification constants
    private static final String CHANNEL_ID = "low_stock_channel";
    private static final String CHANNEL_NAME = "Low Stock Alerts";
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int PERMISSION_REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ginventory);

        recyclerView = findViewById(R.id.recyclerViewInventory); // Make sure this ID exists
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        inventoryList = new ArrayList<>();
        // InventoryAdapter is now a NON-STATIC inner class
        adapter = new InventoryAdapter(inventoryList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userUID = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userUID != null) {
            // Check and request notification permission on app start (Android 13+)
            requestNotificationPermission();
            loadInventoryData();
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Inventory", "Notification permission granted.");
            } else {
                Toast.makeText(this, "Notification permission denied. Low stock alerts may not show.", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void loadInventoryData() {
        // Assume inventory data is stored in the 'reminders' document under the 'meds' array
        listenerRegistration = db.collection("reminders")
                .document(userUID)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("Inventory", "Listen failed.", e);
                        Toast.makeText(this, "Failed to load inventory.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        inventoryList.clear();

                        List<Map<String, Object>> meds = (List<Map<String, Object>>) documentSnapshot.get("meds");

                        if (meds != null) {
                            for (Map<String, Object> med : meds) {
                                // Add to list for RecyclerView
                                inventoryList.add(med);

                                // --- LOW STOCK CHECK ---
                                String medName = (String) med.get("name");
                                Object totalObj = med.get("totalQty");
                                // Use safe helper method for parsing quantity
                                int totalQty = getQuantityValue(totalObj);

                                // Check if stock is low (but not zero/negative)
                                if (medName != null && totalQty > 0 && totalQty < LOW_STOCK_THRESHOLD) {
                                    sendLowStockNotification(medName, totalQty);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        inventoryList.clear();
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * Helper method to safely parse quantity values from Firestore objects.
     * This handles String, Long, Integer, and Double types.
     */
    private int getQuantityValue(Object value) {
        if (value == null) return 0;

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }


    /**
     * Sends a notification if the medication stock is low.
     */
    private void sendLowStockNotification(String medName, int currentQty) {
        createNotificationChannel();

        // Use a unique ID based on the medication name hash to prevent notification spam
        int notificationId = medName.hashCode();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                // Ensure R.drawable.ic_launcher_foreground exists, or use a system icon
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("LOW STOCK ALERT: " + medName)
                .setContentText("Only " + currentQty + " doses of " + medName + " remain. Please restock soon.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            try {
                // Notifies the user, using the unique ID for the medicine
                notificationManager.notify(notificationId, builder.build());
            } catch (SecurityException e) {
                Log.e("Inventory", "Failed to send notification: Missing POST_NOTIFICATIONS permission.", e);
            }
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for when medication inventory runs low.");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Existing method to navigate to AddMedication
    public void addMed(View view) {
        startActivity(new Intent(gInventory.this, AddMedication.class));
    }


    // InventoryAdapter class - NON-STATIC
    class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

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

            // Use the safe helper method defined in the activity
            int totalQty = getQuantityValue(totalObj);

            // Highlight low stock in the UI
            if (totalQty < LOW_STOCK_THRESHOLD) {
                holder.quantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
            } else {
                // Set default color
                holder.quantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
            }

            holder.quantity.setText(totalQty + " left");
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        // InventoryViewHolder class - NON-STATIC
        class InventoryViewHolder extends RecyclerView.ViewHolder {
            TextView name, quantity;

            InventoryViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.medication_name);
                quantity = itemView.findViewById(R.id.doses_left);
            }
        }
    }
}