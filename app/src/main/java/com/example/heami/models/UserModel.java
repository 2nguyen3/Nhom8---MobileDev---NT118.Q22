package com.example.heami.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class UserModel {
    private String user_id;
    private String nickname;
    private String avatar_url;
    private String motto;
    private List<String> mood_goals;
    private Double hr_baseline;
    private boolean is_protected_mode;

    public UserModel() {}

    public UserModel(String user_id, String nickname, String avatar_url) {
        this.user_id = user_id;
        this.nickname = nickname;
        this.avatar_url = avatar_url;
        this.is_protected_mode = false; // Mặc định tắt
        this.hr_baseline = 0.0;
    }

    // Getter và Setter
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar_url() { return avatar_url; }
    public void setAvatar_url(String avatar_url) { this.avatar_url = avatar_url; }

    public String getMotto() { return motto; }
    public void setMotto(String motto) { this.motto = motto; }

    public List<String> getMood_goals() { return mood_goals; }
    public void setMood_goals(List<String> mood_goals) { this.mood_goals = mood_goals; }

    public Double getHr_baseline() { return hr_baseline; }
    public void setHr_baseline(Double hr_baseline) { this.hr_baseline = hr_baseline; }

    public boolean is_protected_mode() { return is_protected_mode; }
    public void setIs_protected_mode(boolean is_protected_mode) { this.is_protected_mode = is_protected_mode; }
}
