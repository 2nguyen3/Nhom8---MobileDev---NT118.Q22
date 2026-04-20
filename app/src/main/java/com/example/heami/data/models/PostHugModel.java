package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class PostHugModel {
    private String user_id;
    private String author_name;
    private String author_avatar;
    private int hug_count;
    private Timestamp created_at;
    private Timestamp updated_at;

    public PostHugModel() {}

    public PostHugModel(
            String user_id,
            String author_name,
            String author_avatar,
            int hug_count,
            Timestamp created_at,
            Timestamp updated_at
    ) {
        this.user_id = user_id;
        this.author_name = author_name;
        this.author_avatar = author_avatar;
        this.hug_count = hug_count;
        this.created_at = created_at;
        this.updated_at = updated_at;
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

    public int getHug_count() {
        return hug_count;
    }

    public void setHug_count(int hug_count) {
        this.hug_count = hug_count;
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
}