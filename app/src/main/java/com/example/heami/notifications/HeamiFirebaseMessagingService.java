package com.example.heami.notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.heami.HeamiApp;
import com.example.heami.R;
import com.example.heami.ui.community.MoodMatchChatActivity;
import com.example.heami.ui.consultation.ConsultationSessionActivity;
import com.example.heami.ui.doctor.DoctorChatDetailActivity;
import com.example.heami.ui.doctor.DoctorMessagesActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class HeamiFirebaseMessagingService extends FirebaseMessagingService {

    public static final String ACTION_CHAT_PUSH_BANNER = "com.example.heami.ACTION_CHAT_PUSH_BANNER";

    private static final String TYPE_COMMUNITY_CHAT = "COMMUNITY_CHAT";
    private static final String TYPE_CONSULTATION_CHAT = "CONSULTATION_CHAT";

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("account_id", user.getUid());
        updates.put("fcm_token", token);

        FirebaseFirestore.getInstance()
                .collection("accounts")
                .document(user.getUid())
                .set(updates, SetOptions.merge());
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        android.util.Log.d("HeamiPush", "onMessageReceived data=" + remoteMessage.getData());

        Map<String, String> data = remoteMessage.getData();
        String type = safeText(data.get("type"), "");

        if (!TYPE_COMMUNITY_CHAT.equals(type) && !TYPE_CONSULTATION_CHAT.equals(type)) {
            return;
        }

        SharedPreferences notiPrefs = getSharedPreferences("HeamiSettings", MODE_PRIVATE);
        boolean chatNotificationEnabled = notiPrefs.getBoolean("notif_chat", true);
        if (!chatNotificationEnabled) {
            return;
        }

        String roomId = safeText(data.get("room_id"), "");
        String sessionId = safeText(data.get("session_id"), "");
        String matchId = safeText(data.get("match_id"), "");
        String senderId = safeText(data.get("sender_id"), "");
        String senderName = safeText(data.get("sender_name"), "Người dùng Heami");
        String senderAvatar = safeText(data.get("sender_avatar"), "");
        String previewText = safeText(data.get("preview_text"), "Bạn có tin nhắn mới");
        String moodTag = safeText(data.get("mood_tag"), "stress");
        String formatType = safeText(data.get("format_type"), "Chat");
        String roomStatus = safeText(data.get("room_status"), "ACTIVE");
        String consultationStatus = safeText(data.get("consultation_status"), "ONGOING");

        if (HeamiApp.isAppForegroundStatic()) {
            Intent bannerIntent = new Intent(ACTION_CHAT_PUSH_BANNER);
            bannerIntent.setPackage(getPackageName());
            bannerIntent.putExtra("type", type);
            bannerIntent.putExtra("room_id", roomId);
            bannerIntent.putExtra("session_id", sessionId);
            bannerIntent.putExtra("match_id", matchId);
            bannerIntent.putExtra("sender_id", senderId);
            bannerIntent.putExtra("sender_name", senderName);
            bannerIntent.putExtra("sender_avatar", senderAvatar);
            bannerIntent.putExtra("preview_text", previewText);
            bannerIntent.putExtra("mood_tag", moodTag);
            bannerIntent.putExtra("format_type", formatType);
            bannerIntent.putExtra("room_status", roomStatus);
            bannerIntent.putExtra("consultation_status", consultationStatus);
            sendBroadcast(bannerIntent);
            return;
        }

        Intent openIntent = buildOpenIntent(
                type,
                roomId,
                sessionId,
                matchId,
                senderId,
                senderName,
                senderAvatar,
                previewText,
                moodTag,
                formatType,
                roomStatus,
                consultationStatus
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                buildNotificationRequestCode(type, roomId, sessionId),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "heami_chat_messages")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(senderName)
                .setContentText(previewText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(previewText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(0xFFE8507A)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this)
                    .notify(
                            buildNotificationId(type, roomId, sessionId),
                            builder.build()
                    );
        }
    }

    @NonNull
    private Intent buildOpenIntent(
            @NonNull String type,
            @NonNull String roomId,
            @NonNull String sessionId,
            @NonNull String matchId,
            @NonNull String senderId,
            @NonNull String senderName,
            @NonNull String senderAvatar,
            @NonNull String previewText,
            @NonNull String moodTag,
            @NonNull String formatType,
            @NonNull String roomStatus,
            @NonNull String consultationStatus
    ) {
        if (TYPE_COMMUNITY_CHAT.equals(type)) {
            Intent intent = new Intent(this, MoodMatchChatActivity.class);
            intent.putExtra("room_id", roomId);
            intent.putExtra("match_id", matchId);
            intent.putExtra("matched_user_id", senderId);
            intent.putExtra("matched_user_name", senderName);
            intent.putExtra("matched_user_avatar", senderAvatar);
            intent.putExtra("preview_text", previewText);
            intent.putExtra("mood_tag", moodTag);
            intent.putExtra("room_status", roomStatus);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
        }

        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);

        if (isDoctor) {
            Intent intent = new Intent(this, DoctorChatDetailActivity.class);
            intent.putExtra(DoctorMessagesActivity.EXTRA_SESSION_ID, sessionId);
            intent.putExtra(DoctorMessagesActivity.EXTRA_ROOM_ID, roomId);
            intent.putExtra(DoctorMessagesActivity.EXTRA_PARTNER_NAME, senderName);
            intent.putExtra(DoctorMessagesActivity.EXTRA_PARTNER_AVATAR, senderAvatar);
            intent.putExtra(DoctorMessagesActivity.EXTRA_ROOM_STATUS, roomStatus);
            intent.putExtra(DoctorMessagesActivity.EXTRA_FORMAT_TYPE, formatType);
            intent.putExtra(DoctorMessagesActivity.EXTRA_USER_ID, senderId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
        }

        Intent intent = new Intent(this, ConsultationSessionActivity.class);
        intent.putExtra(ConsultationSessionActivity.EXTRA_SESSION_ID, sessionId);
        intent.putExtra(ConsultationSessionActivity.EXTRA_FORMAT_TYPE, formatType);
        intent.putExtra(ConsultationSessionActivity.EXTRA_DOCTOR_NAME, senderName);
        intent.putExtra(ConsultationSessionActivity.EXTRA_DOCTOR_AVATAR, senderAvatar);
        intent.putExtra(ConsultationSessionActivity.EXTRA_STATUS, consultationStatus);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private int buildNotificationRequestCode(
            @NonNull String type,
            @NonNull String roomId,
            @NonNull String sessionId
    ) {
        String raw = type + "|" + roomId + "|" + sessionId;
        return raw.hashCode();
    }

    private int buildNotificationId(
            @NonNull String type,
            @NonNull String roomId,
            @NonNull String sessionId
    ) {
        String raw = "notif|" + type + "|" + roomId + "|" + sessionId;
        return raw.hashCode();
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}