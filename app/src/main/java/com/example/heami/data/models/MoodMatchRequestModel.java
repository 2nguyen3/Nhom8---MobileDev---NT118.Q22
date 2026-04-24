package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class MoodMatchRequestModel {
    private String request_id;
    private String user_id;
    private String mood_tag;
    private Timestamp created_at;
    private Timestamp expires_at;
    private String status;          // SEARCHING | MATCHED | CANCELLED | TIMEOUT
    private String matched_user_id;
    private String match_id;

    public MoodMatchRequestModel() {}

    public MoodMatchRequestModel(
            String request_id,
            String user_id,
            String mood_tag,
            Timestamp created_at,
            Timestamp expires_at
    ) {
        this.request_id = request_id;
        this.user_id = user_id;
        this.mood_tag = mood_tag;
        this.created_at = created_at;
        this.expires_at = expires_at;
        this.status = "SEARCHING";
        this.matched_user_id = "";
        this.match_id = "";
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getMood_tag() {
        return mood_tag;
    }

    public void setMood_tag(String mood_tag) {
        this.mood_tag = mood_tag;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public Timestamp getExpires_at() {
        return expires_at;
    }

    public void setExpires_at(Timestamp expires_at) {
        this.expires_at = expires_at;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMatched_user_id() {
        return matched_user_id;
    }

    public void setMatched_user_id(String matched_user_id) {
        this.matched_user_id = matched_user_id;
    }

    public String getMatch_id() {
        return match_id;
    }

    public void setMatch_id(String match_id) {
        this.match_id = match_id;
    }
}