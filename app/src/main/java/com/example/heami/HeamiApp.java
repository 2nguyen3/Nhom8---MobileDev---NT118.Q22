package com.example.heami;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
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

    private static final int PRESENCE_VERSION = 2;

    private static final String PREFS_PRESENCE = "HeamiPresencePrefs";
    private static final String KEY_DEVICE_ID = "presence_device_id";

    private static final long HEARTBEAT_INTERVAL_MS = 15_000L;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseDatabase realtimeDb;

    private FirebaseAuth.AuthStateListener authStateListener;
    private ValueEventListener connectedListener;

    private DatabaseReference connectedRef;
    private DatabaseReference userStatusRef;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference currentConnectionRef;

    private DatabaseReference currentConnectionForegroundRef;
    private DatabaseReference currentConnectionHeartbeatRef;
    private DatabaseReference currentConnectionConnectedAtRef;

    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());

    private String currentPresenceUid = "";
    private String presenceDeviceId = "";

    private boolean isAppForeground = false;
    private static boolean appForegroundStatic = false;

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            syncHeartbeatNow();

            if (isAppForeground && currentConnectionRef != null) {
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        }
    };

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

        syncHeartbeatNow();
        startHeartbeatLoop();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isAppForeground = false;
        appForegroundStatic = false;

        Log.d(TAG, "App background");

        stopHeartbeatLoop();
        markCurrentConnectionBackground();

        if (lastOnlineRef != null) {
            lastOnlineRef.setValue(ServerValue.TIMESTAMP, (error, ref) -> {
                if (error != null) {
                    Log.e(TAG, "Failed to set lastOnline", error.toException());
                } else {
                    Log.d(TAG, "lastOnline updated");
                }
            });
        }
    }

    private void attachPresence(@NonNull String uid) {
        Log.d(TAG, "attachPresence uid = " + uid);

        connectedRef = realtimeDb.getReference(".info/connected");
        userStatusRef = realtimeDb.getReference("status").child(uid);
        myConnectionsRef = userStatusRef.child("connections");
        lastOnlineRef = userStatusRef.child("lastOnline");

        /*
         * Mỗi máy/app chỉ dùng 1 connection cố định theo presenceDeviceId.
         * Không dùng push() nữa để tránh sinh nhiều connection rác trong RTDB.
         */
        currentConnectionRef = myConnectionsRef.child(presenceDeviceId);
        currentConnectionForegroundRef = currentConnectionRef.child("isForeground");
        currentConnectionHeartbeatRef = currentConnectionRef.child("heartbeat_at");
        currentConnectionConnectedAtRef = currentConnectionRef.child("connected_at");

        Map<String, Object> statusUpdates = new HashMap<>();
        statusUpdates.put("presence_version", PRESENCE_VERSION);
        statusUpdates.put("device_id", presenceDeviceId);
        userStatusRef.updateChildren(statusUpdates);

        syncPresenceRole(uid);

        connectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);

                Log.d(TAG, ".info/connected = " + connected);

                if (connected == null || !connected) {
                    stopHeartbeatLoop();
                    return;
                }

                registerOnDisconnectHandlers();
                syncConnectionOnlineNow(true);

                if (isAppForeground) {
                    startHeartbeatLoop();
                } else {
                    stopHeartbeatLoop();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "connectedListener cancelled", error.toException());
            }
        };

        connectedRef.addValueEventListener(connectedListener);
    }

    private void detachPresence() {
        Log.d(TAG, "detachPresence uid = " + currentPresenceUid);

        stopHeartbeatLoop();

        if (connectedRef != null && connectedListener != null) {
            connectedRef.removeEventListener(connectedListener);
        }

        if (currentConnectionRef != null) {
            currentConnectionRef.child("isForeground").setValue(false);
            currentConnectionRef.removeValue();
        }

        if (lastOnlineRef != null) {
            lastOnlineRef.setValue(ServerValue.TIMESTAMP);
        }

        connectedListener = null;
        connectedRef = null;

        userStatusRef = null;
        myConnectionsRef = null;
        lastOnlineRef = null;

        currentConnectionRef = null;
        currentConnectionForegroundRef = null;
        currentConnectionHeartbeatRef = null;
        currentConnectionConnectedAtRef = null;
    }

    private void registerOnDisconnectHandlers() {
        if (currentConnectionRef == null || lastOnlineRef == null) {
            return;
        }

        OnDisconnect removeConnectionOnDisconnect = currentConnectionRef.onDisconnect();
        removeConnectionOnDisconnect.removeValue((error, ref) -> {
            if (error != null) {
                Log.e(TAG, "onDisconnect removeValue failed", error.toException());
            } else {
                Log.d(TAG, "onDisconnect removeValue registered");
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

    private void startHeartbeatLoop() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable);

        if (isAppForeground && currentConnectionRef != null) {
            heartbeatHandler.post(heartbeatRunnable);
        }
    }

    private void stopHeartbeatLoop() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
    }

    private void syncHeartbeatNow() {
        if (currentConnectionRef == null) {
            return;
        }

        syncConnectionOnlineNow(false);
    }

    private void syncConnectionOnlineNow(boolean includeConnectedAt) {
        if (currentConnectionRef == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("presence_version", PRESENCE_VERSION);
        updates.put("device_id", presenceDeviceId);
        updates.put("isForeground", isAppForeground);
        updates.put("heartbeat_at", ServerValue.TIMESTAMP);

        if (includeConnectedAt) {
            updates.put("connected_at", ServerValue.TIMESTAMP);
        }

        currentConnectionRef.updateChildren(updates, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to sync connection online", error.toException());
            } else {
                Log.d(TAG, "connection online synced, foreground = " + isAppForeground);
            }
        });
    }

    private void markCurrentConnectionBackground() {
        if (currentConnectionRef == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("presence_version", PRESENCE_VERSION);
        updates.put("device_id", presenceDeviceId);
        updates.put("isForeground", false);
        updates.put("heartbeat_at", ServerValue.TIMESTAMP);

        currentConnectionRef.updateChildren(updates, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to set connection background", error.toException());
            } else {
                Log.d(TAG, "connection background synced");
            }
        });
    }

    private void syncPresenceRole(@NonNull String uid) {
        if (userStatusRef == null || firestore == null) {
            return;
        }

        firestore.collection("accounts")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String fallbackRole = getLocalRoleFallback();

                    String rawRole = doc != null && doc.exists()
                            ? doc.getString("role")
                            : fallbackRole;

                    final String finalRole = normalizeRole(rawRole, fallbackRole);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("role", finalRole);
                    updates.put("presence_version", PRESENCE_VERSION);
                    updates.put("device_id", presenceDeviceId);

                    userStatusRef.updateChildren(updates, (error, ref) -> {
                        if (error != null) {
                            Log.e(TAG, "Failed to sync presence role", error.toException());
                        } else {
                            Log.d(TAG, "presence role synced = " + finalRole);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    final String fallbackRole = getLocalRoleFallback();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("role", fallbackRole);
                    updates.put("presence_version", PRESENCE_VERSION);
                    updates.put("device_id", presenceDeviceId);

                    userStatusRef.updateChildren(updates, (error, ref) -> {
                        if (error != null) {
                            Log.e(TAG, "Failed to sync fallback presence role", error.toException());
                        } else {
                            Log.d(TAG, "fallback presence role synced = " + fallbackRole);
                        }
                    });
                });
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