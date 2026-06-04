package com.example.heami.data.repositories;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PresenceStatusRepository {

    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final DatabaseReference presenceConnectionsRef;

    private ValueEventListener onlineUserIdsListener;
    private ValueEventListener userOnlineListener;

    public PresenceStatusRepository() {
        FirebaseDatabase realtimeDb = FirebaseDatabase.getInstance(RTDB_URL);
        presenceConnectionsRef = realtimeDb.getReference("presence_connections");
    }

    public interface OnlineUserIdsCallback {
        void onChanged(@NonNull Set<String> onlineUserIds);

        void onError(@NonNull String message);
    }

    public interface UserOnlineCallback {
        void onChanged(boolean online);

        void onError(@NonNull String message);
    }

    public void observeOnlineUserIds(
            @NonNull String excludedUid,
            @NonNull OnlineUserIdsCallback callback
    ) {
        stopObservingOnlineUserIds();

        onlineUserIdsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> onlineIds = new HashSet<>();

                for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                    String uid = uidSnapshot.getKey();

                    if (uid == null || uid.trim().isEmpty()) {
                        continue;
                    }

                    if (uid.equals(excludedUid)) {
                        continue;
                    }

                    if (!isUserSnapshotOnline(uidSnapshot)) {
                        continue;
                    }

                    /*
                     * Community chat là user-user, nên chỉ lấy role USER.
                     * DOCTOR và ADMIN không được hiện như người dùng online trong community chat.
                     */
                    String role = resolveRoleFromUidSnapshot(uidSnapshot);

                    if ("USER".equals(role)) {
                        onlineIds.add(uid);
                    }
                }

                callback.onChanged(onlineIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        presenceConnectionsRef.addValueEventListener(onlineUserIdsListener);
    }

    public void stopObservingOnlineUserIds() {
        if (onlineUserIdsListener != null) {
            presenceConnectionsRef.removeEventListener(onlineUserIdsListener);
            onlineUserIdsListener = null;
        }
    }

    public void observeUserOnline(
            @NonNull String uid,
            @NonNull UserOnlineCallback callback
    ) {
        stopObservingUserOnline();

        if (uid.trim().isEmpty()) {
            callback.onChanged(false);
            return;
        }

        DatabaseReference userPresenceRef = presenceConnectionsRef.child(uid);

        userOnlineListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onChanged(isUserSnapshotOnline(snapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        userPresenceRef.addValueEventListener(userOnlineListener);
    }

    public void stopObservingUserOnline() {
        if (userOnlineListener != null) {
            presenceConnectionsRef.removeEventListener(userOnlineListener);
            userOnlineListener = null;
        }
    }

    private boolean isUserSnapshotOnline(@NonNull DataSnapshot uidSnapshot) {
        return uidSnapshot.exists() && uidSnapshot.getChildrenCount() > 0;
    }

    @NonNull
    private String resolveRoleFromUidSnapshot(@NonNull DataSnapshot uidSnapshot) {
        String selectedRole = "USER";

        for (DataSnapshot connectionSnapshot : uidSnapshot.getChildren()) {
            String role = normalizeRole(
                    connectionSnapshot.child("role").getValue(String.class)
            );

            selectedRole = pickHigherRole(selectedRole, role);
        }

        return selectedRole;
    }

    @NonNull
    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "USER";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);

        if ("ADMIN".equals(normalized)
                || "DOCTOR".equals(normalized)
                || "USER".equals(normalized)) {
            return normalized;
        }

        return "USER";
    }

    @NonNull
    private String pickHigherRole(
            @NonNull String currentRole,
            @NonNull String newRole
    ) {
        if ("ADMIN".equals(currentRole) || "ADMIN".equals(newRole)) {
            return "ADMIN";
        }

        if ("DOCTOR".equals(currentRole) || "DOCTOR".equals(newRole)) {
            return "DOCTOR";
        }

        return "USER";
    }
}