package com.example.heami.data.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.Map;

public class ConsultationModel implements Serializable {
    private String sessionId;       // session_id
    private String userId;          // user_id
    private String doctorId;        // doctor_id
    private String doctorName;      // doctor_name
    private String doctorAvatar;    // doctor_avatar
    private String packageType;     // package_type
    private String formatType;      // format_type
    private String status;          // status
    private String transactionId;   // transaction_id
    private double price;           // price
    private Timestamp startTime;    // start_time
    private Timestamp endTime;      // end_time
    private Timestamp bookedAt;     // booked_at
    private String note;            // note
    private String doctorNotes;     // doctor_notes
    private Map<String, Object> userFeedback; // user_feedback
    private String canceledBy;      // canceled_by

    public ConsultationModel() {
        // Required for Firebase
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorAvatar() { return doctorAvatar; }
    public void setDoctorAvatar(String doctorAvatar) { this.doctorAvatar = doctorAvatar; }

    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }

    public String getFormatType() { return formatType; }
    public void setFormatType(String formatType) { this.formatType = formatType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public Timestamp getBookedAt() { return bookedAt; }
    public void setBookedAt(Timestamp bookedAt) { this.bookedAt = bookedAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }

    public Map<String, Object> getUserFeedback() { return userFeedback; }
    public void setUserFeedback(Map<String, Object> userFeedback) { this.userFeedback = userFeedback; }

    public String getCanceledBy() { return canceledBy; }
    public void setCanceledBy(String canceledBy) { this.canceledBy = canceledBy; }
}
