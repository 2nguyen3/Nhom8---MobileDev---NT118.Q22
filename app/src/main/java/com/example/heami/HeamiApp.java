package com.example.heami;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HeamiApp extends Application implements DefaultLifecycleObserver {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    public void onCreate() {
        super.onCreate();

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        updatePresence(true);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        updatePresence(false);
    }

    private void updatePresence(boolean isOnline) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        firestore.collection("users")
                .document(currentUser.getUid())
                .update(
                        "is_online", isOnline,
                        "last_seen_at", Timestamp.now()
                );
    }
}