package com.example.heami.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.heami.data.models.AdminAnalyticsOverview;
import com.example.heami.data.models.AdminTopPostItem;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminAnalyticsRepository {

    public interface LoadAnalyticsListener {
        void onSuccess(
                @NonNull AdminAnalyticsOverview overview,
                @NonNull List<AdminTopPostItem> topLikedPosts,
                @NonNull List<AdminTopPostItem> topReportedPosts,
                @NonNull List<AdminTopPostItem> topCommentedPosts
        );

        void onFailure(@NonNull String errorMessage);
    }

    private final FirebaseFirestore firestore;

    public AdminAnalyticsRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void loadAnalytics(@NonNull LoadAnalyticsListener listener) {
        Tasks.whenAllSuccess(
                        firestore.collection("accounts").get(),
                        firestore.collection("community_posts").get(),
                        firestore.collection("consultations").get()
                )
                .addOnSuccessListener(results -> {
                    QuerySnapshot accountsSnapshot = (QuerySnapshot) results.get(0);
                    QuerySnapshot postsSnapshot = (QuerySnapshot) results.get(1);
                    QuerySnapshot consultationsSnapshot = (QuerySnapshot) results.get(2);

                    AdminAnalyticsOverview overview = new AdminAnalyticsOverview();

                    fillAccountStats(overview, accountsSnapshot);
                    fillPostStats(overview, postsSnapshot);
                    fillConsultationStats(overview, consultationsSnapshot);

                    overview.setOnlineNowUsers(0);
                    overview.setOnlineRatePercent(0);

                    fillHealthRates(overview);

                    List<AdminTopPostItem> allPosts = buildTopPostItems(postsSnapshot);
                    List<AdminTopPostItem> topLiked = buildTopLikedPosts(allPosts);
                    List<AdminTopPostItem> topReported = buildTopReportedPosts(allPosts);
                    List<AdminTopPostItem> topCommented = buildTopCommentedPosts(allPosts);

                    listener.onSuccess(overview, topLiked, topReported, topCommented);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải analytics";
                    listener.onFailure(message);
                });
    }

    private void fillAccountStats(
            @NonNull AdminAnalyticsOverview overview,
            @NonNull QuerySnapshot snapshot
    ) {
        int totalAccounts = 0;
        int totalUsers = 0;
        int totalDoctors = 0;
        int totalAdmins = 0;
        int active24hUsers = 0;

        long now = System.currentTimeMillis();
        long activeThreshold = now - (24L * 60L * 60L * 1000L);

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            totalAccounts++;

            String role = safeUpper(doc.getString("role"), "");
            if ("USER".equals(role)) {
                totalUsers++;
            } else if ("DOCTOR".equals(role)) {
                totalDoctors++;
            } else if ("ADMIN".equals(role)) {
                totalAdmins++;
            }

            Timestamp lastSignInAt = doc.getTimestamp("last_sign_in_at");
            if (lastSignInAt != null && lastSignInAt.toDate().getTime() >= activeThreshold) {
                active24hUsers++;
            }
        }

        overview.setTotalAccounts(totalAccounts);
        overview.setTotalUsers(totalUsers);
        overview.setTotalDoctors(totalDoctors);
        overview.setTotalAdmins(totalAdmins);
        overview.setActive24hUsers(active24hUsers);
    }

    private void fillPostStats(
            @NonNull AdminAnalyticsOverview overview,
            @NonNull QuerySnapshot snapshot
    ) {
        int totalPosts = 0;
        int reportedPosts = 0;
        int hiddenPosts = 0;
        int deletedPosts = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            totalPosts++;

            int reportCount = safeInt(doc.getLong("report_count"));
            String status = safeUpper(doc.getString("status"), "ACTIVE");
            String moderationStatus = safeUpper(doc.getString("moderation_status"), "VISIBLE");

            if (reportCount > 0) {
                reportedPosts++;
            }

            if ("DELETED".equals(status)) {
                deletedPosts++;
            } else if ("HIDDEN".equals(moderationStatus)) {
                hiddenPosts++;
            }
        }

        overview.setTotalPosts(totalPosts);
        overview.setReportedPosts(reportedPosts);
        overview.setHiddenPosts(hiddenPosts);
        overview.setDeletedPosts(deletedPosts);
    }

    private void fillConsultationStats(
            @NonNull AdminAnalyticsOverview overview,
            @NonNull QuerySnapshot snapshot
    ) {
        int totalConsultations = 0;
        int booked = 0;
        int ongoing = 0;
        int completed = 0;
        int cancelled = 0;
        int chatCount = 0;
        int callCount = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            totalConsultations++;

            String status = safeUpper(doc.getString("status"), "");
            String packageType = safeUpper(
                    firstNonEmpty(doc,
                            "package_type",
                            "consultation_type",
                            "session_type",
                            "type"
                    ),
                    ""
            );

            if ("BOOKED".equals(status)) {
                booked++;
            } else if ("ONGOING".equals(status)) {
                ongoing++;
            } else if ("COMPLETED".equals(status)) {
                completed++;
            } else if ("CANCELLED".equals(status)) {
                cancelled++;
            }

            if ("CHAT".equals(packageType)) {
                chatCount++;
            } else if ("CALL".equals(packageType)) {
                callCount++;
            }
        }

        overview.setTotalConsultations(totalConsultations);
        overview.setBookedConsultations(booked);
        overview.setOngoingConsultations(ongoing);
        overview.setCompletedConsultations(completed);
        overview.setCancelledConsultations(cancelled);
        overview.setChatConsultations(chatCount);
        overview.setCallConsultations(callCount);
    }

    private void fillHealthRates(@NonNull AdminAnalyticsOverview overview) {
        overview.setActiveRatePercent(
                calculatePercent(
                        overview.getActive24hUsers(),
                        overview.getTotalAccounts()
                )
        );

        overview.setOnlineRatePercent(0);

        overview.setReportRatePercent(
                calculatePercent(
                        overview.getReportedPosts(),
                        overview.getTotalPosts()
                )
        );

        overview.setCompletionRatePercent(
                calculatePercent(
                        overview.getCompletedConsultations(),
                        overview.getTotalConsultations()
                )
        );
    }

    @NonNull
    private List<AdminTopPostItem> buildTopPostItems(@NonNull QuerySnapshot snapshot) {
        List<AdminTopPostItem> items = new ArrayList<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            AdminTopPostItem item = new AdminTopPostItem();
            item.setPostId(doc.getId());
            item.setAuthorName(extractAuthorName(doc));
            item.setContentPreview(extractPostPreview(doc));
            item.setLikeCount(safeInt(doc.getLong("like_count")));
            item.setCommentCount(safeInt(doc.getLong("comment_count")));
            item.setReportCount(safeInt(doc.getLong("report_count")));
            item.setStatus(safeUpper(doc.getString("status"), "ACTIVE"));
            item.setModerationStatus(safeUpper(doc.getString("moderation_status"), "VISIBLE"));

            Timestamp updatedAt = doc.getTimestamp("updated_at");
            if (updatedAt == null) {
                updatedAt = doc.getTimestamp("created_at");
            }
            item.setUpdatedAt(updatedAt);

            items.add(item);
        }

        return items;
    }

    @NonNull
    private List<AdminTopPostItem> buildTopLikedPosts(@NonNull List<AdminTopPostItem> allPosts) {
        List<AdminTopPostItem> sorted = new ArrayList<>(allPosts);

        Collections.sort(sorted, new Comparator<AdminTopPostItem>() {
            @Override
            public int compare(AdminTopPostItem left, AdminTopPostItem right) {
                if (left.getLikeCount() != right.getLikeCount()) {
                    return Integer.compare(right.getLikeCount(), left.getLikeCount());
                }

                long leftTime = left.getUpdatedAt() != null ? left.getUpdatedAt().toDate().getTime() : 0L;
                long rightTime = right.getUpdatedAt() != null ? right.getUpdatedAt().toDate().getTime() : 0L;
                return Long.compare(rightTime, leftTime);
            }
        });

        return limit(sorted, 5);
    }

    @NonNull
    private List<AdminTopPostItem> buildTopReportedPosts(@NonNull List<AdminTopPostItem> allPosts) {
        List<AdminTopPostItem> sorted = new ArrayList<>(allPosts);

        Collections.sort(sorted, new Comparator<AdminTopPostItem>() {
            @Override
            public int compare(AdminTopPostItem left, AdminTopPostItem right) {
                if (left.getReportCount() != right.getReportCount()) {
                    return Integer.compare(right.getReportCount(), left.getReportCount());
                }

                long leftTime = left.getUpdatedAt() != null ? left.getUpdatedAt().toDate().getTime() : 0L;
                long rightTime = right.getUpdatedAt() != null ? right.getUpdatedAt().toDate().getTime() : 0L;
                return Long.compare(rightTime, leftTime);
            }
        });

        List<AdminTopPostItem> filtered = new ArrayList<>();
        for (AdminTopPostItem item : sorted) {
            if (item.getReportCount() > 0) {
                filtered.add(item);
            }
        }

        return limit(filtered, 5);
    }

    @NonNull
    private List<AdminTopPostItem> buildTopCommentedPosts(@NonNull List<AdminTopPostItem> allPosts) {
        List<AdminTopPostItem> sorted = new ArrayList<>(allPosts);

        Collections.sort(sorted, new Comparator<AdminTopPostItem>() {
            @Override
            public int compare(AdminTopPostItem left, AdminTopPostItem right) {
                if (left.getCommentCount() != right.getCommentCount()) {
                    return Integer.compare(right.getCommentCount(), left.getCommentCount());
                }

                long leftTime = left.getUpdatedAt() != null ? left.getUpdatedAt().toDate().getTime() : 0L;
                long rightTime = right.getUpdatedAt() != null ? right.getUpdatedAt().toDate().getTime() : 0L;
                return Long.compare(rightTime, leftTime);
            }
        });

        List<AdminTopPostItem> filtered = new ArrayList<>();
        for (AdminTopPostItem item : sorted) {
            if (item.getCommentCount() > 0) {
                filtered.add(item);
            }
        }

        return limit(filtered, 5);
    }

    @NonNull
    private List<AdminTopPostItem> limit(@NonNull List<AdminTopPostItem> items, int maxSize) {
        if (items.size() <= maxSize) {
            return items;
        }
        return new ArrayList<>(items.subList(0, maxSize));
    }

    @NonNull
    private String extractAuthorName(@NonNull DocumentSnapshot doc) {
        boolean isAnonymous = Boolean.TRUE.equals(doc.getBoolean("is_anonymous"));
        if (isAnonymous) {
            return "Ẩn danh";
        }

        String[] keys = new String[]{
                "author_name",
                "nickname",
                "user_name",
                "display_name"
        };

        for (String key : keys) {
            String value = doc.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        String userId = doc.getString("user_id");
        return userId != null && !userId.trim().isEmpty() ? userId.trim() : "Người dùng Heami";
    }

    @NonNull
    private String extractPostPreview(@NonNull DocumentSnapshot doc) {
        String[] keys = new String[]{
                "content",
                "text",
                "caption",
                "body",
                "message",
                "post_text",
                "description"
        };

        for (String key : keys) {
            String value = doc.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        String moodEmoji = safeText(doc.getString("mood_emoji"), "");
        String moodTag = safeText(doc.getString("mood_tag"), "");

        if (!moodEmoji.isEmpty() || !moodTag.isEmpty()) {
            return "Bài viết cảm xúc "
                    + (!moodEmoji.isEmpty() ? moodEmoji : "")
                    + ((!moodEmoji.isEmpty() && !moodTag.isEmpty()) ? " • " : "")
                    + (!moodTag.isEmpty() ? moodTag : "");
        }

        return "(Không có nội dung)";
    }

    @NonNull
    private String firstNonEmpty(@NonNull DocumentSnapshot doc, @NonNull String... keys) {
        for (String key : keys) {
            String value = doc.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    @NonNull
    private String safeUpper(@Nullable String value, @NonNull String fallback) {
        return safeText(value, fallback).toUpperCase(Locale.ROOT);
    }

    private int calculatePercent(int part, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((part * 100f) / total);
    }

    private int safeInt(@Nullable Long value) {
        return value != null ? value.intValue() : 0;
    }
}