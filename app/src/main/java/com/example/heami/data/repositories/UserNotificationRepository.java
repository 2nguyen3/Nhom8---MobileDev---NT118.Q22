package com.example.heami.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.heami.data.models.HomeNotificationModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UserNotificationRepository {

    public interface LoadNotificationsListener {
        void onSuccess(@NonNull List<HomeNotificationModel> notifications);
        void onFailure(@NonNull String errorMessage);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void loadHomeNotifications(
            @NonNull Context context,
            @NonNull LoadNotificationsListener listener
    ) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        Context appContext = context.getApplicationContext();
        String uid = user.getUid();

        loadNotificationSettings(uid, settings -> {
            List<HomeNotificationModel> result = new ArrayList<>();
            AtomicInteger pending = new AtomicInteger(4);

            Runnable finishOne = () -> {
                if (pending.decrementAndGet() == 0) {
                    Collections.sort(result, (a, b) ->
                            Long.compare(b.getCreatedAtMs(), a.getCreatedAtMs())
                    );
                    listener.onSuccess(result);
                }
            };

            if (settings.notifCheckin) {
                loadDailyCheckInNotification(uid, result, finishOne);
            } else {
                finishOne.run();
            }

            if (settings.notifPlan) {
                loadPersonalPlanNotification(appContext, result, finishOne);
            } else {
                finishOne.run();
            }

            if (settings.notifAppoint) {
                loadConsultationNotifications(uid, result, finishOne);
            } else {
                finishOne.run();
            }

            if (settings.notifChat) {
                loadUnreadChatNotifications(uid, result, finishOne);
            } else {
                finishOne.run();
            }
        });
    }

    private void loadNotificationSettings(
            @NonNull String uid,
            @NonNull SettingsLoadedCallback callback
    ) {
        firestore.collection("users")
                .document(uid)
                .collection("settings")
                .document("default")
                .get()
                .addOnSuccessListener(doc -> {
                    NotificationSettings settings = new NotificationSettings();

                    if (doc != null && doc.exists()) {
                        settings.notifCheckin = getBoolean(doc, "notif_checkin", true);
                        settings.notifPlan = getBoolean(doc, "notif_plan", true);
                        settings.notifAppoint = getBoolean(doc, "notif_appoint", true);
                        settings.notifChat = getBoolean(doc, "notif_chat", true);
                    }

                    callback.onLoaded(settings);
                })
                .addOnFailureListener(e -> callback.onLoaded(new NotificationSettings()));
    }

    private void loadDailyCheckInNotification(
            @NonNull String uid,
            @NonNull List<HomeNotificationModel> result,
            @NonNull Runnable done
    ) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 1);

        firestore.collection("users")
                .document(uid)
                .collection("mood_history")
                .whereGreaterThanOrEqualTo("timestamp", new Timestamp(start.getTime()))
                .whereLessThan("timestamp", new Timestamp(end.getTime()))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean checkedInToday = snapshot != null && !snapshot.isEmpty();

                    if (!checkedInToday) {
                        result.add(new HomeNotificationModel(
                                "checkin_today",
                                HomeNotificationModel.TYPE_CHECKIN,
                                "Check-in hằng ngày",
                                "Heami-er ơi, dành một chút thời gian để ghi nhận cảm xúc hôm nay nhé.",
                                "09:00 mỗi sáng",
                                "Check-in ngay",
                                "🔔",
                                System.currentTimeMillis(),
                                true
                        ));
                    }

                    done.run();
                })
                .addOnFailureListener(e -> done.run());
    }

    private void loadPersonalPlanNotification(
            @NonNull Context context,
            @NonNull List<HomeNotificationModel> result,
            @NonNull Runnable done
    ) {
        SharedPreferences prefs = context.getSharedPreferences("HeamiDailyTasks", Context.MODE_PRIVATE);

        boolean waterDone = prefs.getBoolean("water_done", false);
        boolean breathDone = prefs.getBoolean("breath_done", false);
        boolean relaxDone = prefs.getBoolean("relax_done", false);

        int remaining = 0;
        List<String> missing = new ArrayList<>();

        if (!waterDone) {
            remaining++;
            missing.add("uống nước");
        }

        if (!breathDone) {
            remaining++;
            missing.add("hít thở");
        }

        if (!relaxDone) {
            remaining++;
            missing.add("thư giãn");
        }

        if (remaining > 0) {
            String message = "Bạn còn " + remaining + " hoạt động hôm nay";
            if (!missing.isEmpty()) {
                message += ": " + joinShort(missing) + ".";
            }

            result.add(new HomeNotificationModel(
                    "plan_today",
                    HomeNotificationModel.TYPE_PLAN,
                    "Lịch trình cá nhân",
                    message,
                    "Hôm nay",
                    "Xem lịch trình",
                    "✨",
                    System.currentTimeMillis() - 1_000L,
                    true
            ));
        }

        done.run();
    }

    private void loadConsultationNotifications(
            @NonNull String uid,
            @NonNull List<HomeNotificationModel> result,
            @NonNull Runnable done
    ) {
        firestore.collection("consultations")
                .whereEqualTo("user_id", uid)
                .limit(30)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            addConsultationNotificationIfNeeded(doc, result);
                        }
                    }
                    done.run();
                })
                .addOnFailureListener(e -> done.run());
    }

    private void addConsultationNotificationIfNeeded(
            @NonNull DocumentSnapshot doc,
            @NonNull List<HomeNotificationModel> result
    ) {
        String status = safeText(getStringAny(doc, "status"), "BOOKED")
                .toUpperCase(Locale.ROOT);

        if (!"BOOKED".equals(status) && !"ONGOING".equals(status)) {
            return;
        }

        Timestamp startTime = getTimestampAny(doc, "start_time", "startTime");
        long now = System.currentTimeMillis();
        long createdAt = startTime != null ? startTime.toDate().getTime() : now;

        if ("BOOKED".equals(status) && startTime != null) {
            long diff = createdAt - now;
            long twentyFourHours = 24L * 60L * 60L * 1000L;

            if (diff > twentyFourHours || diff < -30L * 60L * 1000L) {
                return;
            }
        }

        String sessionId = safeText(getStringAny(doc, "session_id", "sessionId"), doc.getId());
        String doctorName = safeText(
                getStringAny(doc, "doctor_name", "doctorName"),
                "Bác sĩ Heami"
        );
        String doctorAvatar = safeText(getStringAny(doc, "doctor_avatar", "doctorAvatar"), "");
        String formatType = safeText(getStringAny(doc, "format_type", "formatType"), "Chat");

        String title = "ONGOING".equals(status)
                ? "Phiên tư vấn đang diễn ra"
                : "Lịch tư vấn sắp tới";

        String message = "Bạn có lịch tư vấn cùng " + doctorName;
        if (startTime != null) {
            message += " lúc " + formatFriendlyTime(startTime.toDate().getTime());
        }

        HomeNotificationModel item = new HomeNotificationModel(
                "consultation_" + sessionId,
                HomeNotificationModel.TYPE_APPOINTMENT,
                title,
                message,
                startTime != null ? formatFriendlyTime(startTime.toDate().getTime()) : "Sắp tới",
                "Xem lịch",
                "💗",
                createdAt,
                true
        );

        item.setSessionId(sessionId);
        item.setPartnerName(doctorName);
        item.setPartnerAvatar(doctorAvatar);
        item.setFormatType(formatType);
        item.setStatus(status);

        result.add(item);
    }

    private void loadUnreadChatNotifications(
            @NonNull String uid,
            @NonNull List<HomeNotificationModel> result,
            @NonNull Runnable done
    ) {
        firestore.collection("chat_rooms")
                .whereArrayContains("member_ids", uid)
                .limit(40)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            addChatNotificationIfUnread(uid, doc, result);
                        }
                    }
                    done.run();
                })
                .addOnFailureListener(e -> done.run());
    }

    @SuppressWarnings("unchecked")
    private void addChatNotificationIfUnread(
            @NonNull String uid,
            @NonNull DocumentSnapshot doc,
            @NonNull List<HomeNotificationModel> result
    ) {
        String status = safeText(getStringAny(doc, "status"), "ACTIVE")
                .toUpperCase(Locale.ROOT);

        if (!"ACTIVE".equals(status)) {
            return;
        }

        Object unreadObj = doc.get("unread_count_map");
        if (!(unreadObj instanceof Map)) {
            return;
        }

        Map<String, Object> unreadMap = (Map<String, Object>) unreadObj;
        long unreadCount = getLongFromMap(unreadMap, uid);
        if (unreadCount <= 0) {
            return;
        }

        String roomId = doc.getId();
        String roomType = safeText(getStringAny(doc, "type"), "").toUpperCase(Locale.ROOT);
        String relatedId = safeText(getStringAny(doc, "related_id"), "");
        String lastMessage = safeText(getStringAny(doc, "last_message"), "Bạn có tin nhắn mới");

        Timestamp lastMessageAt = getTimestampAny(doc, "last_message_at", "lastMessageAt");
        long createdAt = lastMessageAt != null
                ? lastMessageAt.toDate().getTime()
                : System.currentTimeMillis();

        String partnerId = resolvePartnerField(doc, uid, "member_ids", "");
        String partnerName = resolvePartnerField(doc, uid, "member_names", "Người bạn Heami");
        String partnerAvatar = resolvePartnerField(doc, uid, "member_avatars", "");

        if ("CONSULTATION".equals(roomType)) {
            HomeNotificationModel item = new HomeNotificationModel(
                    "consultation_chat_" + roomId,
                    HomeNotificationModel.TYPE_CONSULTATION_CHAT,
                    "Tin nhắn tư vấn",
                    partnerName + ": " + lastMessage,
                    lastMessageAt != null ? formatFriendlyTime(createdAt) : "Vừa xong",
                    "Mở chat",
                    "💬",
                    createdAt,
                    true
            );

            item.setRoomId(roomId);
            item.setSessionId(relatedId);
            item.setPartnerId(partnerId);
            item.setPartnerName(partnerName);
            item.setPartnerAvatar(partnerAvatar);
            item.setFormatType("Chat");
            item.setStatus("ONGOING");

            result.add(item);
            return;
        }

        if ("MOOD_MATCH".equals(roomType) || "COMMUNITY_CHAT".equals(roomType)) {
            String matchId = relatedId;
            if (matchId.isEmpty() && roomId.startsWith("mm_")) {
                matchId = roomId.substring(3);
            }

            HomeNotificationModel item = new HomeNotificationModel(
                    "community_chat_" + roomId,
                    HomeNotificationModel.TYPE_COMMUNITY_CHAT,
                    "Tin nhắn cộng đồng",
                    partnerName + ": " + lastMessage,
                    lastMessageAt != null ? formatFriendlyTime(createdAt) : "Vừa xong",
                    "Mở chat",
                    "🌸",
                    createdAt,
                    true
            );

            item.setRoomId(roomId);
            item.setMatchId(matchId);
            item.setPartnerId(partnerId);
            item.setPartnerName(partnerName);
            item.setPartnerAvatar(partnerAvatar);
            item.setMoodTag(safeText(getStringAny(doc, "match_mood_tag"), "stress"));
            item.setStatus(status);

            result.add(item);
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private String resolvePartnerField(
            @NonNull DocumentSnapshot doc,
            @NonNull String uid,
            @NonNull String fieldName,
            @NonNull String fallback
    ) {
        Object idsObj = doc.get("member_ids");
        Object valuesObj = doc.get(fieldName);

        if (!(idsObj instanceof List) || !(valuesObj instanceof List)) {
            return fallback;
        }

        List<Object> ids = (List<Object>) idsObj;
        List<Object> values = (List<Object>) valuesObj;

        int partnerIndex = -1;
        for (int i = 0; i < ids.size(); i++) {
            Object idObj = ids.get(i);
            if (idObj != null && !uid.equals(String.valueOf(idObj))) {
                partnerIndex = i;
                break;
            }
        }

        if (partnerIndex >= 0 && partnerIndex < values.size()) {
            Object value = values.get(partnerIndex);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }

        return fallback;
    }

    private boolean getBoolean(@NonNull DocumentSnapshot doc, @NonNull String key, boolean fallback) {
        Boolean value = doc.getBoolean(key);
        return value != null ? value : fallback;
    }

    @Nullable
    private String getStringAny(@NonNull DocumentSnapshot doc, @NonNull String... keys) {
        for (String key : keys) {
            String value = doc.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    @Nullable
    private Timestamp getTimestampAny(@NonNull DocumentSnapshot doc, @NonNull String... keys) {
        for (String key : keys) {
            Timestamp value = doc.getTimestamp(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private long getLongFromMap(@NonNull Map<String, Object> map, @NonNull String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (Exception ignored) {
            }
        }

        return 0L;
    }

    @NonNull
    private String formatFriendlyTime(long millis) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);

        Calendar now = Calendar.getInstance();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

        if (isSameDay(target, now)) {
            return "Hôm nay " + timeFormat.format(new Date(millis));
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        if (isSameDay(target, tomorrow)) {
            return "Ngày mai " + timeFormat.format(new Date(millis));
        }

        return dateTimeFormat.format(new Date(millis));
    }

    private boolean isSameDay(@NonNull Calendar a, @NonNull Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    @NonNull
    private String joinShort(@NonNull List<String> values) {
        if (values.isEmpty()) return "";
        if (values.size() == 1) return values.get(0);
        if (values.size() == 2) return values.get(0) + ", " + values.get(1);
        return values.get(0) + ", " + values.get(1) + "...";
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private interface SettingsLoadedCallback {
        void onLoaded(@NonNull NotificationSettings settings);
    }

    private static class NotificationSettings {
        boolean notifCheckin = true;
        boolean notifPlan = true;
        boolean notifAppoint = true;
        boolean notifChat = true;
    }
}