package com.example.heami.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.heami.R;
import com.example.heami.ui.consultation.ConsultationsActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppointmentNotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "heami_appointment";
    private static final String CHANNEL_NAME = "Lịch hẹn bác sĩ";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if user has enabled notifications for appointments
        SharedPreferences prefs = context.getSharedPreferences("HeamiSettings", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("notif_appoint", true);
        if (!isEnabled) return;

        String sessionId = intent.getStringExtra("session_id");
        String doctorName = intent.getStringExtra("doctor_name");
        long startTimeMillis = intent.getLongExtra("start_time", 0);
        String reminderType = intent.getStringExtra("reminder_type");
        if (reminderType == null) {
            reminderType = "1_day";
        }

        if (doctorName == null) {
            doctorName = "Bác sĩ chuyên gia";
        }

        String timeStr = "";
        if (startTimeMillis > 0) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeStr = timeFormat.format(new Date(startTimeMillis));
        }

        showNotification(context, doctorName, timeStr, reminderType);
    }

    private void showNotification(Context context, String doctorName, String timeStr, String reminderType) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo nhắc nhở lịch hẹn với bác sĩ");
            notificationManager.createNotificationChannel(channel);
        }

        Intent clickIntent = new Intent(context, ConsultationsActivity.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2001, clickIntent, flags);

        String title;
        String message;
        int notificationId;

        if ("15_min".equals(reminderType)) {
            title = "Lịch hẹn tư vấn sắp bắt đầu! ⏰";
            message = "Lịch hẹn của bạn cùng " + doctorName + " sẽ bắt đầu sau 15 phút nữa (lúc " + timeStr + "). Hãy chuẩn bị nhé!";
            notificationId = doctorName.hashCode() + 15;
        } else {
            title = "Nhắc nhở lịch hẹn ngày mai 📅";
            message = "Bạn có lịch hẹn tư vấn cùng " + doctorName + " vào ngày mai lúc " + timeStr + ". Hãy nhớ chuẩn bị nhé!";
            notificationId = doctorName.hashCode();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_playstore)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(notificationId, builder.build());
    }
}
