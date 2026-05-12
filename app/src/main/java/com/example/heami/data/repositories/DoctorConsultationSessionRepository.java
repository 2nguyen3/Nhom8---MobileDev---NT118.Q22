package com.example.heami.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.heami.data.models.ChatMessageModel;
import com.example.heami.data.models.ConsultationModel;
import com.google.firebase.Timestamp;
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

public class DoctorConsultationSessionRepository {

    public static final String MODE_CHAT = "CHAT";
    public static final String MODE_CALL = "CALL";

    public interface LoadConsultationListener {
        void onSuccess(@NonNull ConsultationModel consultation, @NonNull String sessionMode, @NonNull String roomId);
        void onFailure(@NonNull String errorMessage);
    }

    public interface ConsultationRealtimeListener {
        void onData(@NonNull ConsultationModel consultation, @NonNull String sessionMode, @NonNull String roomId);
        void onError(@NonNull String errorMessage);
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

    private final FirebaseFirestore firestore;

    public DoctorConsultationSessionRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void getConsultationById(
            @NonNull String sessionId,
            @NonNull LoadConsultationListener listener
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

                    listener.onSuccess(model, mode, roomId);
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
                            if (!"ACTIVE".equalsIgnoreCase(status)) {
                                continue;
                            }

                            messages.add(message);
                        }
                    }

                    sortMessagesByTime(messages);
                    listener.onSuccess(messages);
                });
    }

    public void sendConsultationMessageAsActor(
            @NonNull String roomId,
            @NonNull String actorId,
            @NonNull String text,
            @NonNull SendMessageListener listener
    ) {
        String trimmedRoomId = safeText(roomId, "");
        String trimmedActorId = safeText(actorId, "");
        String content = safeText(text, "");

        if (trimmedRoomId.isEmpty()) {
            listener.onFailure("Thiếu room_id của phòng chat");
            return;
        }

        if (trimmedActorId.isEmpty()) {
            listener.onFailure("Thiếu actor_id của bác sĩ");
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
                    if (!"ACTIVE".equalsIgnoreCase(roomStatus)) {
                        listener.onFailure("Phiên chat này đã kết thúc");
                        return;
                    }

                    String messageId = roomRef.collection("messages").document().getId();
                    long clientCreatedAtMs = System.currentTimeMillis();

                    HashMap<String, Object> messageData = new HashMap<>();
                    messageData.put("message_id", messageId);
                    messageData.put("sender_id", trimmedActorId);
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
                    roomUpdates.put("last_sender_id", trimmedActorId);
                    roomUpdates.put("last_message_id", messageId);
                    roomUpdates.put("unread_count_map." + trimmedActorId, 0L);

                    Object rawMemberIds = roomSnapshot.get("member_ids");
                    if (rawMemberIds instanceof List<?>) {
                        for (Object item : (List<?>) rawMemberIds) {
                            if (!(item instanceof String)) continue;

                            String memberId = safeText((String) item, "");
                            if (memberId.isEmpty() || memberId.equals(trimmedActorId)) {
                                continue;
                            }

                            roomUpdates.put(
                                    "unread_count_map." + memberId,
                                    FieldValue.increment(1)
                            );
                        }
                    }

                    WriteBatch batch = firestore.batch();
                    batch.set(roomRef.collection("messages").document(messageId), messageData);
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

    public void resetUnreadCount(
            @NonNull String roomId,
            @NonNull String actorId,
            @NonNull SimpleActionListener listener
    ) {
        String trimmedRoomId = safeText(roomId, "");
        String trimmedActorId = safeText(actorId, "");

        if (trimmedRoomId.isEmpty() || trimmedActorId.isEmpty()) {
            listener.onFailure("Thiếu room_id hoặc actor_id");
            return;
        }

        firestore.collection("chat_rooms")
                .document(trimmedRoomId)
                .update("unread_count_map." + trimmedActorId, 0L)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể reset unread count";
                    listener.onFailure(message);
                });
    }

    public void endConsultationSession(
            @NonNull String sessionId,
            @Nullable String roomId,
            @NonNull String actorId,
            @NonNull SimpleActionListener listener
    ) {
        String trimmedSessionId = safeText(sessionId, "");
        String trimmedRoomId = safeText(roomId, "");
        String trimmedActorId = safeText(actorId, "");

        if (trimmedSessionId.isEmpty()) {
            listener.onFailure("Thiếu session_id của phiên tư vấn");
            return;
        }

        if (trimmedActorId.isEmpty()) {
            listener.onFailure("Thiếu actor_id của bác sĩ");
            return;
        }

        Timestamp now = Timestamp.now();
        WriteBatch batch = firestore.batch();

        DocumentReference consultationRef = firestore.collection("consultations").document(trimmedSessionId);

        HashMap<String, Object> consultationUpdates = new HashMap<>();
        consultationUpdates.put("status", "COMPLETED");
        consultationUpdates.put("completed_at", now);
        consultationUpdates.put("completed_by", trimmedActorId);
        batch.update(consultationRef, consultationUpdates);

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

    public void markMessagesSeen(
            @NonNull String roomId,
            @NonNull String actorId,
            @NonNull List<ChatMessageModel> messages
    ) {
        String trimmedRoomId = safeText(roomId, "");
        String trimmedActorId = safeText(actorId, "");

        if (trimmedRoomId.isEmpty() || trimmedActorId.isEmpty()) {
            return;
        }

        WriteBatch batch = firestore.batch();
        boolean hasUpdates = false;

        for (ChatMessageModel message : messages) {
            String senderId = safeText(message.getSender_id(), "");
            String messageId = safeText(message.getMessage_id(), "");

            if (senderId.isEmpty() || senderId.equals(trimmedActorId) || messageId.isEmpty()) {
                continue;
            }

            List<String> seenIds = message.getSeen_user_ids();
            boolean alreadySeen = seenIds != null && seenIds.contains(trimmedActorId);
            if (alreadySeen) {
                continue;
            }

            DocumentReference messageRef = firestore.collection("chat_rooms")
                    .document(trimmedRoomId)
                    .collection("messages")
                    .document(messageId);

            batch.update(messageRef, "seen_user_ids", FieldValue.arrayUnion(trimmedActorId));
            batch.update(messageRef, "delivered_user_ids", FieldValue.arrayUnion(trimmedActorId));
            hasUpdates = true;
        }

        if (hasUpdates) {
            batch.commit();
        }
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

        if (model.getEndTime() == null) {
            model.setEndTime(doc.getTimestamp("end_time"));
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
    private String resolveSessionMode(@Nullable String formatType) {
        String normalized = safeText(formatType, "").toLowerCase(Locale.ROOT);

        if (normalized.contains("video")
                || normalized.contains("call")
                || normalized.contains("gọi")) {
            return MODE_CALL;
        }

        return MODE_CHAT;
    }

    private void sortMessagesByTime(@NonNull List<ChatMessageModel> messages) {
        messages.sort((left, right) -> Long.compare(extractComparableTime(left), extractComparableTime(right)));
    }

    private long extractComparableTime(@Nullable ChatMessageModel message) {
        if (message == null) return 0L;

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
