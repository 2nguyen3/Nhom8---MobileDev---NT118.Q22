package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class MoodMatchModel {
    private String match_id;
    private String user_a_id;
    private String user_b_id;
    private String mood_tag;
    private Timestamp created_at;
    private String status;
    private String room_id;

    private Timestamp ended_at;
    private String ended_by;
    private String end_reason;

    public MoodMatchModel() {}

    public MoodMatchModel(
            String match_id,
            String user_a_id,
            String user_b_id,
            String mood_tag,
            Timestamp created_at
    ) {
        this.match_id = match_id;
        this.user_a_id = user_a_id;
        this.user_b_id = user_b_id;
        this.mood_tag = mood_tag;
        this.created_at = created_at;
        this.status = "ACTIVE";
        this.room_id = "";

        this.ended_at = null;
        this.ended_by = "";
        this.end_reason = "";
    }

    public String getMatch_id() {
        return match_id;
    }

    public void setMatch_id(String match_id) {
        this.match_id = match_id;
    }

    public String getUser_a_id() {
        return user_a_id;
    }

    public void setUser_a_id(String user_a_id) {
        this.user_a_id = user_a_id;
    }

    public String getUser_b_id() {
        return user_b_id;
    }

    public void setUser_b_id(String user_b_id) {
        this.user_b_id = user_b_id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRoom_id() {
        return room_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public Timestamp getEnded_at() {
        return ended_at;
    }

    public void setEnded_at(Timestamp ended_at) {
        this.ended_at = ended_at;
    }

    public String getEnded_by() {
        return ended_by;
    }

    public void setEnded_by(String ended_by) {
        this.ended_by = ended_by;
    }

    public String getEnd_reason() {
        return end_reason;
    }

    public void setEnd_reason(String end_reason) {
        this.end_reason = end_reason;
    }
}