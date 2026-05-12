package com.example.heami.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.heami.data.models.ChatMessageModel;
import com.example.heami.data.models.ConsultationModel;
import com.example.heami.data.repositories.ConsultationSessionRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConsultationSessionViewModel extends ViewModel {

    private final ConsultationSessionRepository repository;

    private final MutableLiveData<ConsultationModel> consultationLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> sessionModeLiveData =
            new MutableLiveData<>(ConsultationSessionRepository.MODE_CHAT);
    private final MutableLiveData<String> roomIdLiveData = new MutableLiveData<>("");
    private final MutableLiveData<List<ChatMessageModel>> messagesLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>("");
    private final MutableLiveData<String> infoMessageLiveData = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> textEnabledLiveData = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> callEnabledLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> sessionReadyLiveData = new MutableLiveData<>(false);

    private ListenerRegistration consultationRegistration;
    private ListenerRegistration messagesRegistration;

    private String currentSessionId = "";
    private String currentRoomId = "";
    private String currentSessionMode = ConsultationSessionRepository.MODE_CHAT;

    public ConsultationSessionViewModel() {
        this.repository = new ConsultationSessionRepository();
    }

    public LiveData<ConsultationModel> getConsultationLiveData() {
        return consultationLiveData;
    }

    public LiveData<String> getSessionModeLiveData() {
        return sessionModeLiveData;
    }

    public LiveData<String> getRoomIdLiveData() {
        return roomIdLiveData;
    }

    public LiveData<List<ChatMessageModel>> getMessagesLiveData() {
        return messagesLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<String> getInfoMessageLiveData() {
        return infoMessageLiveData;
    }

    public LiveData<Boolean> getTextEnabledLiveData() {
        return textEnabledLiveData;
    }

    public LiveData<Boolean> getCallEnabledLiveData() {
        return callEnabledLiveData;
    }

    public LiveData<Boolean> getSessionReadyLiveData() {
        return sessionReadyLiveData;
    }

    public void loadSession(@NonNull String sessionId) {
        currentSessionId = safeText(sessionId);
        if (currentSessionId.isEmpty()) {
            postError("Thiếu session_id của phiên tư vấn");
            return;
        }

        loadingLiveData.setValue(true);
        sessionReadyLiveData.setValue(false);
        errorMessageLiveData.setValue("");
        infoMessageLiveData.setValue("");

        repository.validateSessionEntry(
                currentSessionId,
                new ConsultationSessionRepository.LoadSessionListener() {
                    @Override
                    public void onSuccess(@NonNull ConsultationSessionRepository.SessionEntryData data) {
                        currentSessionMode = safeText(data.getSessionMode());

                        if (ConsultationSessionRepository.MODE_CHAT.equals(currentSessionMode)) {
                            loadChatSessionFlow();
                        } else {
                            loadCallSessionFlow();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        loadingLiveData.postValue(false);
                        sessionReadyLiveData.postValue(false);
                        postError(errorMessage);
                    }
                }
        );
    }

    private void loadChatSessionFlow() {
        repository.startConsultationSessionIfNeeded(
                currentSessionId,
                new ConsultationSessionRepository.LoadSessionListener() {
                    @Override
                    public void onSuccess(@NonNull ConsultationSessionRepository.SessionEntryData data) {
                        repository.ensureConsultationChatRoom(
                                currentSessionId,
                                new ConsultationSessionRepository.EnsureRoomListener() {
                                    @Override
                                    public void onSuccess(@NonNull String roomId) {
                                        currentRoomId = safeText(roomId);
                                        publishSessionState(
                                                data.getConsultation(),
                                                ConsultationSessionRepository.MODE_CHAT,
                                                currentRoomId
                                        );
                                        startConsultationRealtime();
                                        loadingLiveData.postValue(false);
                                        sessionReadyLiveData.postValue(true);
                                    }

                                    @Override
                                    public void onFailure(@NonNull String errorMessage) {
                                        loadingLiveData.postValue(false);
                                        sessionReadyLiveData.postValue(false);
                                        postError(errorMessage);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        loadingLiveData.postValue(false);
                        sessionReadyLiveData.postValue(false);
                        postError(errorMessage);
                    }
                }
        );
    }

    private void loadCallSessionFlow() {
        repository.startCallSession(
                currentSessionId,
                new ConsultationSessionRepository.LoadSessionListener() {
                    @Override
                    public void onSuccess(@NonNull ConsultationSessionRepository.SessionEntryData data) {
                        currentRoomId = "";
                        publishSessionState(
                                data.getConsultation(),
                                ConsultationSessionRepository.MODE_CALL,
                                ""
                        );
                        startConsultationRealtime();
                        loadingLiveData.postValue(false);
                        sessionReadyLiveData.postValue(true);
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        loadingLiveData.postValue(false);
                        sessionReadyLiveData.postValue(false);
                        postError(errorMessage);
                    }
                }
        );
    }

    private void publishSessionState(
            @NonNull ConsultationModel consultation,
            @NonNull String sessionMode,
            @NonNull String roomId
    ) {
        currentSessionMode = safeText(sessionMode);
        currentRoomId = safeText(roomId);

        consultationLiveData.postValue(consultation);
        sessionModeLiveData.postValue(currentSessionMode);
        roomIdLiveData.postValue(currentRoomId);

        applyControlsByModeAndStatus(
                currentSessionMode,
                safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
        );
    }

    private void startConsultationRealtime() {
        stopConsultationRealtime();

        if (currentSessionId.isEmpty()) {
            return;
        }

        consultationRegistration = repository.startConsultationRealtimeListener(
                currentSessionId,
                new ConsultationSessionRepository.ConsultationRealtimeListener() {
                    @Override
                    public void onData(
                            @NonNull ConsultationModel consultation,
                            @NonNull String sessionMode,
                            @NonNull String roomId
                    ) {
                        publishSessionState(consultation, sessionMode, roomId);
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        postError(errorMessage);
                    }
                }
        );
    }

    private void stopConsultationRealtime() {
        if (consultationRegistration != null) {
            consultationRegistration.remove();
            consultationRegistration = null;
        }
    }

    public void connectChatRealtimeIfNeeded() {
        if (!ConsultationSessionRepository.MODE_CHAT.equals(currentSessionMode)) {
            return;
        }

        if (currentRoomId.isEmpty()) {
            return;
        }

        stopChatRealtime();

        messagesRegistration = repository.startConsultationMessageListener(
                currentRoomId,
                new ConsultationSessionRepository.LoadMessagesListener() {
                    @Override
                    public void onSuccess(@NonNull List<ChatMessageModel> messages) {
                        messagesLiveData.postValue(messages);
                        repository.markMessagesSeen(currentRoomId, messages);
                        repository.resetMyUnreadCount(
                                currentRoomId,
                                new ConsultationSessionRepository.SimpleActionListener() {
                                    @Override
                                    public void onSuccess() { }

                                    @Override
                                    public void onFailure(@NonNull String errorMessage) { }
                                }
                        );
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        postError(errorMessage);
                    }
                }
        );
    }

    public void stopChatRealtime() {
        if (messagesRegistration != null) {
            messagesRegistration.remove();
            messagesRegistration = null;
        }
    }

    public void sendMessage(@NonNull String text) {
        Boolean textEnabled = textEnabledLiveData.getValue();
        if (!Boolean.TRUE.equals(textEnabled)) {
            postError(resolveTextLockedMessage());
            return;
        }

        if (!ConsultationSessionRepository.MODE_CHAT.equals(currentSessionMode)) {
            postError("Gói này không hỗ trợ nhắn tin");
            return;
        }

        if (currentRoomId.isEmpty()) {
            postError("Phòng chat chưa sẵn sàng");
            return;
        }

        repository.sendConsultationMessage(
                currentRoomId,
                text,
                new ConsultationSessionRepository.SendMessageListener() {
                    @Override
                    public void onSuccess() {
                        infoMessageLiveData.postValue("Đã gửi tin nhắn");
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        postError(errorMessage);
                    }
                }
        );
    }

    public void onCallClicked() {
        Boolean callEnabled = callEnabledLiveData.getValue();
        if (!Boolean.TRUE.equals(callEnabled)) {
            postError(resolveCallLockedMessage());
            return;
        }

        infoMessageLiveData.postValue("Sẵn sàng mở giao diện gọi video.");
    }

    public void endSession() {
        ConsultationModel consultation = consultationLiveData.getValue();
        if (consultation == null) {
            postError("Không tìm thấy dữ liệu phiên tư vấn");
            return;
        }

        String status = safeText(consultation.getStatus()).toUpperCase(Locale.ROOT);
        if ("COMPLETED".equals(status)) {
            postError("Phiên tư vấn này đã hoàn thành");
            return;
        }

        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
            postError("Phiên tư vấn này đã bị hủy");
            return;
        }

        repository.endConsultationSession(
                currentSessionId,
                currentRoomId,
                new ConsultationSessionRepository.SimpleActionListener() {
                    @Override
                    public void onSuccess() {
                        infoMessageLiveData.postValue("Đã kết thúc phiên tư vấn");
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        postError(errorMessage);
                    }
                }
        );
    }

    private void applyControlsByModeAndStatus(
            @NonNull String sessionMode,
            @NonNull String status
    ) {
        boolean isClosed =
                "COMPLETED".equals(status)
                        || "CANCELLED".equals(status)
                        || "CANCELED".equals(status);

        if (ConsultationSessionRepository.MODE_CALL.equals(sessionMode)) {
            textEnabledLiveData.postValue(false);
            callEnabledLiveData.postValue(!isClosed);
            return;
        }

        boolean canChat =
                !isClosed
                        && ("BOOKED".equals(status) || "ONGOING".equals(status))
                        && !currentRoomId.isEmpty();

        textEnabledLiveData.postValue(canChat);
        callEnabledLiveData.postValue(false);
    }

    @NonNull
    private String resolveTextLockedMessage() {
        ConsultationModel consultation = consultationLiveData.getValue();
        String status = consultation != null
                ? safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
                : "";

        if ("COMPLETED".equals(status)) {
            return "Phiên tư vấn này đã hoàn thành";
        }

        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
            return "Phiên tư vấn này đã bị hủy";
        }

        if (ConsultationSessionRepository.MODE_CALL.equals(currentSessionMode)) {
            return "Gói này không hỗ trợ nhắn tin";
        }

        if (currentRoomId.isEmpty()) {
            return "Phòng chat chưa sẵn sàng";
        }

        return "Bạn chưa thể nhắn tin ở phiên này";
    }

    @NonNull
    private String resolveCallLockedMessage() {
        ConsultationModel consultation = consultationLiveData.getValue();
        String status = consultation != null
                ? safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
                : "";

        if ("COMPLETED".equals(status)) {
            return "Phiên tư vấn này đã hoàn thành";
        }

        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
            return "Phiên tư vấn này đã bị hủy";
        }

        if (ConsultationSessionRepository.MODE_CHAT.equals(currentSessionMode)) {
            return "Gói này chỉ hỗ trợ chat";
        }

        return "Bạn chưa thể bắt đầu cuộc gọi ở phiên này";
    }

    private void postError(@NonNull String message) {
        errorMessageLiveData.postValue(message);
    }

    @NonNull
    private String safeText(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    protected void onCleared() {
        stopConsultationRealtime();
        stopChatRealtime();
        super.onCleared();
    }
}