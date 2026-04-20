package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class PostReportModel {
    private String user_id;
    private String author_name;
    private String author_avatar;
    private String reason_code;
    private String reason_label;
    private String extra_note;
    private Timestamp created_at;
    private Timestamp updated_at;
    private String status;

    public PostReportModel() {}

    public PostReportModel(
            String user_id,
            String author_name,
            String author_avatar,
            String reason_code,
            String reason_label,
            String extra_note,
            Timestamp created_at,
            Timestamp updated_at,
            String status
    ) {
        this.user_id = user_id;
        this.author_name = author_name;
        this.author_avatar = author_avatar;
        this.reason_code = reason_code;
        this.reason_label = reason_label;
        this.extra_note = extra_note;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.status = status;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
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

    public String getReason_code() {
        return reason_code;
    }

    public void setReason_code(String reason_code) {
        this.reason_code = reason_code;
    }

    public String getReason_label() {
        return reason_label;
    }

    public void setReason_label(String reason_label) {
        this.reason_label = reason_label;
    }

    public String getExtra_note() {
        return extra_note;
    }

    public void setExtra_note(String extra_note) {
        this.extra_note = extra_note;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}