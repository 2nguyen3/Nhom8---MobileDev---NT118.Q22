package com.example.heami.checkin;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class CheckInRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface SaveCallback {
        void onSuccess(String recordId);
        void onError(@NonNull Exception e);
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public void upsertToday(
            @NonNull Map<String, Object> payload,
            @NonNull SaveCallback callback
    ) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("User chưa đăng nhập"));
            return;
        }

        String uid = user.getUid();
        String key = todayKey();
        Timestamp now = Timestamp.now();

        db.collection(CheckInConstants.COL_USERS)
                .document(uid)
                .collection(CheckInConstants.SUB_MOOD_HISTORY)
                .whereEqualTo("checkin_date_key", key)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    String recordId;
                    if (query.isEmpty()) {
                        recordId = db.collection(CheckInConstants.COL_USERS)
                                .document(uid)
                                .collection(CheckInConstants.SUB_MOOD_HISTORY)
                                .document()
                                .getId();
                        payload.put("timestamp", now);
                    } else {
                        recordId = query.getDocuments().get(0).getId();
                        Object oldTimestamp = query.getDocuments().get(0).get("timestamp");
                        payload.put("timestamp", oldTimestamp != null ? oldTimestamp : now);
                    }

                    payload.put("record_id", recordId);
                    payload.put("checkin_date_key", key);
                    payload.put("updated_at", now);

                    db.collection(CheckInConstants.COL_USERS)
                            .document(uid)
                            .collection(CheckInConstants.SUB_MOOD_HISTORY)
                            .document(recordId)
                            .set(payload)
                            .addOnSuccessListener(unused -> callback.onSuccess(recordId))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
}
