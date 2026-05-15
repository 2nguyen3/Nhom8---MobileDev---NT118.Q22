package com.example.heami.data.models;

public class CallTokenResponseModel {

    private boolean success;
    private String message;
    private Data data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String appId;
        private String channelName;
        private String token;
        private String uid;
        private String sessionId;
        private String callChannelId;
        private String callStatus;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getCallChannelId() {
            return callChannelId;
        }

        public void setCallChannelId(String callChannelId) {
            this.callChannelId = callChannelId;
        }

        public String getCallStatus() {
            return callStatus;
        }

        public void setCallStatus(String callStatus) {
            this.callStatus = callStatus;
        }
    }
}