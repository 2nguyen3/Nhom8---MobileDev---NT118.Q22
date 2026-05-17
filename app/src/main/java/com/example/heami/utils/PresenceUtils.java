package com.example.heami.utils;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;

public final class PresenceUtils {

    private PresenceUtils() {
    }

    public static final long ONLINE_HEARTBEAT_TIMEOUT_MS = 30_000L;

    public static boolean isUserOnlineFromConnections(
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
}