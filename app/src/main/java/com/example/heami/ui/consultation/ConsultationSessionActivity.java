package com.example.heami.ui.consultation;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ChatMessageModel;
import com.example.heami.data.models.ConsultationModel;
import com.example.heami.data.repositories.ConsultationSessionRepository;
import com.example.heami.viewmodels.ConsultationSessionViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ConsultationSessionActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "extra_session_id";
    public static final String EXTRA_FORMAT_TYPE = "extra_format_type";
    public static final String EXTRA_DOCTOR_NAME = "extra_doctor_name";
    public static final String EXTRA_DOCTOR_AVATAR = "extra_doctor_avatar";
    public static final String EXTRA_STATUS = "extra_status";

    public static final String MODE_CHAT = "CHAT";
    public static final String MODE_CALL = "CALL";

    private ImageButton btnSessionBack;
    private ImageButton btnSessionCall;
    private ImageButton btnSessionSend;

    private TextView txtSessionDoctorName;
    private TextView txtSessionSubtitle;
    private TextView txtSessionBadge;
    private TextView txtSessionAvatarEmoji;
    private TextView txtSessionEmptyTitle;
    private TextView txtSessionEmptyDesc;

    private EditText edtSessionInput;

    private RecyclerView rvSessionMessages;
    private View layoutSessionEmptyState;

    private String sessionId = "";
    private String sessionMode = MODE_CHAT;
    private String doctorName = "";
    private String doctorAvatar = "";
    private String consultationStatus = "";

    private ConsultationSessionViewModel viewModel;
    private ConsultationMessageAdapter messageAdapter;

    private boolean isTextInputAllowed = true;
    private boolean isCallActionAllowed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_session);

        initViews();
        readIntentData();
        initViewModel();
        setupRecyclerView();
        bindStaticSessionInfo();
        applyModeUi(sessionMode);
        refreshTextInputState();
        refreshCallButtonState();
        refreshSendButtonState();
        setupActions();
        observeViewModel();

        if (!sessionId.isEmpty()) {
            viewModel.loadSession(sessionId);
        } else {
            Toast.makeText(this, "Thiếu session_id của phiên tư vấn", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (MODE_CHAT.equals(sessionMode) && viewModel != null) {
            viewModel.connectChatRealtimeIfNeeded();
        }
    }

    @Override
    protected void onStop() {
        if (viewModel != null) {
            viewModel.stopChatRealtime();
        }
        super.onStop();
    }

    private void initViews() {
        btnSessionBack = findViewById(R.id.btnSessionBack);
        btnSessionCall = findViewById(R.id.btnSessionCall);
        btnSessionSend = findViewById(R.id.btnSessionSend);

        txtSessionDoctorName = findViewById(R.id.txtSessionDoctorName);
        txtSessionSubtitle = findViewById(R.id.txtSessionSubtitle);
        txtSessionBadge = findViewById(R.id.txtSessionBadge);
        txtSessionAvatarEmoji = findViewById(R.id.txtSessionAvatarEmoji);
        txtSessionEmptyTitle = findViewById(R.id.txtSessionEmptyTitle);
        txtSessionEmptyDesc = findViewById(R.id.txtSessionEmptyDesc);

        edtSessionInput = findViewById(R.id.edtSessionInput);

        rvSessionMessages = findViewById(R.id.rvSessionMessages);
        layoutSessionEmptyState = findViewById(R.id.layoutSessionEmptyState);
    }

    private void readIntentData() {
        if (getIntent() == null) {
            return;
        }

        sessionId = safeText(getIntent().getStringExtra(EXTRA_SESSION_ID));
        doctorName = safeText(getIntent().getStringExtra(EXTRA_DOCTOR_NAME));
        doctorAvatar = safeText(getIntent().getStringExtra(EXTRA_DOCTOR_AVATAR));
        consultationStatus = safeText(getIntent().getStringExtra(EXTRA_STATUS));

        String rawFormatType = safeText(getIntent().getStringExtra(EXTRA_FORMAT_TYPE)).toUpperCase();
        if (rawFormatType.contains("CALL") || rawFormatType.contains("VIDEO")) {
            sessionMode = MODE_CALL;
            isTextInputAllowed = false;
            isCallActionAllowed = true;
        } else {
            sessionMode = MODE_CHAT;
            isTextInputAllowed = true;
            isCallActionAllowed = false;
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ConsultationSessionViewModel.class);
    }

    private void setupRecyclerView() {
        messageAdapter = new ConsultationMessageAdapter(resolveCurrentUserId());
        messageAdapter.setDoctorAvatarBadge(resolveDoctorBadge());

        rvSessionMessages.setLayoutManager(new LinearLayoutManager(this));
        rvSessionMessages.setAdapter(messageAdapter);
    }

    private void bindStaticSessionInfo() {
        txtSessionDoctorName.setText(
                doctorName.isEmpty() ? "Bác sĩ tư vấn Heami" : doctorName
        );

        txtSessionAvatarEmoji.setText(resolveDoctorBadge());

        if (MODE_CALL.equals(sessionMode)) {
            txtSessionBadge.setText("Gọi video");
            txtSessionSubtitle.setText(buildSubtitleForCall());
            txtSessionEmptyTitle.setText("Phiên gọi tư vấn");
            txtSessionEmptyDesc.setText("Gói này chỉ hỗ trợ gọi video với bác sĩ. Bạn có thể bắt đầu cuộc gọi bằng nút ở góc trên.");
        } else {
            txtSessionBadge.setText("Chat tư vấn");
            txtSessionSubtitle.setText(buildSubtitleForChat());
            txtSessionEmptyTitle.setText("Chưa có tin nhắn");
            txtSessionEmptyDesc.setText("Khi phiên chat bắt đầu, tin nhắn giữa bạn và bác sĩ sẽ xuất hiện tại đây.");
        }
    }

    private void observeViewModel() {
        viewModel.getConsultationLiveData().observe(this, this::bindConsultationData);

        viewModel.getSessionModeLiveData().observe(this, mode -> {
            sessionMode = safeText(mode);
            applyModeUi(sessionMode);
            refreshTextInputState();
            refreshCallButtonState();
            refreshSendButtonState();
        });

        viewModel.getTextEnabledLiveData().observe(this, enabled -> {
            isTextInputAllowed = Boolean.TRUE.equals(enabled);
            refreshTextInputState();
            refreshSendButtonState();
        });

        viewModel.getCallEnabledLiveData().observe(this, enabled -> {
            isCallActionAllowed = Boolean.TRUE.equals(enabled);
            refreshCallButtonState();
        });

        viewModel.getMessagesLiveData().observe(this, this::bindMessages);

        viewModel.getSessionReadyLiveData().observe(this, ready -> {
            boolean isReady = Boolean.TRUE.equals(ready);
            if (!isReady) {
                return;
            }

            if (MODE_CHAT.equals(sessionMode)) {
                viewModel.connectChatRealtimeIfNeeded();
            }
        });

        viewModel.getErrorMessageLiveData().observe(this, error -> {
            if (!safeText(error).isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getInfoMessageLiveData().observe(this, info -> {
            String message = safeText(info);
            if (message.isEmpty()) {
                return;
            }

            if (!"Đã gửi tin nhắn".equals(message)) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindConsultationData(@Nullable ConsultationModel consultation) {
        if (consultation == null) {
            return;
        }

        consultationStatus = safeText(consultation.getStatus());

        String liveDoctorName = safeText(consultation.getDoctorName());
        if (!liveDoctorName.isEmpty()) {
            doctorName = liveDoctorName;
        }

        String liveDoctorAvatar = safeText(consultation.getDoctorAvatar());
        if (!liveDoctorAvatar.isEmpty()) {
            doctorAvatar = liveDoctorAvatar;
        }

        txtSessionDoctorName.setText(
                doctorName.isEmpty() ? "Bác sĩ tư vấn Heami" : doctorName
        );
        txtSessionAvatarEmoji.setText(resolveDoctorBadge());
        messageAdapter.setDoctorAvatarBadge(resolveDoctorBadge());

        applyModeUi(sessionMode);
        refreshTextInputState();
        refreshCallButtonState();
        refreshSendButtonState();
    }

    private void bindMessages(@Nullable List<ChatMessageModel> messages) {
        List<ChatMessageModel> safeMessages = messages != null ? messages : new ArrayList<>();
        messageAdapter.submitList(safeMessages);
        updateMessageUiState(safeMessages);

        if (!safeMessages.isEmpty()) {
            rvSessionMessages.post(() ->
                    rvSessionMessages.scrollToPosition(safeMessages.size() - 1)
            );
        }
    }

    private void updateMessageUiState(@NonNull List<ChatMessageModel> messages) {
        boolean showEmpty = messages.isEmpty();
        layoutSessionEmptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        rvSessionMessages.setVisibility(showEmpty ? View.INVISIBLE : View.VISIBLE);
    }

    private String buildSubtitleForChat() {
        if ("ONGOING".equalsIgnoreCase(consultationStatus)) {
            return "🟢 Đang tư vấn qua chat";
        }
        if ("COMPLETED".equalsIgnoreCase(consultationStatus)) {
            return "✅ Phiên chat đã hoàn thành";
        }
        if ("CANCELLED".equalsIgnoreCase(consultationStatus) || "CANCELED".equalsIgnoreCase(consultationStatus)) {
            return "⛔ Phiên chat đã bị hủy";
        }
        return "🟡 Chờ bắt đầu phiên chat";
    }

    private String buildSubtitleForCall() {
        if ("ONGOING".equalsIgnoreCase(consultationStatus)) {
            return "🟢 Đang trong phiên gọi";
        }
        if ("COMPLETED".equalsIgnoreCase(consultationStatus)) {
            return "✅ Phiên gọi đã hoàn thành";
        }
        if ("CANCELLED".equalsIgnoreCase(consultationStatus) || "CANCELED".equalsIgnoreCase(consultationStatus)) {
            return "⛔ Phiên gọi đã bị hủy";
        }
        return "🟢 Sẵn sàng gọi tư vấn";
    }

    private void applyModeUi(String mode) {
        boolean isChatMode = MODE_CHAT.equals(mode);

        if (isChatMode) {
            txtSessionBadge.setText("Chat tư vấn");
            txtSessionSubtitle.setText(buildSubtitleForChat());
            txtSessionEmptyTitle.setText("Chưa có tin nhắn");
            txtSessionEmptyDesc.setText("Khi phiên chat bắt đầu, tin nhắn giữa bạn và bác sĩ sẽ xuất hiện tại đây.");
        } else {
            txtSessionBadge.setText("Gọi video");
            txtSessionSubtitle.setText(buildSubtitleForCall());
            txtSessionEmptyTitle.setText("Phiên gọi tư vấn");
            txtSessionEmptyDesc.setText("Gói này chỉ hỗ trợ gọi video với bác sĩ. Bạn có thể bắt đầu cuộc gọi bằng nút ở góc trên.");
        }
    }

    private void setupActions() {
        btnSessionBack.setOnClickListener(v -> finish());

        btnSessionCall.setOnClickListener(v -> {
            if (!btnSessionCall.isEnabled()) {
                return;
            }

            if (viewModel != null) {
                viewModel.onCallClicked();
            }
        });

        btnSessionSend.setOnClickListener(v -> {
            if (!btnSessionSend.isEnabled()) {
                return;
            }

            String message = edtSessionInput.getText().toString().trim();
            if (message.isEmpty()) {
                return;
            }

            if (viewModel != null) {
                viewModel.sendMessage(message);
                edtSessionInput.setText("");
                refreshSendButtonState();
            }
        });

        edtSessionInput.addTextChangedListener(new TextWatcher() {
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

    private void refreshTextInputState() {
        edtSessionInput.setEnabled(isTextInputAllowed);
        edtSessionInput.setFocusable(isTextInputAllowed);
        edtSessionInput.setFocusableInTouchMode(isTextInputAllowed);
        edtSessionInput.setClickable(isTextInputAllowed);

        if (isTextInputAllowed) {
            edtSessionInput.setHint("Nhắn cho bác sĩ...");
            edtSessionInput.setAlpha(1f);
        } else {
            edtSessionInput.setHint(resolveDisabledInputHint());
            edtSessionInput.setAlpha(0.65f);
        }
    }

    private void refreshSendButtonState() {
        boolean hasText = !TextUtils.isEmpty(edtSessionInput.getText().toString().trim());
        boolean enabled = isTextInputAllowed && hasText;

        btnSessionSend.setEnabled(enabled);
        btnSessionSend.setAlpha(enabled ? 1f : 0.45f);
        btnSessionSend.setImageTintList(
                ColorStateList.valueOf(Color.parseColor(enabled ? "#7B61FF" : "#B9B2CC"))
        );
    }

    private void refreshCallButtonState() {
        boolean enabled = isCallActionAllowed;

        btnSessionCall.setEnabled(enabled);
        btnSessionCall.setAlpha(enabled ? 1f : 0.45f);
        btnSessionCall.setImageTintList(
                ColorStateList.valueOf(Color.parseColor(enabled ? "#7B61FF" : "#B9B2CC"))
        );
    }

    private String resolveDisabledInputHint() {
        if ("COMPLETED".equalsIgnoreCase(consultationStatus)) {
            return "Phiên tư vấn này đã hoàn thành";
        }

        if ("CANCELLED".equalsIgnoreCase(consultationStatus) || "CANCELED".equalsIgnoreCase(consultationStatus)) {
            return "Phiên tư vấn này đã bị hủy";
        }

        if (MODE_CALL.equals(sessionMode)) {
            return "Gói này không hỗ trợ nhắn tin";
        }

        return "Bạn chưa thể nhắn tin ở phiên này";
    }

    @NonNull
    private String resolveDoctorBadge() {
        String trimmed = safeText(doctorAvatar);

        if (trimmed.isEmpty()) {
            return "🩺";
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return "🩺";
        }

        return trimmed;
    }

    @NonNull
    private String resolveCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return "";
        }
        return safeText(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
