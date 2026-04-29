package com.example.heami.data.repositories;

import androidx.annotation.NonNull;

import com.example.heami.data.models.CommunityPostModel;
import com.example.heami.data.models.PostCommentModel;
import com.example.heami.data.models.PostEmpathyModel;
import com.example.heami.data.models.PostHugModel;
import com.example.heami.data.models.PostReportModel;
import com.example.heami.data.models.UserModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommunityRepository {

    private static final int AUTO_HIDE_REPORT_THRESHOLD = 5;

    public interface CreatePostListener {
        void onSuccess(@NonNull String postId);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadPostsListener {
        void onSuccess(@NonNull List<CommunityPostModel> posts);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadSinglePostListener {
        void onSuccess(@NonNull CommunityPostModel post);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadCommentsListener {
        void onSuccess(@NonNull List<PostCommentModel> comments);
        void onFailure(@NonNull String errorMessage);
    }

    public interface CreateCommentListener {
        void onSuccess(@NonNull String commentId);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadMyHugCountListener {
        void onSuccess(int myHugCount);
        void onFailure(@NonNull String errorMessage);
    }

    public interface AddHugListener {
        void onSuccess(int newMyHugCount);
        void onFailure(@NonNull String errorMessage);
    }

    public interface ClearMyHugsListener {
        void onSuccess(int removedHugCount);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadPostHugsListener {
        void onSuccess(@NonNull List<PostHugModel> hugs);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadMyEmpathyStateListener {
        void onSuccess(boolean hasEmpathy);
        void onFailure(@NonNull String errorMessage);
    }

    public interface ToggleEmpathyListener {
        void onSuccess(boolean nowHasEmpathy);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadPostEmpathiesListener {
        void onSuccess(@NonNull List<PostEmpathyModel> empathies);
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadMyReportStateListener {
        void onSuccess(boolean hasReported);
        void onFailure(@NonNull String errorMessage);
    }

    public interface SubmitReportListener {
        void onSuccess(boolean autoHidden);
        void onAlreadyReported();
        void onFailure(@NonNull String errorMessage);
    }

    public interface UndoReportListener {
        void onSuccess(boolean postVisibleAgain);
        void onFailure(@NonNull String errorMessage);
    }

    public interface UpdatePostListener {
        void onSuccess(@NonNull Timestamp updatedAt);
        void onFailure(@NonNull String errorMessage);
    }

    public interface DeletePostListener {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    public interface LoadCommunityDashboardStatsListener {
        void onSuccess(@NonNull CommunityDashboardStats stats);
        void onFailure(@NonNull String errorMessage);
    }

    public static class CommunityDashboardStats {
        private final int activeTodayUserCount;
        private final int unreadChatRoomCount;
        private final int searchingCount;
        private final int activeMoodRoomCount;
        private final int averageMatchSeconds;

        public CommunityDashboardStats(
                int activeTodayUserCount,
                int unreadChatRoomCount,
                int searchingCount,
                int activeMoodRoomCount,
                int averageMatchSeconds
        ) {
            this.activeTodayUserCount = activeTodayUserCount;
            this.unreadChatRoomCount = unreadChatRoomCount;
            this.searchingCount = searchingCount;
            this.activeMoodRoomCount = activeMoodRoomCount;
            this.averageMatchSeconds = averageMatchSeconds;
        }

        public int getActiveTodayUserCount() {
            return activeTodayUserCount;
        }

        public int getUnreadChatRoomCount() {
            return unreadChatRoomCount;
        }

        public int getSearchingCount() {
            return searchingCount;
        }

        public int getActiveMoodRoomCount() {
            return activeMoodRoomCount;
        }

        public int getAverageMatchSeconds() {
            return averageMatchSeconds;
        }
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public CommunityRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void createCommunityPost(
            @NonNull String content,
            @NonNull String moodTag,
            @NonNull String moodEmoji,
            boolean isAnonymous,
            @NonNull CreatePostListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy thông tin người dùng");
                        return;
                    }

                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user == null) {
                        listener.onFailure("Không đọc được dữ liệu người dùng");
                        return;
                    }

                    String postId = firestore.collection("community_posts").document().getId();
                    Timestamp now = Timestamp.now();

                    String authorName = isAnonymous
                            ? "Ẩn danh"
                            : safeText(user.getNickname(), "Người dùng Heami");

                    String authorAvatar = isAnonymous
                            ? ""
                            : safeText(user.getAvatar_url(), "");

                    CommunityPostModel post = new CommunityPostModel(
                            postId,
                            uid,
                            content.trim(),
                            moodTag,
                            moodEmoji,
                            isAnonymous,
                            authorName,
                            authorAvatar,
                            now
                    );

                    firestore.collection("community_posts")
                            .document(postId)
                            .set(post)
                            .addOnSuccessListener(unused -> listener.onSuccess(postId))
                            .addOnFailureListener(e -> {
                                String message = e.getMessage() != null
                                        ? e.getMessage()
                                        : "Không thể đăng bài viết";
                                listener.onFailure(message);
                            });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin người dùng";
                    listener.onFailure(message);
                });
    }

    public void getCommunityPosts(@NonNull LoadPostsListener listener) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            loadPostsWithHiddenFilter(new HashSet<>(), listener);
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("users")
                .document(uid)
                .collection("hidden_posts")
                .get()
                .addOnSuccessListener(hiddenSnapshots -> {
                    Set<String> hiddenPostIds = new HashSet<>();

                    if (hiddenSnapshots != null) {
                        for (DocumentSnapshot document : hiddenSnapshots.getDocuments()) {
                            hiddenPostIds.add(document.getId());
                        }
                    }

                    loadPostsWithHiddenFilter(hiddenPostIds, listener);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải danh sách bài viết đã ẩn";
                    listener.onFailure(message);
                });
    }

    private void loadPostsWithHiddenFilter(
            @NonNull Set<String> hiddenPostIds,
            @NonNull LoadPostsListener listener
    ) {
        firestore.collection("community_posts")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CommunityPostModel> posts = new ArrayList<>();

                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            CommunityPostModel post = document.toObject(CommunityPostModel.class);
                            if (post == null) continue;

                            if (post.getPost_id() == null || post.getPost_id().trim().isEmpty()) {
                                post.setPost_id(document.getId());
                            }

                            String postId = safeText(post.getPost_id(), "");
                            String status = safeText(post.getStatus(), "ACTIVE");

                            if (!"ACTIVE".equals(status)) continue;
                            if (hiddenPostIds.contains(postId)) continue;

                            posts.add(post);
                        }
                    }

                    listener.onSuccess(posts);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải danh sách chia sẻ";
                    listener.onFailure(message);
                });
    }

    public void getPostById(
            @NonNull String postId,
            @NonNull LoadSinglePostListener listener
    ) {
        firestore.collection("community_posts")
                .document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy bài viết");
                        return;
                    }

                    CommunityPostModel post = documentSnapshot.toObject(CommunityPostModel.class);
                    if (post == null) {
                        listener.onFailure("Không đọc được dữ liệu bài viết");
                        return;
                    }

                    if (post.getPost_id() == null || post.getPost_id().trim().isEmpty()) {
                        post.setPost_id(documentSnapshot.getId());
                    }

                    listener.onSuccess(post);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải bài viết";
                    listener.onFailure(message);
                });
    }

    public void getPostComments(
            @NonNull String postId,
            @NonNull LoadCommentsListener listener
    ) {
        firestore.collection("community_posts")
                .document(postId)
                .collection("comments")
                .orderBy("created_at", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PostCommentModel> comments = new ArrayList<>();

                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            PostCommentModel comment = document.toObject(PostCommentModel.class);
                            if (comment != null) {
                                if (comment.getComment_id() == null || comment.getComment_id().trim().isEmpty()) {
                                    comment.setComment_id(document.getId());
                                }
                                if (comment.getPost_id() == null || comment.getPost_id().trim().isEmpty()) {
                                    comment.setPost_id(postId);
                                }
                                comments.add(comment);
                            }
                        }
                    }

                    listener.onSuccess(comments);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải bình luận";
                    listener.onFailure(message);
                });
    }

    public void createPostComment(
            @NonNull String postId,
            @NonNull String content,
            boolean isAnonymous,
            @NonNull CreateCommentListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy thông tin người dùng");
                        return;
                    }

                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user == null) {
                        listener.onFailure("Không đọc được dữ liệu người dùng");
                        return;
                    }

                    String commentId = firestore.collection("community_posts")
                            .document(postId)
                            .collection("comments")
                            .document()
                            .getId();

                    Timestamp now = Timestamp.now();

                    String authorName = isAnonymous
                            ? "Ẩn danh"
                            : safeText(user.getNickname(), "Người dùng Heami");

                    String authorAvatar = isAnonymous
                            ? ""
                            : safeText(user.getAvatar_url(), "");

                    PostCommentModel comment = new PostCommentModel(
                            commentId,
                            postId,
                            uid,
                            content.trim(),
                            isAnonymous,
                            authorName,
                            authorAvatar,
                            now
                    );

                    firestore.collection("community_posts")
                            .document(postId)
                            .collection("comments")
                            .document(commentId)
                            .set(comment)
                            .addOnSuccessListener(unused -> firestore.collection("community_posts")
                                    .document(postId)
                                    .update(
                                            "comment_count", FieldValue.increment(1),
                                            "updated_at", now
                                    )
                                    .addOnSuccessListener(unused2 -> listener.onSuccess(commentId))
                                    .addOnFailureListener(e -> {
                                        String message = e.getMessage() != null
                                                ? e.getMessage()
                                                : "Đã tạo bình luận nhưng không cập nhật được số lượng";
                                        listener.onFailure(message);
                                    }))
                            .addOnFailureListener(e -> {
                                String message = e.getMessage() != null
                                        ? e.getMessage()
                                        : "Không thể gửi bình luận";
                                listener.onFailure(message);
                            });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin người dùng";
                    listener.onFailure(message);
                });
    }

    public void getMyHugCountForPost(
            @NonNull String postId,
            @NonNull LoadMyHugCountListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("community_posts")
                .document(postId)
                .collection("hugs")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onSuccess(0);
                        return;
                    }

                    int hugCount = safeInt(documentSnapshot.get("hug_count"));
                    listener.onSuccess(hugCount);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải số ôm của bạn";
                    listener.onFailure(message);
                });
    }

    public void addHugToPost(
            @NonNull String postId,
            @NonNull AddHugListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!userSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy thông tin người dùng");
                        return;
                    }

                    UserModel user = userSnapshot.toObject(UserModel.class);
                    if (user == null) {
                        listener.onFailure("Không đọc được dữ liệu người dùng");
                        return;
                    }

                    DocumentReference postRef = firestore.collection("community_posts").document(postId);
                    DocumentReference hugRef = postRef.collection("hugs").document(uid);
                    Timestamp now = Timestamp.now();

                    String authorName = safeText(user.getNickname(), "Người dùng Heami");
                    String authorAvatar = safeText(user.getAvatar_url(), "");

                    firestore.runTransaction(transaction -> {
                        DocumentSnapshot postSnapshot = transaction.get(postRef);
                        if (!postSnapshot.exists()) {
                            throw new RuntimeException("Không tìm thấy bài viết");
                        }

                        DocumentSnapshot hugSnapshot = transaction.get(hugRef);

                        int currentLikeCount = safeInt(postSnapshot.get("like_count"));
                        int currentMyHugCount = hugSnapshot.exists()
                                ? safeInt(hugSnapshot.get("hug_count"))
                                : 0;

                        Timestamp createdAt = now;
                        if (hugSnapshot.exists()) {
                            Timestamp oldCreatedAt = hugSnapshot.getTimestamp("created_at");
                            if (oldCreatedAt != null) {
                                createdAt = oldCreatedAt;
                            }
                        }

                        int newMyHugCount = currentMyHugCount + 1;

                        PostHugModel hugModel = new PostHugModel(
                                uid,
                                authorName,
                                authorAvatar,
                                newMyHugCount,
                                createdAt,
                                now
                        );

                        transaction.set(hugRef, hugModel);
                        transaction.update(
                                postRef,
                                "like_count", currentLikeCount + 1,
                                "updated_at", now
                        );

                        return newMyHugCount;
                    }).addOnSuccessListener(newMyHugCount -> {
                        int result = newMyHugCount != null ? newMyHugCount : 1;
                        listener.onSuccess(result);
                    }).addOnFailureListener(e -> {
                        String message = e.getMessage() != null
                                ? e.getMessage()
                                : "Không thể gửi ôm lúc này";
                        listener.onFailure(message);
                    });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin người dùng";
                    listener.onFailure(message);
                });
    }

    public void clearMyHugsForPost(
            @NonNull String postId,
            @NonNull ClearMyHugsListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        DocumentReference postRef = firestore.collection("community_posts").document(postId);
        DocumentReference hugRef = postRef.collection("hugs").document(uid);
        Timestamp now = Timestamp.now();

        firestore.runTransaction(transaction -> {
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            if (!postSnapshot.exists()) {
                throw new RuntimeException("Không tìm thấy bài viết");
            }

            DocumentSnapshot hugSnapshot = transaction.get(hugRef);
            if (!hugSnapshot.exists()) {
                return 0;
            }

            int currentLikeCount = safeInt(postSnapshot.get("like_count"));
            int myHugCount = safeInt(hugSnapshot.get("hug_count"));

            if (myHugCount <= 0) {
                transaction.delete(hugRef);
                return 0;
            }

            int newLikeCount = Math.max(0, currentLikeCount - myHugCount);

            transaction.delete(hugRef);
            transaction.update(
                    postRef,
                    "like_count", newLikeCount,
                    "updated_at", now
            );

            return myHugCount;
        }).addOnSuccessListener(removedHugCount -> {
            int result = removedHugCount != null ? removedHugCount : 0;
            listener.onSuccess(result);
        }).addOnFailureListener(e -> {
            String message = e.getMessage() != null
                    ? e.getMessage()
                    : "Không thể huỷ ôm lúc này";
            listener.onFailure(message);
        });
    }

    public void getPostHugs(
            @NonNull String postId,
            @NonNull LoadPostHugsListener listener
    ) {
        firestore.collection("community_posts")
                .document(postId)
                .collection("hugs")
                .orderBy("updated_at", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PostHugModel> hugs = new ArrayList<>();

                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            PostHugModel hug = document.toObject(PostHugModel.class);
                            if (hug != null) {
                                hugs.add(hug);
                            }
                        }
                    }

                    listener.onSuccess(hugs);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải danh sách ôm";
                    listener.onFailure(message);
                });
    }

    public void getMyEmpathyStateForPost(
            @NonNull String postId,
            @NonNull LoadMyEmpathyStateListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("community_posts")
                .document(postId)
                .collection("empathies")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> listener.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải trạng thái đồng cảm";
                    listener.onFailure(message);
                });
    }

    public void toggleEmpathyForPost(
            @NonNull String postId,
            @NonNull ToggleEmpathyListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!userSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy thông tin người dùng");
                        return;
                    }

                    UserModel user = userSnapshot.toObject(UserModel.class);
                    if (user == null) {
                        listener.onFailure("Không đọc được dữ liệu người dùng");
                        return;
                    }

                    DocumentReference postRef = firestore.collection("community_posts").document(postId);
                    DocumentReference empathyRef = postRef.collection("empathies").document(uid);
                    Timestamp now = Timestamp.now();

                    String authorName = safeText(user.getNickname(), "Người dùng Heami");
                    String authorAvatar = safeText(user.getAvatar_url(), "");

                    firestore.runTransaction(transaction -> {
                        DocumentSnapshot postSnapshot = transaction.get(postRef);
                        if (!postSnapshot.exists()) {
                            throw new RuntimeException("Không tìm thấy bài viết");
                        }

                        DocumentSnapshot empathySnapshot = transaction.get(empathyRef);

                        int currentEmpathyCount = safeInt(postSnapshot.get("empathy_count"));
                        boolean alreadyHasEmpathy = empathySnapshot.exists();

                        if (alreadyHasEmpathy) {
                            int newEmpathyCount = Math.max(0, currentEmpathyCount - 1);

                            transaction.delete(empathyRef);
                            transaction.update(
                                    postRef,
                                    "empathy_count", newEmpathyCount,
                                    "updated_at", now
                            );

                            return false;
                        } else {
                            PostEmpathyModel empathyModel = new PostEmpathyModel(
                                    uid,
                                    authorName,
                                    authorAvatar,
                                    now,
                                    now
                            );

                            transaction.set(empathyRef, empathyModel);
                            transaction.update(
                                    postRef,
                                    "empathy_count", currentEmpathyCount + 1,
                                    "updated_at", now
                            );

                            return true;
                        }
                    }).addOnSuccessListener(nowHasEmpathy -> {
                        boolean result = nowHasEmpathy != null && nowHasEmpathy;
                        listener.onSuccess(result);
                    }).addOnFailureListener(e -> {
                        String message = e.getMessage() != null
                                ? e.getMessage()
                                : "Không thể cập nhật đồng cảm lúc này";
                        listener.onFailure(message);
                    });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin người dùng";
                    listener.onFailure(message);
                });
    }

    public void getPostEmpathies(
            @NonNull String postId,
            @NonNull LoadPostEmpathiesListener listener
    ) {
        firestore.collection("community_posts")
                .document(postId)
                .collection("empathies")
                .orderBy("updated_at", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PostEmpathyModel> empathies = new ArrayList<>();

                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            PostEmpathyModel empathy = document.toObject(PostEmpathyModel.class);
                            if (empathy != null) {
                                empathies.add(empathy);
                            }
                        }
                    }

                    listener.onSuccess(empathies);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải danh sách đồng cảm";
                    listener.onFailure(message);
                });
    }

    public void submitPostReport(
            @NonNull String postId,
            @NonNull String reasonCode,
            @NonNull String reasonLabel,
            @NonNull String extraNote,
            @NonNull SubmitReportListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!userSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy thông tin người dùng");
                        return;
                    }

                    UserModel user = userSnapshot.toObject(UserModel.class);
                    if (user == null) {
                        listener.onFailure("Không đọc được dữ liệu người dùng");
                        return;
                    }

                    DocumentReference postRef = firestore.collection("community_posts").document(postId);
                    DocumentReference reportRef = postRef.collection("reports").document(uid);
                    DocumentReference hiddenPostRef = firestore.collection("users")
                            .document(uid)
                            .collection("hidden_posts")
                            .document(postId);

                    Timestamp now = Timestamp.now();

                    String authorName = safeText(user.getNickname(), "Người dùng Heami");
                    String authorAvatar = safeText(user.getAvatar_url(), "");

                    firestore.runTransaction(transaction -> {
                        DocumentSnapshot postSnapshot = transaction.get(postRef);
                        if (!postSnapshot.exists()) {
                            throw new RuntimeException("Không tìm thấy bài viết");
                        }

                        DocumentSnapshot reportSnapshot = transaction.get(reportRef);

                        HashMap<String, Object> hiddenPostData = new HashMap<>();
                        hiddenPostData.put("post_id", postId);
                        hiddenPostData.put("reason", "REPORTED_POST");
                        hiddenPostData.put("created_at", now);

                        if (reportSnapshot.exists()) {
                            transaction.set(hiddenPostRef, hiddenPostData);
                            return "ALREADY_REPORTED";
                        }

                        int currentReportCount = safeInt(postSnapshot.get("report_count"));
                        int newReportCount = currentReportCount + 1;

                        String newPostStatus = newReportCount >= AUTO_HIDE_REPORT_THRESHOLD
                                ? "AUTO_HIDDEN"
                                : "ACTIVE";

                        PostReportModel reportModel = new PostReportModel(
                                uid,
                                authorName,
                                authorAvatar,
                                reasonCode,
                                reasonLabel,
                                extraNote.trim(),
                                now,
                                now,
                                "PENDING"
                        );

                        transaction.set(reportRef, reportModel);
                        transaction.set(hiddenPostRef, hiddenPostData);
                        transaction.update(
                                postRef,
                                "report_count", newReportCount,
                                "status", newPostStatus,
                                "updated_at", now
                        );

                        return newPostStatus;
                    }).addOnSuccessListener(result -> {
                        if ("ALREADY_REPORTED".equals(result)) {
                            listener.onAlreadyReported();
                            return;
                        }

                        boolean autoHidden = "AUTO_HIDDEN".equals(result);
                        listener.onSuccess(autoHidden);
                    }).addOnFailureListener(e -> {
                        String message = e.getMessage() != null
                                ? e.getMessage()
                                : "Không thể gửi báo cáo lúc này";
                        listener.onFailure(message);
                    });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin người dùng";
                    listener.onFailure(message);
                });
    }

    public void undoPostReport(
            @NonNull String postId,
            @NonNull UndoReportListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();

        DocumentReference postRef = firestore.collection("community_posts").document(postId);
        DocumentReference reportRef = postRef.collection("reports").document(uid);
        DocumentReference hiddenPostRef = firestore.collection("users")
                .document(uid)
                .collection("hidden_posts")
                .document(postId);

        Timestamp now = Timestamp.now();

        firestore.runTransaction(transaction -> {
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            if (!postSnapshot.exists()) {
                throw new RuntimeException("Không tìm thấy bài viết");
            }

            DocumentSnapshot reportSnapshot = transaction.get(reportRef);

            if (!reportSnapshot.exists()) {
                return "NO_REPORT";
            }

            int currentReportCount = safeInt(postSnapshot.get("report_count"));
            int newReportCount = Math.max(0, currentReportCount - 1);

            String newPostStatus = newReportCount >= AUTO_HIDE_REPORT_THRESHOLD
                    ? "AUTO_HIDDEN"
                    : "ACTIVE";

            transaction.delete(reportRef);
            transaction.delete(hiddenPostRef);
            transaction.update(
                    postRef,
                    "report_count", newReportCount,
                    "status", newPostStatus,
                    "updated_at", now
            );

            return newPostStatus;
        }).addOnSuccessListener(result -> {
            if ("NO_REPORT".equals(result)) {
                listener.onFailure("Không tìm thấy báo cáo để hoàn tác");
                return;
            }

            boolean postVisibleAgain = "ACTIVE".equals(result);
            listener.onSuccess(postVisibleAgain);
        }).addOnFailureListener(e -> {
            String message = e.getMessage() != null
                    ? e.getMessage()
                    : "Không thể hoàn tác báo cáo lúc này";
            listener.onFailure(message);
        });
    }

    public void updateMyPostContent(
            @NonNull String postId,
            @NonNull String newContent,
            @NonNull UpdatePostListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();
        String trimmedContent = newContent.trim();

        if (trimmedContent.isEmpty()) {
            listener.onFailure("Nội dung bài viết không được để trống");
            return;
        }

        DocumentReference postRef = firestore.collection("community_posts").document(postId);

        postRef.get()
                .addOnSuccessListener(postSnapshot -> {
                    if (!postSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy bài viết");
                        return;
                    }

                    String ownerId = safeText(postSnapshot.getString("user_id"), "");
                    if (!uid.equals(ownerId)) {
                        listener.onFailure("Bạn không có quyền chỉnh sửa bài viết này");
                        return;
                    }

                    Timestamp now = Timestamp.now();

                    postRef.update(
                                    "content", trimmedContent,
                                    "updated_at", now
                            )
                            .addOnSuccessListener(unused -> listener.onSuccess(now))
                            .addOnFailureListener(e -> {
                                String message = e.getMessage() != null
                                        ? e.getMessage()
                                        : "Không thể cập nhật bài viết";
                                listener.onFailure(message);
                            });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin bài viết";
                    listener.onFailure(message);
                });
    }

    public void deleteMyPost(
            @NonNull String postId,
            @NonNull DeletePostListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();
        DocumentReference postRef = firestore.collection("community_posts").document(postId);

        postRef.get()
                .addOnSuccessListener(postSnapshot -> {
                    if (!postSnapshot.exists()) {
                        listener.onFailure("Không tìm thấy bài viết");
                        return;
                    }

                    String ownerId = safeText(postSnapshot.getString("user_id"), "");
                    if (!uid.equals(ownerId)) {
                        listener.onFailure("Bạn không có quyền xóa bài viết này");
                        return;
                    }

                    Timestamp now = Timestamp.now();

                    postRef.update(
                                    "status", "DELETED",
                                    "updated_at", now
                            )
                            .addOnSuccessListener(unused -> listener.onSuccess())
                            .addOnFailureListener(e -> {
                                String message = e.getMessage() != null
                                        ? e.getMessage()
                                        : "Không thể xóa bài viết";
                                listener.onFailure(message);
                            });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể đọc thông tin bài viết";
                    listener.onFailure(message);
                });
    }

    public void loadCommunityDashboardStats(
            @NonNull LoadCommunityDashboardStatsListener listener
    ) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            listener.onFailure("Người dùng chưa đăng nhập");
            return;
        }

        String uid = firebaseUser.getUid();
        Timestamp startOfToday = getStartOfTodayTimestamp();

        Task<QuerySnapshot> searchingTask = firestore.collection("mood_match_requests")
                .whereEqualTo("status", "SEARCHING")
                .get();

        Task<QuerySnapshot> activeRoomsTask = firestore.collection("chat_rooms")
                .whereEqualTo("status", "ACTIVE")
                .get();

        Task<QuerySnapshot> myRoomsTask = firestore.collection("chat_rooms")
                .whereArrayContains("member_ids", uid)
                .get();

        Task<QuerySnapshot> todayRequestsTask = firestore.collection("mood_match_requests")
                .whereGreaterThanOrEqualTo("created_at", startOfToday)
                .get();

        Tasks.whenAllSuccess(
                        searchingTask,
                        activeRoomsTask,
                        myRoomsTask,
                        todayRequestsTask
                )
                .addOnSuccessListener(results -> {
                    QuerySnapshot searchingSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot activeRoomsSnap = (QuerySnapshot) results.get(1);
                    QuerySnapshot myRoomsSnap = (QuerySnapshot) results.get(2);
                    QuerySnapshot todayRequestsSnap = (QuerySnapshot) results.get(3);

                    int searchingCount = 0;
                    if (searchingSnap != null) {
                        for (DocumentSnapshot doc : searchingSnap.getDocuments()) {
                            Timestamp expiresAt = doc.getTimestamp("expires_at");
                            if (!isExpired(expiresAt)) {
                                searchingCount++;
                            }
                        }
                    }

                    int activeMoodRoomCount = 0;
                    if (activeRoomsSnap != null) {
                        for (DocumentSnapshot doc : activeRoomsSnap.getDocuments()) {
                            String type = safeText(doc.getString("type"), "");
                            String status = safeText(doc.getString("status"), "");

                            if ("ACTIVE".equals(status) && "MOOD_MATCH".equals(type)) {
                                activeMoodRoomCount++;
                            }
                        }
                    }

                    int unreadChatRoomCount = 0;
                    if (myRoomsSnap != null) {
                        for (DocumentSnapshot doc : myRoomsSnap.getDocuments()) {
                            String status = safeText(doc.getString("status"), "");
                            if (!"ACTIVE".equals(status)) {
                                continue;
                            }

                            Object unreadRaw = doc.get("unread_count_map." + uid);
                            unreadChatRoomCount += safeInt(unreadRaw);
                        }
                    }

                    Map<String, Timestamp> matchedRequestCreatedAtMap = new HashMap<>();
                    if (todayRequestsSnap != null) {
                        for (DocumentSnapshot doc : todayRequestsSnap.getDocuments()) {
                            String status = safeText(doc.getString("status"), "");
                            String matchId = safeText(doc.getString("match_id"), "");
                            Timestamp createdAt = doc.getTimestamp("created_at");

                            if ("MATCHED".equals(status)
                                    && !matchId.isEmpty()
                                    && createdAt != null
                                    && !matchedRequestCreatedAtMap.containsKey(matchId)) {
                                matchedRequestCreatedAtMap.put(matchId, createdAt);
                            }
                        }
                    }

                    if (matchedRequestCreatedAtMap.isEmpty()) {
                        listener.onSuccess(new CommunityDashboardStats(
                                0,
                                unreadChatRoomCount,
                                searchingCount,
                                activeMoodRoomCount,
                                0
                        ));
                        return;
                    }

                    final int finalUnreadChatRoomCount = unreadChatRoomCount;
                    final int finalSearchingCount = searchingCount;
                    final int finalActiveMoodRoomCount = activeMoodRoomCount;

                    List<Task<DocumentSnapshot>> matchTasks = new ArrayList<>();
                    for (String matchId : matchedRequestCreatedAtMap.keySet()) {
                        matchTasks.add(
                                firestore.collection("mood_matches")
                                        .document(matchId)
                                        .get()
                        );
                    }

                    Tasks.whenAllSuccess(matchTasks)
                            .addOnSuccessListener(matchResults -> {
                                long totalSeconds = 0L;
                                int sampleCount = 0;

                                for (Object result : matchResults) {
                                    if (!(result instanceof DocumentSnapshot)) {
                                        continue;
                                    }

                                    DocumentSnapshot matchDoc = (DocumentSnapshot) result;
                                    if (!matchDoc.exists()) {
                                        continue;
                                    }

                                    Timestamp matchCreatedAt = matchDoc.getTimestamp("created_at");
                                    Timestamp requestCreatedAt = matchedRequestCreatedAtMap.get(matchDoc.getId());

                                    if (matchCreatedAt == null || requestCreatedAt == null) {
                                        continue;
                                    }

                                    long diffMs = matchCreatedAt.toDate().getTime() - requestCreatedAt.toDate().getTime();
                                    if (diffMs < 0) {
                                        diffMs = 0;
                                    }

                                    totalSeconds += (diffMs / 1000L);
                                    sampleCount++;
                                }

                                int averageMatchSeconds = sampleCount > 0
                                        ? (int) Math.round((double) totalSeconds / sampleCount)
                                        : 0;

                                listener.onSuccess(new CommunityDashboardStats(
                                        0,
                                        finalUnreadChatRoomCount,
                                        finalSearchingCount,
                                        finalActiveMoodRoomCount,
                                        averageMatchSeconds
                                ));
                            })
                            .addOnFailureListener(e -> {
                                String message = e.getMessage() != null
                                        ? e.getMessage()
                                        : "Không thể tính thời gian ghép trung bình";
                                listener.onFailure(message);
                            });
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải thống kê cộng đồng";
                    listener.onFailure(message);
                });
    }

    @NonNull
    private Timestamp getStartOfTodayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime());
    }

    private boolean isExpired(Timestamp expiresAt) {
        if (expiresAt == null) {
            return false;
        }
        return expiresAt.toDate().getTime() <= System.currentTimeMillis();
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private int safeInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}