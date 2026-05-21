package com.example.heami.data.repositories;

import com.example.heami.data.models.AdminActionModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.heami.data.models.CommunityReportModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminModerationRepository {

    public interface LoadReportsListener {
        void onSuccess(@NonNull List<CommunityReportModel> reports);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LogAdminActionListener {
        void onSuccess(@NonNull String actionId);
        void onFailure(@NonNull String errorMessage);
    }

    private final FirebaseFirestore firestore;

    public AdminModerationRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void loadReports(@NonNull LoadReportsListener listener) {
        firestore.collection("community_posts")
                .get()
                .addOnSuccessListener(snapshot -> handleCommunityPostsSnapshot(snapshot, listener))
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải danh sách bài viết bị report";
                    listener.onFailure(message);
                });
    }

    public void logAdminAction(
            @NonNull String adminId,
            @NonNull String actionType,
            @NonNull String targetType,
            @NonNull String targetId,
            @NonNull String relatedReportId,
            @NonNull String note,
            @NonNull LogAdminActionListener listener
    ) {
        String actionId = firestore.collection("admin_actions").document().getId();

        AdminActionModel actionModel = new AdminActionModel();
        actionModel.setAction_id(actionId);
        actionModel.setAdmin_id(adminId);
        actionModel.setAction_type(actionType);
        actionModel.setTarget_type(targetType);
        actionModel.setTarget_id(targetId);
        actionModel.setRelated_report_id(relatedReportId);
        actionModel.setNote(note);
        actionModel.setCreated_at(Timestamp.now());

        firestore.collection("admin_actions")
                .document(actionId)
                .set(actionModel)
                .addOnSuccessListener(unused -> listener.onSuccess(actionId))
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể ghi audit log";
                    listener.onFailure(message);
                });
    }

    private void handleCommunityPostsSnapshot(
            @NonNull QuerySnapshot snapshot,
            @NonNull LoadReportsListener listener
    ) {
        List<DocumentSnapshot> reportedPostDocs = new ArrayList<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            long reportCount = getLong(doc, "report_count");
            if (reportCount > 0) {
                reportedPostDocs.add(doc);
            }
        }

        if (reportedPostDocs.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        List<CommunityReportModel> reports = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(reportedPostDocs.size());

        for (DocumentSnapshot postDoc : reportedPostDocs) {
            CommunityReportModel report = buildReportFromPost(postDoc);

            String userId = safeText(postDoc.getString("user_id"), "");
            boolean isAnonymous = Boolean.TRUE.equals(postDoc.getBoolean("is_anonymous"));

            if (isAnonymous || userId.isEmpty()) {
                report.setSnapshot_author_name("Người dùng ẩn danh");
                finishOneReport(report, reports, remaining, listener);
                continue;
            }

            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String authorName = extractUserDisplayName(userDoc, userId);
                        report.setSnapshot_author_name(authorName);
                        finishOneReport(report, reports, remaining, listener);
                    })
                    .addOnFailureListener(e -> {
                        report.setSnapshot_author_name("Người dùng Heami");
                        finishOneReport(report, reports, remaining, listener);
                    });
        }
    }

    private void finishOneReport(
            @NonNull CommunityReportModel report,
            @NonNull List<CommunityReportModel> reports,
            @NonNull AtomicInteger remaining,
            @NonNull LoadReportsListener listener
    ) {
        reports.add(report);

        if (remaining.decrementAndGet() == 0) {
            Collections.sort(reports, new Comparator<CommunityReportModel>() {
                @Override
                public int compare(CommunityReportModel left, CommunityReportModel right) {
                    int leftCount = extractReportCount(left);
                    int rightCount = extractReportCount(right);

                    if (leftCount != rightCount) {
                        return Integer.compare(rightCount, leftCount);
                    }

                    long leftTime = getComparableTime(left.getCreated_at());
                    long rightTime = getComparableTime(right.getCreated_at());
                    return Long.compare(rightTime, leftTime);
                }
            });

            listener.onSuccess(reports);
        }
    }

    @NonNull
    private CommunityReportModel buildReportFromPost(@NonNull DocumentSnapshot postDoc) {
        CommunityReportModel model = new CommunityReportModel();

        String postId = postDoc.getId();
        String userId = safeText(postDoc.getString("user_id"), "");
        int reportCount = (int) getLong(postDoc, "report_count");

        String postStatus = safeUpper(postDoc.getString("status"), "ACTIVE");
        String moderationStatus = safeUpper(postDoc.getString("moderation_status"), "VISIBLE");
        String moderationDecision = safeUpper(postDoc.getString("moderation_decision"), "");
        String moderationReason = safeText(postDoc.getString("moderation_reason"), "");

        model.setReport_id(postId);
        model.setTarget_type("POST");
        model.setTarget_id(postId);
        model.setPost_id(postId);

        model.setReported_by("");
        model.setReported_user_id(userId);

        model.setReason_code("POST_REPORTED");
        model.setReason_text(buildReasonText(reportCount, moderationDecision));

        model.setStatus(mapReportStatus(postStatus, moderationStatus, moderationDecision));
        model.setReviewed_by(safeText(postDoc.getString("moderated_by"), ""));
        model.setAction_taken(mapActionTaken(postStatus, moderationStatus, moderationDecision));

        Timestamp createdAt = getTimestamp(postDoc, "updated_at");
        if (createdAt == null) {
            createdAt = getTimestamp(postDoc, "created_at");
        }
        if (createdAt == null) {
            createdAt = Timestamp.now();
        }
        model.setCreated_at(createdAt);

        model.setReviewed_at(getTimestamp(postDoc, "moderated_at"));
        model.setSnapshot_text(extractPostSnapshot(postDoc));
        model.setSnapshot_author_name("Người dùng Heami");

        return model;
    }

    @NonNull
    private String buildReasonText(int reportCount, @NonNull String moderationDecision) {
        String base;
        if (reportCount <= 1) {
            base = "Bài viết đã bị report 1 lần";
        } else {
            base = "Bài viết đã bị report " + reportCount + " lần";
        }

        if ("KEEP_VISIBLE".equals(moderationDecision)) {
            return base + " • Admin quyết định giữ nguyên hiển thị";
        }

        if ("HIDDEN".equals(moderationDecision)) {
            return base + " • Admin đã ẩn bài viết";
        }

        if ("RESTORED".equals(moderationDecision)) {
            return base + " • Admin đã khôi phục bài viết";
        }

        return base;
    }

    @NonNull
    private String mapReportStatus(
            @NonNull String postStatus,
            @NonNull String moderationStatus,
            @NonNull String moderationDecision
    ) {
        if ("HIDDEN".equals(moderationStatus)
                || "HIDDEN".equals(postStatus)
                || "HIDDEN".equals(moderationDecision)
                || "KEEP_VISIBLE".equals(moderationDecision)
                || "RESTORED".equals(moderationDecision)
                || "DELETED".equals(moderationDecision)
                || "DELETED".equals(postStatus)) {
            return "RESOLVED";
        }

        return "PENDING";
    }

    @NonNull
    private String mapActionTaken(
            @NonNull String postStatus,
            @NonNull String moderationStatus,
            @NonNull String moderationDecision
    ) {
        if ("HIDDEN".equals(moderationStatus)
                || "HIDDEN".equals(postStatus)
                || "HIDDEN".equals(moderationDecision)) {
            return "HIDDEN";
        }

        if ("DELETED".equals(moderationDecision) || "DELETED".equals(postStatus)) {
            return "DELETED";
        }

        if ("RESTORED".equals(moderationDecision)) {
            return "RESTORED";
        }

        if ("KEEP_VISIBLE".equals(moderationDecision)) {
            return "NONE";
        }

        return "NONE";
    }

    @NonNull
    private String extractPostSnapshot(@NonNull DocumentSnapshot doc) {
        String[] candidates = new String[]{
                "content",
                "text",
                "caption",
                "body",
                "message",
                "post_text",
                "description"
        };

        for (String key : candidates) {
            String value = doc.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        String moodTag = safeText(doc.getString("mood_tag"), "");
        String moodEmoji = safeText(doc.getString("mood_emoji"), "");

        if (!moodTag.isEmpty() || !moodEmoji.isEmpty()) {
            if (!moodEmoji.isEmpty() && !moodTag.isEmpty()) {
                return "Bài viết cảm xúc " + moodEmoji + " • " + moodTag;
            }
            if (!moodEmoji.isEmpty()) {
                return "Bài viết cảm xúc " + moodEmoji;
            }
            return "Bài viết cảm xúc • " + moodTag;
        }

        return "(Không có nội dung văn bản để preview)";
    }

    @NonNull
    private String extractUserDisplayName(@NonNull DocumentSnapshot userDoc, @NonNull String fallbackUserId) {
        String nickname = safeText(userDoc.getString("nickname"), "");
        if (!nickname.isEmpty()) {
            return nickname;
        }

        String fullName = safeText(userDoc.getString("full_name"), "");
        if (!fullName.isEmpty()) {
            return fullName;
        }

        String name = safeText(userDoc.getString("name"), "");
        if (!name.isEmpty()) {
            return name;
        }

        return fallbackUserId;
    }

    public int countPending(@NonNull List<CommunityReportModel> reports) {
        int count = 0;
        for (CommunityReportModel report : reports) {
            if ("PENDING".equals(safeUpper(report.getStatus(), ""))) {
                count++;
            }
        }
        return count;
    }

    public int countPostReports(@NonNull List<CommunityReportModel> reports) {
        int count = 0;
        for (CommunityReportModel report : reports) {
            if ("POST".equals(safeUpper(report.getTarget_type(), ""))) {
                count++;
            }
        }
        return count;
    }

    public int countCommentReports(@NonNull List<CommunityReportModel> reports) {
        return 0;
    }

    private int extractReportCount(@NonNull CommunityReportModel report) {
        String reason = safeText(report.getReason_text(), "");
        String digits = reason.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long getComparableTime(@Nullable Timestamp timestamp) {
        return timestamp != null ? timestamp.toDate().getTime() : 0L;
    }

    private long getLong(@NonNull DocumentSnapshot doc, @NonNull String field) {
        Long value = doc.getLong(field);
        return value != null ? value : 0L;
    }

    @Nullable
    private Timestamp getTimestamp(@NonNull DocumentSnapshot doc, @NonNull String field) {
        return doc.getTimestamp(field);
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
}