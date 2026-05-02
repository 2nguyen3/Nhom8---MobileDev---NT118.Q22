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
import android.os.Handler;
import android.os.Looper;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MoodMatchChatActivity extends AppCompatActivity {

    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

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
    private FirebaseDatabase realtimeDb;

    private TextView txtPartnerPresence;
    private TextView txtTypingIndicator;

    private DatabaseReference partnerStatusRef;
    private DatabaseReference roomTypingRef;
    private DatabaseReference myTypingRef;
    private DatabaseReference partnerTypingRef;

    private ValueEventListener partnerPresenceListener;
    private ValueEventListener partnerTypingListener;

    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private final Runnable stopTypingRunnable = () -> setMyTyping(false);

    private MoodMatchRepository moodMatchRepository;

    private ListenerRegistration messageListener;
    private ListenerRegistration roomStatusListener;

    private MoodMatchMessageAdapter messageAdapter;
    private String currentRoomStatus = "ACTIVE";
    private com.google.firebase.Timestamp roomPurgeAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_match_chat);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance(RTDB_URL);
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
        startPartnerPresenceListener();
        startPartnerTypingListener();
        startMessageListener();
        resetMyUnreadCount();
        applyRoomStatusUi();
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

        txtPartnerPresence = findViewById(R.id.txtPartnerPresence);
        txtTypingIndicator = findViewById(R.id.txtTypingIndicator);
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
        currentRoomStatus = safeText(intent.getStringExtra("room_status"), "ACTIVE");
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
                matchedUserId,
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
                handleTypingStateChanged(hasText);
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
                    markIncomingMessagesDelivered(messages);
                    markIncomingMessagesSeen(messages);
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
        if (!"ACTIVE".equals(currentRoomStatus)) {
            Toast.makeText(this, "Cuộc trò chuyện này đã kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCurrentRoomExpired()) {
            Toast.makeText(this, "Cuộc trò chuyện này đã hết thời gian lưu", Toast.LENGTH_SHORT).show();
            openCommunityChatList();
            return;
        }

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
        messageData.put("delivered_user_ids", new java.util.ArrayList<String>());
        messageData.put("seen_user_ids", new java.util.ArrayList<String>());

        DocumentReference roomRef = firestore.collection("chat_rooms").document(roomId);

        HashMap<String, Object> roomUpdates = new HashMap<>();
        roomUpdates.put("last_message", content);
        roomUpdates.put("last_message_at", FieldValue.serverTimestamp());
        roomUpdates.put("last_sender_id", currentUserId);
        roomUpdates.put("unread_count_map." + currentUserId, 0L);
        roomUpdates.put("last_message_id", messageId);

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

                        typingHandler.removeCallbacks(stopTypingRunnable);
                        setMyTyping(false);
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

    private void markIncomingMessagesDelivered(@NonNull List<ChatMessageModel> messages) {
        if (roomId.isEmpty() || currentUserId.isEmpty()) {
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        boolean hasUpdate = false;

        for (ChatMessageModel message : messages) {
            String senderId = safeText(message.getSender_id(), "");
            String messageId = safeText(message.getMessage_id(), "");

            if (senderId.isEmpty() || senderId.equals(currentUserId) || messageId.isEmpty()) {
                continue;
            }

            List<String> deliveredIds = message.getDelivered_user_ids();
            boolean alreadyDelivered = deliveredIds != null && deliveredIds.contains(currentUserId);
            if (alreadyDelivered) {
                continue;
            }

            batch.update(
                    firestore.collection("chat_rooms")
                            .document(roomId)
                            .collection("messages")
                            .document(messageId),
                    "delivered_user_ids", FieldValue.arrayUnion(currentUserId)
            );
            hasUpdate = true;
        }

        if (hasUpdate) {
            batch.commit();
        }
    }

    private void markIncomingMessagesSeen(@NonNull List<ChatMessageModel> messages) {
        if (roomId.isEmpty() || currentUserId.isEmpty()) {
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        boolean hasUpdate = false;

        for (ChatMessageModel message : messages) {
            String senderId = safeText(message.getSender_id(), "");
            String messageId = safeText(message.getMessage_id(), "");

            if (senderId.isEmpty() || senderId.equals(currentUserId) || messageId.isEmpty()) {
                continue;
            }

            List<String> seenIds = message.getSeen_user_ids();
            boolean alreadySeen = seenIds != null && seenIds.contains(currentUserId);
            if (alreadySeen) {
                continue;
            }

            batch.update(
                    firestore.collection("chat_rooms")
                            .document(roomId)
                            .collection("messages")
                            .document(messageId),
                    "seen_user_ids", FieldValue.arrayUnion(currentUserId)
            );
            hasUpdate = true;
        }

        if (hasUpdate) {
            batch.commit();
        }
    }

    private void handleTypingStateChanged(boolean hasText) {
        if (!"ACTIVE".equals(currentRoomStatus)) {
            return;
        }

        if (hasText) {
            setMyTyping(true);
            typingHandler.removeCallbacks(stopTypingRunnable);
            typingHandler.postDelayed(stopTypingRunnable, 1500L);
        } else {
            typingHandler.removeCallbacks(stopTypingRunnable);
            setMyTyping(false);
        }
    }

    private void setMyTyping(boolean typing) {
        if (roomId.isEmpty() || currentUserId.isEmpty()) {
            return;
        }

        if (myTypingRef == null) {
            roomTypingRef = realtimeDb.getReference("typing").child(roomId);
            myTypingRef = roomTypingRef.child(currentUserId);
        }

        myTypingRef.setValue(typing);
    }

    private void startPartnerTypingListener() {
        if (roomId.isEmpty() || matchedUserId.isEmpty()) {
            return;
        }

        roomTypingRef = realtimeDb.getReference("typing").child(roomId);
        myTypingRef = roomTypingRef.child(currentUserId);
        partnerTypingRef = roomTypingRef.child(matchedUserId);

        partnerTypingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isTyping = snapshot.getValue(Boolean.class);
                boolean typing = Boolean.TRUE.equals(isTyping) && "ACTIVE".equals(currentRoomStatus);

                if (txtTypingIndicator != null) {
                    txtTypingIndicator.setVisibility(typing ? View.VISIBLE : View.GONE);
                    txtTypingIndicator.setText(resolveDisplayName() + " đang nhập...");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        partnerTypingRef.addValueEventListener(partnerTypingListener);
    }

    private void startPartnerPresenceListener() {
        if (matchedUserId.isEmpty()) {
            return;
        }

        partnerStatusRef = realtimeDb.getReference("status").child(matchedUserId);

        partnerPresenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot connectionsSnapshot = snapshot.child("connections");
                Boolean isForeground = snapshot.child("isForeground").getValue(Boolean.class);

                boolean hasConnections =
                        connectionsSnapshot.exists() && connectionsSnapshot.getChildrenCount() > 0;

                boolean online = hasConnections && Boolean.TRUE.equals(isForeground);

                if (txtPartnerPresence != null) {
                    txtPartnerPresence.setText(online ? "Đang hoạt động" : "Đang offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (txtPartnerPresence != null) {
                    txtPartnerPresence.setText("Đang offline");
                }
            }
        };

        partnerStatusRef.addValueEventListener(partnerPresenceListener);
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

                    roomPurgeAt = snapshot.getTimestamp("purge_at");
                    currentRoomStatus = safeText(snapshot.getString("status"), "ACTIVE");

                    if (isCurrentRoomExpired()) {
                        Toast.makeText(
                                MoodMatchChatActivity.this,
                                "Cuộc trò chuyện này đã hết thời gian lưu",
                                Toast.LENGTH_SHORT
                        ).show();
                        openCommunityChatList();
                        return;
                    }

                    applyRoomStatusUi();
                });
    }

    private boolean isCurrentRoomExpired() {
        return roomPurgeAt != null && roomPurgeAt.toDate().getTime() <= System.currentTimeMillis();
    }

    private void applyRoomStatusUi() {
        boolean isEnded = !"ACTIVE".equals(currentRoomStatus);

        if (edtMoodChatInput != null) {
            edtMoodChatInput.setEnabled(!isEnded);
            edtMoodChatInput.setAlpha(isEnded ? 0.65f : 1f);
            edtMoodChatInput.setHint(
                    isEnded
                            ? "Cuộc trò chuyện này đã kết thúc"
                            : "Chia sẻ với " + resolveDisplayName() + "..."
            );
        }

        if (btnSendMoodChat != null) {
            btnSendMoodChat.setEnabled(!isEnded);
            btnSendMoodChat.setAlpha(isEnded ? 0.45f : 1f);
        }

        if (btnReactionHug != null) btnReactionHug.setEnabled(!isEnded);
        if (btnReactionHeart != null) btnReactionHeart.setEnabled(!isEnded);
        if (btnReactionFlower != null) btnReactionFlower.setEnabled(!isEnded);
        if (btnReactionSparkle != null) btnReactionSparkle.setEnabled(!isEnded);
        if (btnReactionPray != null) btnReactionPray.setEnabled(!isEnded);

        if (txtSafetyBanner != null) {
            txtSafetyBanner.setText(
                    isEnded
                            ? "Cuộc trò chuyện này đã kết thúc — bạn có thể xem lại lịch sử trong 1 giờ"
                            : "Không gian ẩn danh an toàn — trò chuyện nhẹ nhàng, không phán xét"
            );
        }

        if (txtSystemCard != null) {
            txtSystemCard.setText(
                    isEnded
                            ? "Kết nối Mood Match này đã kết thúc. Heami sẽ giữ lịch sử trong 1 giờ trước khi ẩn khỏi danh sách chat."
                            : "Bạn vừa được kết nối với " + resolveDisplayName()
                            + " qua Mood Match. Hãy bắt đầu bằng một lời chào nhẹ nhàng nhé."
            );
        }

        if (txtTypingIndicator != null && isEnded) {
            txtTypingIndicator.setVisibility(View.GONE);
        }
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
    protected void onStop() {
        super.onStop();
        typingHandler.removeCallbacks(stopTypingRunnable);
        setMyTyping(false);
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

        typingHandler.removeCallbacks(stopTypingRunnable);
        setMyTyping(false);

        if (partnerStatusRef != null && partnerPresenceListener != null) {
            partnerStatusRef.removeEventListener(partnerPresenceListener);
            partnerPresenceListener = null;
        }

        if (partnerTypingRef != null && partnerTypingListener != null) {
            partnerTypingRef.removeEventListener(partnerTypingListener);
            partnerTypingListener = null;
        }
    }
}