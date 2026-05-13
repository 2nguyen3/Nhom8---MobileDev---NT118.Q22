package com.example.heami.data.models;

import java.util.List;

public class DiaryModel {
    private String id;
    private String date;
    private String content;
    private String mood;
    private List<String> tags;
    private boolean is_visible;
    private long created_at;

    // Constructor trống bắt buộc cho Firestore
    public DiaryModel() {}

    public DiaryModel(String id, String date, String content, String mood, List<String> tags, boolean is_visible, long created_at) {
        this.id = id;
        this.date = date;
        this.content = content;
        this.mood = mood;
        this.tags = tags;
        this.is_visible = is_visible;
        this.created_at = created_at;
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date != null ? date : ""; }
    public void setDate(String date) { this.date = date; }

    public String getContent() { return content != null ? content : ""; }
    public void setContent(String content) { this.content = content; }

    public String getMood() { return mood != null ? mood : "Bình yên"; }
    public void setMood(String mood) { this.mood = mood; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public boolean isIs_visible() { return is_visible; }
    public void setIs_visible(boolean is_visible) { this.is_visible = is_visible; }

    public long getCreated_at() { return created_at; }
    public void setCreated_at(long created_at) { this.created_at = created_at; }
}