package com.example.heami;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.OnDisconnect;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HeamiApp extends Application implements DefaultLifecycleObserver {

    private static final String TAG = "HeamiPresence";

    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final String PREFS_PRESENCE = "HeamiPresencePrefs";
    private static final String KEY_DEVICE_ID = "presence_device_id";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseDatabase realtimeDb;

    private FirebaseAuth.AuthStateListener authStateListener;
    private ValueEventListener connectedListener;

    private DatabaseReference connectedRef;

    /*
     * Path mới dùng cho presence:
     *
     * presence_connections/{uid}/{deviceId}
     *
     * Mỗi uid có thể có nhiều deviceId.
     * Cloud Functions sẽ đếm theo uid, không đếm theo device,
     * để tránh 1 tài khoản mở 2 máy bị tính thành 2 người online.
     */
    private DatabaseReference presenceUserRef;
    private DatabaseReference currentConnectionRef;

    /*
     * Path phụ để lưu lần online cuối.
     * Path này không dùng để đếm realtime online.
     */
    private DatabaseReference lastOnlineRef;

    private String currentPresenceUid = "";
    private String presenceDeviceId = "";

    private boolean isAppForeground = false;
    private static boolean appForegroundStatic = false;

    public interface PresenceCleanupCallback {
        void onDone();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance(RTDB_URL);
        presenceDeviceId = getPresenceDeviceId();

        Log.d(TAG, "HeamiApp onCreate");
        Log.d(TAG, "presenceDeviceId = " + presenceDeviceId);

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            String nextUid = user != null ? user.getUid() : "";

            Log.d(TAG, "Auth changed, uid = " + nextUid);

            if (!nextUid.equals(currentPresenceUid)) {
                detachPresence();

                currentPresenceUid = nextUid;

                if (!currentPresenceUid.isEmpty()) {
                    attachPresence(currentPresenceUid);
                }
            }
        };

        auth.addAuthStateListener(authStateListener);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentPresenceUid = currentUser.getUid();
            Log.d(TAG, "Current user at startup = " + currentPresenceUid);
            attachPresence(currentPresenceUid);
        } else {
            Log.d(TAG, "No current user at startup");
        }

        createHeamiChatNotificationChannel();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        isAppForeground = true;
        appForegroundStatic = true;

        Log.d(TAG, "App foreground");

        syncCurrentPresenceOnline();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isAppForeground = false;
        appForegroundStatic = false;

        Log.d(TAG, "App background");

        removeCurrentPresenceConnection();
        updateLastOnlineNow();
    }

    private void attachPresence(@NonNull String uid) {
        Log.d(TAG, "attachPresence uid = " + uid);

        connectedRef = realtimeDb.getReference(".info/connected");

        presenceUserRef = realtimeDb
                .getReference("presence_connections")
                .child(uid);

        currentConnectionRef = presenceUserRef.child(presenceDeviceId);

        lastOnlineRef = realtimeDb
                .getReference("presence_last_online")
                .child(uid);

        connectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);

                Log.d(TAG, ".info/connected = " + connected);

                if (connected == null || !connected) {
                    return;
                }

                registerOnDisconnectHandlers();

                if (isAppForeground) {
                    writeCurrentConnectionOnline();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "connectedListener cancelled", error.toException());
            }
        };

        connectedRef.addValueEventListener(connectedListener);

        if (isAppForeground) {
            syncCurrentPresenceOnline();
        }
    }

    private void detachPresence() {
        Log.d(TAG, "detachPresence uid = " + currentPresenceUid);

        if (connectedRef != null && connectedListener != null) {
            connectedRef.removeEventListener(connectedListener);
        }

        removeCurrentPresenceConnection();
        updateLastOnlineNow();

        connectedListener = null;
        connectedRef = null;

        presenceUserRef = null;
        currentConnectionRef = null;
        lastOnlineRef = null;
    }

    private void syncCurrentPresenceOnline() {
        if (currentPresenceUid == null || currentPresenceUid.trim().isEmpty()) {
            FirebaseUser user = auth != null ? auth.getCurrentUser() : null;
            currentPresenceUid = user != null ? user.getUid() : "";
        }

        if (currentPresenceUid.isEmpty()) {
            return;
        }

        if (currentConnectionRef == null) {
            attachPresence(currentPresenceUid);
            return;
        }

        cleanupStalePresenceForCurrentDeviceThenWrite();
    }

    private void cleanupStalePresenceForCurrentDeviceThenWrite() {
        if (realtimeDb == null || presenceDeviceId == null || presenceDeviceId.trim().isEmpty()) {
            writeCurrentConnectionOnline();
            return;
        }

        realtimeDb.getReference("presence_connections")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> updates = new HashMap<>();

                    for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                        String uid = uidSnapshot.getKey();

                        for (DataSnapshot connectionSnapshot : uidSnapshot.getChildren()) {
                            String connectionId = connectionSnapshot.getKey();
                            String deviceId = connectionSnapshot.child("device_id").getValue(String.class);

                            if (uid == null || connectionId == null) {
                                continue;
                            }

                            boolean isSameDevice = presenceDeviceId.equals(deviceId);
                            boolean isOtherUid = !uid.equals(currentPresenceUid);

                            if (isSameDevice && isOtherUid) {
                                updates.put(uid + "/" + connectionId, null);
                            }
                        }
                    }

                    if (updates.isEmpty()) {
                        writeCurrentConnectionOnline();
                        return;
                    }

                    realtimeDb.getReference("presence_connections")
                            .updateChildren(updates, (error, ref) -> {
                                if (error != null) {
                                    Log.e(TAG, "Failed to cleanup stale presence before write", error.toException());
                                } else {
                                    Log.d(TAG, "stale presence cleaned before write, count = " + updates.size());
                                }

                                writeCurrentConnectionOnline();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to cleanup stale presence before write", e);
                    writeCurrentConnectionOnline();
                });
    }

    private void writeCurrentConnectionOnline() {
        if (currentConnectionRef == null || currentPresenceUid == null || currentPresenceUid.isEmpty()) {
            return;
        }

        String fallbackRole = getLocalRoleFallback();

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", fallbackRole);
        updates.put("platform", "android");
        updates.put("connected_at", ServerValue.TIMESTAMP);
        updates.put("device_id", presenceDeviceId);

        currentConnectionRef.updateChildren(updates, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to write presence connection", error.toException());
            } else {
                Log.d(TAG, "presence connection written with fallback role = " + fallbackRole);
            }
        });

        syncConnectionRoleFromFirestore(currentPresenceUid, fallbackRole);
    }

    private void syncConnectionRoleFromFirestore(
            @NonNull String uid,
            @NonNull String fallbackRole
    ) {
        if (firestore == null || currentConnectionRef == null) {
            return;
        }

        String accountDocId = resolveAccountDocIdForPresence(uid);

        firestore.collection("accounts")
                .document(accountDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    String rawRole = doc != null && doc.exists()
                            ? doc.getString("role")
                            : fallbackRole;

                    String finalRole = normalizeRole(rawRole, fallbackRole);

                    if (currentConnectionRef != null) {
                        currentConnectionRef.child("role").setValue(finalRole, (error, ref) -> {
                            if (error != null) {
                                Log.e(TAG, "Failed to sync connection role", error.toException());
                            } else {
                                Log.d(TAG, "connection role synced = " + finalRole
                                        + " from accountDocId = " + accountDocId);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch account role from " + accountDocId, e);

                    if (currentConnectionRef != null) {
                        currentConnectionRef.child("role").setValue(fallbackRole);
                    }
                });
    }

    private void registerOnDisconnectHandlers() {
        if (currentConnectionRef == null || lastOnlineRef == null) {
            return;
        }

        OnDisconnect removeConnectionOnDisconnect = currentConnectionRef.onDisconnect();
        removeConnectionOnDisconnect.removeValue((error, ref) -> {
            if (error != null) {
                Log.e(TAG, "onDisconnect remove connection failed", error.toException());
            } else {
                Log.d(TAG, "onDisconnect remove connection registered");
            }
        });

        OnDisconnect setLastOnlineOnDisconnect = lastOnlineRef.onDisconnect();
        setLastOnlineOnDisconnect.setValue(ServerValue.TIMESTAMP, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "onDisconnect lastOnline failed", error.toException());
            } else {
                Log.d(TAG, "onDisconnect lastOnline registered");
            }
        });
    }

    private void removeCurrentPresenceConnection() {
        if (currentConnectionRef == null) {
            return;
        }

        currentConnectionRef.removeValue((error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to remove presence connection", error.toException());
            } else {
                Log.d(TAG, "presence connection removed");
            }
        });
    }

    private void updateLastOnlineNow() {
        if (lastOnlineRef == null) {
            return;
        }

        lastOnlineRef.setValue(ServerValue.TIMESTAMP, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to update last online", error.toException());
            } else {
                Log.d(TAG, "last online updated");
            }
        });
    }

    @NonNull
    private String resolveAccountDocIdForPresence(@NonNull String authUid) {
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);

        boolean isDoctor = prefs.getBoolean("is_doctor", false);

        if (isDoctor) {
            String doctorId = prefs.getString("doctor_id", "");

            if (doctorId != null && !doctorId.trim().isEmpty()) {
                return doctorId.trim();
            }

            return "doc_001";
        }

        return authUid;
    }

    @NonNull
    private String getLocalRoleFallback() {
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);

        if (prefs.getBoolean("is_admin", false)) {
            return "ADMIN";
        }

        if (prefs.getBoolean("is_doctor", false)) {
            return "DOCTOR";
        }

        return "USER";
    }

    @NonNull
    private String normalizeRole(String role, @NonNull String fallbackRole) {
        if (role == null || role.trim().isEmpty()) {
            return fallbackRole;
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);

        if ("USER".equals(normalized)
                || "ADMIN".equals(normalized)
                || "DOCTOR".equals(normalized)) {
            return normalized;
        }

        return fallbackRole;
    }

    @NonNull
    private String getPresenceDeviceId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_PRESENCE, MODE_PRIVATE);

        String savedId = prefs.getString(KEY_DEVICE_ID, "");
        if (savedId != null && !savedId.trim().isEmpty()) {
            return savedId;
        }

        String newId = UUID.randomUUID().toString();

        prefs.edit()
                .putString(KEY_DEVICE_ID, newId)
                .apply();

        return newId;
    }

    public void refreshPresenceForCurrentUser() {
        Log.d(TAG, "refreshPresenceForCurrentUser");

        FirebaseUser user = auth != null ? auth.getCurrentUser() : null;
        if (user == null) {
            return;
        }

        currentPresenceUid = user.getUid();

        if (currentConnectionRef == null) {
            attachPresence(currentPresenceUid);
            return;
        }

        writeCurrentConnectionOnline();
    }

    public void forceClearPresenceBeforeLogout(@NonNull PresenceCleanupCallback callback) {
        Log.d(TAG, "forceClearPresenceBeforeLogout");

        if (currentConnectionRef == null) {
            cleanupPresenceByDeviceId(callback);
            return;
        }

        currentConnectionRef.removeValue((error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to force remove current connection", error.toException());
            } else {
                Log.d(TAG, "current presence connection force removed");
            }

            updateLastOnlineNow();
            cleanupPresenceByDeviceId(callback);
        });
    }

    private void cleanupPresenceByDeviceId(@NonNull PresenceCleanupCallback callback) {
        if (realtimeDb == null || presenceDeviceId == null || presenceDeviceId.trim().isEmpty()) {
            callback.onDone();
            return;
        }

        /*
         * Trường hợp test nhiều role trên cùng một máy:
         * cùng device_id có thể còn sót ở uid cũ.
         * Hàm này quét presence_connections và xóa mọi connection
         * có device_id trùng với máy hiện tại.
         */
        realtimeDb.getReference("presence_connections")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> updates = new HashMap<>();

                    for (DataSnapshot uidSnapshot : snapshot.getChildren()) {
                        for (DataSnapshot connectionSnapshot : uidSnapshot.getChildren()) {
                            String deviceId = connectionSnapshot.child("device_id").getValue(String.class);

                            if (presenceDeviceId.equals(deviceId)) {
                                String uid = uidSnapshot.getKey();
                                String connectionId = connectionSnapshot.getKey();

                                if (uid != null && connectionId != null) {
                                    updates.put(uid + "/" + connectionId, null);
                                }
                            }
                        }
                    }

                    if (updates.isEmpty()) {
                        callback.onDone();
                        return;
                    }

                    realtimeDb.getReference("presence_connections")
                            .updateChildren(updates, (error, ref) -> {
                                if (error != null) {
                                    Log.e(TAG, "Failed to cleanup presence by device id", error.toException());
                                } else {
                                    Log.d(TAG, "presence cleanup by device id done, count = " + updates.size());
                                }

                                callback.onDone();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to read presence_connections for cleanup", e);
                    callback.onDone();
                });
    }

    public static boolean isAppForegroundStatic() {
        return appForegroundStatic;
    }

    private void createHeamiChatNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return;
        }

        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                "heami_chat_messages",
                "Tin nhắn Heami",
                android.app.NotificationManager.IMPORTANCE_HIGH
        );

        channel.setDescription("Thông báo tin nhắn mới từ Community và Mood Match");
        channel.enableVibration(true);
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

        android.app.NotificationManager manager =
                getSystemService(android.app.NotificationManager.class);

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}