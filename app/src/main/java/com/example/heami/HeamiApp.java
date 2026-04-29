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

public class HeamiApp extends Application implements DefaultLifecycleObserver {

    private static final String TAG = "HeamiPresence";
    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

    private FirebaseAuth auth;
    private FirebaseDatabase realtimeDb;

    private FirebaseAuth.AuthStateListener authStateListener;
    private ValueEventListener connectedListener;

    private DatabaseReference connectedRef;
    private DatabaseReference userStatusRef;
    private DatabaseReference myConnectionsRef;
    private DatabaseReference lastOnlineRef;
    private DatabaseReference foregroundRef;
    private DatabaseReference currentConnectionRef;

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
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        isAppForeground = true;
        Log.d(TAG, "App foreground");

        if (foregroundRef != null) {
            foregroundRef.setValue(true, (error, ref) -> {
                if (error != null) {
                    Log.e(TAG, "Failed to set isForeground=true", error.toException());
                } else {
                    Log.d(TAG, "isForeground=true");
                }
            });
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isAppForeground = false;
        Log.d(TAG, "App background");

        if (foregroundRef != null) {
            foregroundRef.setValue(false, (error, ref) -> {
                if (error != null) {
                    Log.e(TAG, "Failed to set isForeground=false", error.toException());
                } else {
                    Log.d(TAG, "isForeground=false");
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
        foregroundRef = userStatusRef.child("isForeground");

        connectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                Log.d(TAG, ".info/connected = " + connected);

                if (connected == null || !connected) {
                    return;
                }

                currentConnectionRef = myConnectionsRef.push();

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

                OnDisconnect setForegroundFalseOnDisconnect = foregroundRef.onDisconnect();
                setForegroundFalseOnDisconnect.setValue(false, (error, ref) -> {
                    if (error != null) {
                        Log.e(TAG, "onDisconnect isForeground=false failed", error.toException());
                    } else {
                        Log.d(TAG, "onDisconnect isForeground=false registered");
                    }
                });

                currentConnectionRef.setValue(true, (error, ref) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to create connection node", error.toException());
                    } else {
                        Log.d(TAG, "Connection node created");
                    }
                });

                foregroundRef.setValue(isAppForeground, (error, ref) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to sync foreground state", error.toException());
                    } else {
                        Log.d(TAG, "Foreground synced = " + isAppForeground);
                    }
                });
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

        if (connectedRef != null && connectedListener != null) {
            connectedRef.removeEventListener(connectedListener);
        }

        if (foregroundRef != null) {
            foregroundRef.setValue(false);
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
        foregroundRef = null;
    }
}