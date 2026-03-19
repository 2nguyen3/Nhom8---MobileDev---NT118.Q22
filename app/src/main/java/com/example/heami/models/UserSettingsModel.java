package com.example.heami.models;

import java.util.HashMap;
import java.util.Map;

public class UserSettingsModel {
    private String theme_mode;          // "LIGHT", "DARK"
    private boolean is_notif_enabled;   // Bật/tắt thông báo tổng
    private Map<String, Boolean> notif_config; // Cấu hình riêng: {"chat": true, "appointment": true, "checkin": true, "plan": true}
    private Map<String, String> reminders;     // Giờ nhắc nhở: {"breathing": "08:00", "checkin": "21:00"}

    public UserSettingsModel() {
        this.notif_config = new HashMap<>();
        this.reminders = new HashMap<>();
    }

    public UserSettingsModel(String theme_mode, boolean is_notif_enabled) {
        this.theme_mode = theme_mode;
        this.is_notif_enabled = is_notif_enabled;

        this.notif_config = new HashMap<>();
        this.notif_config.put("chat", true);
        this.notif_config.put("appointment", true);
        this.notif_config.put("checkin", true);
        this.notif_config.put("plan", true);

        this.reminders = new HashMap<>();
        this.reminders.put("breathing", "08:00");
        this.reminders.put("checkin", "09:00");
    }

    public String getTheme_mode() { return theme_mode; }
    public void setTheme_mode(String theme_mode) { this.theme_mode = theme_mode; }

    public boolean isIs_notif_enabled() { return is_notif_enabled; }
    public void setIs_notif_enabled(boolean is_notif_enabled) { this.is_notif_enabled = is_notif_enabled; }

    public Map<String, Boolean> getNotif_config() { return notif_config; }
    public void setNotif_config(Map<String, Boolean> notif_config) { this.notif_config = notif_config; }

    public Map<String, String> getReminders() { return reminders; }
    public void setReminders(Map<String, String> reminders) { this.reminders = reminders; }
}
