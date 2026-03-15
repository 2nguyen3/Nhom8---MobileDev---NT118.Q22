package com.example.heami.interfaces;

public interface AuthCallback {
    void onSuccess(String message);
    void onFailure(String error);
}
