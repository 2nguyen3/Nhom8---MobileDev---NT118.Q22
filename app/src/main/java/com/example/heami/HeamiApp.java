package com.example.heami;

import android.app.Application;
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

import android.os.Handler;
import android.os.Looper;

public class HeamiApp extends Application implements DefaultLifecycleObserver {

    private static final String TAG = "HeamiPresence";
    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

    private FirebaseAuth auth;
    private FirebaseDatabase realtimeDb;

    private FirebaseAuth.AuthStateListener authStateListener;
    private ValueEventListener connectedListener;

    private static boolean appForegroundStatic = false;
    private static final long HEARTBEAT_INTERVAL_MS = 15_000L;

    private DatabaseReference connectedRef;
    private DatabaseReference userStatusRef;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference currentConnectionRef;

    private DatabaseReference currentConnectionForegroundRef;
    private DatabaseReference currentConnectionHeartbeatRef;
    private DatabaseReference currentConnectionConnectedAtRef;

    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            syncHeartbeatNow();

            if (isAppForeground && currentConnectionHeartbeatRef != null) {
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        }
    };

    private String currentPresenceUid = "";
    private boolean isAppForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();

        auth = FirebaseAuth.getInstance();
        realtimeDb = FirebaseDatabase.getInstance(RTDB_URL);

        Log.d(TAG, "HeamiApp onCreate");

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

        if (currentConnectionForegroundRef != null) {
            currentConnectionForegroundRef.setValue(true, (error, ref) -> {
                if (error != null) {
                    Log.e(TAG, "Failed to set connection isForeground=true", error.toException());
                } else {
                    Log.d(TAG, "connection isForeground=true");
                }
            });
        }

        syncHeartbeatNow();
        startHeartbeatLoop();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isAppForeground = false;
        appForegroundStatic = false;
        Log.d(TAG, "App background");

        stopHeartbeatLoop();

        if (currentConnectionForegroundRef != null) {
            currentConnectionForegroundRef.setValue(false, (error, ref) -> {
                if (error != null) {
                    Log.e(TAG, "Failed to set connection isForeground=false", error.toException());
                } else {
                    Log.d(TAG, "connection isForeground=false");
                }
            });
        }

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

        connectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                Log.d(TAG, ".info/connected = " + connected);

                if (connected == null || !connected) {
                    stopHeartbeatLoop();
                    return;
                }

                currentConnectionRef = myConnectionsRef.push();
                currentConnectionForegroundRef = currentConnectionRef.child("isForeground");
                currentConnectionHeartbeatRef = currentConnectionRef.child("heartbeat_at");
                currentConnectionConnectedAtRef = currentConnectionRef.child("connected_at");

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

                currentConnectionConnectedAtRef.setValue(ServerValue.TIMESTAMP, (error, ref) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to set connected_at", error.toException());
                    } else {
                        Log.d(TAG, "connected_at set");
                    }
                });

                currentConnectionForegroundRef.setValue(isAppForeground, (error, ref) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to sync connection isForeground", error.toException());
                    } else {
                        Log.d(TAG, "connection isForeground synced = " + isAppForeground);
                    }
                });

                syncHeartbeatNow();

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

        if (currentConnectionForegroundRef != null) {
            currentConnectionForegroundRef.setValue(false);
        }

        if (currentConnectionRef != null) {
            currentConnectionRef.removeValue();
            currentConnectionRef = null;
        }

        if (lastOnlineRef != null) {
            lastOnlineRef.setValue(ServerValue.TIMESTAMP);
        }

        connectedListener = null;
        connectedRef = null;
        userStatusRef = null;
        myConnectionsRef = null;
        lastOnlineRef = null;

        currentConnectionForegroundRef = null;
        currentConnectionHeartbeatRef = null;
        currentConnectionConnectedAtRef = null;
    }

    private void startHeartbeatLoop() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable);

        if (isAppForeground && currentConnectionHeartbeatRef != null) {
            heartbeatHandler.post(heartbeatRunnable);
        }
    }

    private void stopHeartbeatLoop() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
    }

    private void syncHeartbeatNow() {
        if (currentConnectionHeartbeatRef == null) {
            return;
        }

        currentConnectionHeartbeatRef.setValue(ServerValue.TIMESTAMP, (error, ref) -> {
            if (error != null) {
                Log.e(TAG, "Failed to sync connection heartbeat_at", error.toException());
            } else {
                Log.d(TAG, "connection heartbeat_at synced");
            }
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

        android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}