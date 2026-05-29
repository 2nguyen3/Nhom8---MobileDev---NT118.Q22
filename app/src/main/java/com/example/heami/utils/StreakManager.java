package com.example.heami.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class StreakManager {

    public interface StreakUpdateCallback {
        void onSuccess(int currentStreak, int longestStreak, long totalCheckins);
        void onFailure(Exception e);
    }

    public interface StreakResetCallback {
        void onResult(int currentStreak);
    }

    /**
     * Kiểm tra nếu user không check-in trong ngày hôm qua (hoặc lâu hơn) thì reset current_streak về 0.
     * total_checkins KHÔNG bị ảnh hưởng - chỉ mất streak nếu bỏ lỡ ngày check-in.
     * Nên gọi method này khi mở màn hình Profile để đồng bộ streak hiển thị.
     */
    public static void checkAndResetStreakIfMissed(String userId, StreakResetCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                if (callback != null) callback.onResult(0);
                return;
            }

            long currentStreak = 0;
            if (documentSnapshot.contains("current_streak")) {
                currentStreak = documentSnapshot.getLong("current_streak");
            }

            // Nếu streak đã là 0 thì không cần kiểm tra
            if (currentStreak <= 0) {
                if (callback != null) callback.onResult(0);
                return;
            }

            Timestamp lastActivity = null;
            if (documentSnapshot.contains("last_activity_date")) {
                lastActivity = documentSnapshot.getTimestamp("last_activity_date");
            }

            // Nếu chưa từng check-in thì streak đã là 0
            if (lastActivity == null) {
                if (callback != null) callback.onResult(0);
                return;
            }

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar lastCheckIn = Calendar.getInstance();
            lastCheckIn.setTime(lastActivity.toDate());
            lastCheckIn.set(Calendar.HOUR_OF_DAY, 0);
            lastCheckIn.set(Calendar.MINUTE, 0);
            lastCheckIn.set(Calendar.SECOND, 0);
            lastCheckIn.set(Calendar.MILLISECOND, 0);

            long diffMs = today.getTimeInMillis() - lastCheckIn.getTimeInMillis();
            long diffDays = diffMs / (24 * 60 * 60 * 1000);

            // Nếu bỏ lỡ hơn 1 ngày (tức là hôm qua không check-in) thì reset streak về 0
            if (diffDays > 1) {
                final long resetStreak = 0;
                Map<String, Object> updates = new HashMap<>();
                updates.put("current_streak", resetStreak);
                userRef.update(updates).addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onResult((int) resetStreak);
                }).addOnFailureListener(e -> {
                    // Nếu update thất bại, vẫn trả về 0 để UI hiển thị đúng
                    if (callback != null) callback.onResult(0);
                });
            } else {
                // Streak còn hợp lệ (check-in hôm nay hoặc hôm qua)
                if (callback != null) callback.onResult((int) currentStreak);
            }
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onResult(-1); // -1 = không thể xác định
        });
    }

    public static void updateCheckInStreak(String userId, StreakUpdateCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);
        
        final long[] result = new long[3]; // [currentStreak, longestStreak, totalCheckins]

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot userDoc = transaction.get(userRef);
            
            int currentStreak = 0;
            int longestStreak = 0;
            long totalCheckins = 0;
            Timestamp lastActivity = null;

            if (userDoc.exists()) {
                if (userDoc.contains("current_streak")) {
                    currentStreak = userDoc.getLong("current_streak").intValue();
                }
                if (userDoc.contains("longest_streak")) {
                    longestStreak = userDoc.getLong("longest_streak").intValue();
                }
                if (userDoc.contains("total_checkins")) {
                    totalCheckins = userDoc.getLong("total_checkins");
                }
                if (userDoc.contains("last_activity_date")) {
                    lastActivity = userDoc.getTimestamp("last_activity_date");
                }
            }

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar lastCheckIn = Calendar.getInstance();
            if (lastActivity != null) {
                lastCheckIn.setTime(lastActivity.toDate());
                lastCheckIn.set(Calendar.HOUR_OF_DAY, 0);
                lastCheckIn.set(Calendar.MINUTE, 0);
                lastCheckIn.set(Calendar.SECOND, 0);
                lastCheckIn.set(Calendar.MILLISECOND, 0);
            }

            long diffMs = today.getTimeInMillis() - (lastActivity != null ? lastCheckIn.getTimeInMillis() : 0);
            long diffDays = diffMs / (24 * 60 * 60 * 1000);

            totalCheckins += 1;

            if (lastActivity == null) {
                currentStreak = 1;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                if (diffDays == 0) {
                    // Already checked in today, do not increment current streak
                } else if (diffDays == 1) {
                    // Checked in yesterday, increment streak
                    currentStreak += 1;
                    longestStreak = Math.max(longestStreak, currentStreak);
                } else {
                    // Streak broken
                    currentStreak = 1;
                    longestStreak = Math.max(longestStreak, currentStreak);
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("current_streak", currentStreak);
            updates.put("longest_streak", longestStreak);
            updates.put("total_checkins", totalCheckins);
            updates.put("last_activity_date", Timestamp.now());

            transaction.update(userRef, updates);

            result[0] = currentStreak;
            result[1] = longestStreak;
            result[2] = totalCheckins;

            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) {
                callback.onSuccess((int) result[0], (int) result[1], result[2]);
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onFailure(e);
            }
        });
    }
}
