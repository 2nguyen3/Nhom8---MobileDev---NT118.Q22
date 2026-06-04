package com.example.heami.ui.doctor;

import android.content.Intent;
import com.example.heami.ui.consultation.ConsultationCallActivity;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ChatMessageModel;
import com.example.heami.data.models.ConsultationModel;
import com.example.heami.data.repositories.DoctorConsultationSessionRepository;
import com.example.heami.ui.consultation.ConsultationMessageAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoctorChatDetailActivity extends AppCompatActivity {

    private ImageButton btnChatDetailBack;
    private ImageButton btnChatDetailCall;
    private ImageButton btnChatDetailSend;

    private TextView txtChatDetailAvatarEmoji;
    private TextView txtChatDetailName;
    private TextView txtChatDetailSubtitle;
    private TextView txtChatDetailModeBadge;
    private TextView txtChatDetailBannerText;
    private TextView txtChatDetailEndSession;
    private TextView txtChatDetailEmptyTitle;
    private TextView txtChatDetailEmptySubtitle;

    private EditText edtChatDetailInput;

    private RecyclerView rvChatDetailMessages;
    private View layoutChatDetailEmptyState;
    private ProgressBar progressChatDetail;

    private final DoctorConsultationSessionRepository repository =
            new DoctorConsultationSessionRepository();

    private ListenerRegistration consultationListener;
    private ListenerRegistration messageListener;
    private ConsultationMessageAdapter messageAdapter;

    private String listeningRoomId = "";

    private String sessionId = "";
    private String roomId = "";
    private String partnerName = "";
    private String partnerAvatar = "";
    private String roomStatus = "";
    private String formatType = "";
    private String partnerUserId = "";

    private String doctorActorId = "";
    private String sessionMode = DoctorConsultationSessionRepository.MODE_CHAT;

    private ConsultationModel consultation;
    private boolean textEnabled = false;
    private boolean callEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_chat_detail);

        resolveDoctorActorId();
        readIntentData();
        bindViews();
        setupRecyclerView();
        setupActions();
        bindStaticFallbackInfo();
        startConsultationRealtime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopConsultationListener();
        stopMessageListener();
    }

    private void resolveDoctorActorId() {
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);

        if (isDoctor) {
            doctorActorId = safeText(prefs.getString("doctor_id", ""));

            if (doctorActorId.isEmpty()) {
                doctorActorId = "doc_001";
            }

            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorActorId = safeText(FirebaseAuth.getInstance().getCurrentUser().getUid());
        } else {
            doctorActorId = "";
        }
    }

    private void readIntentData() {
        if (getIntent() == null) return;

        sessionId = safeText(getIntent().getStringExtra(DoctorMessagesActivity.EXTRA_SESSION_ID));
        roomId = safeText(getIntent().getStringExtra(DoctorMessagesActivity.EXTRA_ROOM_ID));
        partnerName = safeText(getIntent().getStringExtra(DoctorMessagesActivity.EXTRA_PARTNER_NAME));
        partnerAvatar = safeText(getIntent().getStringExtra(DoctorMessagesActivity.EXTRA_PARTNER_AVATAR));
        roomStatus = safeText(getIntent().getStringExtra(DoctorMessagesActivity.EXTRA_ROOM_STATUS));
        formatType = safeText(getIntent().getStringExtra(DoctorMessagesActivity.EXTRA_FORMAT_TYPE));
        partnerUserId = safeText(getIntent().getStringExtra(DoctorMessagesActivity.EXTRA_USER_ID));
    }

    private void bindViews() {
        btnChatDetailBack = findViewById(R.id.btnChatDetailBack);
        btnChatDetailCall = findViewById(R.id.btnChatDetailCall);
        btnChatDetailSend = findViewById(R.id.btnChatDetailSend);

        txtChatDetailAvatarEmoji = findViewById(R.id.txtChatDetailAvatarEmoji);
        txtChatDetailName = findViewById(R.id.txtChatDetailName);
        txtChatDetailSubtitle = findViewById(R.id.txtChatDetailSubtitle);
        txtChatDetailModeBadge = findViewById(R.id.txtChatDetailModeBadge);
        txtChatDetailBannerText = findViewById(R.id.txtChatDetailBannerText);
        txtChatDetailEndSession = findViewById(R.id.txtChatDetailEndSession);
        txtChatDetailEmptyTitle = findViewById(R.id.txtChatDetailEmptyTitle);
        txtChatDetailEmptySubtitle = findViewById(R.id.txtChatDetailEmptySubtitle);

        edtChatDetailInput = findViewById(R.id.edtChatDetailInput);

        rvChatDetailMessages = findViewById(R.id.rvChatDetailMessages);
        layoutChatDetailEmptyState = findViewById(R.id.layoutChatDetailEmptyState);
        progressChatDetail = findViewById(R.id.progressChatDetail);
    }

    private void setupRecyclerView() {
        rvChatDetailMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new ConsultationMessageAdapter(doctorActorId);
        messageAdapter.setDoctorAvatarBadge(resolvePartnerBadge());
        rvChatDetailMessages.setAdapter(messageAdapter);
    }

    private void setupActions() {
        btnChatDetailBack.setOnClickListener(v -> finish());

        btnChatDetailCall.setOnClickListener(v -> {
            if (!callEnabled) {
                Toast.makeText(this, resolveCallDisabledMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            openConsultationCallScreen();
        });

        txtChatDetailEndSession.setOnClickListener(v -> {
            if (!canEndSession()) {
                Toast.makeText(this, resolveEndSessionDisabledMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading(true);

            repository.endConsultationSession(
                    sessionId,
                    roomId,
                    doctorActorId,
                    new DoctorConsultationSessionRepository.SimpleActionListener() {
                        @Override
                        public void onSuccess() {
                            showLoading(false);
                            Toast.makeText(
                                    DoctorChatDetailActivity.this,
                                    "Đã kết thúc phiên tư vấn",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onFailure(@NonNull String errorMessage) {
                            showLoading(false);
                            Toast.makeText(DoctorChatDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        btnChatDetailSend.setOnClickListener(v -> {
            if (!textEnabled) {
                Toast.makeText(this, resolveTextDisabledMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            String content = edtChatDetailInput.getText().toString().trim();
            if (content.isEmpty()) {
                return;
            }

            if (roomId.isEmpty()) {
                Toast.makeText(this, "Phòng chat chưa sẵn sàng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (doctorActorId.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy định danh bác sĩ", Toast.LENGTH_SHORT).show();
                return;
            }

            repository.sendConsultationMessageAsActor(
                    roomId,
                    doctorActorId,
                    content,
                    new DoctorConsultationSessionRepository.SendMessageListener() {
                        @Override
                        public void onSuccess() {
                            edtChatDetailInput.setText("");
                            refreshSendButtonState();
                        }

                        @Override
                        public void onFailure(@NonNull String errorMessage) {
                            Toast.makeText(DoctorChatDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        edtChatDetailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshSendButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void openConsultationCallScreen() {
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Thiếu session_id để bắt đầu cuộc gọi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (doctorActorId.isEmpty()) {
            resolveDoctorActorId();
        }

        if (doctorActorId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy định danh bác sĩ", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ConsultationCallActivity.class);
        intent.putExtra(ConsultationCallActivity.EXTRA_SESSION_ID, sessionId);
        intent.putExtra(ConsultationCallActivity.EXTRA_ROLE, ConsultationCallActivity.ROLE_DOCTOR);
        intent.putExtra(ConsultationCallActivity.EXTRA_ACTOR_UID, doctorActorId);

        intent.putExtra(
                ConsultationCallActivity.EXTRA_PARTNER_NAME,
                partnerName.isEmpty() ? "Người dùng Heami" : partnerName
        );

        intent.putExtra(
                ConsultationCallActivity.EXTRA_PARTNER_AVATAR,
                partnerAvatar
        );

        intent.putExtra(
                ConsultationCallActivity.EXTRA_FORMAT_TYPE,
                formatType.isEmpty() ? "CALL" : formatType
        );

        intent.putExtra(
                ConsultationCallActivity.EXTRA_CALL_CHANNEL_ID,
                ""
        );

        startActivity(intent);
    }

    private void bindStaticFallbackInfo() {
        txtChatDetailName.setText(partnerName.isEmpty() ? "Người dùng Heami" : partnerName);
        txtChatDetailAvatarEmoji.setText(resolvePartnerBadge());

        if (isCallFormat(formatType)) {
            sessionMode = DoctorConsultationSessionRepository.MODE_CALL;
        } else {
            sessionMode = DoctorConsultationSessionRepository.MODE_CHAT;
        }

        applyConsultationUiState();
    }

    private void startConsultationRealtime() {
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Thiếu session_id của phiên tư vấn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);
        stopConsultationListener();

        consultationListener = repository.startConsultationRealtimeListener(
                sessionId,
                new DoctorConsultationSessionRepository.ConsultationRealtimeListener() {
                    @Override
                    public void onData(@NonNull ConsultationModel loadedConsultation,
                                       @NonNull String loadedMode,
                                       @NonNull String loadedRoomId) {
                        showLoading(false);
                        handleConsultationChanged(loadedConsultation, loadedMode, loadedRoomId);
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        showLoading(false);
                        Toast.makeText(DoctorChatDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );
    }

    private void handleConsultationChanged(@NonNull ConsultationModel loadedConsultation,
                                           @NonNull String loadedMode,
                                           @NonNull String loadedRoomId) {
        consultation = loadedConsultation;
        sessionMode = safeText(loadedMode);
        formatType = safeText(loadedConsultation.getFormatType());
        roomStatus = safeText(loadedConsultation.getStatus());

        if (partnerUserId.isEmpty()) {
            partnerUserId = safeText(loadedConsultation.getUserId());
        }

        String updatedRoomId = safeText(loadedRoomId);
        boolean roomChanged = !updatedRoomId.equals(roomId);
        roomId = updatedRoomId;

        applyConsultationUiState();

        if (DoctorConsultationSessionRepository.MODE_CHAT.equals(sessionMode) && !roomId.isEmpty()) {
            connectMessageListenerIfNeeded(roomChanged);
        } else {
            stopMessageListener();
            bindMessages(new ArrayList<>());
        }
    }

    private void connectMessageListenerIfNeeded(boolean roomChanged) {
        if (!roomChanged
                && !roomId.isEmpty()
                && roomId.equals(listeningRoomId)
                && messageListener != null) {
            return;
        }

        stopMessageListener();

        if (roomId.isEmpty()) {
            bindMessages(new ArrayList<>());
            return;
        }

        listeningRoomId = roomId;

        messageListener = repository.startConsultationMessageListener(
                roomId,
                new DoctorConsultationSessionRepository.LoadMessagesListener() {
                    @Override
                    public void onSuccess(@NonNull List<ChatMessageModel> messages) {
                        bindMessages(messages);

                        repository.markMessagesSeen(roomId, doctorActorId, messages);
                        repository.resetUnreadCount(roomId, doctorActorId,
                                new DoctorConsultationSessionRepository.SimpleActionListener() {
                                    @Override
                                    public void onSuccess() { }

                                    @Override
                                    public void onFailure(@NonNull String errorMessage) { }
                                });
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        Toast.makeText(DoctorChatDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void stopConsultationListener() {
        if (consultationListener != null) {
            consultationListener.remove();
            consultationListener = null;
        }
    }

    private void stopMessageListener() {
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
        listeningRoomId = "";
    }

    private void bindMessages(@NonNull List<ChatMessageModel> messages) {
        messageAdapter.submitList(messages);

        boolean showEmpty = messages.isEmpty();
        layoutChatDetailEmptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        rvChatDetailMessages.setVisibility(showEmpty ? View.INVISIBLE : View.VISIBLE);

        if (!messages.isEmpty()) {
            rvChatDetailMessages.post(() ->
                    rvChatDetailMessages.scrollToPosition(messages.size() - 1)
            );
        }
    }

    private void applyConsultationUiState() {
        String status = consultation != null
                ? safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
                : safeText(roomStatus).toUpperCase(Locale.ROOT);

        boolean isCall = DoctorConsultationSessionRepository.MODE_CALL.equals(sessionMode);
        boolean isClosed = isClosedStatus(status);
        boolean isActiveWindow = isConsultationInteractive(status);

        txtChatDetailName.setText(partnerName.isEmpty() ? "Người dùng Heami" : partnerName);
        txtChatDetailAvatarEmoji.setText(resolvePartnerBadge());

        if (isCall) {
            txtChatDetailModeBadge.setText("Gọi video");
            txtChatDetailSubtitle.setText(resolveCallSubtitle(status));
            txtChatDetailBannerText.setText(resolveCallBanner(status));
            txtChatDetailEmptyTitle.setText("Phiên gọi tư vấn");
            txtChatDetailEmptySubtitle.setText("Gói này chỉ hỗ trợ gọi video. Bác sĩ có thể dùng nút gọi ở góc trên.");

            textEnabled = false;
            callEnabled = !isClosed && isActiveWindow;
        } else {
            txtChatDetailModeBadge.setText("Chat tư vấn");
            txtChatDetailSubtitle.setText(resolveChatSubtitle(status));
            txtChatDetailBannerText.setText(resolveChatBanner(status));

            if (roomId.isEmpty()) {
                txtChatDetailEmptyTitle.setText("Chờ người dùng bắt đầu chat");
                txtChatDetailEmptySubtitle.setText("Phòng chat sẽ xuất hiện khi người dùng vào phiên chat tư vấn.");
            } else {
                txtChatDetailEmptyTitle.setText("Chưa có tin nhắn");
                txtChatDetailEmptySubtitle.setText("Khi bác sĩ hoặc người dùng nhắn tin, nội dung sẽ hiện ở đây.");
            }

            textEnabled = !isClosed && isActiveWindow && !roomId.isEmpty();
            callEnabled = false;
        }

        applyModeBadgeStyle(isCall, isClosed);
        applyBannerStyle(isCall, isClosed);

        boolean showEndButton = canEndSession();
        txtChatDetailEndSession.setVisibility(showEndButton ? View.VISIBLE : View.GONE);

        refreshInputState();
        refreshCallButtonState();
        refreshSendButtonState();
    }

    private boolean canEndSession() {
        String status = consultation != null
                ? safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
                : safeText(roomStatus).toUpperCase(Locale.ROOT);

        return "ONGOING".equals(status);
    }

    @NonNull
    private String resolveEndSessionDisabledMessage() {
        String status = consultation != null
                ? safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
                : safeText(roomStatus).toUpperCase(Locale.ROOT);

        if ("BOOKED".equals(status)) {
            return "Phiên tư vấn này chưa bắt đầu";
        }

        if ("COMPLETED".equals(status)) {
            return "Phiên tư vấn này đã hoàn thành";
        }

        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
            return "Phiên tư vấn này đã bị hủy";
        }

        return "Bạn chưa thể kết thúc phiên này";
    }

    private boolean isClosedStatus(@NonNull String status) {
        return "COMPLETED".equals(status)
                || "CANCELLED".equals(status)
                || "CANCELED".equals(status);
    }

    private boolean isConsultationInteractive(@NonNull String status) {
        return "BOOKED".equals(status) || "ONGOING".equals(status);
    }

    private void applyModeBadgeStyle(boolean isCall, boolean isClosed) {
        if (isClosed) {
            txtChatDetailModeBadge.setTextColor(Color.parseColor("#8A94A6"));
            txtChatDetailModeBadge.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#EEF1F5"))
            );
            return;
        }

        if (isCall) {
            txtChatDetailModeBadge.setTextColor(Color.parseColor("#09A38C"));
            txtChatDetailModeBadge.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#E8FBF7"))
            );
        } else {
            txtChatDetailModeBadge.setTextColor(Color.parseColor("#7B61FF"));
            txtChatDetailModeBadge.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#F1EDFF"))
            );
        }
    }

    private void applyBannerStyle(boolean isCall, boolean isClosed) {
        if (isClosed) {
            View banner = findViewById(R.id.layoutSessionBanner);
            if (banner != null) {
                banner.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#F3F4F6"))
                );
            }
            txtChatDetailBannerText.setTextColor(Color.parseColor("#8A94A6"));
            return;
        }

        View banner = findViewById(R.id.layoutSessionBanner);
        if (banner != null) {
            banner.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor(isCall ? "#E8FBF7" : "#F5F2FF")
                    )
            );
        }

        txtChatDetailBannerText.setTextColor(
                Color.parseColor(isCall ? "#09A38C" : "#7B61FF")
        );
    }

    private void refreshInputState() {
        edtChatDetailInput.setEnabled(textEnabled);
        edtChatDetailInput.setFocusable(textEnabled);
        edtChatDetailInput.setFocusableInTouchMode(textEnabled);
        edtChatDetailInput.setClickable(textEnabled);
        edtChatDetailInput.setAlpha(textEnabled ? 1f : 0.65f);
        edtChatDetailInput.setHint(textEnabled ? "Nhập tin nhắn..." : resolveInputHint());
    }

    private void refreshCallButtonState() {
        btnChatDetailCall.setEnabled(callEnabled);
        btnChatDetailCall.setAlpha(callEnabled ? 1f : 0.45f);
        btnChatDetailCall.setImageTintList(
                ColorStateList.valueOf(Color.parseColor(callEnabled ? "#09A38C" : "#B5BFD1"))
        );
    }

    private void refreshSendButtonState() {
        boolean hasText = !TextUtils.isEmpty(edtChatDetailInput.getText().toString().trim());
        boolean enabled = textEnabled && hasText;

        btnChatDetailSend.setEnabled(enabled);
        btnChatDetailSend.setAlpha(enabled ? 1f : 0.45f);
        btnChatDetailSend.setImageTintList(
                ColorStateList.valueOf(Color.parseColor(enabled ? "#09A38C" : "#B5BFD1"))
        );
    }

    private void showLoading(boolean loading) {
        progressChatDetail.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private String resolveChatSubtitle(@NonNull String status) {
        if ("ONGOING".equals(status)) return "🟢 Đang tư vấn qua chat";
        if ("COMPLETED".equals(status)) return "✅ Phiên chat đã hoàn thành";
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "⛔ Phiên chat đã bị hủy";
        if (roomId.isEmpty()) return "🟡 Chờ người dùng bắt đầu chat";
        return "🟡 Sẵn sàng tư vấn qua chat";
    }

    @NonNull
    private String resolveCallSubtitle(@NonNull String status) {
        if ("ONGOING".equals(status)) return "🟢 Đang trong phiên gọi";
        if ("COMPLETED".equals(status)) return "✅ Phiên gọi đã hoàn thành";
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "⛔ Phiên gọi đã bị hủy";
        return "🟡 Chờ bắt đầu phiên gọi";
    }

    @NonNull
    private String resolveChatBanner(@NonNull String status) {
        if ("COMPLETED".equals(status)) return "Phiên chat này đã hoàn thành";
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "Phiên chat này đã bị hủy";
        if (roomId.isEmpty()) return "Người dùng chưa tạo phòng chat cho phiên này";
        return "Phòng chat consultation đang hoạt động";
    }

    @NonNull
    private String resolveCallBanner(@NonNull String status) {
        if ("COMPLETED".equals(status)) return "Phiên gọi này đã hoàn thành";
        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "Phiên gọi này đã bị hủy";
        return "Phiên gọi consultation sẵn sàng bắt đầu";
    }

    @NonNull
    private String resolveInputHint() {
        String status = consultation != null
                ? safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
                : safeText(roomStatus).toUpperCase(Locale.ROOT);

        if ("COMPLETED".equals(status)) {
            return "Phiên tư vấn này đã hoàn thành";
        }

        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
            return "Phiên tư vấn này đã bị hủy";
        }

        if (DoctorConsultationSessionRepository.MODE_CALL.equals(sessionMode)) {
            return "Gói này không hỗ trợ nhắn tin";
        }

        if (roomId.isEmpty()) {
            return "Chờ người dùng bắt đầu chat";
        }

        if (!isConsultationInteractive(status)) {
            return "Bạn chưa thể nhắn tin ở phiên này";
        }

        return "Nhập tin nhắn...";
    }

    @NonNull
    private String resolveTextDisabledMessage() {
        String status = consultation != null
                ? safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
                : safeText(roomStatus).toUpperCase(Locale.ROOT);

        if ("COMPLETED".equals(status)) {
            return "Phiên tư vấn này đã hoàn thành";
        }

        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
            return "Phiên tư vấn này đã bị hủy";
        }

        if (DoctorConsultationSessionRepository.MODE_CALL.equals(sessionMode)) {
            return "Gói này không hỗ trợ nhắn tin";
        }

        if (roomId.isEmpty()) {
            return "Người dùng chưa bắt đầu phiên chat này";
        }

        return "Bác sĩ chưa thể nhắn tin ở phiên này";
    }

    @NonNull
    private String resolveCallDisabledMessage() {
        String status = consultation != null
                ? safeText(consultation.getStatus()).toUpperCase(Locale.ROOT)
                : safeText(roomStatus).toUpperCase(Locale.ROOT);

        if ("COMPLETED".equals(status)) {
            return "Phiên tư vấn này đã hoàn thành";
        }

        if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
            return "Phiên tư vấn này đã bị hủy";
        }

        if (DoctorConsultationSessionRepository.MODE_CHAT.equals(sessionMode)) {
            return "Gói này chỉ hỗ trợ chat, không hỗ trợ gọi video";
        }

        return "Bác sĩ chưa thể bắt đầu gọi ở phiên này";
    }

    @NonNull
    private String resolvePartnerBadge() {
        String avatar = safeText(partnerAvatar);

        if (!avatar.isEmpty()
                && !avatar.startsWith("http://")
                && !avatar.startsWith("https://")
                && avatar.length() <= 3) {
            return avatar;
        }

        String display = safeText(partnerName);
        if (display.isEmpty()) {
            return "👤";
        }

        return display.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private boolean isCallFormat(@NonNull String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        return normalized.contains("call")
                || normalized.contains("video")
                || normalized.contains("gọi");
    }

    @NonNull
    private String safeText(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}