package com.example.heami.data.models;

public class PresenceCountModel {

    private int onlineUsers;
    private int onlineDoctors;
    private int onlineAdmins;
    private int onlineTotal;

    public PresenceCountModel() {
    }

    public PresenceCountModel(
            int onlineUsers,
            int onlineDoctors,
            int onlineAdmins,
            int onlineTotal
    ) {
        this.onlineUsers = onlineUsers;
        this.onlineDoctors = onlineDoctors;
        this.onlineAdmins = onlineAdmins;
        this.onlineTotal = onlineTotal;
    }

    public static PresenceCountModel empty() {
        return new PresenceCountModel(0, 0, 0, 0);
    }

    public int getOnlineUsers() {
        return onlineUsers;
    }

    public void setOnlineUsers(int onlineUsers) {
        this.onlineUsers = onlineUsers;
    }

    public int getOnlineDoctors() {
        return onlineDoctors;
    }

    public void setOnlineDoctors(int onlineDoctors) {
        this.onlineDoctors = onlineDoctors;
    }

    public int getOnlineAdmins() {
        return onlineAdmins;
    }

    public void setOnlineAdmins(int onlineAdmins) {
        this.onlineAdmins = onlineAdmins;
    }

    public int getOnlineTotal() {
        return onlineTotal;
    }

    public void setOnlineTotal(int onlineTotal) {
        this.onlineTotal = onlineTotal;
    }
}