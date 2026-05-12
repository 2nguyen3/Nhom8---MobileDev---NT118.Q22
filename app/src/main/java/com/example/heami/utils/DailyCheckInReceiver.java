package com.example.heami.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.heami.R;
import com.example.heami.ui.main.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DailyCheckInReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "heami_daily_checkin";
    private static final String CHANNEL_NAME = "Daily Check-in Reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Reschedule next exact alarm for tomorrow
        NotificationScheduler.scheduleDailyCheckIn(context);

        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Check if daily checkin notification is enabled in SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("HeamiSettings", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("notif_checkin", true);
        if (!isEnabled) return;

        // Trigger notification
        showNotification(context);
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Nhắc nhở ghi nhận cảm xúc hàng ngày");
            notificationManager.createNotificationChannel(channel);
        }

        Intent clickIntent = new Intent(context, HomeActivity.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, clickIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_playstore)
                .setContentTitle("Heami-er ơi, hôm nay thế nào?")
                .setContentText("Dành thời gian để ghi chép lại tâm trạng và cảm xúc ngày hôm nay nhé!")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(1002, builder.build());
    }
}
