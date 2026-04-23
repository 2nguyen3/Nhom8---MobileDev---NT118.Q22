package com.example.heami.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.Map;

public class ConsultationModel implements Serializable {
    @PropertyName("session_id")
    private String sessionId;
    @PropertyName("user_id")
    private String userId;
    @PropertyName("doctor_id")
    private String doctorId;
    @PropertyName("doctor_name")
    private String doctorName;
    @PropertyName("doctor_avatar")
    private String doctorAvatar;
    @PropertyName("package_type")
    private String packageType;
    @PropertyName("format_type")
    private String formatType;
    @PropertyName("status")
    private String status;
    @PropertyName("transaction_id")
    private String transactionId;
    @PropertyName("price")
    private double price;
    @PropertyName("start_time")
    private Timestamp startTime;
    @PropertyName("end_time")
    private Timestamp endTime;
    @PropertyName("booked_at")
    private Timestamp bookedAt;
    @PropertyName("note")
    private String note;
    @PropertyName("doctor_notes")
    private String doctorNotes;
    @PropertyName("user_feedback")
    private Map<String, Object> userFeedback;
    @PropertyName("canceled_by")
    private String canceledBy;
    @PropertyName("is_online")
    private boolean isOnline;
    
    private boolean isExpanded;
    private boolean doctorOnline;

    public ConsultationModel() { }

    @PropertyName("session_id")
    public String getSessionId() { return sessionId; }
    @PropertyName("session_id")
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("doctor_id")
    public String getDoctorId() { return doctorId; }
    @PropertyName("doctor_id")
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    @PropertyName("doctor_name")
    public String getDoctorName() { return doctorName; }
    @PropertyName("doctor_name")
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    @PropertyName("doctor_avatar")
    public String getDoctorAvatar() { return doctorAvatar; }
    @PropertyName("doctor_avatar")
    public void setDoctorAvatar(String doctorAvatar) { this.doctorAvatar = doctorAvatar; }

    @PropertyName("package_type")
    public String getPackageType() { return packageType; }
    @PropertyName("package_type")
    public void setPackageType(String packageType) { this.packageType = packageType; }

    @PropertyName("format_type")
    public String getFormatType() { return formatType; }
    @PropertyName("format_type")
    public void setFormatType(String formatType) { this.formatType = formatType; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("transaction_id")
    public String getTransactionId() { return transactionId; }
    @PropertyName("transaction_id")
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    @PropertyName("price")
    public double getPrice() { return price; }
    @PropertyName("price")
    public void setPrice(double price) { this.price = price; }

    @PropertyName("start_time")
    public Timestamp getStartTime() { return startTime; }
    @PropertyName("start_time")
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    @PropertyName("end_time")
    public Timestamp getEndTime() { return endTime; }
    @PropertyName("end_time")
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    @PropertyName("booked_at")
    public Timestamp getBookedAt() { return bookedAt; }
    @PropertyName("booked_at")
    public void setBookedAt(Timestamp bookedAt) { this.bookedAt = bookedAt; }

    @PropertyName("note")
    public String getNote() { return note; }
    @PropertyName("note")
    public void setNote(String note) { this.note = note; }

    @PropertyName("doctor_notes")
    public String getDoctorNotes() { return doctorNotes; }
    @PropertyName("doctor_notes")
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }

    @PropertyName("user_feedback")
    public Map<String, Object> getUserFeedback() { return userFeedback; }
    @PropertyName("user_feedback")
    public void setUserFeedback(Map<String, Object> userFeedback) { this.userFeedback = userFeedback; }

    @PropertyName("canceled_by")
    public String getCanceledBy() { return canceledBy; }
    @PropertyName("canceled_by")
    public void setCanceledBy(String canceledBy) { this.canceledBy = canceledBy; }

    @PropertyName("is_online")
    public boolean isOnline() { return isOnline; }
    @PropertyName("is_online")
    public void setOnline(boolean online) { isOnline = online; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public boolean isDoctorOnline() { return doctorOnline; }
    public void setDoctorOnline(boolean doctorOnline) { this.doctorOnline = doctorOnline; }
}
