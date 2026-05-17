package com.example.heami.ui.community;

import com.example.heami.utils.PresenceUtils;

import com.example.heami.data.repositories.MoodMatchRepository;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ChatRoomModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.text.Normalizer;
import java.util.Locale;

public class CommunityChatListActivity extends AppCompatActivity {

    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

    private ImageButton btnBackCommunityChatList;
    private TextView tabCommunityChatAll;
    private TextView tabCommunityChatUnread;
    private EditText edtCommunityChatSearch;

    private RecyclerView rvCommunityChatRooms;
    private View layoutCommunityChatEmptyState;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseDatabase realtimeDb;

    private ListenerRegistration roomListener;
    private DatabaseReference statusRootRef;
    private ValueEventListener partnerOnlineListener;

    private String currentUserId = "";

    private final List<ChatRoomModel> fullRoomList = new ArrayList<>();
    private final List<ChatRoomModel> filteredRoomList = new ArrayList<>();
    private final Set<String> onlineUserIds = new HashSet<>();

    private CommunityChatRoomAdapter adapter;

    private final android.os.Handler purgeHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private final Runnable purgeRunnable = this::applyFilters;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_UNREAD = 1;
    private static final int FILTER_ARCHIVED = 2;

    private int currentFilterMode = FILTER_ALL;

    private TextView tabCommunityChatArchived;

    private MoodMatchRepository moodMatchRepository;

