package com.example.heami.data.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageModel {
    private String message_id;
    private String sender_id;
    private String text;
    private Timestamp created_at;
    private String message_type;
    private String status;
    private Long client_created_at_ms;
    private List<String> delivered_user_ids;
    private List<String> seen_user_ids;

    public ChatMessageModel() {}

    public ChatMessageModel(
            String message_id,
            String sender_id,
            String text,
            Timestamp created_at
    ) {
        this.message_id = message_id;
        this.sender_id = sender_id;
        this.text = text;
        this.created_at = created_at;
        this.message_type = "TEXT";
        this.status = "ACTIVE";
        this.client_created_at_ms = System.currentTimeMillis();
        this.delivered_user_ids = new ArrayList<>();
        this.seen_user_ids = new ArrayList<>();
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getClient_created_at_ms() { return client_created_at_ms; }

    public void setClient_created_at_ms(Long client_created_at_ms) { this.client_created_at_ms = client_created_at_ms; }

    public List<String> getDelivered_user_ids() { return delivered_user_ids; }

    public void setDelivered_user_ids(List<String> delivered_user_ids) { this.delivered_user_ids = delivered_user_ids; }

    public List<String> getSeen_user_ids() { return seen_user_ids; }

    public void setSeen_user_ids(List<String> seen_user_ids) { this.seen_user_ids = seen_user_ids; }
}