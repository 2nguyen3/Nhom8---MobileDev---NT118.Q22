package com.example.heami.network;

import com.google.firebase.auth.FirebaseAuth;
import com.example.heami.interfaces.AuthCallback;
import com.google.firebase.auth.UserProfileChangeRequest;

public class NetService {
    private FirebaseAuth mAuth;

    public NetService() {
        mAuth = FirebaseAuth.getInstance();
    }

    // Hàm Đăng ký tài khoản
    public void signUp(String email, String password, String nickname, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sau khi tạo user xong, tiến hành cập nhật Nickname vào Profile
                        if (mAuth.getCurrentUser() != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(nickname)
                                    .build();

                            mAuth.getCurrentUser().updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        callback.onSuccess("Đăng ký thành công, chào " + nickname + "! ✨");
                                    });
                        }
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Hàm Đăng nhập
    public void signIn(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Đăng nhập thành công!");
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }
}

