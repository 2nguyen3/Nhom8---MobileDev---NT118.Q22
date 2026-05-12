package com.example.heami.data.models;

public class UserSettingsModel {
    private String theme_mode;
    private boolean notif_checkin;
    private boolean notif_plan;
    private boolean notif_appoint;
    private boolean notif_chat;
    private boolean is_protected_mode;

    public UserSettingsModel() {
        this.theme_mode = "LIGHT";
        this.notif_checkin = true;
        this.notif_plan = true;
        this.notif_appoint = true;
        this.notif_chat = true;
        this.is_protected_mode = false;
    }

    public UserSettingsModel(String theme_mode, boolean defaultNotifVal) {
        this.theme_mode = theme_mode;
        this.notif_checkin = defaultNotifVal;
        this.notif_plan = defaultNotifVal;
        this.notif_appoint = defaultNotifVal;
        this.notif_chat = defaultNotifVal;
        this.is_protected_mode = false;
    }

    public UserSettingsModel(String theme_mode, boolean notif_checkin, boolean notif_plan, boolean notif_appoint, boolean notif_chat, boolean is_protected_mode) {
        this.theme_mode = theme_mode;
        this.notif_checkin = notif_checkin;
        this.notif_plan = notif_plan;
        this.notif_appoint = notif_appoint;
        this.notif_chat = notif_chat;
        this.is_protected_mode = is_protected_mode;
    }

    public String getTheme_mode() { return theme_mode; }
    public void setTheme_mode(String theme_mode) { this.theme_mode = theme_mode; }

    public boolean isNotif_checkin() { return notif_checkin; }
    public void setNotif_checkin(boolean notif_checkin) { this.notif_checkin = notif_checkin; }

    public boolean isNotif_plan() { return notif_plan; }
    public void setNotif_plan(boolean notif_plan) { this.notif_plan = notif_plan; }

    public boolean isNotif_appoint() { return notif_appoint; }
    public void setNotif_appoint(boolean notif_appoint) { this.notif_appoint = notif_appoint; }

    public boolean isNotif_chat() { return notif_chat; }
    public void setNotif_chat(boolean notif_chat) { this.notif_chat = notif_chat; }

    public boolean isIs_protected_mode() { return is_protected_mode; }
    public void setIs_protected_mode(boolean is_protected_mode) { this.is_protected_mode = is_protected_mode; }
}
