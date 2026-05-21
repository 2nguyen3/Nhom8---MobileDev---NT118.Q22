package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class CommunityReportModel {

    private String report_id;
    private String target_type;      // POST / COMMENT
    private String target_id;
    private String post_id;

    private String reported_by;
    private String reported_user_id;

    private String reason_code;
    private String reason_text;

    private String status;           // PENDING / REVIEWED / RESOLVED / REJECTED
    private Timestamp created_at;
    private Timestamp reviewed_at;
    private String reviewed_by;

    private String action_taken;     // NONE / HIDDEN / RESTORED / DELETED

    private String snapshot_text;
    private String snapshot_author_name;

    public CommunityReportModel() {
    }

    public String getReport_id() {
        return report_id;
    }

    public void setReport_id(String report_id) {
        this.report_id = report_id;
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

    public String getPost_id() {
        return post_id;
    }

    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public String getReported_by() {
        return reported_by;
    }

    public void setReported_by(String reported_by) {
        this.reported_by = reported_by;
    }

    public String getReported_user_id() {
        return reported_user_id;
    }

    public void setReported_user_id(String reported_user_id) {
        this.reported_user_id = reported_user_id;
    }

    public String getReason_code() {
        return reason_code;
    }

    public void setReason_code(String reason_code) {
        this.reason_code = reason_code;
    }

    public String getReason_text() {
        return reason_text;
    }

    public void setReason_text(String reason_text) {
        this.reason_text = reason_text;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public Timestamp getReviewed_at() {
        return reviewed_at;
    }

    public void setReviewed_at(Timestamp reviewed_at) {
        this.reviewed_at = reviewed_at;
    }

    public String getReviewed_by() {
        return reviewed_by;
    }

    public void setReviewed_by(String reviewed_by) {
        this.reviewed_by = reviewed_by;
    }

    public String getAction_taken() {
        return action_taken;
    }

    public void setAction_taken(String action_taken) {
        this.action_taken = action_taken;
    }

    public String getSnapshot_text() {
        return snapshot_text;
    }

    public void setSnapshot_text(String snapshot_text) {
        this.snapshot_text = snapshot_text;
    }

    public String getSnapshot_author_name() {
        return snapshot_author_name;
    }

    public void setSnapshot_author_name(String snapshot_author_name) {
        this.snapshot_author_name = snapshot_author_name;
    }
}