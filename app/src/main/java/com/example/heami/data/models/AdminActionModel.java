package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class AdminActionModel {

    private String action_id;
    private String admin_id;
    private String action_type;      // HIDE_POST / RESTORE_POST / DELETE_POST / ...
    private String target_type;      // POST / COMMENT
    private String target_id;
    private String related_report_id;
    private String note;
    private Timestamp created_at;

    public AdminActionModel() {
    }

    public String getAction_id() {
        return action_id;
    }

    public void setAction_id(String action_id) {
        this.action_id = action_id;
    }

    public String getAdmin_id() {
        return admin_id;
    }

    public void setAdmin_id(String admin_id) {
        this.admin_id = admin_id;
    }

    public String getAction_type() {
        return action_type;
    }

    public void setAction_type(String action_type) {
        this.action_type = action_type;
    }

    public String getTarget_type() {
        return target_type;
    }

    public void setTarget_type(String target_type) {
        this.target_type = target_type;
    }

    public String getTarget_id() {
        return target_id;
    }

    public void setTarget_id(String target_id) {
        this.target_id = target_id;
    }

    public String getRelated_report_id() {
        return related_report_id;
    }

    public void setRelated_report_id(String related_report_id) {
        this.related_report_id = related_report_id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }
}