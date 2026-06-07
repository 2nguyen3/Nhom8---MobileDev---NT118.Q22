package com.example.heami.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.heami.data.models.DoctorHomeSummaryModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DoctorHomeRepository {

    public interface DoctorHomeCallback {
        void onChanged(@NonNull DoctorHomeSummaryModel summary);

        void onError(@NonNull String message);
    }

    private final FirebaseFirestore firestore;

    public DoctorHomeRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    @Nullable
    public ListenerRegistration observeDoctorHomeSummary(
            @NonNull String doctorId,
            @NonNull DoctorHomeCallback callback
    ) {
        String safeDoctorId = safeText(doctorId);

        if (safeDoctorId.isEmpty()) {
            callback.onError("Không xác định được doctor_id");
            return null;
        }

        return firestore.collection("consultations")
                .whereEqualTo("doctor_id", safeDoctorId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage() != null
                                ? error.getMessage()
                                : "Không thể tải dữ liệu Doctor Home");
                        return;
                    }

                    DoctorHomeSummaryModel summary = buildBaseSummary(snapshot);

                    loadUnreadMessages(safeDoctorId, summary, () -> {
                        loadUpcomingPatientNames(summary, () -> {
                            loadAttentionPatients(snapshot, summary, () -> {
                                callback.onChanged(summary);
                            });
                        });
                    });
                });
    }

    @NonNull
    private DoctorHomeSummaryModel buildBaseSummary(@Nullable QuerySnapshot snapshot) {
        DoctorHomeSummaryModel summary = new DoctorHomeSummaryModel();

        if (snapshot == null || snapshot.isEmpty()) {
            return summary;
        }

        long startToday = getStartOfTodayMs();
        long startTomorrow = getStartOfTomorrowMs();
        long now = System.currentTimeMillis();

        List<DoctorHomeSummaryModel.UpcomingSessionItem> upcoming = new ArrayList<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String status = safeText(doc.getString("status")).toUpperCase(Locale.ROOT);
            Timestamp startTimestamp = doc.getTimestamp("start_time");

            long startMs = startTimestamp != null
                    ? startTimestamp.toDate().getTime()
                    : 0L;

            boolean isActiveStatus = "BOOKED".equals(status) || "ONGOING".equals(status);

            if (startMs >= startToday && startMs < startTomorrow && isActiveStatus) {
                summary.setTodaySessions(summary.getTodaySessions() + 1);
            }

            if ("BOOKED".equals(status)) {
                summary.setPendingSessions(summary.getPendingSessions() + 1);
            }

            if ("ONGOING".equals(status)) {
                summary.setOngoingSessions(summary.getOngoingSessions() + 1);
            }

            if (isActiveStatus && startMs >= now) {
                upcoming.add(buildUpcomingItem(doc, status));
            }
        }

        Collections.sort(upcoming, new Comparator<DoctorHomeSummaryModel.UpcomingSessionItem>() {
            @Override
            public int compare(
                    DoctorHomeSummaryModel.UpcomingSessionItem left,
                    DoctorHomeSummaryModel.UpcomingSessionItem right
            ) {
                return Long.compare(left.getStartTimeMs(), right.getStartTimeMs());
            }
        });

        if (upcoming.size() > 3) {
            upcoming = new ArrayList<>(upcoming.subList(0, 3));
        }

        summary.setUpcomingSessions(upcoming);

        return summary;
    }

    @NonNull
    private DoctorHomeSummaryModel.UpcomingSessionItem buildUpcomingItem(
            @NonNull DocumentSnapshot doc,
            @NonNull String status
    ) {
        DoctorHomeSummaryModel.UpcomingSessionItem item =
                new DoctorHomeSummaryModel.UpcomingSessionItem();

        String sessionId = safeText(doc.getString("session_id"));
        if (sessionId.isEmpty()) {
            sessionId = doc.getId();
        }

        String userId = safeText(doc.getString("user_id"));
        String formatType = safeText(doc.getString("format_type"));
        String packageType = safeText(doc.getString("package_type"));

        Timestamp startTimestamp = doc.getTimestamp("start_time");

        long startTimeMs = startTimestamp != null
                ? startTimestamp.toDate().getTime()
                : 0L;

        String timeText = startTimestamp != null
                ? formatUpcomingTimeText(startTimestamp)
                : "--:--";

        item.setSessionId(sessionId);
        item.setUserId(userId);
        item.setPatientName("Bệnh nhân Heami");
        item.setAvatarEmoji("😊");
        item.setStatus(status);
        item.setFormatType(formatType);
        item.setPackageType(packageType);
        item.setTimeText(timeText);
        item.setStartTimeMs(startTimeMs);
        item.setFormatText(buildFormatText(formatType, packageType));

        return item;
    }

    private void loadUnreadMessages(
            @NonNull String doctorId,
            @NonNull DoctorHomeSummaryModel summary,
            @NonNull Runnable done
    ) {
        firestore.collection("chat_rooms")
                .whereArrayContains("member_ids", doctorId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int totalUnread = 0;

                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Object rawMap = doc.get("unread_count_map");

                            if (rawMap instanceof Map) {
                                Map<?, ?> unreadMap = (Map<?, ?>) rawMap;
                                Object rawValue = unreadMap.get(doctorId);

                                if (rawValue instanceof Number) {
                                    totalUnread += ((Number) rawValue).intValue();
                                }
                            }
                        }
                    }

                    summary.setUnreadMessages(totalUnread);
                    done.run();
                })
                .addOnFailureListener(e -> done.run());
    }

    private void loadUpcomingPatientNames(
            @NonNull DoctorHomeSummaryModel summary,
            @NonNull Runnable done
    ) {
        List<DoctorHomeSummaryModel.UpcomingSessionItem> items = summary.getUpcomingSessions();

        if (items == null || items.isEmpty()) {
            done.run();
            return;
        }

        Set<String> userIds = new HashSet<>();
        for (DoctorHomeSummaryModel.UpcomingSessionItem item : items) {
            if (item.getUserId() != null && !item.getUserId().trim().isEmpty()) {
                userIds.add(item.getUserId());
            }
        }

        if (userIds.isEmpty()) {
            done.run();
            return;
        }

        final int[] pending = {userIds.size()};

        for (String userId : userIds) {
            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String nickname = extractUserDisplayName(userDoc);
                        String avatarEmoji = resolveAvatarEmoji(nickname);

                        for (DoctorHomeSummaryModel.UpcomingSessionItem item : items) {
                            if (userId.equals(item.getUserId())) {
                                item.setPatientName(nickname);
                                item.setAvatarEmoji(avatarEmoji);
                            }
                        }

                        pending[0]--;
                        if (pending[0] <= 0) {
                            done.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        pending[0]--;
                        if (pending[0] <= 0) {
                            done.run();
                        }
                    });
        }
    }

    private void loadAttentionPatients(
            @Nullable QuerySnapshot consultationSnapshot,
            @NonNull DoctorHomeSummaryModel summary,
            @NonNull Runnable done
    ) {
        Set<String> patientIds = extractDoctorPatientIds(consultationSnapshot);

        if (patientIds.isEmpty()) {
            summary.setAttentionPatients(new ArrayList<>());
            done.run();
            return;
        }

        List<DoctorHomeSummaryModel.AttentionPatientItem> attentionItems = new ArrayList<>();

        final int[] pending = {patientIds.size()};

        for (String userId : patientIds) {
            loadSingleAttentionPatient(userId, attentionItems, () -> {
                pending[0]--;

                if (pending[0] <= 0) {
                    Collections.sort(attentionItems, new Comparator<DoctorHomeSummaryModel.AttentionPatientItem>() {
                        @Override
                        public int compare(
                                DoctorHomeSummaryModel.AttentionPatientItem left,
                                DoctorHomeSummaryModel.AttentionPatientItem right
                        ) {
                            return Integer.compare(right.getPriority(), left.getPriority());
                        }
                    });

                    if (attentionItems.size() > 3) {
                        summary.setAttentionPatients(new ArrayList<>(attentionItems.subList(0, 3)));
                    } else {
                        summary.setAttentionPatients(attentionItems);
                    }

                    done.run();
                }
            });
        }
    }

    private void loadSingleAttentionPatient(
            @NonNull String userId,
            @NonNull List<DoctorHomeSummaryModel.AttentionPatientItem> attentionItems,
            @NonNull Runnable done
    ) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String patientName = extractUserDisplayName(userDoc);
                    String avatarEmoji = resolveAvatarEmoji(patientName);

                    firestore.collection("users")
                            .document(userId)
                            .collection("mood_history")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(moodSnapshot -> {
                                if (moodSnapshot != null && !moodSnapshot.isEmpty()) {
                                    DocumentSnapshot moodDoc = moodSnapshot.getDocuments().get(0);

                                    DoctorHomeSummaryModel.AttentionPatientItem item =
                                            buildAttentionItem(userId, patientName, avatarEmoji, moodDoc);

                                    if (item != null) {
                                        attentionItems.add(item);
                                    }
                                }

                                done.run();
                            })
                            .addOnFailureListener(e -> done.run());
                })
                .addOnFailureListener(e -> done.run());
    }

    @Nullable
    private DoctorHomeSummaryModel.AttentionPatientItem buildAttentionItem(
            @NonNull String userId,
            @NonNull String patientName,
            @NonNull String avatarEmoji,
            @NonNull DocumentSnapshot moodDoc
    ) {
        String moodTag = safeText(moodDoc.getString("mood_tag"));
        String moodEmoji = safeText(moodDoc.getString("mood_emoji"));
        String moodDesc = safeText(moodDoc.getString("mood_desc"));
        String rawEmotion = safeText(moodDoc.getString("raw_emotion_label"));

        Long moodPercentLong = moodDoc.getLong("mood_percent");
        Long energyLevelLong = moodDoc.getLong("energy_level");

        int moodPercent = moodPercentLong != null ? moodPercentLong.intValue() : -1;
        int energyLevel = energyLevelLong != null ? energyLevelLong.intValue() : -1;

        boolean negativeMood = isNegativeMood(moodTag, moodDesc, rawEmotion);
        boolean lowMoodPercent = moodPercent >= 0 && moodPercent <= 45;
        boolean lowEnergy = energyLevel >= 0 && energyLevel <= 40;

        if (!negativeMood && !lowMoodPercent && !lowEnergy) {
            return null;
        }

        DoctorHomeSummaryModel.AttentionPatientItem item =
                new DoctorHomeSummaryModel.AttentionPatientItem();

        item.setUserId(userId);
        item.setPatientName(patientName);
        item.setAvatarEmoji(avatarEmoji);
        item.setMoodTag(moodTag.isEmpty() ? "Cần theo dõi" : moodTag);
        item.setMoodEmoji(moodEmoji.isEmpty() ? "💭" : moodEmoji);
        item.setMoodPercent(moodPercent);
        item.setEnergyLevel(energyLevel);
        item.setPriority(resolveAttentionPriority(negativeMood, moodPercent, energyLevel));
        item.setReasonText(buildAttentionReason(
                item.getMoodEmoji(),
                item.getMoodTag(),
                moodPercent,
                energyLevel,
                negativeMood,
                lowMoodPercent,
                lowEnergy
        ));

        return item;
    }

    @NonNull
    private Set<String> extractDoctorPatientIds(@Nullable QuerySnapshot snapshot) {
        Set<String> userIds = new HashSet<>();

        if (snapshot == null || snapshot.isEmpty()) {
            return userIds;
        }

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String status = safeText(doc.getString("status")).toUpperCase(Locale.ROOT);
            String userId = safeText(doc.getString("user_id"));

            if (userId.isEmpty()) {
                continue;
            }

            if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
                continue;
            }

            userIds.add(userId);
        }

        return userIds;
    }

    private boolean isNegativeMood(
            @NonNull String moodTag,
            @NonNull String moodDesc,
            @NonNull String rawEmotion
    ) {
        String text = (moodTag + " " + moodDesc + " " + rawEmotion).toLowerCase(Locale.ROOT);

        return text.contains("buồn")
                || text.contains("sad")
                || text.contains("căng")
                || text.contains("stress")
                || text.contains("lo")
                || text.contains("sợ")
                || text.contains("fear")
                || text.contains("tức")
                || text.contains("giận")
                || text.contains("angry")
                || text.contains("khó chịu")
                || text.contains("disgust");
    }

    private int resolveAttentionPriority(
            boolean negativeMood,
            int moodPercent,
            int energyLevel
    ) {
        if (moodPercent >= 0 && moodPercent <= 35) {
            return 3;
        }

        if (energyLevel >= 0 && energyLevel <= 30) {
            return 3;
        }

        if (negativeMood) {
            return 3;
        }

        return 2;
    }

    @NonNull
    private String buildAttentionReason(
            @NonNull String moodEmoji,
            @NonNull String moodTag,
            int moodPercent,
            int energyLevel,
            boolean negativeMood,
            boolean lowMoodPercent,
            boolean lowEnergy
    ) {
        if (lowMoodPercent && moodPercent >= 0) {
            return "Tâm trạng thấp: " + moodEmoji + " " + moodTag + " • " + moodPercent + "%";
        }

        if (lowEnergy && energyLevel >= 0) {
            return "Năng lượng thấp: " + energyLevel + "%";
        }

        if (negativeMood) {
            return "Mood cần theo dõi: " + moodEmoji + " " + moodTag;
        }

        return "Cần theo dõi trạng thái gần đây";
    }

    @NonNull
    private String formatUpcomingTimeText(@NonNull Timestamp timestamp) {
        long timeMs = timestamp.toDate().getTime();

        long startToday = getStartOfTodayMs();
        long startTomorrow = getStartOfTomorrowMs();
        long startAfterTomorrow = startTomorrow + (24L * 60L * 60L * 1000L);

        String hourText = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(timestamp.toDate());

        if (timeMs >= startToday && timeMs < startTomorrow) {
            return "Hôm nay " + hourText;
        }

        if (timeMs >= startTomorrow && timeMs < startAfterTomorrow) {
            return "Mai " + hourText;
        }

        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(timestamp.toDate());
    }

    @NonNull
    private String buildFormatText(
            @NonNull String formatType,
            @NonNull String packageType
    ) {
        String format = formatType.toLowerCase(Locale.ROOT);

        String typeText;
        if (format.contains("call") || format.contains("video") || format.contains("gọi")) {
            typeText = "Call";
        } else {
            typeText = "Chat";
        }

        if (!packageType.isEmpty()) {
            return typeText + " • " + packageType;
        }

        return typeText;
    }

    @NonNull
    private String extractUserDisplayName(@Nullable DocumentSnapshot userDoc) {
        if (userDoc == null || !userDoc.exists()) {
            return "Bệnh nhân Heami";
        }

        String nickname = safeText(userDoc.getString("nickname"));
        if (!nickname.isEmpty()) {
            return nickname;
        }

        String displayName = safeText(userDoc.getString("display_name"));
        if (!displayName.isEmpty()) {
            return displayName;
        }

        String fullName = safeText(userDoc.getString("full_name"));
        if (!fullName.isEmpty()) {
            return fullName;
        }

        return "Bệnh nhân Heami";
    }

    @NonNull
    private String resolveAvatarEmoji(@NonNull String nickname) {
        if (nickname.contains("Bướm")) return "🦋";
        if (nickname.contains("Cáo")) return "🦊";
        if (nickname.contains("Gấu")) return "🐻";
        if (nickname.contains("Ếch")) return "🐸";
        if (nickname.contains("Mèo")) return "🐱";
        if (nickname.contains("Thỏ")) return "🐰";
        return "😊";
    }

    private long getStartOfTodayMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getStartOfTomorrowMs() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @NonNull
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}