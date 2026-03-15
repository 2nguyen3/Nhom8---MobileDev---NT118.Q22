package com.example.heami.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.heami.network.NetService;
import com.example.heami.interfaces.AuthCallback;

public class AuthViewModel extends ViewModel {
    private NetService netService = new NetService();
    // Dùng LiveData để UI có thể quan sát (observe)
    private MutableLiveData<String> authStatus = new MutableLiveData<>();
    public LiveData<String> getAuthStatus() { return authStatus; }
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void login(String email, String pass) {
        if (email.isEmpty() || pass.isEmpty()) {
            authStatus.setValue("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        isLoading.setValue(true);
        netService.signIn(email, pass, new AuthCallback() {
            @Override
            public void onSuccess(String message) {
                authStatus.setValue("SUCCESS:" + message);
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(String error) {
                authStatus.setValue("ERROR:" + error);
                isLoading.setValue(false);
            }
        });
    }

    public void register(String email, String pass, String nickname) {
        if (email.isEmpty() || pass.isEmpty() || nickname.isEmpty()) {
            authStatus.setValue("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        isLoading.setValue(true);
        // Gọi hàm đăng ký từ NetService
        netService.signUp(email, pass, nickname, new AuthCallback() {
            @Override
            public void onSuccess(String message) {
                isLoading.setValue(false);
                authStatus.setValue("SUCCESS_REGISTER:" + message);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                authStatus.setValue("ERROR:" + error);
            }
        });
    }
}
