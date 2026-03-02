package com.example.heami.models;

import java.util.Map;

public class UserSettingsModel {
    private String setting_id;
    private String user_id;
    private String theme_mode;        // "LIGHT", "DARK"
    private boolean is_notif_enabled;
    private Map<String, Boolean> notif_config; // Cấu hình chi tiết (Chat, Lịch hẹn...)
    private Map<String, String> reminders;     // Giờ nhắc nhở (Tập thở, Check-in...)

    public UserSettingsModel() {}

    // Getter và Setter
    public String getSetting_id() { return setting_id; }
    public void setSetting_id(String setting_id) { this.setting_id = setting_id; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public String getTheme_mode() { return theme_mode; }
    public void setTheme_mode(String theme_mode) { this.theme_mode = theme_mode; }

    public boolean isIs_notif_enabled() { return is_notif_enabled; }
    public void setIs_notif_enabled(boolean is_notif_enabled) { this.is_notif_enabled = is_notif_enabled; }

    public Map<String, Boolean> getNotif_config() { return notif_config; }
    public void setNotif_config(Map<String, Boolean> notif_config) { this.notif_config = notif_config; }

    public Map<String, String> getReminders() { return reminders; }
    public void setReminders(Map<String, String> reminders) { this.reminders = reminders; }
}
