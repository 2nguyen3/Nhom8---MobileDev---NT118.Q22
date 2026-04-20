package com.example.heami.data.repositories;

import androidx.annotation.NonNull;

import com.example.heami.data.models.CommunityPostModel;
import com.example.heami.data.models.PostCommentModel;
import com.example.heami.data.models.PostEmpathyModel;
import com.example.heami.data.models.PostHugModel;
import com.example.heami.data.models.UserModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CommunityRepository {

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
        firestore.collection("community_posts")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CommunityPostModel> posts = new ArrayList<>();

                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            CommunityPostModel post = document.toObject(CommunityPostModel.class);
                            if (post != null) {
                                if (post.getPost_id() == null || post.getPost_id().trim().isEmpty()) {
                                    post.setPost_id(document.getId());
                                }
                                posts.add(post);
                            }
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