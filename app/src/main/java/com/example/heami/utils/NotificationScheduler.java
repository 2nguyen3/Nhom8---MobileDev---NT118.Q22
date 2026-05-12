package com.example.heami.utils;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class NotificationScheduler {

    public static void scheduleDailyCheckIn(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyCheckInReceiver.class);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, flags);

        // Schedule at 09:00
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Dùng setAndAllowWhileIdle để không yêu cầu quyền SCHEDULE_EXACT_ALARM
                // trừ khi thực sự cần chính xác từng giây
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            }
        }
    }

    public static void cancelDailyCheckIn(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyCheckInReceiver.class);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, flags);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static void scheduleAppointmentNotification(Context context, String sessionId, long startTimeMillis, String doctorName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int baseRequestCode = sessionId != null ? sessionId.hashCode() : 2001;

        // 1. Nhắc nhở trước 1 ngày
        Intent intent1Day = new Intent(context, AppointmentNotificationReceiver.class);
        intent1Day.putExtra("session_id", sessionId);
        intent1Day.putExtra("doctor_name", doctorName);
        intent1Day.putExtra("start_time", startTimeMillis);
        intent1Day.putExtra("reminder_type", "1_day");

        PendingIntent pi1Day = PendingIntent.getBroadcast(context, baseRequestCode, intent1Day, flags);
        long triggerTime1Day = startTimeMillis - (24L * 60L * 60L * 1000L);

        if (triggerTime1Day > System.currentTimeMillis()) {
            scheduleAlarm(alarmManager, triggerTime1Day, pi1Day);
        }

        // 2. Nhắc nhở trước 15 phút
        Intent intent15Min = new Intent(context, AppointmentNotificationReceiver.class);
        intent15Min.putExtra("session_id", sessionId);
        intent15Min.putExtra("doctor_name", doctorName);
        intent15Min.putExtra("start_time", startTimeMillis);
        intent15Min.putExtra("reminder_type", "15_min");

        PendingIntent pi15Min = PendingIntent.getBroadcast(context, baseRequestCode + 9999, intent15Min, flags);
        long triggerTime15Min = startTimeMillis - (15L * 60L * 1000L);

        if (triggerTime15Min > System.currentTimeMillis()) {
            scheduleAlarm(alarmManager, triggerTime15Min, pi15Min);
        }
    }

    private static void scheduleAlarm(AlarmManager alarmManager, long triggerTime, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Kiểm tra quyền nếu muốn dùng báo thức chính xác, nếu không có thì dùng inexact
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
}
