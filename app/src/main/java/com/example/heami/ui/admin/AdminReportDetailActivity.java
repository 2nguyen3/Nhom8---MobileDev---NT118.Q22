package com.example.heami.ui.admin;

import com.example.heami.data.repositories.AdminModerationRepository;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminReportDetailActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "extra_report_id";

    private ImageView btnBackReportDetail;

    private View layoutLoadingDetail;
    private ScrollView layoutContentDetail;
    private TextView txtDetailError;

    private TextView txtReportDetailId;
    private TextView txtDetailAuthor;
    private TextView txtDetailPostId;
    private TextView txtDetailReportCount;
    private TextView txtDetailPostStatus;
    private TextView txtDetailModerationStatus;
    private TextView txtDetailMood;
    private TextView txtDetailUpdatedAt;
    private TextView txtDetailContent;
    private TextView txtDecisionHint;

    private TextView btnHidePost;
    private TextView btnKeepVisible;
    private TextView btnDeletePost;

    private FirebaseFirestore firestore;
    private String postId = "";
    private boolean isUpdating = false;

    private AdminModerationRepository moderationRepository;

    private String currentPostStatus = "ACTIVE";
    private String currentModerationStatus = "VISIBLE";
    private String currentModerationDecision = "";
    private long currentReportCount = 0L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_detail);

        firestore = FirebaseFirestore.getInstance();
        moderationRepository = new AdminModerationRepository();
        postId = safeText(getIntent().getStringExtra(EXTRA_REPORT_ID), "");

        bindViews();
        setupClicks();

        if (postId.isEmpty()) {
            showError("Thiếu post_id để mở chi tiết moderation");
            return;
        }

        loadPostDetail();
    }

    private void bindViews() {
        btnBackReportDetail = findViewById(R.id.btnBackReportDetail);

        layoutLoadingDetail = findViewById(R.id.layoutLoadingReportDetail);
        layoutContentDetail = findViewById(R.id.layoutContentReportDetail);
        txtDetailError = findViewById(R.id.txtReportDetailError);

        txtReportDetailId = findViewById(R.id.txtReportDetailId);
        txtDetailAuthor = findViewById(R.id.txtDetailAuthor);
        txtDetailPostId = findViewById(R.id.txtDetailPostId);
        txtDetailReportCount = findViewById(R.id.txtDetailReportCount);
        txtDetailPostStatus = findViewById(R.id.txtDetailPostStatus);
        txtDetailModerationStatus = findViewById(R.id.txtDetailModerationStatus);
        txtDetailMood = findViewById(R.id.txtDetailMood);
        txtDetailUpdatedAt = findViewById(R.id.txtDetailUpdatedAt);
        txtDetailContent = findViewById(R.id.txtDetailContent);
        txtDecisionHint = findViewById(R.id.txtDecisionHint);

        btnHidePost = findViewById(R.id.btnHidePost);
        btnKeepVisible = findViewById(R.id.btnKeepVisible);
        btnDeletePost = findViewById(R.id.btnDeletePost);
    }

    private void setupClicks() {
        btnBackReportDetail.setOnClickListener(v -> finish());

        btnHidePost.setOnClickListener(v -> {
            if ("HIDDEN".equals(currentModerationStatus)) {
                updateModerationDecision("RESTORED");
            } else {
                updateModerationDecision("HIDDEN");
            }
        });

        btnKeepVisible.setOnClickListener(v -> updateModerationDecision("KEEP_VISIBLE"));
        btnDeletePost.setOnClickListener(v -> updateModerationDecision("DELETED"));
    }

    private void loadPostDetail() {
        showLoading(true);

        firestore.collection("community_posts")
                .document(postId)
                .get()
                .addOnSuccessListener(this::handlePostSnapshot)
                .addOnFailureListener(e -> showError(
                        e.getMessage() != null ? e.getMessage() : "Không thể tải chi tiết bài viết"
                ));
    }

    private void handlePostSnapshot(@NonNull DocumentSnapshot doc) {
        if (!doc.exists()) {
            showError("Không tìm thấy bài viết");
            return;
        }

        String authorName = "Người dùng Heami";
        boolean isAnonymous = Boolean.TRUE.equals(doc.getBoolean("is_anonymous"));
        String userId = safeText(doc.getString("user_id"), "");
        String nickname = safeText(doc.getString("nickname"), "");

        if (isAnonymous) {
            authorName = "Người dùng ẩn danh";
        } else if (!nickname.isEmpty()) {
            authorName = nickname;
        } else if (!userId.isEmpty()) {
            authorName = userId;
        }

        currentReportCount = getLong(doc, "report_count");
        currentPostStatus = safeUpper(doc.getString("status"), "ACTIVE");
        currentModerationStatus = safeUpper(doc.getString("moderation_status"), "VISIBLE");
        currentModerationDecision = safeUpper(doc.getString("moderation_decision"), "");

        txtReportDetailId.setText(postId);
        txtDetailAuthor.setText(authorName);
        txtDetailPostId.setText(postId);
        txtDetailReportCount.setText(String.valueOf(currentReportCount));
        txtDetailPostStatus.setText(currentPostStatus);
        txtDetailModerationStatus.setText(currentModerationStatus);

        String moodEmoji = safeText(doc.getString("mood_emoji"), "");
        String moodTag = safeText(doc.getString("mood_tag"), "");
        if (!moodEmoji.isEmpty() || !moodTag.isEmpty()) {
            txtDetailMood.setText(
                    (!moodEmoji.isEmpty() ? moodEmoji : "")
                            + ((!moodEmoji.isEmpty() && !moodTag.isEmpty()) ? " • " : "")
                            + (!moodTag.isEmpty() ? moodTag : "")
            );
        } else {
            txtDetailMood.setText("--");
        }

        Timestamp updatedAt = doc.getTimestamp("updated_at");
        if (updatedAt == null) {
            updatedAt = doc.getTimestamp("created_at");
        }
        txtDetailUpdatedAt.setText(formatTimestamp(updatedAt));

        txtDetailContent.setText(extractPostContent(doc));
        txtDecisionHint.setText(buildDecisionHint());

        bindPostStatusChip(txtDetailPostStatus, currentPostStatus);
        bindModerationStatusChip(txtDetailModerationStatus, currentModerationStatus);
        updateActionButtons();

        showContent();
    }

    private void updateModerationDecision(@NonNull String decision) {
        if (isUpdating || postId.isEmpty()) {
            return;
        }

        isUpdating = true;
        updateActionButtons();

        String adminId = safeText(FirebaseAuth.getInstance().getUid(), "");

        Map<String, Object> updates = new HashMap<>();
        updates.put("moderated_at", Timestamp.now());
        updates.put("moderated_by", adminId);

        switch (decision) {
            case "HIDDEN":
                updates.put("status", "ACTIVE");
                updates.put("moderation_status", "HIDDEN");
                updates.put("moderation_decision", "HIDDEN");
                updates.put("moderation_reason", "ADMIN_HIDE_AFTER_REPORT");
                break;

            case "RESTORED":
                updates.put("status", "ACTIVE");
                updates.put("moderation_status", "VISIBLE");
                updates.put("moderation_decision", "RESTORED");
                updates.put("moderation_reason", "ADMIN_RESTORE_POST");
                break;

            case "DELETED":
                updates.put("status", "DELETED");
                updates.put("moderation_status", "HIDDEN");
                updates.put("moderation_decision", "DELETED");
                updates.put("moderation_reason", "ADMIN_DELETE_POST");
                break;

            case "KEEP_VISIBLE":
            default:
                updates.put("status", "ACTIVE");
                updates.put("moderation_status", "VISIBLE");
                updates.put("moderation_decision", "KEEP_VISIBLE");
                updates.put("moderation_reason", "ADMIN_KEEP_VISIBLE");
                break;
        }

        firestore.collection("community_posts")
                .document(postId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    if (adminId.isEmpty()) {
                        completeModerationSuccess(decision, false);
                        return;
                    }

                    moderationRepository.logAdminAction(
                            adminId,
                            resolveAdminActionType(decision),
                            "POST",
                            postId,
                            postId,
                            buildAdminActionNote(decision),
                            new AdminModerationRepository.LogAdminActionListener() {
                                @Override
                                public void onSuccess(@NonNull String actionId) {
                                    completeModerationSuccess(decision, true);
                                }

                                @Override
                                public void onFailure(@NonNull String errorMessage) {
                                    completeModerationSuccess(decision, false);
                                }
                            }
                    );
                })
                .addOnFailureListener(e -> {
                    isUpdating = false;
                    updateActionButtons();
                    Toast.makeText(
                            this,
                            e.getMessage() != null ? e.getMessage() : "Không thể cập nhật moderation",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    @NonNull
    private String resolveAdminActionType(@NonNull String decision) {
        switch (decision) {
            case "HIDDEN":
                return "HIDE_POST";
            case "RESTORED":
                return "RESTORE_POST";
            case "DELETED":
                return "DELETE_POST";
            case "KEEP_VISIBLE":
            default:
                return "KEEP_VISIBLE_POST";
        }
    }

    @NonNull
    private String buildAdminActionNote(@NonNull String decision) {
        String humanDecision;
        switch (decision) {
            case "HIDDEN":
                humanDecision = "Ẩn bài viết";
                break;
            case "RESTORED":
                humanDecision = "Khôi phục bài viết";
                break;
            case "DELETED":
                humanDecision = "Xóa bài viết";
                break;
            case "KEEP_VISIBLE":
            default:
                humanDecision = "Giữ nguyên hiển thị";
                break;
        }

        return humanDecision
                + " | postId=" + postId
                + " | reportCount=" + currentReportCount
                + " | postStatus=" + currentPostStatus
                + " | moderationStatus=" + currentModerationStatus;
    }

    private void completeModerationSuccess(@NonNull String decision, boolean auditLogged) {
        isUpdating = false;

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_REPORT_ID, postId);
        setResult(RESULT_OK, resultIntent);

        String message = resolveSuccessMessage(decision);
        if (!auditLogged) {
            message = message + " (nhưng chưa ghi được audit log)";
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        loadPostDetail();
    }

    private void updateActionButtons() {
        btnHidePost.setEnabled(!isUpdating);
        btnKeepVisible.setEnabled(!isUpdating);
        btnDeletePost.setEnabled(!isUpdating);

        btnHidePost.setAlpha(isUpdating ? 0.7f : 1f);
        btnKeepVisible.setAlpha(isUpdating ? 0.7f : 1f);
        btnDeletePost.setAlpha(isUpdating ? 0.7f : 1f);

        if (isUpdating) {
            btnHidePost.setText("Đang cập nhật...");
            btnKeepVisible.setText("Đang cập nhật...");
            btnDeletePost.setText("Đang cập nhật...");
            return;
        }

        // 1) Reset toàn bộ về trạng thái trung tính trước
        btnHidePost.setTextColor(Color.parseColor("#6E5F7F"));
        btnKeepVisible.setTextColor(Color.parseColor("#6E5F7F"));
        btnDeletePost.setTextColor(Color.parseColor("#6E5F7F"));

        btnHidePost.setBackgroundResource(R.drawable.bg_admin_stat_card);
        btnKeepVisible.setBackgroundResource(R.drawable.bg_admin_stat_card);
        btnDeletePost.setBackgroundResource(R.drawable.bg_admin_stat_card);

        btnHidePost.setEnabled(true);
        btnKeepVisible.setEnabled(true);
        btnDeletePost.setEnabled(true);

        // 2) Nếu đã xóa thì chỉ khóa nút Giữ nguyên hiển thị , chỉ hiện trạng thái đã xóa
        if ("DELETED".equals(currentPostStatus) || "DELETED".equals(currentModerationDecision)) {
            btnHidePost.setText("Khôi phục bài viết");
            btnHidePost.setBackgroundResource(R.drawable.bg_button_teal);
            btnHidePost.setTextColor(Color.WHITE);
            btnHidePost.setEnabled(true);

            btnKeepVisible.setText("Giữ nguyên hiển thị");
            btnKeepVisible.setBackgroundResource(R.drawable.bg_admin_stat_card);
            btnKeepVisible.setTextColor(Color.parseColor("#A39AAF"));
            btnKeepVisible.setEnabled(false);

            btnDeletePost.setText("Đã xóa bài viết");
            btnDeletePost.setBackgroundResource(R.drawable.bg_chip_inactive);
            btnDeletePost.setTextColor(Color.parseColor("#C2516A"));
            btnDeletePost.setEnabled(false);
            return;
        }

        // 3) Nếu đang bị ẩn -> nút đầu thành Khôi phục và lên màu xanh
        if ("HIDDEN".equals(currentModerationStatus)) {
            btnHidePost.setText("Khôi phục bài viết");
            btnHidePost.setBackgroundResource(R.drawable.bg_button_teal);
            btnHidePost.setTextColor(Color.WHITE);

            // Khi đang hidden thì không cần cho bấm "Giữ nguyên hiển thị"
            btnKeepVisible.setText("Giữ nguyên hiển thị");
            btnKeepVisible.setEnabled(false);
            btnKeepVisible.setTextColor(Color.parseColor("#A39AAF"));

            btnDeletePost.setText("Xóa bài viết");
            return;
        }

        // 4) Nếu admin đã quyết định giữ nguyên hiển thị -> chỉ nút đó màu xanh
        if ("KEEP_VISIBLE".equals(currentModerationDecision)) {
            btnHidePost.setText("Ẩn bài viết");

            btnKeepVisible.setText("Giữ nguyên hiển thị");
            btnKeepVisible.setBackgroundResource(R.drawable.bg_button_teal);
            btnKeepVisible.setTextColor(Color.WHITE);

            btnDeletePost.setText("Xóa bài viết");
            return;
        }

        // 5) Nếu admin đã khôi phục -> nút khôi phục vẫn xanh để thể hiện quyết định gần nhất
        if ("RESTORED".equals(currentModerationDecision)) {
            btnHidePost.setText("Khôi phục bài viết");
            btnHidePost.setBackgroundResource(R.drawable.bg_button_teal);
            btnHidePost.setTextColor(Color.WHITE);

            btnKeepVisible.setText("Giữ nguyên hiển thị");
            btnDeletePost.setText("Xóa bài viết");
            return;
        }

        // 6) Trạng thái mặc định chưa quyết định gì: tất cả trung tính
        btnHidePost.setText("Ẩn bài viết");
        btnKeepVisible.setText("Giữ nguyên hiển thị");
        btnDeletePost.setText("Xóa bài viết");
    }

    private void bindPostStatusChip(@NonNull TextView view, @NonNull String status) {
        if ("HIDDEN".equals(status) || "DELETED".equals(status)) {
            view.setBackgroundResource(R.drawable.bg_chip_inactive);
            view.setTextColor(Color.parseColor("#C2516A"));
        } else {
            view.setBackgroundResource(R.drawable.bg_chip_active_teal);
            view.setTextColor(Color.parseColor("#1D9E92"));
        }
    }

    private void bindModerationStatusChip(@NonNull TextView view, @NonNull String status) {
        if ("HIDDEN".equals(status)) {
            view.setBackgroundResource(R.drawable.bg_chip_inactive);
            view.setTextColor(Color.parseColor("#C2516A"));
        } else {
            view.setBackgroundResource(R.drawable.bg_chip_active_purple);
            view.setTextColor(Color.parseColor("#6A42C2"));
        }
    }

    @NonNull
    private String buildDecisionHint() {

        if ("DELETED".equals(currentPostStatus) || "DELETED".equals(currentModerationDecision)) {
            return "Bài viết đã bị admin xóa khỏi hệ thống hiển thị.";
        }

        if (currentReportCount >= 5) {
            if ("HIDDEN".equals(currentModerationStatus)) {
                return "Bài viết đã vượt ngưỡng 5 report và hiện đang bị ẩn khỏi feed.";
            }
            if ("KEEP_VISIBLE".equals(currentModerationDecision)) {
                return "Bài viết đã vượt ngưỡng 5 report nhưng admin quyết định giữ nguyên hiển thị.";
            }
            return "Bài viết đã vượt ngưỡng 5 report. Admin cần quyết định ẩn hoặc giữ nguyên hiển thị.";
        }

        if ("HIDDEN".equals(currentModerationStatus)) {
            return "Bài viết hiện đang bị admin ẩn khỏi feed.";
        }

        if ("KEEP_VISIBLE".equals(currentModerationDecision)) {
            return "Admin đã xem xét và quyết định giữ nguyên hiển thị bài viết.";
        }

        if ("RESTORED".equals(currentModerationDecision)) {
            return "Admin đã khôi phục bài viết về trạng thái hiển thị.";
        }

        return "Bài viết đang được theo dõi vì đã có report từ cộng đồng.";
    }

    @NonNull
    private String resolveSuccessMessage(@NonNull String decision) {
        switch (decision) {
            case "HIDDEN":
                return "Đã ẩn bài viết";
            case "RESTORED":
                return "Đã khôi phục bài viết";
            case "DELETED":
                return "Đã xóa bài viết";
            case "KEEP_VISIBLE":
            default:
                return "Đã giữ nguyên hiển thị bài viết";
        }
    }

    @NonNull
    private String extractPostContent(@NonNull DocumentSnapshot doc) {
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
            if (!TextUtils.isEmpty(value)) {
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

        return "(Không có nội dung văn bản để hiển thị)";
    }

    private void showLoading(boolean isLoading) {
        layoutLoadingDetail.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        layoutContentDetail.setVisibility(View.GONE);
        txtDetailError.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoadingDetail.setVisibility(View.GONE);
        layoutContentDetail.setVisibility(View.VISIBLE);
        txtDetailError.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        layoutLoadingDetail.setVisibility(View.GONE);
        layoutContentDetail.setVisibility(View.GONE);
        txtDetailError.setVisibility(View.VISIBLE);
        txtDetailError.setText(message);
    }

    @NonNull
    private String formatTimestamp(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "--";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(timestamp.toDate());
    }

    private long getLong(@NonNull DocumentSnapshot doc, @NonNull String field) {
        Long value = doc.getLong(field);
        return value != null ? value : 0L;
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