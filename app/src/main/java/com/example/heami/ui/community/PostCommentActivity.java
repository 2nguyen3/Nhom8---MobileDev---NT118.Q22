package com.example.heami.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.data.models.CommunityPostModel;
import com.example.heami.data.models.PostCommentModel;
import com.example.heami.data.repositories.CommunityRepository;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class PostCommentActivity extends AppCompatActivity {

    private View layoutPostCommentSheet;
    private TextView txtPostCommentTitle;
    private TextView txtPreviewAvatar;
    private TextView txtPreviewContent;
    private LinearLayout layoutCommentsContainer;
    private EditText edtPostComment;
    private FrameLayout btnSendComment;

    private CommunityRepository communityRepository;

    private String postId = "";
    private CommunityPostModel currentPost;
    private final List<PostCommentModel> allComments = new ArrayList<>();

    private boolean isSendingComment = false;
    private boolean isLoadingComments = false;
    private boolean hasCommentChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_comment);

        bindViews();
        initData();
        prepareSheetIntro();
        setupActions();
        setupInputWatcher();
        setupBackPressHandler();
        updateSendButtonState();
        startSheetIntroAnimation();
        loadPostAndComments();
    }

    private void bindViews() {
        layoutPostCommentSheet = findViewById(R.id.layoutPostCommentSheet);
        txtPostCommentTitle = findViewById(R.id.txtPostCommentTitle);
        txtPreviewAvatar = findViewById(R.id.txtPreviewAvatar);
        txtPreviewContent = findViewById(R.id.txtPreviewContent);
        layoutCommentsContainer = findViewById(R.id.layoutCommentsContainer);
        edtPostComment = findViewById(R.id.edtPostComment);
        btnSendComment = findViewById(R.id.btnSendComment);
    }

    private void initData() {
        communityRepository = new CommunityRepository();

        if (getIntent() != null) {
            postId = safeText(getIntent().getStringExtra("post_id"), "");
        }
    }

    private void prepareSheetIntro() {
        if (layoutPostCommentSheet != null) {
            layoutPostCommentSheet.setTranslationY(1400f);
            layoutPostCommentSheet.setAlpha(1f);
        }
    }

    private void startSheetIntroAnimation() {
        if (layoutPostCommentSheet != null) {
            layoutPostCommentSheet.animate()
                    .translationY(0f)
                    .setDuration(420)
                    .start();
        }
    }

    private void setupActions() {
        View root = findViewById(R.id.postCommentRoot);
        if (root != null) {
            root.setOnClickListener(v -> {
                if (isSendingComment) return;
                finishWithRefreshResult();
            });
        }

        if (layoutPostCommentSheet != null) {
            layoutPostCommentSheet.setOnClickListener(v -> {
                // chặn click xuyên xuống overlay
            });
        }

        View btnClose = findViewById(R.id.btnClosePostComment);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                if (isSendingComment) return;
                finishWithRefreshResult();
            });
        }

        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> {
                if (isSendingComment) return;
                submitComment();
            });
        }
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isSendingComment) return;
                finishWithRefreshResult();
            }
        });
    }

    private void setupInputWatcher() {
        if (edtPostComment == null) return;

        edtPostComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateSendButtonState() {
        if (btnSendComment == null) return;

        boolean hasText = edtPostComment != null
                && edtPostComment.getText() != null
                && !edtPostComment.getText().toString().trim().isEmpty();

        boolean enabled = hasText && !isSendingComment;

        btnSendComment.setEnabled(enabled);
        btnSendComment.setAlpha(enabled ? 1f : 0.55f);
    }

    private void loadPostAndComments() {
        if (postId.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin bài viết", Toast.LENGTH_SHORT).show();
            finishWithRefreshResult();
            return;
        }

        showLoadingComments();

        communityRepository.getPostById(postId, new CommunityRepository.LoadSinglePostListener() {
            @Override
            public void onSuccess(@NonNull CommunityPostModel post) {
                currentPost = post;
                bindPostPreview(post);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                Toast.makeText(PostCommentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finishWithRefreshResult();
            }
        });

        loadComments();
    }

    private void loadComments() {
        if (isLoadingComments) return;
        isLoadingComments = true;

        communityRepository.getPostComments(postId, new CommunityRepository.LoadCommentsListener() {
            @Override
            public void onSuccess(@NonNull List<PostCommentModel> comments) {
                isLoadingComments = false;

                allComments.clear();
                allComments.addAll(comments);

                renderComments();
                updateCommentTitle();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                isLoadingComments = false;

                if (layoutCommentsContainer != null) {
                    layoutCommentsContainer.removeAllViews();
                    layoutCommentsContainer.addView(createInfoTextView("Không thể tải bình luận"));
                }

                updateCommentTitle();

                Toast.makeText(PostCommentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindPostPreview(@NonNull CommunityPostModel post) {
        if (txtPreviewAvatar != null) {
            txtPreviewAvatar.setText(getMoodPreviewEmoji(post.getMood_tag()));
        }

        if (txtPreviewContent != null) {
            txtPreviewContent.setText(safeText(post.getContent(), ""));
        }

        updateCommentTitle();
    }

    private void updateCommentTitle() {
        if (txtPostCommentTitle == null) return;

        int count = allComments.size();

        if (count == 0) {
            txtPostCommentTitle.setText("0 bình luận");
        } else if (count == 1) {
            txtPostCommentTitle.setText("1 bình luận");
        } else {
            txtPostCommentTitle.setText(count + " bình luận");
        }
    }

    private void showLoadingComments() {
        if (layoutCommentsContainer == null) return;

        layoutCommentsContainer.removeAllViews();
        layoutCommentsContainer.addView(createInfoTextView("Đang tải bình luận..."));
    }

    private void renderComments() {
        if (layoutCommentsContainer == null) return;

        layoutCommentsContainer.removeAllViews();

        if (allComments.isEmpty()) {
            layoutCommentsContainer.addView(createInfoTextView("Chưa có bình luận nào. Hãy là người đầu tiên động viên nhé."));
            return;
        }

        for (PostCommentModel comment : allComments) {
            View itemView = getLayoutInflater().inflate(
                    R.layout.item_comment,
                    layoutCommentsContainer,
                    false
            );

            bindCommentItem(itemView, comment);
            layoutCommentsContainer.addView(itemView);
        }
    }

    private void bindCommentItem(@NonNull View itemView, @NonNull PostCommentModel comment) {
        TextView txtCommentAvatarEmoji = itemView.findViewById(R.id.txtCommentAvatarEmoji);
        TextView txtCommentBubble = itemView.findViewById(R.id.txtCommentBubble);
        TextView txtCommentTime = itemView.findViewById(R.id.txtCommentTime);

        if (txtCommentAvatarEmoji != null) {
            txtCommentAvatarEmoji.setText(getCommentAvatarEmoji(comment));
        }

        if (txtCommentBubble != null) {
            txtCommentBubble.setText(safeText(comment.getContent(), ""));
        }

        if (txtCommentTime != null) {
            txtCommentTime.setText(formatRelativeTime(comment.getCreated_at()));
        }
    }

    private void submitComment() {
        if (edtPostComment == null) return;

        String content = edtPostComment.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "Hãy nhập lời động viên trước khi gửi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        isSendingComment = true;
        edtPostComment.setEnabled(false);
        updateSendButtonState();

        communityRepository.createPostComment(
                postId,
                content,
                true,
                new CommunityRepository.CreateCommentListener() {
                    @Override
                    public void onSuccess(@NonNull String commentId) {
                        hasCommentChanged = true;

                        isSendingComment = false;
                        edtPostComment.setEnabled(true);
                        edtPostComment.setText("");
                        updateSendButtonState();

                        Toast.makeText(
                                PostCommentActivity.this,
                                "Đã gửi lời động viên",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadComments();
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        isSendingComment = false;
                        edtPostComment.setEnabled(true);
                        updateSendButtonState();

                        Toast.makeText(
                                PostCommentActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private TextView createInfoTextView(String message) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textView.setPadding(dp(10), dp(12), dp(10), dp(12));
        textView.setText(message);
        textView.setTextSize(13f);
        textView.setTextColor(0xFF8E7AA7);
        return textView;
    }

    private String getMoodPreviewEmoji(String moodTag) {
        String mood = safeText(moodTag, "stress");

        switch (mood) {
            case "happy":
                return "😊";
            case "sad":
                return "🥲";
            case "stress":
                return "😮‍💨";
            case "fear":
                return "😟";
            case "disgust":
                return "😣";
            case "angry":
                return "😤";
            default:
                return "🌸";
        }
    }

    private String getCommentAvatarEmoji(PostCommentModel comment) {
        if (comment.isIs_anonymous()) {
            return "🌸";
        }
        return "🙂";
    }

    private String formatRelativeTime(Timestamp timestamp) {
        if (timestamp == null) return "Vừa xong";

        long now = System.currentTimeMillis();
        long time = timestamp.toDate().getTime();
        long diff = Math.max(0L, now - time);

        long minute = 60_000L;
        long hour = 60 * minute;
        long day = 24 * hour;

        if (diff < minute) return "Vừa xong";
        if (diff < hour) return (diff / minute) + " phút trước";
        if (diff < day) return (diff / hour) + " giờ trước";
        return (diff / day) + " ngày trước";
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void finishWithRefreshResult() {
        if (hasCommentChanged) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("refresh_community_feed", true);
            setResult(RESULT_OK, resultIntent);
        }
        finish();
    }
}