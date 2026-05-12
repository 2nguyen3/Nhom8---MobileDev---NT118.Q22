package com.example.heami.ui.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ConsultationModel;
import com.example.heami.utils.ExitDialogHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class DoctorMessagesActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "extra_room_id";
    public static final String EXTRA_SESSION_ID = "extra_session_id";
    public static final String EXTRA_PARTNER_NAME = "extra_partner_name";
    public static final String EXTRA_PARTNER_AVATAR = "extra_partner_avatar";
    public static final String EXTRA_ROOM_STATUS = "extra_room_status";
    public static final String EXTRA_FORMAT_TYPE = "extra_format_type";
    public static final String EXTRA_USER_ID = "extra_user_id";

    private RecyclerView rvDoctorConsultationRooms;
    private View layoutDoctorMessagesEmptyState;
    private View progressDoctorMessages;
    private TextView txtDoctorMessagesEmptyTitle;
    private TextView txtDoctorMessagesEmptySubtitle;

    private TextView txtUnreadCountBadge;
    private TextView txtConversationCount;
    private EditText edtDoctorMessagesSearch;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private String currentDoctorId = "";
    private ListenerRegistration consultationListener;

    private final List<DoctorConsultationThreadItem> masterItems = new ArrayList<>();
    private final Map<String, UserLiteProfile> userProfileCache = new HashMap<>();
    private final Map<String, RoomLiteMeta> roomMetaCache = new HashMap<>();

    private DoctorConsultationThreadAdapter adapter;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_messages);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        resolveCurrentDoctorId();
        bindViews();
        setupRecyclerView();
        setupSearch();

        DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_MESSAGES);
        ExitDialogHelper.registerExitHandler(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startConsultationListener();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (consultationListener != null) {
            consultationListener.remove();
            consultationListener = null;
        }
    }

    private void resolveCurrentDoctorId() {
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);

        if (isDoctor) {
            currentDoctorId = safeText(prefs.getString("doctor_id", "doc_001"), "doc_001");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        currentDoctorId = currentUser != null ? safeText(currentUser.getUid(), "") : "";
    }

    private void bindViews() {
        rvDoctorConsultationRooms = findViewById(R.id.rvDoctorConsultationRooms);
        layoutDoctorMessagesEmptyState = findViewById(R.id.layoutDoctorMessagesEmptyState);
        progressDoctorMessages = findViewById(R.id.progressDoctorMessages);
        txtDoctorMessagesEmptyTitle = findViewById(R.id.txtDoctorMessagesEmptyTitle);
        txtDoctorMessagesEmptySubtitle = findViewById(R.id.txtDoctorMessagesEmptySubtitle);

        txtUnreadCountBadge = findViewById(R.id.txtUnreadCountBadge);
        txtConversationCount = findViewById(R.id.txtConversationCount);
        edtDoctorMessagesSearch = findViewById(R.id.edtDoctorMessagesSearch);
    }

    private void setupRecyclerView() {
        rvDoctorConsultationRooms.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DoctorConsultationThreadAdapter(new OnConsultationItemClickListener() {
            @Override
            public void onConsultationClick(@NonNull DoctorConsultationThreadItem item) {
                openDoctorChatDetail(item);
            }
        });

        rvDoctorConsultationRooms.setAdapter(adapter);
    }

    private void setupSearch() {
        if (edtDoctorMessagesSearch == null) return;

        edtDoctorMessagesSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                applySearchAndRender();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void startConsultationListener() {
        if (currentDoctorId.isEmpty()) {
            showLoading(false);
            showEmptyState(
                    "Không tìm thấy tài khoản bác sĩ",
                    "Bạn cần đăng nhập lại để tải danh sách phiên tư vấn."
            );
            updateHeaderStats(new ArrayList<>());
            return;
        }

        showLoading(true);

        if (consultationListener != null) {
            consultationListener.remove();
            consultationListener = null;
        }

        consultationListener = firestore.collection("consultations")
                .whereEqualTo("doctor_id", currentDoctorId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showLoading(false);
                        String message = error.getMessage() != null
                                ? error.getMessage()
                                : "Không thể tải danh sách phiên tư vấn";
                        Toast.makeText(DoctorMessagesActivity.this, message, Toast.LENGTH_SHORT).show();
                        showEmptyState(
                                "Chưa tải được danh sách chat",
                                "Vui lòng kiểm tra mạng hoặc thử lại sau."
                        );
                        updateHeaderStats(new ArrayList<>());
                        return;
                    }

                    List<DoctorConsultationThreadItem> loadedItems = new ArrayList<>();
                    List<String> missingUserIds = new ArrayList<>();
                    List<String> missingRoomIds = new ArrayList<>();

                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            ConsultationModel consultation = mapConsultation(document);
                            String sessionId = safeText(consultation.getSessionId(), "");
                            String userId = safeText(consultation.getUserId(), "");

                            if (sessionId.isEmpty() || userId.isEmpty()) {
                                continue;
                            }

                            String roomId = safeText(document.getString("room_id"), "");

                            DoctorConsultationThreadItem item = new DoctorConsultationThreadItem(
                                    consultation,
                                    roomId
                            );
                            loadedItems.add(item);

                            if (!userProfileCache.containsKey(userId) && !missingUserIds.contains(userId)) {
                                missingUserIds.add(userId);
                            }

                            if (!roomId.isEmpty() && !roomMetaCache.containsKey(roomId) && !missingRoomIds.contains(roomId)) {
                                missingRoomIds.add(roomId);
                            }
                        }
                    }

                    preloadThreadDependencies(loadedItems, missingUserIds, missingRoomIds);
                });
    }

    private void preloadThreadDependencies(
            @NonNull List<DoctorConsultationThreadItem> loadedItems,
            @NonNull List<String> missingUserIds,
            @NonNull List<String> missingRoomIds
    ) {
        prefetchUserProfiles(missingUserIds, () ->
                prefetchRoomMetadata(missingRoomIds, () -> {
                    enrichThreadItems(loadedItems);
                    sortConsultationsDescending(loadedItems);

                    showLoading(false);

                    masterItems.clear();
                    masterItems.addAll(loadedItems);

                    applySearchAndRender();
                })
        );
    }

    private void prefetchUserProfiles(@NonNull List<String> userIds, @NonNull Runnable onDone) {
        if (userIds.isEmpty()) {
            onDone.run();
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (String userId : userIds) {
            tasks.add(
                    firestore.collection("users")
                            .document(userId)
                            .get()
            );
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(results -> {
                    for (Task<DocumentSnapshot> task : tasks) {
                        if (!task.isSuccessful()) continue;

                        DocumentSnapshot doc = task.getResult();
                        if (doc == null || !doc.exists()) continue;

                        String userId = safeText(doc.getId(), "");
                        if (userId.isEmpty()) continue;

                        String displayName = safeText(doc.getString("nickname"), "");
                        if (displayName.isEmpty()) {
                            displayName = safeText(doc.getString("full_name"), "");
                        }
                        if (displayName.isEmpty()) {
                            displayName = safeText(doc.getString("name"), "");
                        }
                        if (displayName.isEmpty()) {
                            displayName = "Người dùng Heami";
                        }

                        String avatar = safeText(doc.getString("avatar_url"), "");
                        if (avatar.isEmpty()) {
                            avatar = safeText(doc.getString("avatar"), "");
                        }

                        userProfileCache.put(userId, new UserLiteProfile(displayName, avatar));
                    }

                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private void prefetchRoomMetadata(@NonNull List<String> roomIds, @NonNull Runnable onDone) {
        if (roomIds.isEmpty()) {
            onDone.run();
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (String roomId : roomIds) {
            tasks.add(
                    firestore.collection("chat_rooms")
                            .document(roomId)
                            .get()
            );
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(results -> {
                    for (Task<DocumentSnapshot> task : tasks) {
                        if (!task.isSuccessful()) continue;

                        DocumentSnapshot doc = task.getResult();
                        if (doc == null || !doc.exists()) continue;

                        String roomId = safeText(doc.getId(), "");
                        if (roomId.isEmpty()) continue;

                        String lastMessage = safeText(doc.getString("last_message"), "");
                        Timestamp lastMessageAt = doc.getTimestamp("last_message_at");
                        String lastSenderId = safeText(doc.getString("last_sender_id"), "");

                        int unreadCount = 0;
                        Object unreadMapObj = doc.get("unread_count_map");
                        if (unreadMapObj instanceof Map) {
                            Object value = ((Map<?, ?>) unreadMapObj).get(currentDoctorId);
                            if (value instanceof Number) {
                                unreadCount = ((Number) value).intValue();
                            }
                        }

                        roomMetaCache.put(roomId, new RoomLiteMeta(
                                lastMessage,
                                unreadCount,
                                lastMessageAt,
                                lastSenderId
                        ));
                    }

                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private void enrichThreadItems(@NonNull List<DoctorConsultationThreadItem> items) {
        for (DoctorConsultationThreadItem item : items) {
            if (item.roomId.isEmpty()) continue;

            RoomLiteMeta meta = roomMetaCache.get(item.roomId);
            if (meta == null) continue;

            item.lastMessage = safeText(meta.lastMessage, "");
            item.unreadCount = Math.max(meta.unreadCount, 0);
            item.lastMessageAt = meta.lastMessageAt;
            item.lastSenderId = safeText(meta.lastSenderId, "");
        }
    }

    private void applySearchAndRender() {
        List<DoctorConsultationThreadItem> filtered = new ArrayList<>();

        String query = currentSearchQuery != null ? currentSearchQuery.trim().toLowerCase(Locale.ROOT) : "";

        if (query.isEmpty()) {
            filtered.addAll(masterItems);
        } else {
            for (DoctorConsultationThreadItem item : masterItems) {
                String userName = resolveUserName(item).toLowerCase(Locale.ROOT);
                String lastMessage = resolvePreviewMessage(item).toLowerCase(Locale.ROOT);
                String formatType = safeText(item.consultation.getFormatType(), "").toLowerCase(Locale.ROOT);
                String status = safeText(item.consultation.getStatus(), "").toLowerCase(Locale.ROOT);

                if (userName.contains(query)
                        || lastMessage.contains(query)
                        || formatType.contains(query)
                        || status.contains(query)) {
                    filtered.add(item);
                }
            }
        }

        adapter.submitList(filtered);
        updateHeaderStats(filtered);
        updateEmptyState(filtered);
    }

    private void openDoctorChatDetail(@NonNull DoctorConsultationThreadItem item) {
        ConsultationModel consultation = item.consultation;

        String sessionId = safeText(consultation.getSessionId(), "");
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Phiên tư vấn chưa hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, DoctorChatDetailActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, safeText(item.roomId, ""));
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        intent.putExtra(EXTRA_PARTNER_NAME, resolveUserName(item));
        intent.putExtra(EXTRA_PARTNER_AVATAR, resolveUserAvatar(item));
        intent.putExtra(EXTRA_ROOM_STATUS, safeText(consultation.getStatus(), "BOOKED"));
        intent.putExtra(EXTRA_FORMAT_TYPE, safeText(consultation.getFormatType(), ""));
        intent.putExtra(EXTRA_USER_ID, safeText(consultation.getUserId(), ""));
        startActivity(intent);
    }

    private void updateHeaderStats(@NonNull List<DoctorConsultationThreadItem> visibleItems) {
        int unreadTotal = 0;
        for (DoctorConsultationThreadItem item : visibleItems) {
            unreadTotal += Math.max(item.unreadCount, 0);
        }

        if (txtConversationCount != null) {
            txtConversationCount.setText(visibleItems.size() + " cuộc trò chuyện");
        }

        if (txtUnreadCountBadge != null) {
            txtUnreadCountBadge.setText(unreadTotal + " chưa đọc");
        }
    }

    private void updateEmptyState(@NonNull List<DoctorConsultationThreadItem> visibleItems) {
        boolean isEmpty = visibleItems.isEmpty();
        layoutDoctorMessagesEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvDoctorConsultationRooms.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            txtDoctorMessagesEmptyTitle.setText("Chưa có cuộc trò chuyện tư vấn nào");
            txtDoctorMessagesEmptySubtitle.setText("Khi người dùng bắt đầu phiên chat hoặc gọi tư vấn, cuộc trò chuyện sẽ xuất hiện ở đây.");
        }
    }

    private void showEmptyState(@NonNull String title, @NonNull String subtitle) {
        rvDoctorConsultationRooms.setVisibility(View.GONE);
        layoutDoctorMessagesEmptyState.setVisibility(View.VISIBLE);
        txtDoctorMessagesEmptyTitle.setText(title);
        txtDoctorMessagesEmptySubtitle.setText(subtitle);
    }

    private void showLoading(boolean loading) {
        progressDoctorMessages.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void sortConsultationsDescending(@NonNull List<DoctorConsultationThreadItem> items) {
        Collections.sort(items, new Comparator<DoctorConsultationThreadItem>() {
            @Override
            public int compare(DoctorConsultationThreadItem left, DoctorConsultationThreadItem right) {
                long leftTime = extractComparableTime(left);
                long rightTime = extractComparableTime(right);
                return Long.compare(rightTime, leftTime);
            }
        });
    }

    private long extractComparableTime(@Nullable DoctorConsultationThreadItem item) {
        if (item == null || item.consultation == null) return 0L;

        if (item.lastMessageAt != null && item.lastMessageAt.toDate() != null) {
            return item.lastMessageAt.toDate().getTime();
        }

        ConsultationModel consultation = item.consultation;

        if (consultation.getStartTime() != null && consultation.getStartTime().toDate() != null) {
            return consultation.getStartTime().toDate().getTime();
        }

        if (consultation.getBookedAt() != null && consultation.getBookedAt().toDate() != null) {
            return consultation.getBookedAt().toDate().getTime();
        }

        return 0L;
    }

    @NonNull
    private ConsultationModel mapConsultation(@NonNull DocumentSnapshot doc) {
        ConsultationModel model = doc.toObject(ConsultationModel.class);
        if (model == null) {
            model = new ConsultationModel();
        }

        if (safeText(model.getSessionId(), "").isEmpty()) {
            model.setSessionId(safeText(doc.getString("session_id"), ""));
        }
        if (safeText(model.getSessionId(), "").isEmpty()) {
            model.setSessionId(doc.getId());
        }

        if (safeText(model.getUserId(), "").isEmpty()) {
            model.setUserId(safeText(doc.getString("user_id"), ""));
        }

        if (safeText(model.getDoctorId(), "").isEmpty()) {
            model.setDoctorId(safeText(doc.getString("doctor_id"), ""));
        }

        if (safeText(model.getDoctorName(), "").isEmpty()) {
            model.setDoctorName(safeText(doc.getString("doctor_name"), ""));
        }

        if (safeText(model.getDoctorAvatar(), "").isEmpty()) {
            model.setDoctorAvatar(safeText(doc.getString("doctor_avatar"), ""));
        }

        if (safeText(model.getFormatType(), "").isEmpty()) {
            model.setFormatType(safeText(doc.getString("format_type"), ""));
        }

        if (safeText(model.getStatus(), "").isEmpty()) {
            model.setStatus(safeText(doc.getString("status"), "BOOKED"));
        }

        if (model.getStartTime() == null) {
            model.setStartTime(doc.getTimestamp("start_time"));
        }

        if (model.getBookedAt() == null) {
            model.setBookedAt(doc.getTimestamp("booked_at"));
        }

        if (safeText(model.getNote(), "").isEmpty()) {
            model.setNote(safeText(doc.getString("note"), ""));
        }

        return model;
    }

    @NonNull
    private String resolveUserName(@NonNull DoctorConsultationThreadItem item) {
        String userId = safeText(item.consultation.getUserId(), "");
        UserLiteProfile profile = userProfileCache.get(userId);
        if (profile != null) {
            return safeText(profile.displayName, "Người dùng Heami");
        }
        return "Người dùng Heami";
    }

    @NonNull
    private String resolveUserAvatar(@NonNull DoctorConsultationThreadItem item) {
        String userId = safeText(item.consultation.getUserId(), "");
        UserLiteProfile profile = userProfileCache.get(userId);
        if (profile != null) {
            return safeText(profile.avatar, "");
        }
        return "";
    }

    @NonNull
    private String resolvePreviewMessage(@NonNull DoctorConsultationThreadItem item) {
        if (!safeText(item.lastMessage, "").isEmpty()) {
            if (currentDoctorId.equals(item.lastSenderId)) {
                return "Bạn: " + item.lastMessage;
            }
            return item.lastMessage;
        }

        String status = safeText(item.consultation.getStatus(), "BOOKED").toUpperCase(Locale.ROOT);
        boolean isCall = isCallFormat(item.consultation.getFormatType());

        if (isCall) {
            if ("ONGOING".equals(status)) return "Phiên gọi đang diễn ra";
            if ("COMPLETED".equals(status)) return "Phiên gọi đã hoàn thành";
            if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "Phiên gọi đã bị hủy";
            return "Phiên gọi video chờ bắt đầu";
        } else {
            if (!item.roomId.isEmpty()) {
                if ("ONGOING".equals(status)) return "Phòng chat tư vấn đang hoạt động";
                if ("COMPLETED".equals(status)) return "Phiên chat đã hoàn thành";
                if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "Phiên chat đã bị hủy";
                return "Đã có phòng chat tư vấn";
            } else {
                if ("COMPLETED".equals(status)) return "Phiên chat đã hoàn thành";
                if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "Phiên chat đã bị hủy";
                return "Chưa bắt đầu chat tư vấn";
            }
        }
    }

    @NonNull
    private String resolveRelativeTime(@NonNull DoctorConsultationThreadItem item) {
        long timestamp = extractComparableTime(item);
        if (timestamp <= 0L) return "--";

        long now = System.currentTimeMillis();
        long diff = Math.max(0L, now - timestamp);

        long minute = 60_000L;
        long hour = 60 * minute;
        long day = 24 * hour;

        if (diff < minute) return "Vừa xong";
        if (diff < hour) return (diff / minute) + " phút trước";
        if (diff < day) return (diff / hour) + " giờ trước";
        return (diff / day) + " ngày trước";
    }

    @NonNull
    private String resolveStatusChipText(@NonNull DoctorConsultationThreadItem item) {
        String status = safeText(item.consultation.getStatus(), "BOOKED").toUpperCase(Locale.ROOT);

        if ("ONGOING".equals(status)) return "Đang tư vấn";
        if ("BOOKED".equals(status)) return "Sắp tới";
        if ("COMPLETED".equals(status)) return "Hoàn thành";
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "Đã hủy";
        return "Tư vấn";
    }

    private boolean isCallFormat(@Nullable String rawFormatType) {
        String normalized = safeText(rawFormatType, "").toLowerCase(Locale.ROOT);
        return normalized.contains("call")
                || normalized.contains("video")
                || normalized.contains("gọi");
    }

    @NonNull
    private String resolveModeChipText(@NonNull DoctorConsultationThreadItem item) {
        return isCallFormat(item.consultation.getFormatType()) ? "CALL" : "CHAT";
    }

    @NonNull
    private String resolveAvatarEmoji(@NonNull DoctorConsultationThreadItem item) {
        String avatar = resolveUserAvatar(item);

        if (!avatar.isEmpty()
                && !avatar.startsWith("http://")
                && !avatar.startsWith("https://")
                && avatar.length() <= 3) {
            return avatar;
        }

        String[] cuteEmojis = {"🦋", "🦊", "🐨", "🐸", "🐰", "🐼"};
        String userId = safeText(item.consultation.getUserId(), "heami");
        int index = Math.abs(userId.hashCode()) % cuteEmojis.length;
        return cuteEmojis[index];
    }

    @NonNull
    private String resolveMoodEmoji(@NonNull DoctorConsultationThreadItem item) {
        String status = safeText(item.consultation.getStatus(), "BOOKED").toUpperCase(Locale.ROOT);
        boolean isCall = isCallFormat(item.consultation.getFormatType());

        if ("COMPLETED".equals(status)) return "😊";
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "😔";
        if ("ONGOING".equals(status)) return isCall ? "📹" : "💬";
        return "🕒";
    }

    private int resolveAvatarBackground(@NonNull DoctorConsultationThreadItem item) {
        String status = safeText(item.consultation.getStatus(), "BOOKED").toUpperCase(Locale.ROOT);

        if ("ONGOING".equals(status)) return R.drawable.bg_stat_icon_green;
        if ("BOOKED".equals(status)) return R.drawable.bg_stat_icon_pink;
        if ("COMPLETED".equals(status)) return R.drawable.bg_stat_icon_blue;
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return R.drawable.bg_light_gray_circle;
        return R.drawable.bg_stat_icon_blue;
    }

    private int resolveStatusChipBackground(@NonNull DoctorConsultationThreadItem item) {
        String status = safeText(item.consultation.getStatus(), "BOOKED").toUpperCase(Locale.ROOT);

        if ("ONGOING".equals(status)) return R.drawable.bg_stat_icon_green;
        if ("BOOKED".equals(status)) return R.drawable.bg_stat_icon_pink;
        if ("COMPLETED".equals(status)) return R.drawable.bg_chip_inactive;
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return R.drawable.bg_chip_inactive;
        return R.drawable.bg_chip_inactive;
    }

    private int resolveStatusChipTextColor(@NonNull DoctorConsultationThreadItem item) {
        String status = safeText(item.consultation.getStatus(), "BOOKED").toUpperCase(Locale.ROOT);

        if ("ONGOING".equals(status)) return 0xFF09A38C;
        if ("BOOKED".equals(status)) return 0xFFE8507A;
        if ("COMPLETED".equals(status)) return 0xFF7F8C8D;
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return 0xFFFF5A5F;
        return 0xFF7F8C8D;
    }

    private int resolveModeChipBackground(@NonNull DoctorConsultationThreadItem item) {
        return isCallFormat(item.consultation.getFormatType())
                ? R.drawable.bg_stat_icon_green
                : R.drawable.bg_chip_inactive;
    }

    private int resolveModeChipTextColor(@NonNull DoctorConsultationThreadItem item) {
        return isCallFormat(item.consultation.getFormatType())
                ? 0xFF09A38C
                : 0xFF7B61FF;
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private interface OnConsultationItemClickListener {
        void onConsultationClick(@NonNull DoctorConsultationThreadItem item);
    }

    private static class UserLiteProfile {
        final String displayName;
        final String avatar;

        UserLiteProfile(@NonNull String displayName, @NonNull String avatar) {
            this.displayName = displayName;
            this.avatar = avatar;
        }
    }

    private static class RoomLiteMeta {
        final String lastMessage;
        final int unreadCount;
        final Timestamp lastMessageAt;
        final String lastSenderId;

        RoomLiteMeta(
                @NonNull String lastMessage,
                int unreadCount,
                @Nullable Timestamp lastMessageAt,
                @NonNull String lastSenderId
        ) {
            this.lastMessage = lastMessage;
            this.unreadCount = unreadCount;
            this.lastMessageAt = lastMessageAt;
            this.lastSenderId = lastSenderId;
        }
    }

    private static class DoctorConsultationThreadItem {
        final ConsultationModel consultation;
        final String roomId;

        String lastMessage = "";
        int unreadCount = 0;
        Timestamp lastMessageAt = null;
        String lastSenderId = "";

        DoctorConsultationThreadItem(@NonNull ConsultationModel consultation, @NonNull String roomId) {
            this.consultation = consultation;
            this.roomId = roomId;
        }
    }

    private class DoctorConsultationThreadAdapter
            extends RecyclerView.Adapter<DoctorConsultationThreadAdapter.ThreadViewHolder> {

        private final List<DoctorConsultationThreadItem> items = new ArrayList<>();
        private final OnConsultationItemClickListener listener;

        DoctorConsultationThreadAdapter(@NonNull OnConsultationItemClickListener listener) {
            this.listener = listener;
        }

        void submitList(@NonNull List<DoctorConsultationThreadItem> newList) {
            items.clear();
            items.addAll(newList);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_doctor_consultation_thread, parent, false);
            return new ThreadViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ThreadViewHolder extends RecyclerView.ViewHolder {
            private final FrameLayout layoutThreadAvatarBg;
            private final TextView txtThreadAvatarEmoji;
            private final TextView txtThreadMoodEmoji;
            private final TextView txtThreadPartnerName;
            private final TextView txtThreadStatusChip;
            private final TextView txtThreadLastMessage;
            private final TextView txtThreadTime;
            private final TextView txtThreadModeChip;
            private final TextView txtThreadUnreadBadge;
            private final ImageView imgThreadChevron;
            private final CardView cardDoctorConsultationThread;

            ThreadViewHolder(@NonNull View itemView) {
                super(itemView);
                cardDoctorConsultationThread = itemView.findViewById(R.id.cardDoctorConsultationThread);
                layoutThreadAvatarBg = itemView.findViewById(R.id.layoutThreadAvatarBg);
                txtThreadAvatarEmoji = itemView.findViewById(R.id.txtThreadAvatarEmoji);
                txtThreadMoodEmoji = itemView.findViewById(R.id.txtThreadMoodEmoji);
                txtThreadPartnerName = itemView.findViewById(R.id.txtThreadPartnerName);
                txtThreadStatusChip = itemView.findViewById(R.id.txtThreadStatusChip);
                txtThreadLastMessage = itemView.findViewById(R.id.txtThreadLastMessage);
                txtThreadTime = itemView.findViewById(R.id.txtThreadTime);
                txtThreadModeChip = itemView.findViewById(R.id.txtThreadModeChip);
                txtThreadUnreadBadge = itemView.findViewById(R.id.txtThreadUnreadBadge);
                imgThreadChevron = itemView.findViewById(R.id.imgThreadChevron);
            }

            void bind(@NonNull DoctorConsultationThreadItem item) {
                txtThreadAvatarEmoji.setText(resolveAvatarEmoji(item));
                txtThreadMoodEmoji.setText(resolveMoodEmoji(item));
                txtThreadPartnerName.setText(resolveUserName(item));
                txtThreadStatusChip.setText(resolveStatusChipText(item));
                txtThreadLastMessage.setText(resolvePreviewMessage(item));
                txtThreadTime.setText(resolveRelativeTime(item));
                txtThreadModeChip.setText(resolveModeChipText(item));

                layoutThreadAvatarBg.setBackgroundResource(resolveAvatarBackground(item));
                txtThreadStatusChip.setBackgroundResource(resolveStatusChipBackground(item));
                txtThreadStatusChip.setTextColor(resolveStatusChipTextColor(item));
                txtThreadModeChip.setBackgroundResource(resolveModeChipBackground(item));
                txtThreadModeChip.setTextColor(resolveModeChipTextColor(item));

                if (item.unreadCount > 0) {
                    txtThreadUnreadBadge.setVisibility(View.VISIBLE);
                    imgThreadChevron.setVisibility(View.GONE);
                    txtThreadUnreadBadge.setText(item.unreadCount > 9 ? "9+" : String.valueOf(item.unreadCount));
                } else {
                    txtThreadUnreadBadge.setVisibility(View.GONE);
                    imgThreadChevron.setVisibility(View.VISIBLE);
                }

                itemView.setOnClickListener(v -> listener.onConsultationClick(item));
            }
        }
    }
}