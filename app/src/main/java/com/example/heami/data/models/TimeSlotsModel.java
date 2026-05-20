package com.example.heami.data.models;

import com.google.firebase.Timestamp;

public class TimeSlotsModel {
    private String slot_id;
    private String doctor_id;
    private String session_id; // Trường liên kết quan trọng
    private String status;
    private Timestamp start_time;
    private Timestamp end_time;

    // Constructor trống bắt buộc phải có cho Firebase
    public TimeSlotsModel() {}

    public String getSlot_id() { return slot_id; }
    public void setSlot_id(String slot_id) { this.slot_id = slot_id; }

    public String getDoctor_id() { return doctor_id; }
    public void setDoctor_id(String doctor_id) { this.doctor_id = doctor_id; }

    public String getSession_id() { return session_id; }
    public void setSession_id(String session_id) { this.session_id = session_id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getStart_time() { return start_time; }
    public void setStart_time(Timestamp start_time) { this.start_time = start_time; }

    public Timestamp getEnd_time() { return end_time; }
    public void setEnd_time(Timestamp end_time) { this.end_time = end_time; }
}