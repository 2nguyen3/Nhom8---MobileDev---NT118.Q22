package com.example.heami.data.models;

import java.io.Serializable;

public class BookingModel implements Serializable {
    private String doctorId;
    private String doctorName;
    private String doctorAvatar;
    private String doctorSpecialty;
    private String date; // dd/MM/yyyy
    private String time; // HH:mm
    private String packageType; // e.g., "30 phút", "Gói 7 ngày"
    private String formatType; // e.g., "Gọi video", "Chat"
    private String price;
    private String note;

    public BookingModel() {}

    // Getters and Setters
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorAvatar() { return doctorAvatar; }
    public void setDoctorAvatar(String doctorAvatar) { this.doctorAvatar = doctorAvatar; }

    public String getDoctorSpecialty() { return doctorSpecialty; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }

    public String getFormatType() { return formatType; }
    public void setFormatType(String formatType) { this.formatType = formatType; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
