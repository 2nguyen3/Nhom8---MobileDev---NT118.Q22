package com.example.heami.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ChatRoomModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommunityChatListActivity extends AppCompatActivity {

    private ImageButton btnBackCommunityChatList;
    private TextView tabCommunityChatAll;
    private TextView tabCommunityChatUnread;
    private EditText edtCommunityChatSearch;

    private RecyclerView rvCommunityChatRooms;
    private View layoutCommunityChatEmptyState;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration roomListener;

    private String currentUserId = "";

    private final List<ChatRoomModel> fullRoomList = new ArrayList<>();
    private final List<ChatRoomModel> filteredRoomList = new ArrayList<>();

    private CommunityChatRoomAdapter adapter;

    private boolean showingUnreadOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat_list);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        currentUserId = currentUser != null ? safeText(currentUser.getUid(), "") : "";

        bindViews();
        setupRecyclerView();
        setupActions();
        startRealtimeRoomListener();
    }

    private void bindViews() {
        btnBackCommunityChatList = findViewById(R.id.btnBackCommunityChatList);
        tabCommunityChatAll = findViewById(R.id.tabCommunityChatAll);
        tabCommunityChatUnread = findViewById(R.id.tabCommunityChatUnread);
        edtCommunityChatSearch = findViewById(R.id.edtCommunityChatSearch);
        rvCommunityChatRooms = findViewById(R.id.rvCommunityChatRooms);
        layoutCommunityChatEmptyState = findViewById(R.id.layoutCommunityChatEmptyState);
    }

    private void setupRecyclerView() {
        adapter = new CommunityChatRoomAdapter(currentUserId, room -> openChatRoom(room));
        rvCommunityChatRooms.setLayoutManager(new LinearLayoutManager(this));
        rvCommunityChatRooms.setAdapter(adapter);
    }

    private void setupActions() {
        if (btnBackCommunityChatList != null) {
            btnBackCommunityChatList.setOnClickListener(v -> finish());
        }

        if (tabCommunityChatAll != null) {
            tabCommunityChatAll.setOnClickListener(v -> {
                showingUnreadOnly = false;
                updateTabUi();
                applyFilters();
            });
        }

        if (tabCommunityChatUnread != null) {
            tabCommunityChatUnread.setOnClickListener(v -> {
                showingUnreadOnly = true;
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

    private void startRealtimeRoomListener() {
        if (currentUserId.isEmpty()) {
            applyFilters();
            return;
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

                            String status = safeText(room.getStatus(), "ACTIVE");
                            if (!"ACTIVE".equals(status)) {
                                return;
                            }

                            fullRoomList.add(room);
                        });
                    }

                    sortRoomsNewestFirst(fullRoomList);
                    applyFilters();
                });
    }

    private void sortRoomsNewestFirst(@NonNull List<ChatRoomModel> roomList) {
        Collections.sort(roomList, new Comparator<ChatRoomModel>() {
            @Override
            public int compare(ChatRoomModel left, ChatRoomModel right) {
                long leftTime = resolveRoomSortTime(left);
                long rightTime = resolveRoomSortTime(right);
                return Long.compare(rightTime, leftTime);
            }
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
                ? edtCommunityChatSearch.getText().toString().trim().toLowerCase()
                : "";

        for (ChatRoomModel room : fullRoomList) {
            if (showingUnreadOnly && !isUnreadLike(room)) {
                continue;
            }

            if (!keyword.isEmpty() && !matchesKeyword(room, keyword)) {
                continue;
            }

            filteredRoomList.add(room);
        }

        adapter.submitList(new ArrayList<>(filteredRoomList));
        updateCounts();
        updateEmptyState();
    }

    private boolean matchesKeyword(@NonNull ChatRoomModel room, @NonNull String keyword) {
        String partnerName = resolvePartnerName(room).toLowerCase();
        String lastMessage = safeText(room.getLast_message(), "").toLowerCase();
        return partnerName.contains(keyword) || lastMessage.contains(keyword);
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
        int allCount = fullRoomList.size();

        int unreadCount = 0;
        for (ChatRoomModel room : fullRoomList) {
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
    }

    private void updateTabUi() {
        if (tabCommunityChatAll != null) {
            tabCommunityChatAll.setBackgroundResource(
                    showingUnreadOnly
                            ? R.drawable.bg_community_chat_tab
                            : R.drawable.bg_community_chat_tab_active
            );
            tabCommunityChatAll.setTextColor(
                    getResources().getColor(showingUnreadOnly ? android.R.color.darker_gray : android.R.color.holo_red_light)
            );
        }

        if (tabCommunityChatUnread != null) {
            tabCommunityChatUnread.setBackgroundResource(
                    showingUnreadOnly
                            ? R.drawable.bg_community_chat_tab_active
                            : R.drawable.bg_community_chat_tab
            );
            tabCommunityChatUnread.setTextColor(
                    getResources().getColor(showingUnreadOnly ? android.R.color.holo_red_light : android.R.color.darker_gray)
            );
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredRoomList == null || filteredRoomList.isEmpty();

        if (layoutCommunityChatEmptyState != null) {
            layoutCommunityChatEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            layoutCommunityChatEmptyState.bringToFront();
        }

        if (rvCommunityChatRooms != null) {
            rvCommunityChatRooms.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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
        startActivity(intent);
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
        if (roomListener != null) {
            roomListener.remove();
            roomListener = null;
        }
    }
}