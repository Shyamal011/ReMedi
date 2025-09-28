package com.example.remedi;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medication_reminder_channel";
    private static final String CHANNEL_NAME = "Medication Alarms";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"com.example.remedi.MEDICATION_ALARM".equals(intent.getAction())) {
            Log.w("AlarmReceiver", "Received intent with unknown action or null.");
            return;
        }

        String medName = intent.getStringExtra("MED_NAME");
        String medTime = intent.getStringExtra("MED_TIME");
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", (int) System.currentTimeMillis());

        if (medName == null || medTime == null) {
            Log.e("AlarmReceiver", "Medication name or time is null. Cannot show notification.");
            return;
        }

        // 1. Reschedule the alarm for the next day immediately.
        rescheduleAlarm(context, medName, medTime, notificationId, 1); // Reschedule for tomorrow


        // 2. Create Full-Screen Intent for Navigation to Reminders
        Intent reminderIntent = new Intent(context, Reminders.class);
        reminderIntent.putExtra("FROM_ALARM", true);
        reminderIntent.putExtra("NOTIFICATION_ID", notificationId); // Pass ID to Reminders to dismiss notification
        reminderIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 1,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        createNotificationChannel(context);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // 3. Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a relevant icon
                .setContentTitle("MEDICATION TIME: " + medName)
                .setContentText("It's " + medTime + ", please take your " + medName + ".")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(alarmSound)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent);

        // 4. Show the notification
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.i("AlarmReceiver", "Notification shown successfully for ID: " + notificationId);
        } catch (SecurityException e) {
            Log.e("AlarmReceiver", "SecurityException: Missing POST_NOTIFICATIONS permission. Notification failed.", e);
        } catch (Exception e) {
            Log.e("AlarmReceiver", "Error showing notification.", e);
        }
    }

    /**
     * Reschedules the alarm for the same time, a number of days in the future.
     * MADE STATIC so it can be called from Reminders.java
     */
    public static void rescheduleAlarm(Context context, String medName, String medTime, int notificationId, int daysToAdd) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.example.remedi.MEDICATION_ALARM");
        intent.putExtra("MED_NAME", medName);
        intent.putExtra("MED_TIME", medTime);
        intent.putExtra("NOTIFICATION_ID", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Calendar calendar = Calendar.getInstance();
        try {
            // Logic to parse "HH:MM AM/PM" format
            String[] timeParts = medTime.split(" ");
            String time = timeParts[0];
            String ampm = timeParts[1];

            String[] hourMinute = time.split(":");
            int hour = Integer.parseInt(hourMinute[0]);
            int minute = Integer.parseInt(hourMinute[1]);

            if (ampm.equalsIgnoreCase("PM") && hour != 12) {
                hour += 12;
            } else if (ampm.equalsIgnoreCase("AM") && hour == 12) {
                hour = 0; // Midnight case
            }

            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

        } catch (Exception e) {
            Log.e("AlarmReceiver", "Error parsing time for reschedule: " + medTime, e);
            return;
        }

        // Schedule for the correct time offset
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);
        long alarmTime = calendar.getTimeInMillis();

        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                }
                Log.d("AlarmReceiver", "Alarm rescheduled for " + medName + " in " + daysToAdd + " day(s) at " + calendar.getTime().toString());
            } catch (SecurityException e) {
                Log.e("AlarmReceiver", "Failed to reschedule alarm: Exact alarm permission denied.", e);
            }
        }
    }

    private void createNotificationChannel(Context context) {
        // Ensure we retrieve the manager safely here as well
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled medication times.");
            channel.setSound(alarmSound, null);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000});

            notificationManager.createNotificationChannel(channel);
        }
    }
}