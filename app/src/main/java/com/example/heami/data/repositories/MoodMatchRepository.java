package com.example.heami.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.heami.data.models.ChatRoomModel;
import com.example.heami.data.models.MoodMatchModel;
import com.example.heami.data.models.MoodMatchRequestModel;
import com.example.heami.data.models.UserModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoodMatchRepository {

    private static final long REQUEST_TIMEOUT_MS = 12_000L;
    private static final int CANDIDATE_SCAN_LIMIT = 40;

    public interface StartMoodMatchListener {
        void onSearching(@NonNull MoodMatchSessionInfo sessionInfo);
        void onMatched(@NonNull MoodMatchSessionInfo sessionInfo);
        void onFailure(@NonNull String errorMessage);
    }

    public interface CheckMatchStatusListener {
        void onSearching(@NonNull MoodMatchSessionInfo sessionInfo);
        void onMatched(@NonNull MoodMatchSessionInfo sessionInfo);
        void onTimeout();
        void onCancelled();
        void onFailure(@NonNull String errorMessage);
    }

    public interface EnsureRoomListener {
        void onSuccess(@NonNull String roomId);
        void onFailure(@NonNull String errorMessage);
    }

    @NonNull
    private String buildMoodMatchRoomId(@NonNull String matchId) {
        return "mm_" + matchId;
    }

    public interface SimpleActionListener {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    public static class MoodMatchSessionInfo {
        private final String requestId;
        private final String matchId;
        private final String roomId;
        private final String status;
        private final String moodTag;
        private final Timestamp expiresAt;
        private final String matchedUserId;
        private final String matchedUserName;
        private final String matchedUserAvatar;

        public MoodMatchSessionInfo(
                @NonNull String requestId,
                @NonNull String matchId,
                @NonNull String roomId,
                @NonNull String status,
                @NonNull String moodTag,
                @Nullable Timestamp expiresAt,
                @NonNull String matchedUserId,
                @NonNull String matchedUserName,
                @NonNull String matchedUserAvatar
        ) {
            this.requestId = requestId;
            this.matchId = matchId;
            this.roomId = roomId;
            this.status = status;
            this.moodTag = moodTag;
            this.expiresAt = expiresAt;
            this.matchedUserId = matchedUserId;
            this.matchedUserName = matchedUserName;
            this.matchedUserAvatar = matchedUserAvatar;
        }

        @NonNull
        public String getRequestId() {
            return requestId;
        }

        @NonNull
        public String getMatchId() {
            return matchId;
        }

        @NonNull
        public String getRoomId() {
            return roomId;
        }

        @NonNull
        public String getStatus() {
            return status;
        }

        @NonNull
        public String getMoodTag() {
            return moodTag;
        }

        @Nullable
        public Timestamp getExpiresAt() {
            return expiresAt;
        }

        @NonNull
        public String getMatchedUserId() {
            return matchedUserId;
        }

        @NonNull
        public String getMatchedUserName() {
            return matchedUserName;
        }

        @NonNull
        public String getMatchedUserAvatar() {
            return matchedUserAvatar;
        }
    }

    private interface UserLoadCallback {
        void onSuccess(@NonNull UserModel user);
        void onFailure(@NonNull String errorMessage);
    }

    private interface RequestLoadCallback {
        void onSuccess(@Nullable MoodMatchRequestModel request);
        void onFailure(@NonNull String errorMessage);
    }

    private interface CandidateFindCallback {
        void onSuccess(@Nullable MoodMatchRequestModel candidate);
        void onFailure(@NonNull String errorMessage);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public MoodMatchRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void startMoodMatch(
            @NonNull String moodTag,
            @NonNull StartMoodMatchListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();
        String normalizedMoodTag = safeText(moodTag, "").toLowerCase();

        if (normalizedMoodTag.isEmpty()) {
            listener.onFailure("Thiếu mood để ghép cặp");
            return;
        }

        loadUserProfile(uid, new UserLoadCallback() {
            @Override
            public void onSuccess(@NonNull UserModel currentUser) {
                findSearchingRequestOfUser(uid, new RequestLoadCallback() {
                    @Override
                    public void onSuccess(@Nullable MoodMatchRequestModel existingRequest) {
                        if (existingRequest != null) {
                            if (isExpired(existingRequest.getExpires_at())) {
                                markRequestStatus(existingRequest.getRequest_id(), "TIMEOUT", new SimpleActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        createRequestAndTryMatch(uid, normalizedMoodTag, currentUser, listener);
                                    }

                                    @Override
                                    public void onFailure(@NonNull String errorMessage) {
                                        createRequestAndTryMatch(uid, normalizedMoodTag, currentUser, listener);
                                    }
                                });
                            } else {
                                tryImmediateMatch(existingRequest, currentUser, listener);
                            }
                            return;
                        }

                        createRequestAndTryMatch(uid, normalizedMoodTag, currentUser, listener);
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        listener.onFailure(errorMessage);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                listener.onFailure(errorMessage);
            }
        });
    }

    public void checkExistingMatchedRequest(
            @NonNull String requestId,
            @NonNull CheckMatchStatusListener listener
    ) {
        firestore.collection("mood_match_requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy yêu cầu ghép cặp");
                        return;
                    }

                    MoodMatchRequestModel request = documentSnapshot.toObject(MoodMatchRequestModel.class);
                    if (request == null) {
                        listener.onFailure("Không đọc được dữ liệu ghép cặp");
                        return;
                    }

                    request.setRequest_id(documentSnapshot.getId());
                    String status = safeText(request.getStatus(), "SEARCHING");

                    switch (status) {
                        case "MATCHED":
                            resolveMatchedSession(request, listener);
                            break;

                        case "TIMEOUT":
                            listener.onTimeout();
                            break;

                        case "CANCELLED":
                            listener.onCancelled();
                            break;

                        case "SEARCHING":
                        default:
                            listener.onSearching(buildSearchingSessionInfo(request));
                            break;
                    }
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể kiểm tra trạng thái ghép cặp";
                    listener.onFailure(message);
                });
    }

    public void cancelMoodMatchRequestSafely(
            @NonNull String requestId,
            @NonNull SimpleActionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        DocumentReference requestRef = firestore.collection("mood_match_requests").document(requestId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot requestSnap = transaction.get(requestRef);
                    if (!requestSnap.exists()) {
                        return null;
                    }

                    String status = safeText(requestSnap.getString("status"), "SEARCHING");
                    String matchId = safeText(requestSnap.getString("match_id"), "");

                    // Nếu đã MATCHED hoặc đã có match_id thì không cho hủy kiểu searching nữa
                    if (!"SEARCHING".equals(status) || !matchId.isEmpty()) {
                        return null;
                    }

                    transaction.update(
                            requestRef,
                            "status", "CANCELED",
                            "canceled_at", Timestamp.now()
                    );

                    return null;
                }).addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể hủy tìm kiếm";
                    listener.onFailure(message);
                });
    }

    public void cancelSearchingRequest(
            @NonNull String requestId,
            @NonNull SimpleActionListener listener
    ) {
        cancelMoodMatchRequestSafely(requestId, listener);
    }

    public void markRequestTimeout(
            @NonNull String requestId,
            @NonNull SimpleActionListener listener
    ) {
        firestore.collection("mood_match_requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onSuccess();
                        return;
                    }

                    MoodMatchRequestModel request = documentSnapshot.toObject(MoodMatchRequestModel.class);
                    if (request == null) {
                        listener.onFailure("Không đọc được dữ liệu ghép cặp");
                        return;
                    }

                    String status = safeText(request.getStatus(), "SEARCHING");

                    if (!"SEARCHING".equals(status)) {
                        listener.onSuccess();
                        return;
                    }

                    if (!isExpired(request.getExpires_at())) {
                        listener.onSuccess();
                        return;
                    }

                    markRequestStatus(requestId, "TIMEOUT", listener);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể cập nhật timeout";
                    listener.onFailure(message);
                });
    }

    public void endMoodMatch(
            @NonNull String matchId,
            @NonNull String roomId,
            @NonNull String endReason,
            @NonNull SimpleActionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        Timestamp now = Timestamp.now();

        Timestamp purgeAt = new Timestamp(
                new java.util.Date(
                        now.toDate().getTime() + java.util.concurrent.TimeUnit.HOURS.toMillis(1)
                )
        );

        firestore.collection("mood_matches")
                .document(matchId)
                .update(
                        "status", "ENDED",
                        "ended_at", now,
                        "ended_by", firebaseUser.getUid(),
                        "end_reason", safeText(endReason, "USER_ENDED")
                )
                .addOnSuccessListener(unused -> firestore.collection("chat_rooms")
                        .document(roomId)
                        .update(
                                "status", "ENDED",
                                "ended_at", now,
                                "purge_at", purgeAt
                        )
                        .addOnSuccessListener(unused2 -> listener.onSuccess())
                        .addOnFailureListener(e -> {
                            String message = e.getMessage() != null
                                    ? e.getMessage()
                                    : "Đã kết thúc match nhưng chưa cập nhật được chat room";
                            listener.onFailure(message);
                        }))
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể kết thúc MoodMatch";
                    listener.onFailure(message);
                });
    }

    public void ensureMoodMatchChatRoom(
            @NonNull String matchId,
            @NonNull String partnerUserId,
            @NonNull String moodTag,
            @NonNull EnsureRoomListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String currentUserId = firebaseUser.getUid();
        String roomId = buildMoodMatchRoomId(matchId);

        DocumentReference roomRef = firestore.collection("chat_rooms").document(roomId);
        DocumentReference matchRef = firestore.collection("mood_matches").document(matchId);
        DocumentReference myUserRef = firestore.collection("users").document(currentUserId);
        DocumentReference partnerUserRef = firestore.collection("users").document(partnerUserId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot matchSnap = transaction.get(matchRef);
                    if (!matchSnap.exists()) {
                        throw new RuntimeException("Không tìm thấy match");
                    }

                    String matchStatus = safeText(matchSnap.getString("status"), "ACTIVE");
                    if ("ENDED".equals(matchStatus) || "CANCELED".equals(matchStatus)) {
                        throw new RuntimeException("Match đã không còn hiệu lực");
                    }

                    DocumentSnapshot roomSnap = transaction.get(roomRef);
                    if (roomSnap.exists()) {
                        return roomId;
                    }

                    DocumentSnapshot myUserSnap = transaction.get(myUserRef);
                    DocumentSnapshot partnerUserSnap = transaction.get(partnerUserRef);

                    String myName = safeText(myUserSnap.getString("nickname"), "Người dùng ẩn danh");
                    String myAvatar = safeText(myUserSnap.getString("avatar_url"), "");

                    String partnerName = safeText(partnerUserSnap.getString("nickname"), "Người bạn ẩn danh");
                    String partnerAvatar = safeText(partnerUserSnap.getString("avatar_url"), "");

                    List<String> memberIds = new ArrayList<>();
                    List<String> memberNames = new ArrayList<>();
                    List<String> memberAvatars = new ArrayList<>();

                    if (currentUserId.compareTo(partnerUserId) <= 0) {
                        memberIds.add(currentUserId);
                        memberIds.add(partnerUserId);

                        memberNames.add(myName);
                        memberNames.add(partnerName);

                        memberAvatars.add(myAvatar);
                        memberAvatars.add(partnerAvatar);
                    } else {
                        memberIds.add(partnerUserId);
                        memberIds.add(currentUserId);

                        memberNames.add(partnerName);
                        memberNames.add(myName);

                        memberAvatars.add(partnerAvatar);
                        memberAvatars.add(myAvatar);
                    }

                    HashMap<String, Long> unreadMap = new HashMap<>();
                    unreadMap.put(currentUserId, 0L);
                    unreadMap.put(partnerUserId, 0L);

                    Timestamp now = Timestamp.now();

                    HashMap<String, Object> roomData = new HashMap<>();
                    roomData.put("room_id", roomId);
                    roomData.put("member_ids", memberIds);
                    roomData.put("member_names", memberNames);
                    roomData.put("member_avatars", memberAvatars);
                    roomData.put("type", "MOOD_MATCH");
                    roomData.put("related_id", matchId);
                    roomData.put("match_mood_tag", safeText(moodTag, "stress"));
                    roomData.put("created_at", now);
                    roomData.put("last_message", "");
                    roomData.put("last_message_id", "");
                    roomData.put("last_message_at", now);
                    roomData.put("last_sender_id", "");
                    roomData.put("status", "ACTIVE");
                    roomData.put("unread_count_map", unreadMap);

                    transaction.set(roomRef, roomData);
                    transaction.update(matchRef, "room_id", roomId);

                    return roomId;
                }).addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tạo phòng chat";
                    listener.onFailure(message);
                });
    }

    public void reportChatConversation(
            @NonNull String roomId,
            @NonNull String matchId,
            @NonNull String reason,
            @NonNull String note,
            @NonNull SimpleActionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String reportId = firestore.collection("chat_reports").document().getId();

        HashMap<String, Object> reportData = new HashMap<>();
        reportData.put("report_id", reportId);
        reportData.put("room_id", roomId);
        reportData.put("match_id", matchId);
        reportData.put("reported_by", firebaseUser.getUid());
        reportData.put("reason", safeText(reason, "UNSAFE_CHAT"));
        reportData.put("note", safeText(note, ""));
        reportData.put("status", "PENDING");
        reportData.put("created_at", FieldValue.serverTimestamp());
        reportData.put("reviewed_by", "");
        reportData.put("reviewed_at", null);

        firestore.collection("chat_reports")
                .document(reportId)
                .set(reportData)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể gửi báo cáo cuộc trò chuyện";
                    listener.onFailure(message);
                });
    }

    private void createRequestAndTryMatch(
            @NonNull String uid,
            @NonNull String moodTag,
            @NonNull UserModel currentUser,
            @NonNull StartMoodMatchListener listener
    ) {
        String requestId = firestore.collection("mood_match_requests").document().getId();
        Timestamp now = Timestamp.now();
        Timestamp expiresAt = new Timestamp(now.getSeconds() + (REQUEST_TIMEOUT_MS / 1000L), now.getNanoseconds());

        MoodMatchRequestModel request = new MoodMatchRequestModel(
                requestId,
                uid,
                moodTag,
                now,
                expiresAt
        );

        firestore.collection("mood_match_requests")
                .document(requestId)
                .set(request)
                .addOnSuccessListener(unused -> tryImmediateMatch(request, currentUser, listener))
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tạo yêu cầu ghép cặp";
                    listener.onFailure(message);
                });
    }

    private void tryImmediateMatch(
            @NonNull MoodMatchRequestModel myRequest,
            @NonNull UserModel currentUser,
            @NonNull StartMoodMatchListener listener
    ) {
        findBestCandidate(myRequest, new CandidateFindCallback() {
            @Override
            public void onSuccess(@Nullable MoodMatchRequestModel candidate) {
                if (candidate == null) {
                    listener.onSearching(buildSearchingSessionInfo(myRequest));
                    return;
                }

                loadUserProfile(candidate.getUser_id(), new UserLoadCallback() {
                    @Override
                    public void onSuccess(@NonNull UserModel candidateUser) {
                        createMatchTransaction(myRequest, candidate, currentUser, candidateUser, listener);
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        listener.onSearching(buildSearchingSessionInfo(myRequest));
                    }
                });
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                listener.onFailure(errorMessage);
            }
        });
    }

    private void createMatchTransaction(
            @NonNull MoodMatchRequestModel myRequest,
            @NonNull MoodMatchRequestModel candidateRequest,
            @NonNull UserModel currentUser,
            @NonNull UserModel candidateUser,
            @NonNull StartMoodMatchListener listener
    ) {
        DocumentReference myRequestRef = firestore.collection("mood_match_requests")
                .document(myRequest.getRequest_id());
        DocumentReference candidateRequestRef = firestore.collection("mood_match_requests")
                .document(candidateRequest.getRequest_id());

        String matchId = firestore.collection("mood_matches").document().getId();
        String roomId = buildMoodMatchRoomId(matchId);

        DocumentReference matchRef = firestore.collection("mood_matches").document(matchId);
        DocumentReference roomRef = firestore.collection("chat_rooms").document(roomId);

        Timestamp now = Timestamp.now();

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot myRequestSnapshot = transaction.get(myRequestRef);
                    DocumentSnapshot candidateSnapshot = transaction.get(candidateRequestRef);

                    if (!myRequestSnapshot.exists()) {
                        throw new RuntimeException("Yêu cầu ghép cặp của bạn không còn tồn tại");
                    }

                    if (!candidateSnapshot.exists()) {
                        throw new RuntimeException("Không tìm thấy người phù hợp");
                    }

                    MoodMatchRequestModel latestMine = myRequestSnapshot.toObject(MoodMatchRequestModel.class);
                    MoodMatchRequestModel latestCandidate = candidateSnapshot.toObject(MoodMatchRequestModel.class);

                    if (latestMine == null || latestCandidate == null) {
                        throw new RuntimeException("Không đọc được dữ liệu ghép cặp");
                    }

                    String myStatus = safeText(latestMine.getStatus(), "SEARCHING");
                    String candidateStatus = safeText(latestCandidate.getStatus(), "SEARCHING");

                    if (!"SEARCHING".equals(myStatus)) {
                        throw new RuntimeException("Yêu cầu ghép cặp của bạn không còn ở trạng thái chờ");
                    }

                    if (!"SEARCHING".equals(candidateStatus)) {
                        throw new RuntimeException("Người kia vừa thoát khỏi hàng chờ");
                    }

                    if (isExpired(latestMine.getExpires_at())) {
                        throw new RuntimeException("Yêu cầu ghép cặp của bạn đã hết hạn");
                    }

                    if (isExpired(latestCandidate.getExpires_at())) {
                        throw new RuntimeException("Yêu cầu của người kia đã hết hạn");
                    }

                    String myUserId = safeText(latestMine.getUser_id(), "");
                    String candidateUserId = safeText(latestCandidate.getUser_id(), "");

                    if (myUserId.equals(candidateUserId)) {
                        throw new RuntimeException("Không thể ghép với chính mình");
                    }

                    MoodMatchModel matchModel = new MoodMatchModel(
                            matchId,
                            myUserId,
                            candidateUserId,
                            safeText(latestMine.getMood_tag(), ""),
                            now
                    );
                    matchModel.setRoom_id(roomId);

                    List<String> memberIds = new ArrayList<>();
                    List<String> memberNames = new ArrayList<>();
                    List<String> memberAvatars = new ArrayList<>();

                    if (myUserId.compareTo(candidateUserId) <= 0) {
                        memberIds.add(myUserId);
                        memberIds.add(candidateUserId);

                        memberNames.add(safeText(currentUser.getNickname(), "Người dùng Heami"));
                        memberNames.add(safeText(candidateUser.getNickname(), "Người dùng Heami"));

                        memberAvatars.add(safeText(currentUser.getAvatar_url(), ""));
                        memberAvatars.add(safeText(candidateUser.getAvatar_url(), ""));
                    } else {
                        memberIds.add(candidateUserId);
                        memberIds.add(myUserId);

                        memberNames.add(safeText(candidateUser.getNickname(), "Người dùng Heami"));
                        memberNames.add(safeText(currentUser.getNickname(), "Người dùng Heami"));

                        memberAvatars.add(safeText(candidateUser.getAvatar_url(), ""));
                        memberAvatars.add(safeText(currentUser.getAvatar_url(), ""));
                    }

                    HashMap<String, Long> unreadMap = new HashMap<>();
                    unreadMap.put(myUserId, 0L);
                    unreadMap.put(candidateUserId, 0L);

                    ChatRoomModel roomModel = new ChatRoomModel(
                            roomId,
                            memberIds,
                            memberNames,
                            memberAvatars,
                            "MOOD_MATCH",
                            matchId,
                            safeText(latestMine.getMood_tag(), ""),
                            now
                    );
                    roomModel.setUnread_count_map(unreadMap);
                    roomModel.setLast_message_id("");

                    transaction.set(matchRef, matchModel);
                    transaction.set(roomRef, roomModel);

                    transaction.update(
                            myRequestRef,
                            "status", "MATCHED",
                            "matched_user_id", candidateUserId,
                            "match_id", matchId
                    );

                    transaction.update(
                            candidateRequestRef,
                            "status", "MATCHED",
                            "matched_user_id", myUserId,
                            "match_id", matchId
                    );

                    return new MoodMatchSessionInfo(
                            safeText(latestMine.getRequest_id(), myRequest.getRequest_id()),
                            matchId,
                            roomId,
                            "MATCHED",
                            safeText(latestMine.getMood_tag(), ""),
                            latestMine.getExpires_at(),
                            candidateUserId,
                            safeText(candidateUser.getNickname(), "Người dùng Heami"),
                            safeText(candidateUser.getAvatar_url(), "")
                    );
                }).addOnSuccessListener(listener::onMatched)
                .addOnFailureListener(e -> listener.onSearching(buildSearchingSessionInfo(myRequest)));
    }

    private void findBestCandidate(
            @NonNull MoodMatchRequestModel myRequest,
            @NonNull CandidateFindCallback callback
    ) {
        firestore.collection("mood_match_requests")
                .whereEqualTo("status", "SEARCHING")
                .limit(CANDIDATE_SCAN_LIMIT)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MoodMatchRequestModel> allRequests = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        MoodMatchRequestModel request = document.toObject(MoodMatchRequestModel.class);
                        if (request == null) continue;

                        if (request.getRequest_id() == null || request.getRequest_id().trim().isEmpty()) {
                            request.setRequest_id(document.getId());
                        }

                        allRequests.add(request);
                    }

                    // Sắp xếp in-memory theo created_at tăng dần (ưu tiên người chờ lâu nhất)
                    // để không cần composite index trên Firestore
                    allRequests.sort((a, b) -> {
                        com.google.firebase.Timestamp tA = a.getCreated_at();
                        com.google.firebase.Timestamp tB = b.getCreated_at();
                        if (tA == null && tB == null) return 0;
                        if (tA == null) return 1;
                        if (tB == null) return -1;
                        return tA.compareTo(tB);
                    });

                    MoodMatchRequestModel exactCandidate = null;
                    MoodMatchRequestModel nearCandidate = null;

                    for (MoodMatchRequestModel request : allRequests) {
                        if (!isValidSearchingCandidate(request, myRequest)) {
                            continue;
                        }

                        String candidateMood = safeText(request.getMood_tag(), "");
                        String myMood = safeText(myRequest.getMood_tag(), "");

                        if (candidateMood.equals(myMood)) {
                            exactCandidate = request;
                            break;
                        }

                        if (isNearbyMood(myMood, candidateMood) && nearCandidate == null) {
                            nearCandidate = request;
                        }
                    }

                    if (exactCandidate != null) {
                        callback.onSuccess(exactCandidate);
                        return;
                    }

                    callback.onSuccess(nearCandidate);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tìm người phù hợp lúc này";
                    callback.onFailure(message);
                });
    }

    private boolean isValidSearchingCandidate(
            @Nullable MoodMatchRequestModel candidate,
            @NonNull MoodMatchRequestModel myRequest
    ) {
        if (candidate == null) return false;

        String candidateRequestId = safeText(candidate.getRequest_id(), "");
        String myRequestId = safeText(myRequest.getRequest_id(), "");

        if (candidateRequestId.equals(myRequestId)) return false;

        String candidateUserId = safeText(candidate.getUser_id(), "");
        String myUserId = safeText(myRequest.getUser_id(), "");

        if (candidateUserId.isEmpty() || candidateUserId.equals(myUserId)) return false;
        if (!"SEARCHING".equals(safeText(candidate.getStatus(), ""))) return false;
        if (isExpired(candidate.getExpires_at())) return false;

        return true;
    }

    private boolean isNearbyMood(@NonNull String baseMood, @NonNull String candidateMood) {
        switch (baseMood) {
            case "sad":
                return "stress".equals(candidateMood) || "fear".equals(candidateMood);

            case "stress":
                return "sad".equals(candidateMood)
                        || "fear".equals(candidateMood)
                        || "angry".equals(candidateMood);

            case "fear":
                return "sad".equals(candidateMood) || "stress".equals(candidateMood);

            case "angry":
                return "stress".equals(candidateMood) || "sad".equals(candidateMood);

            case "happy":
                return false;

            default:
                return false;
        }
    }

    private void resolveMatchedSession(
            @NonNull MoodMatchRequestModel request,
            @NonNull CheckMatchStatusListener listener
    ) {
        String matchId = safeText(request.getMatch_id(), "");

        if (matchId.isEmpty()) {
            listener.onFailure("Thiếu thông tin match");
            return;
        }

        firestore.collection("mood_matches")
                .document(matchId)
                .get()
                .addOnSuccessListener(matchSnapshot -> {
                    if (!matchSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy match");
                        return;
                    }

                    MoodMatchModel match = matchSnapshot.toObject(MoodMatchModel.class);
                    if (match == null) {
                        listener.onFailure("Không đọc được dữ liệu match");
                        return;
                    }

                    if (match.getMatch_id() == null || match.getMatch_id().trim().isEmpty()) {
                        match.setMatch_id(matchSnapshot.getId());
                    }

                    final String roomId = safeText(match.getRoom_id(), "");
                    final String currentUserId = safeText(request.getUser_id(), "");

                    final String initialPartnerUserId =
                            safeText(match.getUser_a_id(), "").equals(currentUserId)
                                    ? safeText(match.getUser_b_id(), "")
                                    : safeText(match.getUser_a_id(), "");

                    if (roomId.isEmpty()) {
                        loadUserProfile(initialPartnerUserId, new UserLoadCallback() {
                            @Override
                            public void onSuccess(@NonNull UserModel partnerUser) {
                                listener.onMatched(new MoodMatchSessionInfo(
                                        safeText(request.getRequest_id(), ""),
                                        safeText(match.getMatch_id(), ""),
                                        "",
                                        "MATCHED",
                                        safeText(request.getMood_tag(), ""),
                                        request.getExpires_at(),
                                        initialPartnerUserId,
                                        safeText(partnerUser.getNickname(), "Người dùng Heami"),
                                        safeText(partnerUser.getAvatar_url(), "")
                                ));
                            }

                            @Override
                            public void onFailure(@NonNull String errorMessage) {
                                listener.onMatched(new MoodMatchSessionInfo(
                                        safeText(request.getRequest_id(), ""),
                                        safeText(match.getMatch_id(), ""),
                                        "",
                                        "MATCHED",
                                        safeText(request.getMood_tag(), ""),
                                        request.getExpires_at(),
                                        initialPartnerUserId,
                                        "Người dùng Heami",
                                        ""
                                ));
                            }
                        });
                        return;
                    }

                    firestore.collection("chat_rooms")
                            .document(roomId)
                            .get()
                            .addOnSuccessListener(roomSnapshot -> {
                                if (!roomSnapshot.exists()) {
                                    listener.onFailure("Không tìm thấy phòng chat");
                                    return;
                                }

                                ChatRoomModel room = roomSnapshot.toObject(ChatRoomModel.class);
                                if (room == null) {
                                    listener.onFailure("Không đọc được dữ liệu phòng chat");
                                    return;
                                }

                                String resolvedPartnerUserId = initialPartnerUserId;
                                String partnerName = "Người dùng Heami";
                                String partnerAvatar = "";

                                List<String> memberIds = room.getMember_ids();
                                List<String> memberNames = room.getMember_names();
                                List<String> memberAvatars = room.getMember_avatars();

                                if (memberIds != null) {
                                    for (int i = 0; i < memberIds.size(); i++) {
                                        String memberId = safeText(memberIds.get(i), "");
                                        if (!memberId.equals(currentUserId)) {
                                            resolvedPartnerUserId = memberId;

                                            if (memberNames != null && i < memberNames.size()) {
                                                partnerName = safeText(memberNames.get(i), "Người dùng Heami");
                                            }

                                            if (memberAvatars != null && i < memberAvatars.size()) {
                                                partnerAvatar = safeText(memberAvatars.get(i), "");
                                            }
                                            break;
                                        }
                                    }
                                }

                                listener.onMatched(new MoodMatchSessionInfo(
                                        safeText(request.getRequest_id(), ""),
                                        safeText(match.getMatch_id(), ""),
                                        roomId,
                                        "MATCHED",
                                        safeText(request.getMood_tag(), ""),
                                        request.getExpires_at(),
                                        resolvedPartnerUserId,
                                        partnerName,
                                        partnerAvatar
                                ));
                            })
                            .addOnFailureListener(e -> {
                                String message = e.getMessage() != null
                                        ? e.getMessage()
                                        : "Không thể tải phòng chat";
                                listener.onFailure(message);
                            });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải match";
                    listener.onFailure(message);
                });
    }

    private MoodMatchSessionInfo buildSearchingSessionInfo(@NonNull MoodMatchRequestModel request) {
        return new MoodMatchSessionInfo(
                safeText(request.getRequest_id(), ""),
                safeText(request.getMatch_id(), ""),
                "",
                safeText(request.getStatus(), "SEARCHING"),
                safeText(request.getMood_tag(), ""),
                request.getExpires_at(),
                safeText(request.getMatched_user_id(), ""),
                "",
                ""
        );
    }

    private void findSearchingRequestOfUser(
            @NonNull String uid,
            @NonNull RequestLoadCallback callback
    ) {
        firestore.collection("mood_match_requests")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("status", "SEARCHING")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }

                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                    MoodMatchRequestModel request = document.toObject(MoodMatchRequestModel.class);

                    if (request == null) {
                        callback.onSuccess(null);
                        return;
                    }

                    if (request.getRequest_id() == null || request.getRequest_id().trim().isEmpty()) {
                        request.setRequest_id(document.getId());
                    }

                    callback.onSuccess(request);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể kiểm tra yêu cầu ghép cặp hiện tại";
                    callback.onFailure(message);
                });
    }

    private void loadUserProfile(
            @NonNull String uid,
            @NonNull UserLoadCallback callback
    ) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure("Không tìm thấy thông tin người dùng");
                        return;
                    }

                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user == null) {
                        callback.onFailure("Không đọc được dữ liệu người dùng");
                        return;
                    }

                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin người dùng";
                    callback.onFailure(message);
                });
    }

    private void markRequestStatus(
            @NonNull String requestId,
            @NonNull String status,
            @NonNull SimpleActionListener listener
    ) {
        firestore.collection("mood_match_requests")
                .document(requestId)
                .update("status", status)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể cập nhật trạng thái yêu cầu";
                    listener.onFailure(message);
                });
    }

    private boolean isExpired(@Nullable Timestamp expiresAt) {
        if (expiresAt == null) return false;
        return expiresAt.toDate().getTime() <= System.currentTimeMillis();
    }

    public void setRoomPinned(
            @NonNull String roomId,
            boolean pinned,
            @NonNull SimpleActionListener listener
    ) {
        updateRoomUserFlag(roomId, "pinned_by_map", pinned, listener);
    }

    public void setRoomMuted(
            @NonNull String roomId,
            boolean muted,
            @NonNull SimpleActionListener listener
    ) {
        updateRoomUserFlag(roomId, "muted_by_map", muted, listener);
    }

    public void setRoomArchived(
            @NonNull String roomId,
            boolean archived,
            @NonNull SimpleActionListener listener
    ) {
        updateRoomUserFlag(roomId, "archived_by_map", archived, listener);
    }

    private void updateRoomUserFlag(
            @NonNull String roomId,
            @NonNull String fieldPrefix,
            boolean enabled,
            @NonNull SimpleActionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();
        Object value = enabled ? true : FieldValue.delete();

        firestore.collection("chat_rooms")
                .document(roomId)
                .update(fieldPrefix + "." + uid, value)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể cập nhật trạng thái room";
                    listener.onFailure(message);
                });
    }

    public void cleanupCurrentUserCommunityGarbage(
            @NonNull SimpleActionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("mood_match_requests")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("status", "SEARCHING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        listener.onSuccess();
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = firestore.batch();
                    boolean hasWrite = false;
                    long nowMs = System.currentTimeMillis();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Timestamp expiresAt = doc.getTimestamp("expires_at");
                        if (expiresAt != null && expiresAt.toDate().getTime() <= nowMs) {
                            batch.update(
                                    doc.getReference(),
                                    "status", "TIMEOUT"
                            );
                            hasWrite = true;
                        }
                    }

                    if (!hasWrite) {
                        listener.onSuccess();
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> listener.onSuccess())
                            .addOnFailureListener(e -> {
                                String message = e.getMessage() != null
                                        ? e.getMessage()
                                        : "Không thể cleanup request timeout";
                                listener.onFailure(message);
                            });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải request để cleanup";
                    listener.onFailure(message);
                });
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}