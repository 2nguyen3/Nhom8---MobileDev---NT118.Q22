package com.example.heami.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserModel {
    private String user_id;
    private String nickname;
    private String avatar_url;
    private String motto;
    private List<String> mood_goals;
    private Double hr_baseline;
    private boolean is_protected_mode;
    private int current_streak;
    private int longest_streak;
    private Timestamp last_activity_date;

    public UserModel() {}

    public UserModel(String user_id, String nickname, String avatar_url) {
        this.user_id = user_id;
        this.nickname = nickname;
        this.avatar_url = avatar_url;
        this.motto = "";
        this.mood_goals = new ArrayList<>();
        this.hr_baseline = 0.0;
        this.is_protected_mode = false;
        this.current_streak = 0;
        this.longest_streak = 0;
        this.last_activity_date = Timestamp.now();
    }

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

    public boolean isIs_protected_mode() { return is_protected_mode; }
    public void setIs_protected_mode(boolean is_protected_mode) { this.is_protected_mode = is_protected_mode; }

    public int getCurrent_streak() { return current_streak; }
    public void setCurrent_streak(int current_streak) { this.current_streak = current_streak; }

    public int getLongest_streak() { return longest_streak; }
    public void setLongest_streak(int longest_streak) { this.longest_streak = longest_streak; }

    public Timestamp getLast_activity_date() { return last_activity_date; }
    public void setLast_activity_date(Timestamp last_activity_date) { this.last_activity_date = last_activity_date; }
}