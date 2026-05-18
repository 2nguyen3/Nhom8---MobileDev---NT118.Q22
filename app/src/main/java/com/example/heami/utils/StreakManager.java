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
