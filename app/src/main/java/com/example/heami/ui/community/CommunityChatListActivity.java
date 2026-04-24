package com.example.heami.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class CommunityChatListActivity extends AppCompatActivity {

    private ImageButton btnBackCommunityChatList;

    private TextView tabCommunityChatAll;
    private TextView tabCommunityChatUnread;

    private View itemChatFox;
    private TextView avatarChatFox;
    private TextView txtChatFoxName;
    private TextView txtChatFoxTime;
    private TextView txtChatFoxLastMessage;
    private TextView badgeChatFoxUnread;

    private String highlightRoomId = "";
    private String highlightMatchId = "";
    private String highlightUserId = "";
    private String highlightUserName = "";
    private String highlightUserAvatar = "";
    private String highlightMoodTag = "stress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat_list);

        bindViews();
        readIntentData();
        bindHighlightRoomIfNeeded();
        setupActions();
    }

    private void bindViews() {
        btnBackCommunityChatList = findViewById(R.id.btnBackCommunityChatList);

        tabCommunityChatAll = findViewById(R.id.tabCommunityChatAll);
        tabCommunityChatUnread = findViewById(R.id.tabCommunityChatUnread);

        itemChatFox = findViewById(R.id.itemChatFox);
        avatarChatFox = findViewById(R.id.avatarChatFox);
        txtChatFoxName = findViewById(R.id.txtChatFoxName);
        txtChatFoxTime = findViewById(R.id.txtChatFoxTime);
        txtChatFoxLastMessage = findViewById(R.id.txtChatFoxLastMessage);
        badgeChatFoxUnread = findViewById(R.id.badgeChatFoxUnread);
    }

    private void readIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        highlightRoomId = safeText(intent.getStringExtra("highlight_room_id"), "");
        highlightMatchId = safeText(intent.getStringExtra("highlight_match_id"), "");
        highlightUserId = safeText(intent.getStringExtra("highlight_user_id"), "");
        highlightUserName = safeText(intent.getStringExtra("highlight_user_name"), "");
        highlightUserAvatar = safeText(intent.getStringExtra("highlight_user_avatar"), "");
        highlightMoodTag = safeText(intent.getStringExtra("highlight_mood_tag"), "stress").toLowerCase();
    }

    private void bindHighlightRoomIfNeeded() {
        if (highlightRoomId.isEmpty()) {
            return;
        }

        if (avatarChatFox != null) {
            avatarChatFox.setText(resolveAvatarEmoji(highlightMoodTag));
        }

        if (txtChatFoxName != null) {
            txtChatFoxName.setText(
                    highlightUserName.isEmpty() ? "Người bạn vừa match" : highlightUserName
            );
        }

        if (txtChatFoxTime != null) {
            txtChatFoxTime.setText("Vừa xong");
        }

        if (txtChatFoxLastMessage != null) {
            txtChatFoxLastMessage.setText("Bạn vừa được kết nối qua Mood Match");
        }

        if (badgeChatFoxUnread != null) {
            badgeChatFoxUnread.setVisibility(View.GONE);
        }

        if (tabCommunityChatAll != null) {
            tabCommunityChatAll.setText("Tất cả  (1)");
        }

        if (tabCommunityChatUnread != null) {
            tabCommunityChatUnread.setText("Chưa đọc  (0)");
        }
    }

    private void setupActions() {
        if (btnBackCommunityChatList != null) {
            btnBackCommunityChatList.setOnClickListener(v -> finish());
        }

        if (itemChatFox != null) {
            itemChatFox.setOnClickListener(v -> {
                if (highlightRoomId.isEmpty()) {
                    return;
                }

                Intent intent = new Intent(CommunityChatListActivity.this, MoodMatchChatActivity.class);
                intent.putExtra("room_id", highlightRoomId);
                intent.putExtra("match_id", highlightMatchId);
                intent.putExtra("matched_user_id", highlightUserId);
                intent.putExtra("matched_user_name", highlightUserName);
                intent.putExtra("matched_user_avatar", highlightUserAvatar);
                intent.putExtra("mood_tag", highlightMoodTag);
                startActivity(intent);
            });
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