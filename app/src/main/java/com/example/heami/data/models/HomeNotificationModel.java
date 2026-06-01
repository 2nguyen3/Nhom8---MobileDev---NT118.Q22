package com.example.heami.data.models;

public class HomeNotificationModel {

    public static final String TYPE_CHECKIN = "CHECKIN";
    public static final String TYPE_PLAN = "PLAN";
    public static final String TYPE_APPOINTMENT = "APPOINTMENT";
    public static final String TYPE_COMMUNITY_CHAT = "COMMUNITY_CHAT";
    public static final String TYPE_CONSULTATION_CHAT = "CONSULTATION_CHAT";

    private String notificationId;
    private String type;
    private String title;
    private String message;
    private String timeText;
    private String actionText;
    private String iconEmoji;
    private long createdAtMs;
    private boolean unread;

    private String roomId;
    private String matchId;
    private String sessionId;
    private String partnerId;
    private String partnerName;
    private String partnerAvatar;
    private String moodTag;
    private String formatType;
    private String status;

    public HomeNotificationModel() {
    }

    public HomeNotificationModel(
            String notificationId,
            String type,
            String title,
            String message,
            String timeText,
            String actionText,
            String iconEmoji,
            long createdAtMs,
            boolean unread
    ) {
        this.notificationId = notificationId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.timeText = timeText;
        this.actionText = actionText;
        this.iconEmoji = iconEmoji;
        this.createdAtMs = createdAtMs;
        this.unread = unread;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeText() {
        return timeText;
    }

    public String getActionText() {
        return actionText;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }

    public boolean isUnread() {
        return unread;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getPartnerAvatar() {
        return partnerAvatar;
    }

    public void setPartnerAvatar(String partnerAvatar) {
        this.partnerAvatar = partnerAvatar;
    }

    public String getMoodTag() {
        return moodTag;
    }

    public void setMoodTag(String moodTag) {
        this.moodTag = moodTag;
    }

    public String getFormatType() {
        return formatType;
    }

    public void setFormatType(String formatType) {
        this.formatType = formatType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}