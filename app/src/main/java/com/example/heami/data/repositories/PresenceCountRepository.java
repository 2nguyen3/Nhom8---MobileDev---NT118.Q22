package com.example.heami.data.repositories;

import androidx.annotation.NonNull;

import com.example.heami.data.models.PresenceCountModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class PresenceCountRepository {

    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final DatabaseReference presenceConnectionsRef;

    private ValueEventListener presenceCountListener;

    public PresenceCountRepository() {
        FirebaseDatabase realtimeDb = FirebaseDatabase.getInstance(RTDB_URL);
        presenceConnectionsRef = realtimeDb.getReference("presence_connections");
    }

    public interface PresenceCountCallback {
        void onChanged(@NonNull PresenceCountModel countModel);

        void onError(@NonNull String message);
    }

    public void observePresenceCounts(@NonNull PresenceCountCallback callback) {
        stopObservingPresenceCounts();

        presenceCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                PresenceCountModel countModel = buildCountModel(snapshot);
                callback.onChanged(countModel);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        presenceConnectionsRef.addValueEventListener(presenceCountListener);
    }

    public void stopObservingPresenceCounts() {
        if (presenceCountListener != null) {
            presenceConnectionsRef.removeEventListener(presenceCountListener);
            presenceCountListener = null;
        }
    }

    @NonNull
    private PresenceCountModel buildCountModel(@NonNull DataSnapshot rootSnapshot) {
        int onlineUsers = 0;
        int onlineDoctors = 0;
        int onlineAdmins = 0;

        if (!rootSnapshot.exists() || rootSnapshot.getChildrenCount() <= 0) {
            return PresenceCountModel.empty();
        }

        for (DataSnapshot uidSnapshot : rootSnapshot.getChildren()) {
            if (!uidSnapshot.exists() || uidSnapshot.getChildrenCount() <= 0) {
                continue;
            }

            boolean hasConnection = false;
            String selectedRole = "USER";

            for (DataSnapshot connectionSnapshot : uidSnapshot.getChildren()) {
                if (!connectionSnapshot.exists()) {
                    continue;
                }

                hasConnection = true;

                String role = normalizeRole(
                        connectionSnapshot.child("role").getValue(String.class)
                );

                selectedRole = pickHigherRole(selectedRole, role);
            }

            if (!hasConnection) {
                continue;
            }

            switch (selectedRole) {
                case "ADMIN":
                    onlineAdmins++;
                    break;

                case "DOCTOR":
                    onlineDoctors++;
                    break;

                case "USER":
                default:
                    onlineUsers++;
                    break;
            }
        }

        int onlineTotal = onlineUsers + onlineDoctors + onlineAdmins;

        return new PresenceCountModel(
                onlineUsers,
                onlineDoctors,
                onlineAdmins,
                onlineTotal
        );
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
        /*
         * Nếu cùng 1 uid có nhiều device mà role bị lệch tạm thời,
         * ưu tiên role mạnh hơn:
         *
         * ADMIN > DOCTOR > USER
         */
        if ("ADMIN".equals(currentRole) || "ADMIN".equals(newRole)) {
            return "ADMIN";
        }

        if ("DOCTOR".equals(currentRole) || "DOCTOR".equals(newRole)) {
            return "DOCTOR";
        }

        return "USER";
    }
}