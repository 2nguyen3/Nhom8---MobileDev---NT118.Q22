package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class AdminAccountListItem {

    private String accountId;
    private String email;
    private String role;
    private String status;

    private String displayName;
    private String avatarUrl;
    private String subtitle;

    private boolean online;
    private Timestamp lastSignInAt;

    public AdminAccountListItem() {
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Timestamp getLastSignInAt() {
        return lastSignInAt;
    }

    public void setLastSignInAt(Timestamp lastSignInAt) {
        this.lastSignInAt = lastSignInAt;
    }
}