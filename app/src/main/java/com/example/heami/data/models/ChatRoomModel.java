package com.example.heami.data.models;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomModel {
    private String room_id;
    private List<String> member_ids;
    private List<String> member_names;
    private List<String> member_avatars;
    private String type;          // MOOD_MATCH / CONSULTATION
    private String related_id;    // match_id / session_id
    private String match_mood_tag;
    private Timestamp created_at;
    private String last_message;
    private Timestamp last_message_at;
    private String last_sender_id;
    private String status;

    public ChatRoomModel() {
        this.member_ids = new ArrayList<>();
        this.member_names = new ArrayList<>();
        this.member_avatars = new ArrayList<>();
    }

    public ChatRoomModel(
            String room_id,
            List<String> member_ids,
            List<String> member_names,
            List<String> member_avatars,
            String type,
            String related_id,
            String match_mood_tag,
            Timestamp created_at
    ) {
        this.room_id = room_id;
        this.member_ids = member_ids != null ? member_ids : new ArrayList<>();
        this.member_names = member_names != null ? member_names : new ArrayList<>();
        this.member_avatars = member_avatars != null ? member_avatars : new ArrayList<>();
        this.type = type;
        this.related_id = related_id;
        this.match_mood_tag = match_mood_tag;
        this.created_at = created_at;
        this.last_message = "";
        this.last_message_at = created_at;
        this.last_sender_id = "";
        this.status = "ACTIVE";
    }

    public String getRoom_id() {
        return room_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public List<String> getMember_ids() {
        return member_ids;
    }

    public void setMember_ids(List<String> member_ids) {
        this.member_ids = member_ids;
    }

    public List<String> getMember_names() {
        return member_names;
    }

    public void setMember_names(List<String> member_names) {
        this.member_names = member_names;
    }

    public List<String> getMember_avatars() {
        return member_avatars;
    }

    public void setMember_avatars(List<String> member_avatars) {
        this.member_avatars = member_avatars;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRelated_id() {
        return related_id;
    }

    public void setRelated_id(String related_id) {
        this.related_id = related_id;
    }

    public String getMatch_mood_tag() {
        return match_mood_tag;
    }

    public void setMatch_mood_tag(String match_mood_tag) {
        this.match_mood_tag = match_mood_tag;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public String getLast_message() {
        return last_message;
    }

    public void setLast_message(String last_message) {
        this.last_message = last_message;
    }

    public Timestamp getLast_message_at() {
        return last_message_at;
    }

    public void setLast_message_at(Timestamp last_message_at) {
        this.last_message_at = last_message_at;
    }

    public String getLast_sender_id() {
        return last_sender_id;
    }

    public void setLast_sender_id(String last_sender_id) {
        this.last_sender_id = last_sender_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}