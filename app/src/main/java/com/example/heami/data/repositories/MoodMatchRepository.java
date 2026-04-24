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

    public void cancelSearchingRequest(
            @NonNull String requestId,
            @NonNull SimpleActionListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

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

                    String ownerId = safeText(request.getUser_id(), "");
                    String status = safeText(request.getStatus(), "SEARCHING");

                    if (!firebaseUser.getUid().equals(ownerId)) {
                        listener.onFailure("Bạn không có quyền hủy yêu cầu này");
                        return;
                    }

                    if (!"SEARCHING".equals(status)) {
                        listener.onSuccess();
                        return;
                    }

                    markRequestStatus(requestId, "CANCELLED", listener);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể hủy tìm kiếm";
                    listener.onFailure(message);
                });
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
                        .update("status", "ENDED")
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
        String roomId = firestore.collection("chat_rooms").document().getId();

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

                    if (safeText(latestMine.getUser_id(), "").equals(safeText(latestCandidate.getUser_id(), ""))) {
                        throw new RuntimeException("Không thể ghép với chính mình");
                    }

                    MoodMatchModel matchModel = new MoodMatchModel(
                            matchId,
                            safeText(latestMine.getUser_id(), ""),
                            safeText(latestCandidate.getUser_id(), ""),
                            safeText(latestMine.getMood_tag(), ""),
                            now
                    );
                    matchModel.setRoom_id(roomId);

                    List<String> memberIds = Arrays.asList(
                            safeText(latestMine.getUser_id(), ""),
                            safeText(latestCandidate.getUser_id(), "")
                    );

                    List<String> memberNames = Arrays.asList(
                            safeText(currentUser.getNickname(), "Người dùng Heami"),
                            safeText(candidateUser.getNickname(), "Người dùng Heami")
                    );

                    List<String> memberAvatars = Arrays.asList(
                            safeText(currentUser.getAvatar_url(), ""),
                            safeText(candidateUser.getAvatar_url(), "")
                    );

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

                    transaction.set(matchRef, matchModel);
                    transaction.set(roomRef, roomModel);

                    transaction.update(
                            myRequestRef,
                            "status", "MATCHED",
                            "matched_user_id", safeText(latestCandidate.getUser_id(), ""),
                            "match_id", matchId
                    );

                    transaction.update(
                            candidateRequestRef,
                            "status", "MATCHED",
                            "matched_user_id", safeText(latestMine.getUser_id(), ""),
                            "match_id", matchId
                    );

                    return new MoodMatchSessionInfo(
                            safeText(latestMine.getRequest_id(), myRequest.getRequest_id()),
                            matchId,
                            roomId,
                            "MATCHED",
                            safeText(latestMine.getMood_tag(), ""),
                            latestMine.getExpires_at(),
                            safeText(latestCandidate.getUser_id(), ""),
                            safeText(candidateUser.getNickname(), "Người dùng Heami"),
                            safeText(candidateUser.getAvatar_url(), "")
                    );
                }).addOnSuccessListener(listener::onMatched)
                .addOnFailureListener(e -> {
                    listener.onSearching(buildSearchingSessionInfo(myRequest));
                });
    }

    private void findBestCandidate(
            @NonNull MoodMatchRequestModel myRequest,
            @NonNull CandidateFindCallback callback
    ) {
        firestore.collection("mood_match_requests")
                .orderBy("created_at", Query.Direction.ASCENDING)
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

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}