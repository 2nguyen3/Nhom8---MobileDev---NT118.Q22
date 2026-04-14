package com.example.heami.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class FavoriteManager {
    private static final String PREF_NAME = "HeamiFavoritePrefs_New";
    private static final String KEY_FAVORITES = "favorite_doctor_ids";
    
    private static FavoriteManager instance;
    private SharedPreferences sharedPreferences;
    private Set<String> favoriteDoctorIds;

    private FavoriteManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> savedSet = sharedPreferences.getStringSet(KEY_FAVORITES, new HashSet<>());
        favoriteDoctorIds = new HashSet<>(savedSet);
    }

    public static synchronized FavoriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoriteManager(context);
        }
        return instance;
    }

    public boolean isFavorite(String doctorId) {
        if (doctorId == null) return false;
        return favoriteDoctorIds.contains(doctorId);
    }

    public void toggleFavorite(String doctorId) {
        if (doctorId == null) return;
        
        if (favoriteDoctorIds.contains(doctorId)) {
            favoriteDoctorIds.remove(doctorId);
        } else {
            favoriteDoctorIds.add(doctorId);
        }
        
        // QUAN TRỌNG: Xóa sạch và ghi mới hoàn toàn Set để SharedPreferences nhận diện
        sharedPreferences.edit().remove(KEY_FAVORITES).apply();
        sharedPreferences.edit().putStringSet(KEY_FAVORITES, new HashSet<>(favoriteDoctorIds)).apply();
    }
}
