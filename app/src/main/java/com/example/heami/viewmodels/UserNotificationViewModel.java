package com.example.heami.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.heami.data.models.HomeNotificationModel;
import com.example.heami.data.repositories.UserNotificationRepository;

import java.util.ArrayList;
import java.util.List;

public class UserNotificationViewModel extends AndroidViewModel {

    private final UserNotificationRepository repository = new UserNotificationRepository();

    private final MutableLiveData<List<HomeNotificationModel>> notifications =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>("");

    public UserNotificationViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<HomeNotificationModel>> getNotifications() {
        return notifications;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void refresh() {
        loading.setValue(true);
        errorMessage.setValue("");

        repository.loadHomeNotifications(
                getApplication(),
                new UserNotificationRepository.LoadNotificationsListener() {
                    @Override
                    public void onSuccess(@NonNull List<HomeNotificationModel> data) {
                        loading.setValue(false);
                        notifications.setValue(data);
                    }

                    @Override
                    public void onFailure(@NonNull String error) {
                        loading.setValue(false);
                        errorMessage.setValue(error);
                        notifications.setValue(new ArrayList<>());
                    }
                }
        );
    }
}