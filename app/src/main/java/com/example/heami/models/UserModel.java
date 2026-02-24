package com.example.heami.models;

public class UserModel {
    private String uid;
    private String email;
    private String nickname;
    private String role;
    private long createdAt;

    public UserModel() {}

    public UserModel(String uid, String email, String nickname, String role, long createdAt) {
        this.uid = uid;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.createdAt = createdAt;
    }

    // Getter và Setter
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}