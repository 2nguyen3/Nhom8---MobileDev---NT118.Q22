package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class CommunityPostModel {
    private String post_id;
    private String user_id;
    private String content;
    private String mood_tag;
    private String mood_emoji;
    private boolean is_anonymous;
    private String author_name;
    private String author_avatar;
    private Timestamp created_at;
    private Timestamp updated_at;
    private int comment_count;
    private int like_count;
    private int empathy_count;
    private int report_count;
    private String status;

    private String moderation_status;
    private String moderation_decision;
    private String moderation_reason;
    private String moderated_by;
    private Timestamp moderated_at;

    public CommunityPostModel() {}

    public CommunityPostModel(
            String post_id,
            String user_id,
            String content,
            String mood_tag,
            String mood_emoji,
            boolean is_anonymous,
            String author_name,
            String author_avatar,
            Timestamp created_at
    ) {
        this.post_id = post_id;
        this.user_id = user_id;
        this.content = content;
        this.mood_tag = mood_tag;
        this.mood_emoji = mood_emoji;
        this.is_anonymous = is_anonymous;
        this.author_name = author_name;
        this.author_avatar = author_avatar;
        this.created_at = created_at;
        this.updated_at = created_at;
        this.comment_count = 0;
        this.like_count = 0;
        this.empathy_count = 0;
        this.report_count = 0;
        this.status = "ACTIVE";
        this.moderation_status = "VISIBLE";
        this.moderation_decision = "";
        this.moderation_reason = "";
        this.moderated_by = "";
        this.moderated_at = null;
    }

    public String getPost_id() {
        return post_id;
    }

    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMood_tag() {
        return mood_tag;
    }

    public void setMood_tag(String mood_tag) {
        this.mood_tag = mood_tag;
    }

    public String getMood_emoji() {
        return mood_emoji;
    }

    public void setMood_emoji(String mood_emoji) {
        this.mood_emoji = mood_emoji;
    }

    public boolean isIs_anonymous() {
        return is_anonymous;
    }

    public void setIs_anonymous(boolean is_anonymous) {
        this.is_anonymous = is_anonymous;
    }

    public String getAuthor_name() {
        return author_name;
    }

    public void setAuthor_name(String author_name) {
        this.author_name = author_name;
    }

    public String getAuthor_avatar() {
        return author_avatar;
    }

    public void setAuthor_avatar(String author_avatar) {
        this.author_avatar = author_avatar;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public Timestamp getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Timestamp updated_at) {
        this.updated_at = updated_at;
    }

    public int getComment_count() {
        return comment_count;
    }

    public void setComment_count(int comment_count) {
        this.comment_count = comment_count;
    }

    public int getLike_count() {
        return like_count;
    }

    public void setLike_count(int like_count) {
        this.like_count = like_count;
    }

    public int getEmpathy_count() {
        return empathy_count;
    }

    public void setEmpathy_count(int empathy_count) {
        this.empathy_count = empathy_count;
    }

    public int getReport_count() {
        return report_count;
    }

    public void setReport_count(int report_count) {
        this.report_count = report_count;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getModeration_status() { return moderation_status; }

    public void setModeration_status(String moderation_status) { this.moderation_status = moderation_status; }

    public String getModeration_decision() { return moderation_decision; }

    public void setModeration_decision(String moderation_decision) { this.moderation_decision = moderation_decision; }

    public String getModeration_reason() { return moderation_reason; }

    public void setModeration_reason(String moderation_reason) { this.moderation_reason = moderation_reason; }

    public String getModerated_by() { return moderated_by; }

    public void setModerated_by(String moderated_by) { this.moderated_by = moderated_by; }

    public Timestamp getModerated_at() { return moderated_at; }

    public void setModerated_at(Timestamp moderated_at) { this.moderated_at = moderated_at; }
}