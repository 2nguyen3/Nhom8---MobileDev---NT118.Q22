package com.example.heami.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.data.repositories.AdminModerationRepository;
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
    private TextView txtDetailReportCount;
    private TextView txtDetailPostStatus;
    private TextView txtDetailModerationStatus;
    private TextView txtDetailMood;
    private TextView txtDetailUpdatedAt;
    private TextView txtDetailContent;
    private TextView txtDecisionHint;

    private TextView txtDecisionSectionTitle;
    private TextView txtDecisionSectionDesc;

    private LinearLayout cardDecisionPrimary;
    private LinearLayout cardDecisionSecondary;
    private TextView txtDecisionPrimaryTitle;
    private TextView txtDecisionPrimaryDesc;
    private TextView txtDecisionSecondaryTitle;
    private TextView txtDecisionSecondaryDesc;
    private TextView btnConfirmDecision;

    private LinearLayout layoutDangerZone;
    private LinearLayout cardDeletePost;
    private TextView txtDeletePostTitle;
    private TextView txtDeletePostDesc;

    private FirebaseFirestore firestore;
    private AdminModerationRepository moderationRepository;

    private String postId = "";
    private boolean isUpdating = false;

    private String currentPostStatus = "ACTIVE";
    private String currentModerationStatus = "VISIBLE";
    private String currentModerationDecision = "";
    private long currentReportCount = 0L;

    private String selectedDecision = "";

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
            showError("Thiếu post_id để mở chi tiết kiểm ");
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
        txtDetailReportCount = findViewById(R.id.txtDetailReportCount);
        txtDetailPostStatus = findViewById(R.id.txtDetailPostStatus);
        txtDetailModerationStatus = findViewById(R.id.txtDetailModerationStatus);
        txtDetailMood = findViewById(R.id.txtDetailMood);
        txtDetailUpdatedAt = findViewById(R.id.txtDetailUpdatedAt);
        txtDetailContent = findViewById(R.id.txtDetailContent);
        txtDecisionHint = findViewById(R.id.txtDecisionHint);

        txtDecisionSectionTitle = findViewById(R.id.txtDecisionSectionTitle);
        txtDecisionSectionDesc = findViewById(R.id.txtDecisionSectionDesc);

        cardDecisionPrimary = findViewById(R.id.cardDecisionPrimary);
        cardDecisionSecondary = findViewById(R.id.cardDecisionSecondary);
        txtDecisionPrimaryTitle = findViewById(R.id.txtDecisionPrimaryTitle);
        txtDecisionPrimaryDesc = findViewById(R.id.txtDecisionPrimaryDesc);
        txtDecisionSecondaryTitle = findViewById(R.id.txtDecisionSecondaryTitle);
        txtDecisionSecondaryDesc = findViewById(R.id.txtDecisionSecondaryDesc);
        btnConfirmDecision = findViewById(R.id.btnConfirmDecision);

        layoutDangerZone = findViewById(R.id.layoutDangerZone);
        cardDeletePost = findViewById(R.id.cardDeletePost);
        txtDeletePostTitle = findViewById(R.id.txtDeletePostTitle);
        txtDeletePostDesc = findViewById(R.id.txtDeletePostDesc);
    }

    private void setupClicks() {
        btnBackReportDetail.setOnClickListener(v -> finish());

        cardDecisionPrimary.setOnClickListener(v -> {
            if (isUpdating || !cardDecisionPrimary.isEnabled()) return;
            selectedDecision = getPrimaryDecision();
            bindDecisionUi();
        });

        cardDecisionSecondary.setOnClickListener(v -> {
            if (isUpdating || !cardDecisionSecondary.isEnabled()) return;
            selectedDecision = getSecondaryDecision();
            bindDecisionUi();
        });

        btnConfirmDecision.setOnClickListener(v -> {
            if (isUpdating || selectedDecision.trim().isEmpty()) return;
            updateModerationDecision(selectedDecision);
        });

        cardDeletePost.setOnClickListener(v -> {
            if (isUpdating || !cardDeletePost.isEnabled()) return;
            updateModerationDecision("DELETED");
        });
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

        selectedDecision = resolveInitialSelectedDecision();
        bindDecisionUi();

        showContent();
    }

    @NonNull
    private String resolveInitialSelectedDecision() {
        if (isDeletedState()) {
            return "RESTORED";
        }

        if (isHiddenState()) {
            return "HIDDEN";
        }

        if ("KEEP_VISIBLE".equals(currentModerationDecision)) {
            return "KEEP_VISIBLE";
        }

        if ("RESTORED".equals(currentModerationDecision)) {
            return "KEEP_VISIBLE";
        }

        return "KEEP_VISIBLE";
    }

    private void bindDecisionUi() {
        if (isDeletedState()) {
            bindDeletedDecisionUi();
            return;
        }

        if (isHiddenState()) {
            bindHiddenDecisionUi();
            return;
        }

        bindVisibleDecisionUi();
    }

    private void bindVisibleDecisionUi() {
        txtDecisionSectionTitle.setText("Quyết định kiểm duyệt");
        txtDecisionSectionDesc.setText("Bài viết đang hiển thị. Hãy chọn giữ nguyên hoặc ẩn khỏi feed.");

        cardDecisionPrimary.setVisibility(View.VISIBLE);
        cardDecisionPrimary.setEnabled(!isUpdating);
        txtDecisionPrimaryTitle.setText("Giữ nguyên hiển thị");
        txtDecisionPrimaryDesc.setText("Bài viết tiếp tục hiển thị trên feed cho người dùng.");

        cardDecisionSecondary.setVisibility(View.VISIBLE);
        cardDecisionSecondary.setEnabled(!isUpdating);
        txtDecisionSecondaryTitle.setText("Ẩn khỏi feed");
        txtDecisionSecondaryDesc.setText("Bài viết sẽ không còn xuất hiện trên feed nhưng vẫn còn dữ liệu duyệt.");

        styleDecisionCard(cardDecisionPrimary, txtDecisionPrimaryTitle, txtDecisionPrimaryDesc,
                "KEEP_VISIBLE".equals(selectedDecision), true, !isUpdating);
        styleDecisionCard(cardDecisionSecondary, txtDecisionSecondaryTitle, txtDecisionSecondaryDesc,
                "HIDDEN".equals(selectedDecision), false, !isUpdating);

        styleConfirmButton(selectedDecision);

        layoutDangerZone.setAlpha(isUpdating ? 0.7f : 1f);
        cardDeletePost.setEnabled(!isUpdating);
        cardDeletePost.setBackgroundResource(R.drawable.bg_admin_stat_card);
        txtDeletePostTitle.setText("Xóa bài viết");
        txtDeletePostTitle.setTextColor(Color.parseColor("#C2516A"));
        txtDeletePostDesc.setText("Bài viết sẽ bị đánh dấu đã xóa và không còn hiển thị trên feed.");
        txtDeletePostDesc.setTextColor(Color.parseColor("#84788F"));
    }

    private void bindHiddenDecisionUi() {
        txtDecisionSectionTitle.setText("Quyết định kiểm duyệt");
        txtDecisionSectionDesc.setText("Bài viết hiện đang bị ẩn. Hãy chọn tiếp tục ẩn hoặc khôi phục hiển thị.");

        cardDecisionPrimary.setVisibility(View.VISIBLE);
        cardDecisionPrimary.setEnabled(!isUpdating);
        txtDecisionPrimaryTitle.setText("Tiếp tục ẩn khỏi feed");
        txtDecisionPrimaryDesc.setText("Giữ bài viết ở trạng thái ẩn, người dùng sẽ không thấy trên feed.");

        cardDecisionSecondary.setVisibility(View.VISIBLE);
        cardDecisionSecondary.setEnabled(!isUpdating);
        txtDecisionSecondaryTitle.setText("Khôi phục hiển thị");
        txtDecisionSecondaryDesc.setText("Đưa bài viết trở lại feed cho người dùng.");

        styleDecisionCard(cardDecisionPrimary, txtDecisionPrimaryTitle, txtDecisionPrimaryDesc,
                "HIDDEN".equals(selectedDecision), false, !isUpdating);
        styleDecisionCard(cardDecisionSecondary, txtDecisionSecondaryTitle, txtDecisionSecondaryDesc,
                "RESTORED".equals(selectedDecision), true, !isUpdating);

        styleConfirmButton(selectedDecision);

        layoutDangerZone.setAlpha(isUpdating ? 0.7f : 1f);
        cardDeletePost.setEnabled(!isUpdating);
        cardDeletePost.setBackgroundResource(R.drawable.bg_admin_stat_card);
        txtDeletePostTitle.setText("Xóa bài viết");
        txtDeletePostTitle.setTextColor(Color.parseColor("#C2516A"));
        txtDeletePostDesc.setText("Xóa là thao tác mạnh hơn ẩn. Bài viết sẽ được đánh dấu đã xóa.");
        txtDeletePostDesc.setTextColor(Color.parseColor("#84788F"));
    }

    private void bindDeletedDecisionUi() {
        txtDecisionSectionTitle.setText("Khôi phục sau khi xóa");
        txtDecisionSectionDesc.setText("Bài viết đã bị xóa. Bạn chỉ có thể khôi phục lại trạng thái hiển thị.");

        cardDecisionPrimary.setVisibility(View.VISIBLE);
        cardDecisionPrimary.setEnabled(!isUpdating);
        txtDecisionPrimaryTitle.setText("Khôi phục bài viết");
        txtDecisionPrimaryDesc.setText("Khôi phục bài viết về trạng thái hiển thị để người dùng thấy lại trên feed.");

        cardDecisionSecondary.setVisibility(View.GONE);

        styleDecisionCard(cardDecisionPrimary, txtDecisionPrimaryTitle, txtDecisionPrimaryDesc,
                true, true, !isUpdating);

        btnConfirmDecision.setEnabled(!isUpdating);
        btnConfirmDecision.setAlpha(isUpdating ? 0.7f : 1f);
        btnConfirmDecision.setText(isUpdating ? "Đang cập nhật..." : "Khôi phục bài viết");
        btnConfirmDecision.setBackgroundResource(R.drawable.bg_button_teal);
        btnConfirmDecision.setTextColor(Color.WHITE);

        layoutDangerZone.setAlpha(0.8f);
        cardDeletePost.setEnabled(false);
        cardDeletePost.setBackgroundResource(R.drawable.bg_chip_inactive);
        txtDeletePostTitle.setText("Bài viết đã bị xóa");
        txtDeletePostTitle.setTextColor(Color.parseColor("#C2516A"));
        txtDeletePostDesc.setText("Không thể xóa thêm. Hãy dùng khôi phục nếu muốn đưa bài viết trở lại.");
        txtDeletePostDesc.setTextColor(Color.parseColor("#84788F"));
    }

    private void styleDecisionCard(
            @NonNull LinearLayout card,
            @NonNull TextView title,
            @NonNull TextView desc,
            boolean selected,
            boolean positive,
            boolean enabled
    ) {
        if (!enabled) {
            card.setBackgroundResource(R.drawable.bg_chip_inactive);
            title.setTextColor(Color.parseColor("#A39AAF"));
            desc.setTextColor(Color.parseColor("#A39AAF"));
            return;
        }

        if (selected) {
            if (positive) {
                card.setBackgroundResource(R.drawable.bg_button_teal);
            } else {
                card.setBackgroundResource(R.drawable.bg_button_soft_red);
            }
            title.setTextColor(Color.WHITE);
            desc.setTextColor(Color.WHITE);
        } else {
            card.setBackgroundResource(R.drawable.bg_admin_stat_card);
            title.setTextColor(Color.parseColor("#1B1730"));
            desc.setTextColor(Color.parseColor("#84788F"));
        }
    }

    private void styleConfirmButton(@NonNull String decision) {
        btnConfirmDecision.setEnabled(!isUpdating);
        btnConfirmDecision.setAlpha(isUpdating ? 0.7f : 1f);

        if (isUpdating) {
            btnConfirmDecision.setText("Đang cập nhật...");
            btnConfirmDecision.setBackgroundResource(R.drawable.bg_admin_stat_card);
            btnConfirmDecision.setTextColor(Color.parseColor("#6E5F7F"));
            return;
        }

        switch (decision) {
            case "HIDDEN":
                btnConfirmDecision.setText("Xác nhận ẩn bài viết");
                btnConfirmDecision.setBackgroundResource(R.drawable.bg_button_soft_red);
                btnConfirmDecision.setTextColor(Color.WHITE);
                break;

            case "RESTORED":
                btnConfirmDecision.setText("Xác nhận khôi phục hiển thị");
                btnConfirmDecision.setBackgroundResource(R.drawable.bg_button_teal);
                btnConfirmDecision.setTextColor(Color.WHITE);
                break;

            case "KEEP_VISIBLE":
            default:
                btnConfirmDecision.setText("Xác nhận giữ nguyên hiển thị");
                btnConfirmDecision.setBackgroundResource(R.drawable.bg_button_teal);
                btnConfirmDecision.setTextColor(Color.WHITE);
                break;
        }
    }

    @NonNull
    private String getPrimaryDecision() {
        if (isDeletedState()) {
            return "RESTORED";
        }
        if (isHiddenState()) {
            return "HIDDEN";
        }
        return "KEEP_VISIBLE";
    }

    @NonNull
    private String getSecondaryDecision() {
        if (isHiddenState()) {
            return "RESTORED";
        }
        return "HIDDEN";
    }

    private boolean isHiddenState() {
        return "HIDDEN".equals(currentModerationStatus) && !isDeletedState();
    }

    private boolean isDeletedState() {
        return "DELETED".equals(currentPostStatus) || "DELETED".equals(currentModerationDecision);
    }

    private void updateModerationDecision(@NonNull String decision) {
        if (isUpdating || postId.isEmpty()) {
            return;
        }

        isUpdating = true;
        bindDecisionUi();

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
                    bindDecisionUi();
                    Toast.makeText(
                            this,
                            e.getMessage() != null ? e.getMessage() : "Không thể cập nhật kiểm duyệt",
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
        if (isDeletedState()) {
            return "Bài viết đã bị admin xóa khỏi hệ thống hiển thị. Bạn có thể khôi phục nếu cần.";
        }

        if (currentReportCount >= 5) {
            if ("HIDDEN".equals(currentModerationStatus)) {
                return "Bài viết đã vượt ngưỡng 5 report và hiện đang bị ẩn khỏi feed.";
            }
            if ("KEEP_VISIBLE".equals(currentModerationDecision)) {
                return "Bài viết đã vượt ngưỡng 5 report nhưng admin quyết định giữ nguyên hiển thị.";
            }
            return "Bài viết đã vượt ngưỡng 5 report. Hãy chọn ẩn khỏi feed hoặc giữ nguyên hiển thị.";
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