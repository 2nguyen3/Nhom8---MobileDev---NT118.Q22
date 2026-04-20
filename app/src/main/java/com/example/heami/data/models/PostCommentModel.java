package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class PostCommentModel {
    private String comment_id;
    private String post_id;
    private String user_id;
    private String content;
    private boolean is_anonymous;
    private String author_name;
    private String author_avatar;
    private Timestamp created_at;
    private int report_count;
    private String status;

    public PostCommentModel() {}

    public PostCommentModel(
            String comment_id,
            String post_id,
            String user_id,
            String content,
            boolean is_anonymous,
            String author_name,
            String author_avatar,
            Timestamp created_at
    ) {
        this.comment_id = comment_id;
        this.post_id = post_id;
        this.user_id = user_id;
        this.content = content;
        this.is_anonymous = is_anonymous;
        this.author_name = author_name;
        this.author_avatar = author_avatar;
        this.created_at = created_at;
        this.report_count = 0;
        this.status = "ACTIVE";
    }

    public String getComment_id() {
        return comment_id;
    }

    public void setComment_id(String comment_id) {
        this.comment_id = comment_id;
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
}