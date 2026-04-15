package com.example.heami.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.heami.R;

public class MoodMatchChatActivity extends AppCompatActivity {

    private ImageButton btnBackMoodChat;
    private FrameLayout btnSendMoodChat;
    private AppCompatButton btnEndMoodChat;

    private TextView btnReactionHug;
    private TextView btnReactionHeart;
    private TextView btnReactionFlower;
    private TextView btnReactionSparkle;
    private TextView btnReactionPray;

    private EditText edtMoodChatInput;
    private ImageView imgSendMoodChat;

    private boolean isSendActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_match_chat);

        bindViews();
        setupActions();
        setupTypingEffect();
        updateSendButtonState(false);
    }

    private void bindViews() {
        btnBackMoodChat = findViewById(R.id.btnBackMoodChat);
        btnSendMoodChat = findViewById(R.id.btnSendMoodChat);
        btnEndMoodChat = findViewById(R.id.btnEndMoodChat);

        btnReactionHug = findViewById(R.id.btnReactionHug);
        btnReactionHeart = findViewById(R.id.btnReactionHeart);
        btnReactionFlower = findViewById(R.id.btnReactionFlower);
        btnReactionSparkle = findViewById(R.id.btnReactionSparkle);
        btnReactionPray = findViewById(R.id.btnReactionPray);

        edtMoodChatInput = findViewById(R.id.edtMoodChatInput);
        imgSendMoodChat = findViewById(R.id.imgSendMoodChat);
    }

    private void setupActions() {
        btnBackMoodChat.setOnClickListener(v -> {
            Intent intent = new Intent(MoodMatchChatActivity.this, CommunityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnEndMoodChat.setOnClickListener(v -> {
            Intent intent = new Intent(MoodMatchChatActivity.this, CommunityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnSendMoodChat.setOnClickListener(v -> {
            // Tạm thời chưa gửi tin nhắn thật
        });

        btnReactionHug.setOnClickListener(v -> {
            // để trống tạm
        });

        btnReactionHeart.setOnClickListener(v -> {
            // để trống tạm
        });

        btnReactionFlower.setOnClickListener(v -> {
            // để trống tạm
        });

        btnReactionSparkle.setOnClickListener(v -> {
            // để trống tạm
        });

        btnReactionPray.setOnClickListener(v -> {
            // để trống tạm
        });
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
}