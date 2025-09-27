package com.example.remedi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medication_reminder_channel";
    private static final String CHANNEL_NAME = "Medication Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Essential safety check for the Intent
        if (intent == null || intent.getAction() == null) {
            Log.e("AlarmReceiver", "Received null Intent or Intent with null action.");
            return;
        }

        // Only process the intent if it's our specific alarm action
        if (!"com.example.remedi.MEDICATION_ALARM".equals(intent.getAction())) {
            Log.w("AlarmReceiver", "Received intent with unknown action: " + intent.getAction());
            return;
        }

        String medName = intent.getStringExtra("MED_NAME");
        String medTime = intent.getStringExtra("MED_TIME");
        // Ensure default value is unique and not 0, which might conflict with system notifications.
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", (int) System.currentTimeMillis());

        if (medName == null || medName.isEmpty()) {
            Log.e("AlarmReceiver", "Medication name is null or empty. Cannot show notification.");
            return;
        }

        Log.d("AlarmReceiver", "Alarm triggered for: " + medName + " at " + medTime + " (ID: " + notificationId + ")");

        // 1. Create Notification Channel (required for Android O and above)
        createNotificationChannel(context);

        // Ensure we retrieve the manager safely
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e("AlarmReceiver", "NotificationManager service not available.");
            return;
        }

        // 2. Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // CRITICAL FIX: Add a small icon resource. Assumes you have a drawable named ic_notification
                // If not, use a simple one like android.R.drawable.ic_lock_idle_alarm as a placeholder.
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Time to take your medicine!")
                .setContentText("It's " + medTime + ", please take your " + medName + ".")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // 3. Show the notification
        try {
            // notificationId must be unique for each alarm!
            notificationManager.notify(notificationId, builder.build());
            Log.i("AlarmReceiver", "Notification shown successfully for ID: " + notificationId);
        } catch (SecurityException e) {
            Log.e("AlarmReceiver", "SecurityException: Missing POST_NOTIFICATIONS permission. Notification failed.", e);
        } catch (Exception e) {
            Log.e("AlarmReceiver", "Error showing notification.", e);
        }
    }

    private void createNotificationChannel(Context context) {
        // Ensure we retrieve the manager safely here as well
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled medication times.");
            notificationManager.createNotificationChannel(channel);
        }
    }
}
