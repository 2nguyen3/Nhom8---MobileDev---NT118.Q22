package com.example.heami.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ChatMessageModel;
import com.example.heami.data.repositories.MoodMatchRepository;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

    private RecyclerView rvMoodChatMessages;

    private View layoutMoodChatEmptyState;
    private View progressMoodChatLoading;

    private boolean isSendActive = false;
    private boolean isSendingMessage = false;
    private boolean isEndingChat = false;

    private String roomId = "";
    private String matchId = "";
    private String matchedUserId = "";
    private String matchedUserName = "";
    private String matchedUserAvatar = "";
    private String moodTag = "stress";
    private String currentUserId = "";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private MoodMatchRepository moodMatchRepository;

    private ListenerRegistration messageListener;
    private ListenerRegistration roomStatusListener;

    private MoodMatchMessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_match_chat);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        moodMatchRepository = new MoodMatchRepository();

        if (auth.getCurrentUser() != null) {
            currentUserId = safeText(auth.getCurrentUser().getUid(), "");
        }

        bindViews();
        readIntentData();
        bindMatchUi();
        setupRecyclerView();
        setupActions();
        setupTypingEffect();
        updateSendButtonState(false);
        startRoomStatusListener();
        startMessageListener();
        resetMyUnreadCount();
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

        rvMoodChatMessages = findViewById(R.id.rvMoodChatMessages);
        layoutMoodChatEmptyState = findViewById(R.id.layoutMoodChatEmptyState);
        progressMoodChatLoading = findViewById(R.id.progressMoodChatLoading);
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

    private void setupRecyclerView() {
        messageAdapter = new MoodMatchMessageAdapter(
                currentUserId,
                resolveAvatarEmoji(moodTag)
        );

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        rvMoodChatMessages.setLayoutManager(layoutManager);
        rvMoodChatMessages.setAdapter(messageAdapter);
    }

    private void setupActions() {
        if (btnBackMoodChat != null) {
            btnBackMoodChat.setOnClickListener(v -> openCommunityChatList());
        }

        if (btnEndMoodChat != null) {
            btnEndMoodChat.setOnClickListener(v -> endCurrentMoodMatch());
        }

        if (btnMoreMoodChat != null) {
            btnMoreMoodChat.setOnClickListener(v -> Toast.makeText(
                    MoodMatchChatActivity.this,
                    "Tùy chọn chat sẽ hoàn thiện thêm sau nhé",
                    Toast.LENGTH_SHORT
            ).show());
        }

        if (btnSendMoodChat != null) {
            btnSendMoodChat.setOnClickListener(v -> sendMessage());
        }

        if (btnReactionHug != null) {
            btnReactionHug.setOnClickListener(v -> appendReaction("🤗 "));
        }

        if (btnReactionHeart != null) {
            btnReactionHeart.setOnClickListener(v -> appendReaction("💙 "));
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

    private void startMessageListener() {
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Thiếu room_id để mở cuộc trò chuyện", Toast.LENGTH_SHORT).show();
            updateMessageUiState(false, 0);
            return;
        }

        updateMessageUiState(true, 0);

        messageListener = firestore.collection("chat_rooms")
                .document(roomId)
                .collection("messages")
                .orderBy("created_at")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        updateMessageUiState(false, 0);
                        Toast.makeText(
                                MoodMatchChatActivity.this,
                                "Không thể tải tin nhắn lúc này",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    List<ChatMessageModel> messages = new ArrayList<>();

                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            ChatMessageModel message = document.toObject(ChatMessageModel.class);
                            if (message == null) continue;

                            if (message.getMessage_id() == null || message.getMessage_id().trim().isEmpty()) {
                                message.setMessage_id(document.getId());
                            }

                            String status = safeText(message.getStatus(), "ACTIVE");
                            if (!"ACTIVE".equals(status)) {
                                continue;
                            }

                            messages.add(message);
                        }
                    }

                    sortMessagesByTime(messages);
                    updateMessageUiState(false, messages.size());
                    messageAdapter.submitList(messages);

                    resetMyUnreadCount();

                    if (!messages.isEmpty()) {
                        rvMoodChatMessages.post(() ->
                                rvMoodChatMessages.scrollToPosition(messages.size() - 1)
                        );
                    }
                });
    }

    private void sendMessage() {
        if (!isSendActive || edtMoodChatInput == null) return;
        if (roomId.isEmpty()) return;
        if (currentUserId.isEmpty()) return;
        if (isSendingMessage) return;

        String content = edtMoodChatInput.getText() != null
                ? edtMoodChatInput.getText().toString().trim()
                : "";

        if (content.isEmpty()) return;

        isSendingMessage = true;
        btnSendMoodChat.setEnabled(false);

        String messageId = firestore.collection("chat_rooms")
                .document(roomId)
                .collection("messages")
                .document()
                .getId();

        long clientCreatedAtMs = System.currentTimeMillis();

        HashMap<String, Object> messageData = new HashMap<>();
        messageData.put("message_id", messageId);
        messageData.put("sender_id", currentUserId);
        messageData.put("text", content);
        messageData.put("created_at", FieldValue.serverTimestamp());
        messageData.put("client_created_at_ms", clientCreatedAtMs);
        messageData.put("message_type", "TEXT");
        messageData.put("status", "ACTIVE");

        DocumentReference roomRef = firestore.collection("chat_rooms").document(roomId);

        HashMap<String, Object> roomUpdates = new HashMap<>();
        roomUpdates.put("last_message", content);
        roomUpdates.put("last_message_at", FieldValue.serverTimestamp());
        roomUpdates.put("last_sender_id", currentUserId);
        roomUpdates.put("unread_count_map." + currentUserId, 0L);

        roomRef.get().addOnSuccessListener(roomSnapshot -> {
            Object rawMemberIds = roomSnapshot.get("member_ids");

            if (rawMemberIds instanceof List<?>) {
                for (Object item : (List<?>) rawMemberIds) {
                    if (!(item instanceof String)) continue;

                    String memberId = safeText((String) item, "");
                    if (memberId.isEmpty() || memberId.equals(currentUserId)) {
                        continue;
                    }

                    roomUpdates.put(
                            "unread_count_map." + memberId,
                            FieldValue.increment(1)
                    );
                }
            }

            WriteBatch batch = firestore.batch();

            batch.set(
                    firestore.collection("chat_rooms")
                            .document(roomId)
                            .collection("messages")
                            .document(messageId),
                    messageData
            );

            batch.update(roomRef, roomUpdates);

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        edtMoodChatInput.setText("");
                        isSendingMessage = false;
                        btnSendMoodChat.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        isSendingMessage = false;
                        btnSendMoodChat.setEnabled(true);

                        Toast.makeText(
                                MoodMatchChatActivity.this,
                                "Gửi tin nhắn chưa thành công, thử lại nhé",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
        }).addOnFailureListener(e -> {
            isSendingMessage = false;
            btnSendMoodChat.setEnabled(true);

            Toast.makeText(
                    MoodMatchChatActivity.this,
                    "Không thể đọc thông tin phòng chat",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void endCurrentMoodMatch() {
        if (isEndingChat) return;
        isEndingChat = true;

        if (matchId.isEmpty() || roomId.isEmpty()) {
            openCommunityChatList();
            return;
        }

        moodMatchRepository.endMoodMatch(matchId, roomId, "USER_ENDED", new MoodMatchRepository.SimpleActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(
                        MoodMatchChatActivity.this,
                        "Đã kết thúc cuộc trò chuyện",
                        Toast.LENGTH_SHORT
                ).show();
                openCommunityChatList();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                isEndingChat = false;
                Toast.makeText(
                        MoodMatchChatActivity.this,
                        "Chưa thể kết thúc cuộc trò chuyện lúc này",
                        Toast.LENGTH_SHORT
                ).show();
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

    private void startRoomStatusListener() {
        if (roomId.isEmpty()) {
            return;
        }

        roomStatusListener = firestore.collection("chat_rooms")
                .document(roomId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    String status = safeText(snapshot.getString("status"), "ACTIVE");

                    if (!"ACTIVE".equals(status) && !isEndingChat) {
                        Toast.makeText(
                                MoodMatchChatActivity.this,
                                "Cuộc trò chuyện này đã kết thúc",
                                Toast.LENGTH_SHORT
                        ).show();
                        openCommunityChatList();
                    }
                });
    }

    private void updateMessageUiState(boolean isLoading, int messageCount) {
        if (progressMoodChatLoading != null) {
            progressMoodChatLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        boolean isEmpty = !isLoading && messageCount == 0;

        if (layoutMoodChatEmptyState != null) {
            layoutMoodChatEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        if (rvMoodChatMessages != null) {
            rvMoodChatMessages.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void sortMessagesByTime(@NonNull List<ChatMessageModel> messages) {
        Collections.sort(messages, (left, right) -> {
            long leftTime = resolveMessageSortTime(left);
            long rightTime = resolveMessageSortTime(right);

            int compareTime = Long.compare(leftTime, rightTime);
            if (compareTime != 0) {
                return compareTime;
            }

            String leftId = safeText(left.getMessage_id(), "");
            String rightId = safeText(right.getMessage_id(), "");
            return leftId.compareTo(rightId);
        });
    }

    private long resolveMessageSortTime(@NonNull ChatMessageModel message) {
        if (message.getCreated_at() != null) {
            return message.getCreated_at().toDate().getTime();
        }

        if (message.getClient_created_at_ms() != null) {
            return message.getClient_created_at_ms();
        }

        return 0L;
    }

    @NonNull
    private String resolveDisplayName() {
        if (!TextUtils.isEmpty(matchedUserName)) {
            return matchedUserName;
        }
        return "Người bạn ẩn danh";
    }

    private void resetMyUnreadCount() {
        if (roomId.isEmpty() || currentUserId.isEmpty()) {
            return;
        }

        firestore.collection("chat_rooms")
                .document(roomId)
                .update("unread_count_map." + currentUserId, 0L);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        if (roomStatusListener != null) {
            roomStatusListener.remove();
            roomStatusListener = null;
        }
    }
}