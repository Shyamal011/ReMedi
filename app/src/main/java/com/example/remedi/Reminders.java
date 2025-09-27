package com.example.remedi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
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

    private static final String TAG = "RemindersActivity"; // Log tag for debugging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        // Get the patient UID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            patientUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            patientUID = "anonymous_user";
        }


        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        medList = new ArrayList<>();
        adapter = new MedAdapter(medList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        listenForReminders();
    }

    private void listenForReminders() {
        if (patientUID.equals("anonymous_user")) {
            Log.w(TAG, "Cannot listen for reminders: User is anonymous.");
            return;
        }

        listenerRegistration = db.collection("reminders")
                .document(patientUID)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        medList.clear();

                        Object medsObject = documentSnapshot.get("meds");
                        if (medsObject instanceof List) {
                            try {
                                List<?> rawList = (List<?>) medsObject;
                                for (Object item : rawList) {
                                    if (item instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> medMap = (Map<String, Object>) item;
                                        medList.add(medMap);
                                    }
                                }
                            } catch (ClassCastException cce) {
                                Log.e(TAG, "Error casting Firestore data to List<Map<String, Object>>", cce);
                            }
                        }

                        // CRITICAL: Schedule alarms every time the list changes
                        scheduleAllAlarms(medList, this);
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d(TAG, "Current data: null or document does not exist.");
                        medList.clear();
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
        Intent intent = new Intent(this, AddMedication.class);
        startActivity(intent);
    }

    /**
     * Schedules a daily repeating alarm for every medication in the list.
     * @param medList The list of medications to schedule.
     * @param context The application context.
     */
    public static void scheduleAllAlarms(List<Map<String, Object>> medList, Context context) {
        Log.d(TAG, "Attempting to schedule " + medList.size() + " alarms.");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service not available.");
            return;
        }

        // Iterate through all medications and set an alarm for each
        for (Map<String, Object> med : medList) {
            String timeString = (String) med.get("time");
            String medName = (String) med.get("name");

            if (timeString == null || medName == null || timeString.isEmpty()) {
                Log.e(TAG, "Skipping alarm: time or name is null/missing.");
                continue;
            }

            // Ensure we have a unique ID for the alarm
            int notificationId;
            Object idObject = med.get("notificationId");
            if (idObject instanceof Long) {
                notificationId = ((Long) idObject).intValue();
            } else if (idObject instanceof Integer) {
                notificationId = (Integer) idObject;
            } else {
                notificationId = (medName + timeString).hashCode();
                Log.w(TAG, "notificationId missing or invalid. Using hash code: " + notificationId);
            }

            try {
                // Parse time (e.g., "14:30")
                String[] parts = timeString.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                Calendar calendar = Calendar.getInstance();
                // Set the hour and minute for TODAY
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                // CRITICAL LOGIC: If the scheduled time is already past TODAY, set it for tomorrow
                long alarmTime = calendar.getTimeInMillis();
                long currentTime = System.currentTimeMillis();

                if (alarmTime <= currentTime) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    alarmTime = calendar.getTimeInMillis();
                    Log.d(TAG, "Time " + timeString + " is in the past. Scheduling for tomorrow.");
                }


                Intent intent = new Intent(context, AlarmReceiver.class);
                // Unique action to help the PendingIntent be distinct and identifiable
                intent.setAction("com.example.remedi.MEDICATION_ALARM");
                intent.putExtra("MED_NAME", medName);
                intent.putExtra("MED_TIME", timeString);
                intent.putExtra("NOTIFICATION_ID", notificationId);

                // FIX: Use FLAG_IMMUTABLE for API 31+
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    flags |= PendingIntent.FLAG_IMMUTABLE;
                }

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId, // Use unique ID for PendingIntent request code
                        intent,
                        flags
                );

                // Set a repeating alarm (daily)
                alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
                Log.i(TAG, "SUCCESS: Alarm set for " + medName + " at " + timeString + " (ID: " + notificationId + ")");

            } catch (Exception e) {
                Log.e(TAG, "Error scheduling alarm for " + medName + ": " + e.getMessage(), e);
            }
        }
    }


    // RecyclerView Adapter
    private class MedAdapter extends RecyclerView.Adapter<MedAdapter.MedViewHolder> {

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
            holder.checkbox.setChecked(Boolean.TRUE.equals(med.get("taken")));

            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                med.put("taken", isChecked);

                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous_user";

                if (!currentUid.equals("anonymous_user")) {
                    FirebaseFirestore.getInstance()
                            .collection("reminders")
                            .document(currentUid)
                            .set(new HashMap<String, Object>() {{
                                put("meds", meds);
                            }}, com.google.firebase.firestore.SetOptions.merge())
                            .addOnFailureListener(e ->
                                    Toast.makeText(buttonView.getContext(), "Failed to update taken status: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                } else {
                    Toast.makeText(buttonView.getContext(), "Cannot update: User not authenticated.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return meds.size();
        }

        public class MedViewHolder extends RecyclerView.ViewHolder {
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
