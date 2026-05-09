package com.example.heami.notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.heami.HeamiApp;
import com.example.heami.R;
import com.example.heami.ui.main.HomeActivity;
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
        Map<String, String> data = remoteMessage.getData();
        String type = safeText(data.get("type"), "");

        if (!"COMMUNITY_CHAT".equals(type)) {
            return;
        }

        String roomId = safeText(data.get("room_id"), "");
        String matchId = safeText(data.get("match_id"), "");
        String senderId = safeText(data.get("sender_id"), "");
        String senderName = safeText(data.get("sender_name"), "Người bạn ẩn danh");
        String previewText = safeText(data.get("preview_text"), "Bạn có tin nhắn mới");
        String moodTag = safeText(data.get("mood_tag"), "stress");

        if (HeamiApp.isAppForegroundStatic()) {
            Intent bannerIntent = new Intent(ACTION_CHAT_PUSH_BANNER);
            bannerIntent.setPackage(getPackageName());
            bannerIntent.putExtra("type", "COMMUNITY_CHAT");
            bannerIntent.putExtra("room_id", roomId);
            bannerIntent.putExtra("match_id", matchId);
            bannerIntent.putExtra("matched_user_id", senderId);
            bannerIntent.putExtra("matched_user_name", senderName);
            bannerIntent.putExtra("preview_text", previewText);
            bannerIntent.putExtra("mood_tag", moodTag);
            sendBroadcast(bannerIntent);
            return;
        }

        Intent openIntent = new Intent(this, HomeActivity.class);
        openIntent.putExtra("type", "COMMUNITY_CHAT");
        openIntent.putExtra("room_id", roomId);
        openIntent.putExtra("match_id", matchId);
        openIntent.putExtra("matched_user_id", senderId);
        openIntent.putExtra("matched_user_name", senderName);
        openIntent.putExtra("preview_text", previewText);
        openIntent.putExtra("mood_tag", moodTag);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                roomId.isEmpty() ? 0 : roomId.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "heami_chat_messages")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(senderName)
                .setContentText(previewText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(previewText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setColor(0xFFE8507A)
                .setContentIntent(pendingIntent);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this)
                    .notify(roomId.isEmpty() ? (int) System.currentTimeMillis() : roomId.hashCode(), builder.build());
        }
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}