package com.example.heami.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.heami.data.models.AccountModel;
import com.example.heami.data.models.AdminAccountListItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminAccountRepository {

    public interface LoadAccountsListener {
        void onSuccess(@NonNull List<AdminAccountListItem> items);
        void onFailure(@NonNull String errorMessage);
    }

    public interface UpdateStatusListener {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    private interface ItemReadyCallback {
        void onReady(@NonNull AdminAccountListItem item);
    }

    private interface FailureCallback {
        void onFailure(@NonNull String errorMessage);
    }

    private final FirebaseFirestore firestore;

    public AdminAccountRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void loadAllAccounts(@NonNull LoadAccountsListener listener) {
        firestore.collection("accounts")
                .get()
                .addOnSuccessListener(snapshot -> handleAccountsSnapshot(snapshot, listener))
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể tải danh sách tài khoản";
                    listener.onFailure(message);
                });
    }

    public void updateAccountStatus(
            @NonNull String accountId,
            @NonNull String newStatus,
            @NonNull UpdateStatusListener listener
    ) {
        firestore.collection("accounts")
                .document(accountId)
                .update("status", safeUpper(newStatus, "ACTIVE"))
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể cập nhật trạng thái tài khoản";
                    listener.onFailure(message);
                });
    }

    private void handleAccountsSnapshot(
            @NonNull QuerySnapshot snapshot,
            @NonNull LoadAccountsListener listener
    ) {
        if (snapshot.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        List<AdminAccountListItem> items = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(snapshot.size());
        AtomicBoolean failed = new AtomicBoolean(false);

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            AccountModel account = doc.toObject(AccountModel.class);
            if (account == null) {
                account = new AccountModel();
            }

            String uid = safeText(account.getAccount_id(), "");
            if (uid.isEmpty()) {
                uid = doc.getId();
            }

            AdminAccountListItem baseItem = new AdminAccountListItem();
            baseItem.setAccountId(uid);
            baseItem.setEmail(safeText(account.getEmail(), ""));
            baseItem.setRole(safeUpper(account.getRole(), "USER"));
            baseItem.setStatus(safeUpper(account.getStatus(), "ACTIVE"));
            baseItem.setLastSignInAt(account.getLast_sign_in_at());
            baseItem.setDisplayName(uid);
            baseItem.setAvatarUrl("");
            baseItem.setSubtitle("Tài khoản Heami");
            baseItem.setOnline(false);

            enrichAccount(baseItem, new ItemReadyCallback() {
                @Override
                public void onReady(@NonNull AdminAccountListItem item) {
                    if (failed.get()) return;

                    items.add(item);

                    if (remaining.decrementAndGet() == 0) {
                        items.sort((left, right) -> {
                            long leftTime = getComparableTime(left.getLastSignInAt());
                            long rightTime = getComparableTime(right.getLastSignInAt());

                            int statusCompare = statusRank(left.getStatus()) - statusRank(right.getStatus());
                            if (statusCompare != 0) {
                                return statusCompare;
                            }

                            return Long.compare(rightTime, leftTime);
                        });

                        listener.onSuccess(items);
                    }
                }
            }, errorMessage -> {
                if (failed.compareAndSet(false, true)) {
                    listener.onFailure(errorMessage);
                }
            });
        }
    }

    private void enrichAccount(
            @NonNull AdminAccountListItem item,
            @NonNull ItemReadyCallback callback,
            @NonNull FailureCallback failureListener
    ) {
        String role = safeUpper(item.getRole(), "USER");
        String uid = safeText(item.getAccountId(), "");

        if (uid.isEmpty()) {
            callback.onReady(item);
            return;
        }

        switch (role) {
            case "DOCTOR":
                firestore.collection("doctors")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            String fullName = safeText(doc.getString("full_name"), "Bác sĩ Heami");
                            String avatarUrl = safeText(doc.getString("avatar_url"), "");
                            boolean isOnline = Boolean.TRUE.equals(doc.getBoolean("is_online"));

                            List<String> specialization = (List<String>) doc.get("specialization");
                            String subtitle = "Bác sĩ";
                            if (specialization != null && !specialization.isEmpty()) {
                                subtitle = "Bác sĩ • " + specialization.get(0);
                            }

                            item.setDisplayName(fullName);
                            item.setAvatarUrl(avatarUrl);
                            item.setSubtitle(subtitle);
                            item.setOnline(isOnline);

                            callback.onReady(item);
                        })
                        .addOnFailureListener(e -> {
                            String message = e.getMessage() != null
                                    ? e.getMessage()
                                    : "Không thể tải hồ sơ bác sĩ";
                            failureListener.onFailure(message);
                        });
                break;

            case "ADMIN":
                firestore.collection("admins")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            String fullName = safeText(doc.getString("full_name"), "Heami Admin");
                            String avatarUrl = safeText(doc.getString("avatar_url"), "");

                            item.setDisplayName(fullName);
                            item.setAvatarUrl(avatarUrl);
                            item.setSubtitle("Quản trị viên hệ thống");
                            item.setOnline(false);

                            callback.onReady(item);
                        })
                        .addOnFailureListener(e -> {
                            String message = e.getMessage() != null
                                    ? e.getMessage()
                                    : "Không thể tải hồ sơ admin";
                            failureListener.onFailure(message);
                        });
                break;

            case "USER":
            default:
                firestore.collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            String nickname = safeText(doc.getString("nickname"), "Người dùng Heami");
                            String avatarUrl = safeText(doc.getString("avatar_url"), "");
                            String motto = safeText(doc.getString("motto"), "");

                            item.setDisplayName(nickname);
                            item.setAvatarUrl(avatarUrl);
                            item.setSubtitle(motto.isEmpty() ? "Người dùng Heami" : motto);
                            item.setOnline(false);

                            callback.onReady(item);
                        })
                        .addOnFailureListener(e -> {
                            String message = e.getMessage() != null
                                    ? e.getMessage()
                                    : "Không thể tải hồ sơ người dùng";
                            failureListener.onFailure(message);
                        });
                break;
        }
    }

    private int statusRank(@Nullable String status) {
        String normalized = safeUpper(status, "ACTIVE");
        switch (normalized) {
            case "ACTIVE":
                return 0;
            case "PENDING_VERIFY":
                return 1;
            case "BANNED":
                return 2;
            default:
                return 3;
        }
    }

    private long getComparableTime(@Nullable Timestamp timestamp) {
        return timestamp != null ? timestamp.toDate().getTime() : 0L;
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    @NonNull
    private String safeUpper(@Nullable String value, @NonNull String fallback) {
        return safeText(value, fallback).toUpperCase(Locale.ROOT);
    }
}