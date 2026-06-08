package com.example.heami.data.models;

import java.util.ArrayList;
import java.util.List;

public class DoctorHomeSummaryModel {

    private int todaySessions;
    private int pendingSessions;
    private int ongoingSessions;
    private int unreadMessages;

    private List<UpcomingSessionItem> upcomingSessions;
    private List<AttentionPatientItem> attentionPatients;

    public DoctorHomeSummaryModel() {
        this.upcomingSessions = new ArrayList<>();
        this.attentionPatients = new ArrayList<>();
    }

    public int getTodaySessions() {
        return todaySessions;
    }

    public void setTodaySessions(int todaySessions) {
        this.todaySessions = todaySessions;
    }

    public int getPendingSessions() {
        return pendingSessions;
    }

    public void setPendingSessions(int pendingSessions) {
        this.pendingSessions = pendingSessions;
    }

    public int getOngoingSessions() {
        return ongoingSessions;
    }

    public void setOngoingSessions(int ongoingSessions) {
        this.ongoingSessions = ongoingSessions;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public List<UpcomingSessionItem> getUpcomingSessions() {
        return upcomingSessions;
    }

    public void setUpcomingSessions(List<UpcomingSessionItem> upcomingSessions) {
        this.upcomingSessions = upcomingSessions != null ? upcomingSessions : new ArrayList<>();
    }

    public List<AttentionPatientItem> getAttentionPatients() {
        return attentionPatients;
    }

    public void setAttentionPatients(List<AttentionPatientItem> attentionPatients) {
        this.attentionPatients = attentionPatients != null ? attentionPatients : new ArrayList<>();
    }

    public static class UpcomingSessionItem {
        private String sessionId;
        private String userId;
        private String patientName;
        private String avatarEmoji;
        private String formatText;
        private String timeText;
        private long startTimeMs;
        private String status;
        private String formatType;
        private String packageType;

        public UpcomingSessionItem() {
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(String patientName) {
            this.patientName = patientName;
        }

        public String getAvatarEmoji() {
            return avatarEmoji;
        }

        public void setAvatarEmoji(String avatarEmoji) {
            this.avatarEmoji = avatarEmoji;
        }

        public String getFormatText() {
            return formatText;
        }

        public void setFormatText(String formatText) {
            this.formatText = formatText;
        }

        public String getTimeText() {
            return timeText;
        }

        public void setTimeText(String timeText) {
            this.timeText = timeText;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getFormatType() {
            return formatType;
        }

        public void setFormatType(String formatType) {
            this.formatType = formatType;
        }

        public String getPackageType() {
            return packageType;
        }

        public void setPackageType(String packageType) {
            this.packageType = packageType;
        }

        public long getStartTimeMs() {
            return startTimeMs;
        }

        public void setStartTimeMs(long startTimeMs) {
            this.startTimeMs = startTimeMs;
        }
    }

    public static class AttentionPatientItem {
        private String userId;
        private String patientName;
        private String avatarEmoji;
        private String moodTag;
        private String moodEmoji;
        private String reasonText;
        private int moodPercent;
        private int energyLevel;
        private int priority;

        public AttentionPatientItem() {
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(String patientName) {
            this.patientName = patientName;
        }

        public String getAvatarEmoji() {
            return avatarEmoji;
        }

        public void setAvatarEmoji(String avatarEmoji) {
            this.avatarEmoji = avatarEmoji;
        }

        public String getMoodTag() {
            return moodTag;
        }

        public void setMoodTag(String moodTag) {
            this.moodTag = moodTag;
        }

        public String getMoodEmoji() {
            return moodEmoji;
        }

        public void setMoodEmoji(String moodEmoji) {
            this.moodEmoji = moodEmoji;
        }

        public String getReasonText() {
            return reasonText;
        }

        public void setReasonText(String reasonText) {
            this.reasonText = reasonText;
        }

        public int getMoodPercent() {
            return moodPercent;
        }

        public void setMoodPercent(int moodPercent) {
            this.moodPercent = moodPercent;
        }

        public int getEnergyLevel() {
            return energyLevel;
        }

        public void setEnergyLevel(int energyLevel) {
            this.energyLevel = energyLevel;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }
}