    private TextView txtCommunityChatEmptyTitle;
    private TextView txtCommunityChatEmptySubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat_list);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance(RTDB_URL);

        FirebaseUser currentUser = auth.getCurrentUser();
        currentUserId = currentUser != null ? safeText(currentUser.getUid(), "") : "";

        moodMatchRepository = new MoodMatchRepository();
        bindViews();
        setupRecyclerView();
        setupActions();
    }

    @Override
    protected void onStart() {
        super.onStart();

        moodMatchRepository.cleanupCurrentUserCommunityGarbage(
                new MoodMatchRepository.SimpleActionListener() {
                    @Override
                    public void onSuccess() {
                        startRealtimeRoomListener();
                        startPartnerOnlineListener();
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        startRealtimeRoomListener();
                        startPartnerOnlineListener();
                    }
                }
        );
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (roomListener != null) {
            roomListener.remove();
            roomListener = null;
        }

        if (statusRootRef != null && partnerOnlineListener != null) {
            statusRootRef.removeEventListener(partnerOnlineListener);
            partnerOnlineListener = null;
        }
        purgeHandler.removeCallbacks(purgeRunnable);
    }

    private void bindViews() {
        btnBackCommunityChatList = findViewById(R.id.btnBackCommunityChatList);
        tabCommunityChatAll = findViewById(R.id.tabCommunityChatAll);
        tabCommunityChatUnread = findViewById(R.id.tabCommunityChatUnread);
        edtCommunityChatSearch = findViewById(R.id.edtCommunityChatSearch);
        rvCommunityChatRooms = findViewById(R.id.rvCommunityChatRooms);
        layoutCommunityChatEmptyState = findViewById(R.id.layoutCommunityChatEmptyState);
        tabCommunityChatArchived = findViewById(R.id.tabCommunityChatArchived);
        txtCommunityChatEmptyTitle = findViewById(R.id.txtCommunityChatEmptyTitle);
        txtCommunityChatEmptySubtitle = findViewById(R.id.txtCommunityChatEmptySubtitle);
    }

    private void setupRecyclerView() {
        adapter = new CommunityChatRoomAdapter(currentUserId, new CommunityChatRoomAdapter.OnChatRoomClickListener() {
            @Override
            public void onChatRoomClick(@NonNull ChatRoomModel room) {
                openChatRoom(room);
            }

            @Override
            public void onChatRoomLongClick(@NonNull ChatRoomModel room, @NonNull View anchor) {
                showRoomActionsMenu(room, anchor);
            }
        });

        rvCommunityChatRooms.setLayoutManager(new LinearLayoutManager(this));
        rvCommunityChatRooms.setAdapter(adapter);
        attachSwipeActions();
    }

    private void setupActions() {
        if (btnBackCommunityChatList != null) {
            btnBackCommunityChatList.setOnClickListener(v -> finish());
        }

        if (tabCommunityChatAll != null) {
            tabCommunityChatAll.setOnClickListener(v -> {
                currentFilterMode = FILTER_ALL;
                updateTabUi();
                applyFilters();
            });
        }

        if (tabCommunityChatUnread != null) {
            tabCommunityChatUnread.setOnClickListener(v -> {
                currentFilterMode = FILTER_UNREAD;
                updateTabUi();
                applyFilters();
            });
        }

        if (tabCommunityChatArchived != null) {
            tabCommunityChatArchived.setOnClickListener(v -> {
                currentFilterMode = FILTER_ARCHIVED;
                updateTabUi();
                applyFilters();
            });
        }

        if (edtCommunityChatSearch != null) {
            edtCommunityChatSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        updateTabUi();
    }

    private boolean isPinned(@NonNull ChatRoomModel room) {
        return room.getPinned_by_map() != null
                && Boolean.TRUE.equals(room.getPinned_by_map().get(currentUserId));
    }

    private boolean isMuted(@NonNull ChatRoomModel room) {
        return room.getMuted_by_map() != null
                && Boolean.TRUE.equals(room.getMuted_by_map().get(currentUserId));
    }

    private boolean isArchived(@NonNull ChatRoomModel room) {
        return room.getArchived_by_map() != null
                && Boolean.TRUE.equals(room.getArchived_by_map().get(currentUserId));
    }

    private void startRealtimeRoomListener() {
        if (currentUserId.isEmpty()) {
            applyFilters();
            return;
        }

        if (roomListener != null) {
            roomListener.remove();
            roomListener = null;
        }

        roomListener = firestore.collection("chat_rooms")
                .whereArrayContains("member_ids", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        fullRoomList.clear();
                        applyFilters();
                        return;
                    }

                    fullRoomList.clear();

                    if (value != null) {
                        value.getDocuments().forEach(document -> {
                            ChatRoomModel room = document.toObject(ChatRoomModel.class);
                            if (room == null) return;

                            if (room.getRoom_id() == null || room.getRoom_id().trim().isEmpty()) {
                                room.setRoom_id(document.getId());
                            }

                            if (!shouldShowRoom(room)) {
                                return;
                            }

                            fullRoomList.add(room);
                            markLastMessageDeliveredIfNeeded(room);
                        });
                    }

                    sortRoomsNewestFirst(fullRoomList);
                    applyFilters();
                });
    }

    private void startPartnerOnlineListener() {
        if (currentUserId.isEmpty()) {
            return;
        }

        if (statusRootRef == null) {
            statusRootRef = realtimeDb.getReference("status");
        }

        if (partnerOnlineListener != null) {
            statusRootRef.removeEventListener(partnerOnlineListener);
        }

        partnerOnlineListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                onlineUserIds.clear();
                long now = System.currentTimeMillis();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String uid = userSnapshot.getKey();
                    if (uid == null || uid.equals(currentUserId)) {
                        continue;
                    }

                    if (PresenceUtils.isUserOnlineFromConnections(userSnapshot, now)) {
                        onlineUserIds.add(uid);
                    }
                }

                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onlineUserIds.clear();
                applyFilters();
            }
        };

        statusRootRef.addValueEventListener(partnerOnlineListener);
    }

    private void sortRoomsNewestFirst(@NonNull List<ChatRoomModel> roomList) {
        Collections.sort(roomList, (left, right) -> {
            boolean leftPinned = isPinned(left);
            boolean rightPinned = isPinned(right);

            if (leftPinned != rightPinned) {
                return leftPinned ? -1 : 1;
            }

            long leftTime = resolveRoomSortTime(left);
            long rightTime = resolveRoomSortTime(right);
            return Long.compare(rightTime, leftTime);
        });
    }

    private long resolveRoomSortTime(@NonNull ChatRoomModel room) {
        if (room.getLast_message_at() != null) {
            return room.getLast_message_at().toDate().getTime();
        }
        if (room.getCreated_at() != null) {
            return room.getCreated_at().toDate().getTime();
        }
        return 0L;
    }

    private void applyFilters() {
        filteredRoomList.clear();

        String keyword = edtCommunityChatSearch != null && edtCommunityChatSearch.getText() != null
                ? normalizeForSearch(edtCommunityChatSearch.getText().toString())
                : "";

        for (ChatRoomModel room : fullRoomList) {
            boolean archived = isArchived(room);

            if (currentFilterMode == FILTER_ARCHIVED) {
                if (!archived) {
                    continue;
                }
            } else {
                if (archived) {
                    continue;
                }

                if (currentFilterMode == FILTER_UNREAD && !isUnreadLike(room)) {
                    continue;
                }
            }

            if (!keyword.isEmpty() && !matchesKeyword(room, keyword)) {
                continue;
            }

            filteredRoomList.add(room);
        }

        adapter.submitList(new ArrayList<>(filteredRoomList));
        adapter.setOnlineUserIds(new HashSet<>(onlineUserIds));
        updateCounts();
        updateEmptyState();
        scheduleNextPurgeRefresh();
    }

    private boolean matchesKeyword(@NonNull ChatRoomModel room, @NonNull String keyword) {
        String partnerName = normalizeForSearch(resolvePartnerName(room));
        String lastMessage = normalizeForSearch(safeText(room.getLast_message(), ""));
        String searchBlob = normalizeForSearch(safeText(room.getSearch_blob(), ""));
        String stateKeywords = normalizeForSearch(buildRoomSearchStateText(room));

        return partnerName.contains(keyword)
                || lastMessage.contains(keyword)
                || searchBlob.contains(keyword)
                || stateKeywords.contains(keyword);
    }

    @NonNull
    private String normalizeForSearch(String raw) {
        String value = safeText(raw, "").toLowerCase(Locale.getDefault()).trim();
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }

    @NonNull
    private String buildRoomSearchStateText(@NonNull ChatRoomModel room) {
        StringBuilder builder = new StringBuilder();

        if (isPinned(room)) {
            builder.append(" ghim");
        }

        if (isMuted(room)) {
            builder.append(" tat thong bao");
        }

        if (isArchived(room)) {
            builder.append(" luu tru");
        }

        String status = safeText(room.getStatus(), "ACTIVE");
        if ("ENDED".equals(status)) {
            builder.append(" da ket thuc");
        }

        return builder.toString();
    }

    private boolean isUnreadLike(@NonNull ChatRoomModel room) {
        if (room.getUnread_count_map() == null) {
            return false;
        }

        Long unreadValue = room.getUnread_count_map().get(currentUserId);
        return unreadValue != null && unreadValue > 0;
    }

    @NonNull
    private String resolvePartnerName(@NonNull ChatRoomModel room) {
        List<String> memberIds = room.getMember_ids();
        List<String> memberNames = room.getMember_names();

        if (memberIds != null) {
            for (int i = 0; i < memberIds.size(); i++) {
                String memberId = safeText(memberIds.get(i), "");
                if (!memberId.equals(currentUserId)) {
                    if (memberNames != null && i < memberNames.size()) {
                        return safeText(memberNames.get(i), "Người bạn ẩn danh");
                    }
                }
            }
        }

        return "Người bạn ẩn danh";
    }

    private void updateCounts() {
        int allCount = 0;
        int unreadCount = 0;
        int archivedCount = 0;

        for (ChatRoomModel room : fullRoomList) {
            boolean archived = isArchived(room);

            if (archived) {
                archivedCount++;
                continue;
            }

            allCount++;

            if (isUnreadLike(room)) {
                unreadCount++;
            }
        }

        if (tabCommunityChatAll != null) {
            tabCommunityChatAll.setText("Tất cả  (" + allCount + ")");
        }

        if (tabCommunityChatUnread != null) {
            tabCommunityChatUnread.setText("Chưa đọc  (" + unreadCount + ")");
        }

        if (tabCommunityChatArchived != null) {
            tabCommunityChatArchived.setText("Lưu trữ  (" + archivedCount + ")");
        }
    }
    private void updateTabUi() {
        updateSingleTabUi(tabCommunityChatAll, currentFilterMode == FILTER_ALL);
        updateSingleTabUi(tabCommunityChatUnread, currentFilterMode == FILTER_UNREAD);
        updateSingleTabUi(tabCommunityChatArchived, currentFilterMode == FILTER_ARCHIVED);
    }

    private void updateSingleTabUi(TextView tab, boolean active) {
        if (tab == null) return;

        tab.setBackgroundResource(
                active ? R.drawable.bg_community_chat_tab_active : R.drawable.bg_community_chat_tab
        );
        tab.setTextColor(
                getResources().getColor(
                        active ? android.R.color.holo_red_light : android.R.color.darker_gray
                )
        );
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredRoomList.isEmpty();

        if (layoutCommunityChatEmptyState != null) {
            layoutCommunityChatEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            layoutCommunityChatEmptyState.bringToFront();
        }

        if (rvCommunityChatRooms != null) {
            rvCommunityChatRooms.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }

        if (!isEmpty) {
            return;
        }

        String keyword = edtCommunityChatSearch != null && edtCommunityChatSearch.getText() != null
                ? edtCommunityChatSearch.getText().toString().trim()
                : "";

        if (!keyword.isEmpty()) {
            if (txtCommunityChatEmptyTitle != null) {
                txtCommunityChatEmptyTitle.setText("Không tìm thấy cuộc trò chuyện phù hợp");
            }
            if (txtCommunityChatEmptySubtitle != null) {
                txtCommunityChatEmptySubtitle.setText("Thử từ khóa khác như tên người bạn hoặc nội dung tin nhắn gần nhất");
            }
            return;
        }

        if (currentFilterMode == FILTER_UNREAD) {
            if (txtCommunityChatEmptyTitle != null) {
                txtCommunityChatEmptyTitle.setText("Không có tin nhắn chưa đọc");
            }
            if (txtCommunityChatEmptySubtitle != null) {
                txtCommunityChatEmptySubtitle.setText("Khi có tin nhắn mới chưa xem, chúng sẽ hiện ở đây");
            }
            return;
        }

        if (currentFilterMode == FILTER_ARCHIVED) {
            if (txtCommunityChatEmptyTitle != null) {
                txtCommunityChatEmptyTitle.setText("Chưa có cuộc trò chuyện lưu trữ");
            }
            if (txtCommunityChatEmptySubtitle != null) {
                txtCommunityChatEmptySubtitle.setText("Những cuộc trò chuyện bạn lưu trữ sẽ hiện ở tab này");
            }
            return;
        }

        if (txtCommunityChatEmptyTitle != null) {
            txtCommunityChatEmptyTitle.setText("Chưa có cuộc trò chuyện nào");
        }
        if (txtCommunityChatEmptySubtitle != null) {
            txtCommunityChatEmptySubtitle.setText("Khi Mood Match thành công,\nđoạn chat sẽ hiện ở đây");
        }
    }

    private void openChatRoom(@NonNull ChatRoomModel room) {
        String roomId = safeText(room.getRoom_id(), "");
        if (roomId.isEmpty()) return;

        String partnerId = "";
        String partnerName = "Người bạn ẩn danh";
        String partnerAvatar = "";
        String moodTag = safeText(room.getMatch_mood_tag(), "stress");

        List<String> memberIds = room.getMember_ids();
        List<String> memberNames = room.getMember_names();
        List<String> memberAvatars = room.getMember_avatars();

        if (memberIds != null) {
            for (int i = 0; i < memberIds.size(); i++) {
                String memberId = safeText(memberIds.get(i), "");
                if (!memberId.equals(currentUserId)) {
                    partnerId = memberId;

                    if (memberNames != null && i < memberNames.size()) {
                        partnerName = safeText(memberNames.get(i), partnerName);
                    }

                    if (memberAvatars != null && i < memberAvatars.size()) {
                        partnerAvatar = safeText(memberAvatars.get(i), "");
                    }
                    break;
                }
            }
        }

        Intent intent = new Intent(CommunityChatListActivity.this, MoodMatchChatActivity.class);
        intent.putExtra("room_id", roomId);
        intent.putExtra("match_id", safeText(room.getRelated_id(), ""));
        intent.putExtra("matched_user_id", partnerId);
        intent.putExtra("matched_user_name", partnerName);
        intent.putExtra("matched_user_avatar", partnerAvatar);
        intent.putExtra("mood_tag", moodTag);
        intent.putExtra("room_status", safeText(room.getStatus(), "ACTIVE"));
        startActivity(intent);
    }

    private boolean shouldShowRoom(@NonNull ChatRoomModel room) {
        String status = safeText(room.getStatus(), "ACTIVE");

        if ("ACTIVE".equals(status)) {
            return true;
        }

        if (!"ENDED".equals(status)) {
            return false;
        }

        return !isRoomExpired(room);
    }

    private boolean isRoomExpired(@NonNull ChatRoomModel room) {
        if (room.getPurge_at() == null) {
            return false;
        }

        return room.getPurge_at().toDate().getTime() <= System.currentTimeMillis();
    }

    private void scheduleNextPurgeRefresh() {
        purgeHandler.removeCallbacks(purgeRunnable);

        long now = System.currentTimeMillis();
        long nearestDelayMs = Long.MAX_VALUE;

        for (ChatRoomModel room : fullRoomList) {
            if (room.getPurge_at() == null) {
                continue;
            }

            long purgeTime = room.getPurge_at().toDate().getTime();
            long delay = purgeTime - now;

            if (delay > 0 && delay < nearestDelayMs) {
                nearestDelayMs = delay;
            }
        }

        if (nearestDelayMs != Long.MAX_VALUE) {
            purgeHandler.postDelayed(purgeRunnable, nearestDelayMs + 500L);
        }
    }

    private void markLastMessageDeliveredIfNeeded(@NonNull ChatRoomModel room) {
        String roomId = safeText(room.getRoom_id(), "");
        String lastMessageId = safeText(room.getLast_message_id(), "");
        String lastSenderId = safeText(room.getLast_sender_id(), "");

        if (roomId.isEmpty() || lastMessageId.isEmpty() || lastSenderId.isEmpty()) {
            return;
        }

        if (lastSenderId.equals(currentUserId)) {
            return;
        }

        firestore.collection("chat_rooms")
                .document(roomId)
                .collection("messages")
                .document(lastMessageId)
                .update("delivered_user_ids",
                        com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId));
    }

    private void showRoomActionsMenu(@NonNull ChatRoomModel room, @NonNull View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);

        popupMenu.getMenu().add(
                0, 1, 0,
                isPinned(room) ? "Bỏ ghim" : "Ghim"
        );
        popupMenu.getMenu().add(
                0, 2, 1,
                isMuted(room) ? "Bỏ tắt thông báo" : "Tắt thông báo"
        );
        popupMenu.getMenu().add(
                0, 3, 2,
                isArchived(room) ? "Bỏ lưu trữ" : "Lưu trữ"
        );

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            String roomId = safeText(room.getRoom_id(), "");

            if (roomId.isEmpty()) {
                return true;
            }

            if (itemId == 1) {
                moodMatchRepository.setRoomPinned(roomId, !isPinned(room), new MoodMatchRepository.SimpleActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(
                                CommunityChatListActivity.this,
                                isPinned(room) ? "Đã bỏ ghim" : "Đã ghim cuộc trò chuyện",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        Toast.makeText(CommunityChatListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }

            if (itemId == 2) {
                moodMatchRepository.setRoomMuted(roomId, !isMuted(room), new MoodMatchRepository.SimpleActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(
                                CommunityChatListActivity.this,
                                isMuted(room) ? "Đã bật lại thông báo" : "Đã tắt thông báo",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        Toast.makeText(CommunityChatListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }

            if (itemId == 3) {
                moodMatchRepository.setRoomArchived(roomId, !isArchived(room), new MoodMatchRepository.SimpleActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(
                                CommunityChatListActivity.this,
                                isArchived(room) ? "Đã bỏ lưu trữ" : "Đã lưu trữ cuộc trò chuyện",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        Toast.makeText(CommunityChatListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void attachSwipeActions() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                ChatRoomModel room = adapter.getItem(position);
                String roomId = safeText(room.getRoom_id(), "");

                if (roomId.isEmpty()) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                if (direction == ItemTouchHelper.RIGHT) {
                    moodMatchRepository.setRoomPinned(roomId, !isPinned(room), new MoodMatchRepository.SimpleActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(
                                    CommunityChatListActivity.this,
                                    isPinned(room) ? "Đã bỏ ghim" : "Đã ghim cuộc trò chuyện",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onFailure(@NonNull String errorMessage) {
                            adapter.notifyItemChanged(position);
                            Toast.makeText(CommunityChatListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                    adapter.notifyItemChanged(position);
                    return;
                }

                if (direction == ItemTouchHelper.LEFT) {
                    moodMatchRepository.setRoomArchived(roomId, !isArchived(room), new MoodMatchRepository.SimpleActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(
                                    CommunityChatListActivity.this,
                                    isArchived(room) ? "Đã bỏ lưu trữ" : "Đã lưu trữ cuộc trò chuyện",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onFailure(@NonNull String errorMessage) {
                            adapter.notifyItemChanged(position);
                            Toast.makeText(CommunityChatListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                    adapter.notifyItemChanged(position);
                }
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(rvCommunityChatRooms);
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}