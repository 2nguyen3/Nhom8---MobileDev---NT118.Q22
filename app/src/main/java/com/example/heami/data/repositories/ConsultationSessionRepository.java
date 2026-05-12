package com.example.heami.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.heami.data.models.ChatMessageModel;
import com.example.heami.data.models.ConsultationModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConsultationSessionRepository {

    public static final String MODE_CHAT = "CHAT";
    public static final String MODE_CALL = "CALL";

    public interface LoadSessionListener {
        void onSuccess(@NonNull SessionEntryData data);
        void onFailure(@NonNull String errorMessage);
    }

    public interface ConsultationRealtimeListener {
        void onData(@NonNull ConsultationModel consultation, @NonNull String sessionMode, @NonNull String roomId);
        void onError(@NonNull String errorMessage);
    }

    public interface EnsureRoomListener {
        void onSuccess(@NonNull String roomId);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadMessagesListener {
        void onSuccess(@NonNull List<ChatMessageModel> messages);
        void onFailure(@NonNull String errorMessage);
    }

    public interface SendMessageListener {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    public interface SimpleActionListener {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    public static class SessionEntryData {
        private final ConsultationModel consultation;
        private final String sessionMode;
        private final String roomId;

        public SessionEntryData(
                @NonNull ConsultationModel consultation,
                @NonNull String sessionMode,
                @NonNull String roomId
        ) {
            this.consultation = consultation;
            this.sessionMode = sessionMode;
            this.roomId = roomId;
        }

        @NonNull
        public ConsultationModel getConsultation() {
            return consultation;
        }

        @NonNull
        public String getSessionMode() {
            return sessionMode;
        }

        @NonNull
        public String getRoomId() {
            return roomId;
        }
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public ConsultationSessionRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void getConsultationById(
            @NonNull String sessionId,
            @NonNull LoadSessionListener listener
    ) {
        String trimmedSessionId = safeText(sessionId, "");
        if (trimmedSessionId.isEmpty()) {
            listener.onFailure("Thiếu session_id của phiên tư vấn");
            return;
        }

        firestore.collection("consultations")
                .document(trimmedSessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy phiên tư vấn");
                        return;
                    }

                    ConsultationModel model = mapConsultation(documentSnapshot);
                    String roomId = extractRoomId(documentSnapshot);
                    String mode = resolveSessionMode(extractFormatType(model, documentSnapshot));

                    listener.onSuccess(new SessionEntryData(model, mode, roomId));
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải dữ liệu phiên tư vấn";
                    listener.onFailure(message);
                });
    }

    @Nullable
    public ListenerRegistration startConsultationRealtimeListener(
            @NonNull String sessionId,
            @NonNull ConsultationRealtimeListener listener
    ) {
        String trimmedSessionId = safeText(sessionId, "");
        if (trimmedSessionId.isEmpty()) {
            listener.onError("Thiếu session_id của phiên tư vấn");
            return null;
        }

        return firestore.collection("consultations")
                .document(trimmedSessionId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        String message = error.getMessage() != null
                                ? error.getMessage()
                                : "Không thể theo dõi phiên tư vấn";
                        listener.onError(message);
                        return;
                    }

                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        listener.onError("Không tìm thấy phiên tư vấn");
                        return;
                    }

                    ConsultationModel model = mapConsultation(documentSnapshot);
                    String roomId = extractRoomId(documentSnapshot);
                    String mode = resolveSessionMode(extractFormatType(model, documentSnapshot));

                    listener.onData(model, mode, roomId);
                });
    }

    public void validateSessionEntry(
            @NonNull String sessionId,
            @NonNull LoadSessionListener listener
    ) {
        getConsultationById(sessionId, new LoadSessionListener() {
            @Override
            public void onSuccess(@NonNull SessionEntryData data) {
                String status = safeText(data.getConsultation().getStatus(), "BOOKED").toUpperCase(Locale.ROOT);

                if ("COMPLETED".equals(status)) {
                    listener.onFailure("Phiên tư vấn này đã hoàn thành");
                    return;
                }

                if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
                    listener.onFailure("Phiên tư vấn này đã bị hủy");
                    return;
                }

                listener.onSuccess(data);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                listener.onFailure(errorMessage);
            }
        });
    }

    public void startConsultationSessionIfNeeded(
            @NonNull String sessionId,
            @NonNull LoadSessionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String currentUserId = firebaseUser.getUid();
        String trimmedSessionId = safeText(sessionId, "");
        if (trimmedSessionId.isEmpty()) {
            listener.onFailure("Thiếu session_id của phiên tư vấn");
            return;
        }

        DocumentReference consultationRef = firestore.collection("consultations").document(trimmedSessionId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot consultationSnapshot = transaction.get(consultationRef);
                    if (!consultationSnapshot.exists()) {
                        throw new RuntimeException("Không tìm thấy phiên tư vấn");
                    }

                    ConsultationModel model = mapConsultation(consultationSnapshot);
                    String status = safeText(extractStatus(model, consultationSnapshot), "BOOKED").toUpperCase(Locale.ROOT);
                    String mode = resolveSessionMode(extractFormatType(model, consultationSnapshot));
                    String roomId = extractRoomId(consultationSnapshot);

                    if ("COMPLETED".equals(status)) {
                        throw new RuntimeException("Phiên tư vấn này đã hoàn thành");
                    }

                    if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
                        throw new RuntimeException("Phiên tư vấn này đã bị hủy");
                    }

                    if ("BOOKED".equals(status)) {
                        Timestamp now = Timestamp.now();

                        HashMap<String, Object> updates = new HashMap<>();
                        updates.put("status", "ONGOING");
                        updates.put("started_at", now);
                        updates.put("started_by", currentUserId);

                        transaction.update(consultationRef, updates);
                        model.setStatus("ONGOING");
                    }

                    return new SessionEntryData(model, mode, roomId);
                }).addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể bắt đầu phiên tư vấn";
                    listener.onFailure(message);
                });
    }

    public void startCallSession(
            @NonNull String sessionId,
            @NonNull LoadSessionListener listener
    ) {
        startConsultationSessionIfNeeded(sessionId, new LoadSessionListener() {
            @Override
            public void onSuccess(@NonNull SessionEntryData data) {
                if (!MODE_CALL.equals(data.getSessionMode())) {
                    listener.onFailure("Phiên tư vấn này không phải gói gọi video");
                    return;
                }
                listener.onSuccess(data);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                listener.onFailure(errorMessage);
            }
        });
    }

    public void ensureConsultationChatRoom(
            @NonNull String sessionId,
            @NonNull EnsureRoomListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String trimmedSessionId = safeText(sessionId, "");
        if (trimmedSessionId.isEmpty()) {
            listener.onFailure("Thiếu session_id của phiên tư vấn");
            return;
        }

        DocumentReference consultationRef = firestore.collection("consultations").document(trimmedSessionId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot consultationSnapshot = transaction.get(consultationRef);
                    if (!consultationSnapshot.exists()) {
                        throw new RuntimeException("Không tìm thấy phiên tư vấn");
                    }

                    ConsultationModel consultation = mapConsultation(consultationSnapshot);
                    String mode = resolveSessionMode(extractFormatType(consultation, consultationSnapshot));
                    String status = safeText(extractStatus(consultation, consultationSnapshot), "BOOKED").toUpperCase(Locale.ROOT);

                    if (!MODE_CHAT.equals(mode)) {
                        throw new RuntimeException("Phiên tư vấn này không phải gói chat");
                    }

                    if ("COMPLETED".equals(status)) {
                        throw new RuntimeException("Phiên tư vấn này đã hoàn thành");
                    }

                    if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
                        throw new RuntimeException("Phiên tư vấn này đã bị hủy");
                    }

                    String userId = safeText(consultation.getUserId(), "");
                    String doctorId = safeText(consultation.getDoctorId(), "");

                    if (userId.isEmpty() || doctorId.isEmpty()) {
                        throw new RuntimeException("Phiên tư vấn thiếu thông tin user hoặc doctor");
                    }

                    String existingRoomId = extractRoomId(consultationSnapshot);
                    String roomId = existingRoomId.isEmpty()
                            ? buildConsultationRoomId(trimmedSessionId)
                            : existingRoomId;

                    DocumentReference roomRef = firestore.collection("chat_rooms").document(roomId);
                    DocumentReference userRef = firestore.collection("users").document(userId);

                    DocumentSnapshot roomSnapshot = transaction.get(roomRef);
                    DocumentSnapshot userSnapshot = transaction.get(userRef);

                    if (!roomSnapshot.exists()) {
                        String userName = safeText(userSnapshot.getString("nickname"), "Người dùng Heami");
                        String userAvatar = safeText(userSnapshot.getString("avatar_url"), "");

                        String doctorName = safeText(consultation.getDoctorName(), "Bác sĩ tư vấn");
                        String doctorAvatar = safeText(consultation.getDoctorAvatar(), "");

                        List<String> memberIds = new ArrayList<>();
                        memberIds.add(userId);
                        memberIds.add(doctorId);

                        List<String> memberNames = new ArrayList<>();
                        memberNames.add(userName);
                        memberNames.add(doctorName);

                        List<String> memberAvatars = new ArrayList<>();
                        memberAvatars.add(userAvatar);
                        memberAvatars.add(doctorAvatar);

                        HashMap<String, Long> unreadMap = new HashMap<>();
                        unreadMap.put(userId, 0L);
                        unreadMap.put(doctorId, 0L);

                        Timestamp now = Timestamp.now();

                        HashMap<String, Object> roomData = new HashMap<>();
                        roomData.put("room_id", roomId);
                        roomData.put("member_ids", memberIds);
                        roomData.put("member_names", memberNames);
                        roomData.put("member_avatars", memberAvatars);
                        roomData.put("type", "CONSULTATION");
                        roomData.put("related_id", trimmedSessionId);
                        roomData.put("match_mood_tag", "");
                        roomData.put("created_at", now);
                        roomData.put("last_message", "");
                        roomData.put("last_message_id", "");
                        roomData.put("last_message_at", now);
                        roomData.put("last_sender_id", "");
                        roomData.put("status", "ACTIVE");
                        roomData.put("unread_count_map", unreadMap);
                        roomData.put("search_blob", "");
                        roomData.put("recent_messages_preview", new ArrayList<String>());

                        transaction.set(roomRef, roomData);
                    }

                    if (existingRoomId.isEmpty()) {
                        HashMap<String, Object> consultationUpdates = new HashMap<>();
                        consultationUpdates.put("room_id", roomId);
                        transaction.update(consultationRef, consultationUpdates);
                    }

                    return roomId;
                }).addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể chuẩn bị phòng chat tư vấn";
                    listener.onFailure(message);
                });
    }

    @Nullable
    public ListenerRegistration startConsultationMessageListener(
            @NonNull String roomId,
            @NonNull LoadMessagesListener listener
    ) {
        String trimmedRoomId = safeText(roomId, "");
        if (trimmedRoomId.isEmpty()) {
            listener.onFailure("Thiếu room_id của phòng chat");
            return null;
        }

        return firestore.collection("chat_rooms")
                .document(trimmedRoomId)
                .collection("messages")
                .orderBy("created_at", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        String message = error.getMessage() != null
                                ? error.getMessage()
                                : "Không thể tải tin nhắn tư vấn";
                        listener.onFailure(message);
                        return;
                    }

                    List<ChatMessageModel> messages = new ArrayList<>();

                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            ChatMessageModel message = document.toObject(ChatMessageModel.class);
                            if (message == null) continue;

                            if (safeText(message.getMessage_id(), "").isEmpty()) {
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
                    listener.onSuccess(messages);
                });
    }

    public void sendConsultationMessage(
            @NonNull String roomId,
            @NonNull String text,
            @NonNull SendMessageListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String currentUserId = firebaseUser.getUid();
        String trimmedRoomId = safeText(roomId, "");
        String content = safeText(text, "");

        if (trimmedRoomId.isEmpty()) {
            listener.onFailure("Thiếu room_id của phòng chat");
            return;
        }

        if (content.isEmpty()) {
            listener.onFailure("Tin nhắn không được để trống");
            return;
        }

        DocumentReference roomRef = firestore.collection("chat_rooms").document(trimmedRoomId);

        roomRef.get()
                .addOnSuccessListener(roomSnapshot -> {
                    if (!roomSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy phòng chat tư vấn");
                        return;
                    }

                    String roomStatus = safeText(roomSnapshot.getString("status"), "ACTIVE");
                    if (!"ACTIVE".equals(roomStatus)) {
                        listener.onFailure("Phiên chat này đã kết thúc");
                        return;
                    }

                    String messageId = roomRef.collection("messages").document().getId();
                    long clientCreatedAtMs = System.currentTimeMillis();

                    HashMap<String, Object> messageData = new HashMap<>();
                    messageData.put("message_id", messageId);
                    messageData.put("sender_id", currentUserId);
                    messageData.put("text", content);
                    messageData.put("created_at", FieldValue.serverTimestamp());
                    messageData.put("client_created_at_ms", clientCreatedAtMs);
                    messageData.put("message_type", "TEXT");
                    messageData.put("status", "ACTIVE");
                    messageData.put("delivered_user_ids", new ArrayList<String>());
                    messageData.put("seen_user_ids", new ArrayList<String>());

                    HashMap<String, Object> roomUpdates = new HashMap<>();
                    roomUpdates.put("last_message", content);
                    roomUpdates.put("last_message_at", FieldValue.serverTimestamp());
                    roomUpdates.put("last_sender_id", currentUserId);
                    roomUpdates.put("last_message_id", messageId);
                    roomUpdates.put("unread_count_map." + currentUserId, 0L);

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
                            roomRef.collection("messages").document(messageId),
                            messageData
                    );
                    batch.update(roomRef, roomUpdates);

                    batch.commit()
                            .addOnSuccessListener(unused -> listener.onSuccess())
                            .addOnFailureListener(e -> {
                                String message = e.getMessage() != null
                                        ? e.getMessage()
                                        : "Không thể gửi tin nhắn lúc này";
                                listener.onFailure(message);
                            });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin phòng chat";
                    listener.onFailure(message);
                });
    }

    public void resetMyUnreadCount(
            @NonNull String roomId,
            @NonNull SimpleActionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String trimmedRoomId = safeText(roomId, "");
        if (trimmedRoomId.isEmpty()) {
            listener.onFailure("Thiếu room_id của phòng chat");
            return;
        }

        firestore.collection("chat_rooms")
                .document(trimmedRoomId)
                .update("unread_count_map." + firebaseUser.getUid(), 0L)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể reset số tin nhắn chưa đọc";
                    listener.onFailure(message);
                });
    }

    public void markMessagesSeen(
            @NonNull String roomId,
            @NonNull List<ChatMessageModel> messages
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            return;
        }

        String currentUserId = firebaseUser.getUid();
        String trimmedRoomId = safeText(roomId, "");
        if (trimmedRoomId.isEmpty()) {
            return;
        }

        WriteBatch batch = firestore.batch();
        boolean hasUpdates = false;

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

            DocumentReference messageRef = firestore.collection("chat_rooms")
                    .document(trimmedRoomId)
                    .collection("messages")
                    .document(messageId);

            batch.update(messageRef, "seen_user_ids", FieldValue.arrayUnion(currentUserId));
            batch.update(messageRef, "delivered_user_ids", FieldValue.arrayUnion(currentUserId));
            hasUpdates = true;
        }

        if (hasUpdates) {
            batch.commit();
        }
    }

    public void endConsultationSession(
            @NonNull String sessionId,
            @Nullable String roomId,
            @NonNull SimpleActionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String trimmedSessionId = safeText(sessionId, "");
        if (trimmedSessionId.isEmpty()) {
            listener.onFailure("Thiếu session_id của phiên tư vấn");
            return;
        }

        Timestamp now = Timestamp.now();
        WriteBatch batch = firestore.batch();

        DocumentReference consultationRef = firestore.collection("consultations").document(trimmedSessionId);
        batch.update(
                consultationRef,
                buildConsultationCompleteUpdates(firebaseUser.getUid(), now)
        );

        String trimmedRoomId = safeText(roomId, "");
        if (!trimmedRoomId.isEmpty()) {
            DocumentReference roomRef = firestore.collection("chat_rooms").document(trimmedRoomId);
            HashMap<String, Object> roomUpdates = new HashMap<>();
            roomUpdates.put("status", "ENDED");
            roomUpdates.put("ended_at", now);
            batch.update(roomRef, roomUpdates);
        }

        batch.commit()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể kết thúc phiên tư vấn";
                    listener.onFailure(message);
                });
    }

    @NonNull
    private Map<String, Object> buildConsultationCompleteUpdates(
            @NonNull String completedBy,
            @NonNull Timestamp now
    ) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("status", "COMPLETED");
        updates.put("completed_at", now);
        updates.put("completed_by", completedBy);
        return updates;
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
            model.setSessionId(safeText(doc.getString("sessionId"), ""));
        }
        if (safeText(model.getSessionId(), "").isEmpty()) {
            model.setSessionId(doc.getId());
        }

        if (safeText(model.getUserId(), "").isEmpty()) {
            model.setUserId(safeText(doc.getString("user_id"), ""));
        }
        if (safeText(model.getUserId(), "").isEmpty()) {
            model.setUserId(safeText(doc.getString("userId"), ""));
        }

        if (safeText(model.getDoctorId(), "").isEmpty()) {
            model.setDoctorId(safeText(doc.getString("doctor_id"), ""));
        }
        if (safeText(model.getDoctorId(), "").isEmpty()) {
            model.setDoctorId(safeText(doc.getString("doctorId"), ""));
        }

        if (safeText(model.getDoctorName(), "").isEmpty()) {
            model.setDoctorName(safeText(doc.getString("doctor_name"), ""));
        }
        if (safeText(model.getDoctorName(), "").isEmpty()) {
            model.setDoctorName(safeText(doc.getString("doctorName"), ""));
        }

        if (safeText(model.getDoctorAvatar(), "").isEmpty()) {
            model.setDoctorAvatar(safeText(doc.getString("doctor_avatar"), ""));
        }
        if (safeText(model.getDoctorAvatar(), "").isEmpty()) {
            model.setDoctorAvatar(safeText(doc.getString("doctorAvatar"), ""));
        }

        if (safeText(model.getPackageType(), "").isEmpty()) {
            model.setPackageType(safeText(doc.getString("package_type"), ""));
        }
        if (safeText(model.getPackageType(), "").isEmpty()) {
            model.setPackageType(safeText(doc.getString("packageType"), ""));
        }

        if (safeText(model.getFormatType(), "").isEmpty()) {
            model.setFormatType(safeText(doc.getString("format_type"), ""));
        }
        if (safeText(model.getFormatType(), "").isEmpty()) {
            model.setFormatType(safeText(doc.getString("formatType"), ""));
        }

        if (safeText(model.getStatus(), "").isEmpty()) {
            model.setStatus(safeText(doc.getString("status"), "BOOKED"));
        }

        if (model.getStartTime() == null) {
            model.setStartTime(doc.getTimestamp("start_time"));
        }
        if (model.getStartTime() == null) {
            model.setStartTime(doc.getTimestamp("startTime"));
        }

        if (model.getEndTime() == null) {
            model.setEndTime(doc.getTimestamp("end_time"));
        }
        if (model.getEndTime() == null) {
            model.setEndTime(doc.getTimestamp("endTime"));
        }

        if (model.getBookedAt() == null) {
            model.setBookedAt(doc.getTimestamp("booked_at"));
        }
        if (model.getBookedAt() == null) {
            model.setBookedAt(doc.getTimestamp("bookedAt"));
        }

        if (safeText(model.getNote(), "").isEmpty()) {
            model.setNote(safeText(doc.getString("note"), ""));
        }

        if (safeText(model.getDoctorNotes(), "").isEmpty()) {
            model.setDoctorNotes(safeText(doc.getString("doctor_notes"), ""));
        }
        if (safeText(model.getDoctorNotes(), "").isEmpty()) {
            model.setDoctorNotes(safeText(doc.getString("doctorNotes"), ""));
        }

        return model;
    }

    @NonNull
    private String extractRoomId(@NonNull DocumentSnapshot doc) {
        String roomId = safeText(doc.getString("room_id"), "");
        if (roomId.isEmpty()) {
            roomId = safeText(doc.getString("roomId"), "");
        }
        return roomId;
    }

    @NonNull
    private String extractFormatType(
            @NonNull ConsultationModel model,
            @NonNull DocumentSnapshot doc
    ) {
        String formatType = safeText(model.getFormatType(), "");
        if (formatType.isEmpty()) {
            formatType = safeText(doc.getString("format_type"), "");
        }
        if (formatType.isEmpty()) {
            formatType = safeText(doc.getString("formatType"), "");
        }
        return formatType;
    }

    @NonNull
    private String extractStatus(
            @NonNull ConsultationModel model,
            @NonNull DocumentSnapshot doc
    ) {
        String status = safeText(model.getStatus(), "");
        if (status.isEmpty()) {
            status = safeText(doc.getString("status"), "BOOKED");
        }
        return status;
    }

    @NonNull
    private String resolveSessionMode(@Nullable String formatType) {
        String normalized = safeText(formatType, "").toLowerCase(Locale.ROOT);

        if (normalized.contains("video")
                || normalized.contains("call")
                || normalized.contains("gọi")) {
            return MODE_CALL;
        }

        return MODE_CHAT;
    }

    @NonNull
    private String buildConsultationRoomId(@NonNull String sessionId) {
        return "consult_" + sessionId;
    }

    private void sortMessagesByTime(@NonNull List<ChatMessageModel> messages) {
        messages.sort((left, right) -> {
            long leftTime = extractComparableTime(left);
            long rightTime = extractComparableTime(right);
            return Long.compare(leftTime, rightTime);
        });
    }

    private long extractComparableTime(@Nullable ChatMessageModel message) {
        if (message == null) {
            return 0L;
        }

        if (message.getCreated_at() != null) {
            Date createdAt = message.getCreated_at().toDate();
            if (createdAt != null) {
                return createdAt.getTime();
            }
        }

        Long clientCreatedAt = message.getClient_created_at_ms();
        return clientCreatedAt != null ? clientCreatedAt : 0L;
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
