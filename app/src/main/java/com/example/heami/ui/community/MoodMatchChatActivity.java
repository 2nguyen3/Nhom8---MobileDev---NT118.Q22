package com.example.heami.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.annotation.NonNull;

import com.example.heami.R;

public class MoodMatchChatActivity extends AppCompatActivity {

    private View btnBackMoodChat;
    private View btnMoreMoodChat;
    private FrameLayout btnSendMoodChat;
    private AppCompatButton btnEndMoodChat;

    private TextView btnReactionHug;
    private TextView btnReactionHeart;
    private TextView btnReactionFlower;
    private TextView btnReactionSparkle;
    private TextView btnReactionPray;

    private EditText edtMoodChatInput;
    private ImageView imgSendMoodChat;

    private TextView txtAvatarEmoji;
    private TextView txtChatName;
    private TextView txtChatMood;
    private TextView txtSafetyBanner;
    private TextView txtSystemCard;

    private boolean isSendActive = false;

    private String roomId = "";
    private String matchId = "";
    private String matchedUserId = "";
    private String matchedUserName = "";
    private String matchedUserAvatar = "";
    private String moodTag = "stress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_match_chat);

        bindViews();
        readIntentData();
        bindMatchUi();
        setupActions();
        setupTypingEffect();
        updateSendButtonState(false);
    }

    private void bindViews() {
        btnBackMoodChat = findViewById(R.id.btnBackMoodChat);
        btnMoreMoodChat = findViewById(R.id.btnMoreMoodChat);
        btnSendMoodChat = findViewById(R.id.btnSendMoodChat);
        btnEndMoodChat = findViewById(R.id.btnEndMoodChat);

        btnReactionHug = findViewById(R.id.btnReactionHug);
        btnReactionHeart = findViewById(R.id.btnReactionHeart);
        btnReactionFlower = findViewById(R.id.btnReactionFlower);
        btnReactionSparkle = findViewById(R.id.btnReactionSparkle);
        btnReactionPray = findViewById(R.id.btnReactionPray);

        edtMoodChatInput = findViewById(R.id.edtMoodChatInput);
        imgSendMoodChat = findViewById(R.id.imgSendMoodChat);

        txtAvatarEmoji = findViewById(R.id.txtAvatarEmoji);
        txtChatName = findViewById(R.id.txtChatName);
        txtChatMood = findViewById(R.id.txtChatMood);
        txtSafetyBanner = findViewById(R.id.txtSafetyBanner);
        txtSystemCard = findViewById(R.id.txtSystemCard);
    }

    private void readIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        roomId = safeText(intent.getStringExtra("room_id"), "");
        matchId = safeText(intent.getStringExtra("match_id"), "");
        matchedUserId = safeText(intent.getStringExtra("matched_user_id"), "");
        matchedUserName = safeText(intent.getStringExtra("matched_user_name"), "Người bạn ẩn danh");
        matchedUserAvatar = safeText(intent.getStringExtra("matched_user_avatar"), "");
        moodTag = safeText(intent.getStringExtra("mood_tag"), "stress").toLowerCase();
    }

    private void bindMatchUi() {
        if (txtChatName != null) {
            txtChatName.setText(resolveDisplayName());
        }

        if (txtChatMood != null) {
            txtChatMood.setText(resolveMoodLabel(moodTag));
        }

        if (txtAvatarEmoji != null) {
            txtAvatarEmoji.setText(resolveAvatarEmoji(moodTag));
        }

        if (txtSafetyBanner != null) {
            txtSafetyBanner.setText("Không gian ẩn danh an toàn — trò chuyện nhẹ nhàng, không phán xét");
        }

        if (txtSystemCard != null) {
            txtSystemCard.setText(
                    "Bạn vừa được kết nối với " + resolveDisplayName()
                            + " qua Mood Match. Hãy bắt đầu bằng một lời chào nhẹ nhàng nhé."
            );
        }

        if (edtMoodChatInput != null) {
            edtMoodChatInput.setHint("Chia sẻ với " + resolveDisplayName() + "...");
        }
    }

    private void setupActions() {
        if (btnBackMoodChat != null) {
            btnBackMoodChat.setOnClickListener(v -> openCommunityChatList());
        }

        if (btnEndMoodChat != null) {
            btnEndMoodChat.setOnClickListener(v -> openCommunityChatList());
        }

        if (btnMoreMoodChat != null) {
            btnMoreMoodChat.setOnClickListener(v -> Toast.makeText(
                    MoodMatchChatActivity.this,
                    "Tùy chọn chat sẽ hoàn thiện ở bước 14 nhé",
                    Toast.LENGTH_SHORT
            ).show());
        }

        if (btnSendMoodChat != null) {
            btnSendMoodChat.setOnClickListener(v -> {
                if (!isSendActive || edtMoodChatInput == null) return;

                String content = edtMoodChatInput.getText() != null
                        ? edtMoodChatInput.getText().toString().trim()
                        : "";

                if (content.isEmpty()) return;

                Toast.makeText(
                        MoodMatchChatActivity.this,
                        "Gửi tin nhắn thật sẽ nối backend ở bước 14",
                        Toast.LENGTH_SHORT
                ).show();

                edtMoodChatInput.setText("");
            });
        }

        if (btnReactionHug != null) {
            btnReactionHug.setOnClickListener(v -> appendReaction("🤗 "));
        }

        if (btnReactionHeart != null) {
            btnReactionHeart.setOnClickListener(v -> appendReaction("💖 "));
        }

        if (btnReactionFlower != null) {
            btnReactionFlower.setOnClickListener(v -> appendReaction("🌸 "));
        }

        if (btnReactionSparkle != null) {
            btnReactionSparkle.setOnClickListener(v -> appendReaction("✨ "));
        }

        if (btnReactionPray != null) {
            btnReactionPray.setOnClickListener(v -> appendReaction("🙏 "));
        }
    }

    private void setupTypingEffect() {
        if (edtMoodChatInput == null) return;

        edtMoodChatInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                updateSendButtonState(hasText);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateSendButtonState(boolean active) {
        if (btnSendMoodChat == null) return;
        if (isSendActive == active) return;

        isSendActive = active;

        if (active) {
            btnSendMoodChat.setBackgroundResource(R.drawable.bg_mood_chat_send_active);

            btnSendMoodChat.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(140)
                    .withEndAction(() -> btnSendMoodChat.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start())
                    .start();

            if (imgSendMoodChat != null) {
                imgSendMoodChat.animate()
                        .rotation(-12f)
                        .setDuration(120)
                        .withEndAction(() -> imgSendMoodChat.animate()
                                .rotation(0f)
                                .setDuration(120)
                                .start())
                        .start();
            }
        } else {
            btnSendMoodChat.setBackgroundResource(R.drawable.bg_mood_chat_send_inactive);

            btnSendMoodChat.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start();

            if (imgSendMoodChat != null) {
                imgSendMoodChat.setRotation(0f);
            }
        }
    }

    private void appendReaction(@NonNull String reactionText) {
        if (edtMoodChatInput == null) return;

        String current = edtMoodChatInput.getText() != null
                ? edtMoodChatInput.getText().toString()
                : "";

        String nextValue = current + reactionText;
        edtMoodChatInput.setText(nextValue);
        edtMoodChatInput.setSelection(nextValue.length());
    }

    private void openCommunityChatList() {
        Intent intent = new Intent(MoodMatchChatActivity.this, CommunityChatListActivity.class);
        intent.putExtra("highlight_room_id", roomId);
        intent.putExtra("highlight_match_id", matchId);
        intent.putExtra("highlight_user_id", matchedUserId);
        intent.putExtra("highlight_user_name", matchedUserName);
        intent.putExtra("highlight_user_avatar", matchedUserAvatar);
        intent.putExtra("highlight_mood_tag", moodTag);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @NonNull
    private String resolveDisplayName() {
        if (!TextUtils.isEmpty(matchedUserName)) {
            return matchedUserName;
        }
        return "Người bạn ẩn danh";
    }

    @NonNull
    private String resolveMoodLabel(@NonNull String moodTag) {
        switch (moodTag) {
            case "happy":
                return "Đang vui";
            case "sad":
                return "Đang buồn";
            case "stress":
                return "Đang stress";
            case "fear":
                return "Đang lo lắng";
            case "angry":
                return "Đang khó chịu";
            default:
                return "Đang chia sẻ";
        }
    }

    @NonNull
    private String resolveAvatarEmoji(@NonNull String moodTag) {
        switch (moodTag) {
            case "happy":
                return "😊";
            case "sad":
                return "🥺";
            case "stress":
                return "🫠";
            case "fear":
                return "😟";
            case "angry":
                return "😤";
            default:
                return "🌸";
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