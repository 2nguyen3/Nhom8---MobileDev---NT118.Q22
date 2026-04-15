package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class AccountModel {
    private String account_id;        // Firebase UID (Primary Key)
    private String email;
    private String role;              // "USER", "DOCTOR", "ADMIN"
    private String status;            // "ACTIVE", "BANNED", "PENDING_VERIFY"
    private Timestamp created_at;
    private Timestamp last_sign_in_at;
    private Timestamp last_sign_out_at;
    private String active_session_id;
    private String fcm_token;         // Dùng cho thông báo Push Notification

    // Constructor rỗng bắt buộc cho Firebase
    public AccountModel() {}

    public AccountModel(String account_id, String email, String role, String status, Timestamp created_at) {
        this.account_id = account_id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.created_at = created_at;
    }

    // Getter và Setter
    public String getAccount_id() { return account_id; }
    public void setAccount_id(String account_id) { this.account_id = account_id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    public Timestamp getLast_sign_in_at() { return last_sign_in_at; }
    public void setLast_sign_in_at(Timestamp last_sign_in_at) { this.last_sign_in_at = last_sign_in_at; }

    public Timestamp getLast_sign_out_at() { return last_sign_out_at; }
    public void setLast_sign_out_at(Timestamp last_sign_out_at) { this.last_sign_out_at = last_sign_out_at; }

    public String getActive_session_id() { return active_session_id; }
    public void setActive_session_id(String active_session_id) { this.active_session_id = active_session_id; }

    public String getFcm_token() { return fcm_token; }
    public void setFcm_token(String fcm_token) { this.fcm_token = fcm_token; }
}