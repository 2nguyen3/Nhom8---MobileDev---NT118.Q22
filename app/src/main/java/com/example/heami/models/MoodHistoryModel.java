package com.example.heami.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MoodHistoryModel {
    private String record_id;
    private String user_id;

    private String mood_tag;
    private String mood_emoji;
    private String mood_desc;
    private int mood_percent;
    private int energy_level;

    private String source;
    private boolean ai_analysis;

    private List<String> causes;
    private List<String> recommendations;
    private String note;

    private Timestamp timestamp;
    private Timestamp updated_at;

    public MoodHistoryModel() {
        this.causes = new ArrayList<>();
        this.recommendations = new ArrayList<>();
    }

    public MoodHistoryModel(
            String record_id,
            String user_id,
            String mood_tag,
            String mood_emoji,
            String mood_desc,
            int mood_percent,
            int energy_level,
            String source,
            boolean ai_analysis,
            List<String> causes,
            String note,
            Timestamp timestamp,
            Timestamp updated_at
    ) {
        this.record_id = record_id;
        this.user_id = user_id;
        this.mood_tag = mood_tag;
        this.mood_emoji = mood_emoji;
        this.mood_desc = mood_desc;
        this.mood_percent = mood_percent;
        this.energy_level = energy_level;
        this.source = source;
        this.ai_analysis = ai_analysis;
        this.causes = causes != null ? causes : new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.note = note;
        this.timestamp = timestamp;
        this.updated_at = updated_at;
    }

    public String getRecord_id() { return record_id; }
    public void setRecord_id(String record_id) { this.record_id = record_id; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public String getMood_tag() { return mood_tag; }
    public void setMood_tag(String mood_tag) { this.mood_tag = mood_tag; }

    public String getMood_emoji() { return mood_emoji; }
    public void setMood_emoji(String mood_emoji) { this.mood_emoji = mood_emoji; }

    public String getMood_desc() { return mood_desc; }
    public void setMood_desc(String mood_desc) { this.mood_desc = mood_desc; }

    public int getMood_percent() { return mood_percent; }
    public void setMood_percent(int mood_percent) { this.mood_percent = mood_percent; }

    public int getEnergy_level() { return energy_level; }
    public void setEnergy_level(int energy_level) { this.energy_level = energy_level; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isAi_analysis() { return ai_analysis; }
    public void setAi_analysis(boolean ai_analysis) { this.ai_analysis = ai_analysis; }

    public List<String> getCauses() { return causes; }
    public void setCauses(List<String> causes) { this.causes = causes; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public Timestamp getUpdated_at() { return updated_at; }
    public void setUpdated_at(Timestamp updated_at) { this.updated_at = updated_at; }
}