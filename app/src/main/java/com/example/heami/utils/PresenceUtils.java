package com.example.heami.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;

import java.util.Locale;
import java.util.Set;

public final class PresenceUtils {

    private PresenceUtils() {
    }

    public static final int PRESENCE_VERSION = 2;

    // 90s để tránh Firebase delay nhẹ làm online nhảy về 0.
    public static final long ONLINE_HEARTBEAT_TIMEOUT_MS = 90_000L;

    public static boolean isOnlineFromConnections(
            @NonNull DataSnapshot userSnapshot,
            long now
    ) {
        DataSnapshot connectionsSnapshot = userSnapshot.child("connections");

        if (!connectionsSnapshot.exists() || connectionsSnapshot.getChildrenCount() <= 0) {
            return false;
        }

        for (DataSnapshot connectionSnapshot : connectionsSnapshot.getChildren()) {
            if (isConnectionOnline(connectionSnapshot, now)) {
                return true;
            }
        }

        return false;
    }

    // Giữ tên hàm cũ để các file khác như CommunityChatListActivity không bị lỗi build.
    public static boolean isUserOnlineFromConnections(
            @NonNull DataSnapshot userSnapshot,
            long now
    ) {
        return isOnlineFromConnections(userSnapshot, now);
    }

    public static int countOnlineAllRoles(
            @NonNull DataSnapshot statusSnapshot,
            long now
    ) {
        int onlineCount = 0;

        for (DataSnapshot userSnapshot : statusSnapshot.getChildren()) {
            if (isOnlineFromConnections(userSnapshot, now)) {
                onlineCount++;
            }
        }

        return onlineCount;
    }

    public static int countOnlineUsersOnly(
            @NonNull DataSnapshot statusSnapshot,
            long now
    ) {
        int onlineCount = 0;

        for (DataSnapshot userSnapshot : statusSnapshot.getChildren()) {
            String role = safeUpper(userSnapshot.child("role").getValue(String.class));

            if ("USER".equals(role) && isOnlineFromConnections(userSnapshot, now)) {
                onlineCount++;
            }
        }

        return onlineCount;
    }

    public static int countOnlineUsersOnlyByUidSet(
            @NonNull DataSnapshot statusSnapshot,
            long now,
            @NonNull Set<String> userUidSet
    ) {
        int onlineCount = 0;

        for (DataSnapshot userSnapshot : statusSnapshot.getChildren()) {
            String uid = userSnapshot.getKey();

            if (uid == null || !userUidSet.contains(uid)) {
                continue;
            }

            if (isOnlineFromConnections(userSnapshot, now)) {
                onlineCount++;
            }
        }

        return onlineCount;
    }

    public static boolean isConnectionOnline(
            @NonNull DataSnapshot connectionSnapshot,
            long now
    ) {
        Boolean isForeground = connectionSnapshot.child("isForeground").getValue(Boolean.class);
        Long heartbeatAt = connectionSnapshot.child("heartbeat_at").getValue(Long.class);

        if (!Boolean.TRUE.equals(isForeground) || heartbeatAt == null) {
            return false;
        }

        long diff = now - heartbeatAt;

        return diff >= 0 && diff <= ONLINE_HEARTBEAT_TIMEOUT_MS;
    }

    @Nullable
    private static Integer getInteger(@NonNull DataSnapshot snapshot) {
        Long longValue = snapshot.getValue(Long.class);
        if (longValue != null) {
            return longValue.intValue();
        }

        Integer intValue = snapshot.getValue(Integer.class);
        if (intValue != null) {
            return intValue;
        }

        return null;
    }

    @NonNull
    private static String safeUpper(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}