package com.example.heami.ui.consultation;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.content.SharedPreferences;

import com.example.heami.data.models.CallTokenResponseModel;
import com.example.heami.data.repositories.CallTokenRepository;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

import java.util.Locale;

import android.view.SurfaceView;
import android.widget.FrameLayout;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.video.VideoCanvas;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class ConsultationCallActivity extends AppCompatActivity {

    private static final String TAG = "ConsultationCall";

    public static final String EXTRA_SESSION_ID = "extra_session_id";
    public static final String EXTRA_ROLE = "extra_role";
    public static final String EXTRA_PARTNER_NAME = "extra_partner_name";
    public static final String EXTRA_PARTNER_AVATAR = "extra_partner_avatar";
    public static final String EXTRA_FORMAT_TYPE = "extra_format_type";
    public static final String EXTRA_CALL_CHANNEL_ID = "extra_call_channel_id";
    public static final String EXTRA_ACTOR_UID = "extra_actor_uid";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_DOCTOR = "DOCTOR";

    private ImageButton btnCallBack;
    private ImageButton btnCallSwitch;
    private TextView txtCallTimer;
    private TextView txtCallRemoteEmoji;
    private TextView txtCallPartnerName;
    private TextView txtCallPartnerTitle;
    private TextView txtCallConnectionStatus;

    private TextView txtCallLocalEmoji;
    private TextView txtCallLocalLabel;

    private FrameLayout frameCallRemoteVideo;
    private FrameLayout frameCallLocalVideo;

    private boolean isJoinedChannel = false;
    private int remoteAgoraUid = -1;

    private FirebaseFirestore firestore;
    private ListenerRegistration consultationCallListener;

    private boolean hasMarkedInCall = false;
    private boolean hasMarkedEnded = false;
    private boolean isRemoteEndingFlow = false;

    private ImageButton btnCallSpeaker;
    private ImageButton btnCallCamera;
    private ImageButton btnCallMic;
    private ImageButton btnCallEnd;
    private ImageButton btnCallChat;

    private TextView txtCallSpeakerLabel;
    private TextView txtCallCameraLabel;
    private TextView txtCallMicLabel;
    private TextView txtCallEndLabel;
    private TextView txtCallChatLabel;

    private final CallTokenRepository callTokenRepository = new CallTokenRepository();

    private String currentActorUid = "";
    private String rtcAppId = "";
    private String rtcToken = "";
    private String rtcChannelName = "";

    private static final int REQ_CALL_PERMISSIONS = 4105;

    private static final String[] CALL_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private RtcEngine rtcEngine;
    private boolean isAgoraInitialized = false;
    private boolean isCallPermissionsGranted = false;

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            Log.d(TAG, "onConnectionStateChanged: state=" + state + ", reason=" + reason);

            runOnUiThread(() -> {
                if (state == 3) { // connected
                    hasTokenError = false;
                    renderConnectionState();
                    return;
                }

                if (state == 4) { // reconnecting
                    hasTokenError = false;
                    isConnected = false;
                    renderConnectionState();
                    return;
                }

                if (state == 5) { // failed
                    hasTokenError = true;
                    isConnected = false;
                    renderConnectionState();
                    return;
                }

                if (state == 1) { // idle/disconnected
                    if (!isJoinedChannel) {
                        hasTokenError = false;
                    }
                    isConnected = false;
                    renderConnectionState();
                }
            });
        }
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG, "onJoinChannelSuccess: channel=" + channel + ", uid=" + uid + ", elapsed=" + elapsed);

            runOnUiThread(() -> {
                isJoinedChannel = true;
                isConnected = false;
                applySpeakerState();
                applyMicState();
                applyCameraState();
                renderConnectionState();
                renderControlStates();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d(TAG, "onUserJoined: uid=" + uid + ", elapsed=" + elapsed);

            runOnUiThread(() -> {
                remoteAgoraUid = uid;
                setupRemoteVideo(uid);
                isConnected = true;

                if (!hasMarkedInCall) {
                    hasMarkedInCall = true;
                    updateConsultationCallState("IN_CALL", false);
                }

                renderConnectionState();
                renderControlStates();
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(TAG, "onUserOffline: uid=" + uid + ", reason=" + reason);

            runOnUiThread(() -> {
                if (remoteAgoraUid == uid) {
                    remoteAgoraUid = -1;
                    clearRemoteVideo();
                    isConnected = false;
                    renderConnectionState();
                    renderControlStates();
                }
            });
        }

        public void onWarning(int warn) {
            Log.w(TAG, "Agora onWarning: warn=" + warn);
        }
        @Override
        public void onError(int err) {
            Log.e(TAG, "Agora onError: err=" + err);

            // 1052 = audio device glitch / high CPU audio issue
            if (err == 1052) {
                return;
            }

            runOnUiThread(() -> {
                hasTokenError = true;
                isConnected = false;
                renderConnectionState();
            });
        }
    };

    private String rtcCallChannelId = "";

    private boolean isFetchingToken = false;
    private boolean isTokenReady = false;
    private boolean hasTokenError = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            callDurationSeconds++;
            renderTimer();
            timerHandler.postDelayed(this, 1000L);
        }
    };

    private String sessionId = "";
    private String role = ROLE_USER;
    private String partnerName = "";
    private String partnerAvatar = "";
    private String formatType = "";
    private String callChannelId = "";
    private String actorUidFromIntent = "";

    private static final String CALL_NOTE_PREFS = "HeamiCallNotes";
    private String localCallNoteDraft = "";

    private boolean isConnected = true;
    private boolean isSpeakerEnabled = true;
    private boolean isCameraEnabled = true;
    private boolean isMicEnabled = true;

    private boolean isFrontCamera = true;

    private int callDurationSeconds = 17;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_call);

        firestore = FirebaseFirestore.getInstance();

        bindViews();
        readExtras();
        loadLocalCallNoteDraft();
        setupActions();
        resolveActorUid();
        renderScreen();
        ensurePermissionsAndMaybeInitAgora();
        fetchCallToken();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startFakeTimer();
        startConsultationCallListener();
    }

    @Override
    protected void onStop() {
        stopConsultationCallListener();
        super.onStop();
        stopFakeTimer();
    }

    private void bindViews() {
        btnCallBack = findViewById(R.id.btnCallBack);
        btnCallSwitch = findViewById(R.id.btnCallSwitch);

        txtCallTimer = findViewById(R.id.txtCallTimer);
        txtCallRemoteEmoji = findViewById(R.id.txtCallRemoteEmoji);
        txtCallPartnerName = findViewById(R.id.txtCallPartnerName);
        txtCallPartnerTitle = findViewById(R.id.txtCallPartnerTitle);
        txtCallConnectionStatus = findViewById(R.id.txtCallConnectionStatus);

        txtCallLocalEmoji = findViewById(R.id.txtCallLocalEmoji);
        txtCallLocalLabel = findViewById(R.id.txtCallLocalLabel);

        btnCallSpeaker = findViewById(R.id.btnCallSpeaker);
        btnCallCamera = findViewById(R.id.btnCallCamera);
        btnCallMic = findViewById(R.id.btnCallMic);
        btnCallEnd = findViewById(R.id.btnCallEnd);
        btnCallChat = findViewById(R.id.btnCallChat);

        txtCallSpeakerLabel = findViewById(R.id.txtCallSpeakerLabel);
        txtCallCameraLabel = findViewById(R.id.txtCallCameraLabel);
        txtCallMicLabel = findViewById(R.id.txtCallMicLabel);
        txtCallEndLabel = findViewById(R.id.txtCallEndLabel);
        txtCallChatLabel = findViewById(R.id.txtCallChatLabel);

        frameCallRemoteVideo = findViewById(R.id.frameCallRemoteVideo);
        frameCallLocalVideo = findViewById(R.id.frameCallLocalVideo);
    }

    private void readExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            applyFallbackValues();
            return;
        }

        sessionId = safeText(extras.getString(EXTRA_SESSION_ID), "");
        role = safeText(extras.getString(EXTRA_ROLE), ROLE_USER);
        partnerName = safeText(extras.getString(EXTRA_PARTNER_NAME), "");
        partnerAvatar = safeText(extras.getString(EXTRA_PARTNER_AVATAR), "");
        formatType = safeText(extras.getString(EXTRA_FORMAT_TYPE), "");
        callChannelId = safeText(extras.getString(EXTRA_CALL_CHANNEL_ID), "");
        actorUidFromIntent = safeText(extras.getString(EXTRA_ACTOR_UID), "");

        applyFallbackValues();
    }

    private void applyFallbackValues() {
        if (!ROLE_DOCTOR.equalsIgnoreCase(role)) {
            role = ROLE_USER;
        } else {
            role = ROLE_DOCTOR;
        }

        if (partnerName.isEmpty()) {
            partnerName = ROLE_USER.equals(role)
                    ? "Bác sĩ tư vấn Heami"
                    : "Người dùng Heami";
        }

        if (formatType.isEmpty()) {
            formatType = "CALL";
        }
    }

    private void setupActions() {
        btnCallBack.setOnClickListener(v -> finish());

        btnCallSwitch.setOnClickListener(v -> {
            if (hasTokenError) {
                fetchCallToken();
                return;
            }

            if (!isTokenReady || !isAgoraInitialized || !isJoinedChannel) {
                return;
            }

            switchCameraLens();
        });

        btnCallSpeaker.setOnClickListener(v -> {
            if (!isTokenReady || !isAgoraInitialized) {
                return;
            }

            isSpeakerEnabled = !isSpeakerEnabled;
            applySpeakerState();
            renderControlStates();
        });

        btnCallCamera.setOnClickListener(v -> {
            if (!isTokenReady || !isAgoraInitialized) {
                return;
            }

            isCameraEnabled = !isCameraEnabled;
            applyCameraState();
            renderControlStates();
        });

        btnCallMic.setOnClickListener(v -> {
            if (!isTokenReady || !isAgoraInitialized) {
                return;
            }

            isMicEnabled = !isMicEnabled;
            applyMicState();
            renderControlStates();
        });

        btnCallEnd.setOnClickListener(v -> endCallAndSync(true));

        btnCallChat.setOnClickListener(v -> openInCallNoteDialog());
    }

    private void renderScreen() {
        renderPartnerInfo();
        renderConnectionState();
        renderTimer();
        renderLocalPreview();
        renderControlStates();
    }

    private void renderPartnerInfo() {
        txtCallPartnerName.setText(partnerName);
        txtCallPartnerTitle.setText(resolvePartnerTitle());
        txtCallRemoteEmoji.setText(resolvePartnerEmoji());
    }

    private void renderConnectionState() {
        if (isFetchingToken) {
            txtCallConnectionStatus.setText("Đang chuẩn bị cuộc gọi");
            return;
        }

        if (hasTokenError) {
            txtCallConnectionStatus.setText("Không thể kết nối");
            return;
        }

        if (!isAgoraInitialized) {
            txtCallConnectionStatus.setText("Đang khởi tạo cuộc gọi");
            return;
        }

        if (isJoinedChannel && isConnected) {
            txtCallConnectionStatus.setText("Đã kết nối");
            return;
        }

        if (isJoinedChannel) {
            txtCallConnectionStatus.setText("Đang chờ đối phương");
            return;
        }

        txtCallConnectionStatus.setText("Chuẩn bị vào phòng gọi");
    }

    private void renderTimer() {
        int minutes = callDurationSeconds / 60;
        int seconds = callDurationSeconds % 60;
        txtCallTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void renderLocalPreview() {
        txtCallLocalLabel.setText("Bạn");
        txtCallLocalEmoji.setText(resolveLocalEmoji());
    }

    private void renderControlStates() {
        boolean controlsEnabled = isTokenReady && !isFetchingToken && !hasTokenError;

        applyToggleVisual(
                btnCallSpeaker,
                txtCallSpeakerLabel,
                controlsEnabled && isSpeakerEnabled,
                "Loa"
        );

        applyToggleVisual(
                btnCallCamera,
                txtCallCameraLabel,
                controlsEnabled && isCameraEnabled,
                "Camera"
        );

        applyToggleVisual(
                btnCallMic,
                txtCallMicLabel,
                controlsEnabled && isMicEnabled,
                "Micro"
        );

        btnCallSpeaker.setEnabled(controlsEnabled);
        btnCallCamera.setEnabled(controlsEnabled);
        btnCallMic.setEnabled(controlsEnabled);
        btnCallChat.setEnabled(controlsEnabled);

        btnCallChat.setAlpha(controlsEnabled ? 0.92f : 0.42f);
        txtCallChatLabel.setAlpha(controlsEnabled ? 0.82f : 0.45f);

        btnCallEnd.setEnabled(true);
        btnCallEnd.setAlpha(1f);
        txtCallEndLabel.setAlpha(0.95f);

        btnCallSwitch.setEnabled(true);
        btnCallSwitch.setAlpha(1f);
    }

    private void applyToggleVisual(
            @NonNull ImageButton button,
            @NonNull TextView label,
            boolean enabled,
            @NonNull String defaultLabel
    ) {
        button.setSelected(enabled);
        button.setAlpha(enabled ? 1f : 0.48f);
        label.setText(defaultLabel);
        label.setAlpha(enabled ? 0.92f : 0.55f);
    }

    private void startFakeTimer() {
        stopFakeTimer();
        timerHandler.postDelayed(timerRunnable, 1000L);
    }

    private void stopFakeTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    @NonNull
    private String resolvePartnerTitle() {
        if (ROLE_USER.equals(role)) {
            return "Chuyên gia Trị liệu Nhận thức";
        }
        return "Phiên tư vấn CALL";
    }

    @NonNull
    private String resolvePartnerEmoji() {
        if (!partnerAvatar.isEmpty()
                && !partnerAvatar.startsWith("http://")
                && !partnerAvatar.startsWith("https://")
                && partnerAvatar.length() <= 3) {
            return partnerAvatar;
        }

        if (ROLE_USER.equals(role)) {
            return "🩺";
        }
        return "🙂";
    }

    @NonNull
    private String resolveLocalEmoji() {
        if (ROLE_USER.equals(role)) {
            return "🙂";
        }
        return "🩺";
    }

    private void resolveActorUid() {
        currentActorUid = safeText(actorUidFromIntent, "");
        if (!currentActorUid.isEmpty()) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);

        if (ROLE_DOCTOR.equals(role) && isDoctor) {
            currentActorUid = safeText(prefs.getString("doctor_id", ""), "");

            if (currentActorUid.isEmpty()) {
                /*
                 * Fallback cho tài khoản doctor nội bộ hiện tại.
                 * AuthViewModel đang dùng account/doctor id cố định là doc_001.
                 */
                currentActorUid = "doc_001";
            }

            return;
        }

        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        currentActorUid = user != null ? safeText(user.getUid(), "") : "";
    }

    private void fetchCallToken() {
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Thiếu session_id để lấy token call", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentActorUid.isEmpty()) {
            Toast.makeText(this, "Không xác định được actor uid", Toast.LENGTH_SHORT).show();
            return;
        }

        isFetchingToken = true;
        isTokenReady = false;
        hasTokenError = false;
        renderConnectionState();
        renderControlStates();

        callTokenRepository.fetchAgoraToken(
                sessionId,
                role,
                currentActorUid,
                new CallTokenRepository.FetchCallTokenListener() {
                    @Override
                    public void onSuccess(@NonNull CallTokenResponseModel.Data data) {
                        runOnUiThread(() -> {
                            isFetchingToken = false;
                            isTokenReady = true;
                            hasTokenError = false;

                            rtcAppId = safeText(data.getAppId(), "");
                            rtcToken = safeText(data.getToken(), "");
                            rtcChannelName = safeText(data.getChannelName(), "");
                            rtcCallChannelId = safeText(data.getCallChannelId(), "");
                            maybeInitAgoraEngine();
                            maybeJoinAgoraChannel();

                            if (!rtcCallChannelId.isEmpty()) {
                                callChannelId = rtcCallChannelId;
                            }

                            isConnected = false;
                            renderConnectionState();
                            renderControlStates();

                            Toast.makeText(
                                    ConsultationCallActivity.this,
                                    "Đang kết nối cuộc gọi...",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        runOnUiThread(() -> {
                            isFetchingToken = false;
                            isTokenReady = false;
                            hasTokenError = true;

                            renderConnectionState();
                            renderControlStates();

                            Toast.makeText(
                                    ConsultationCallActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    }
                }
        );
    }

    private boolean hasAllCallPermissions() {
        for (String permission : CALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestCallPermissions() {
        ActivityCompat.requestPermissions(
                this,
                CALL_PERMISSIONS,
                REQ_CALL_PERMISSIONS
        );
    }

    private void ensurePermissionsAndMaybeInitAgora() {
        isCallPermissionsGranted = hasAllCallPermissions();

        if (!isCallPermissionsGranted) {
            requestCallPermissions();
            return;
        }

        maybeInitAgoraEngine();
    }

    private void maybeInitAgoraEngine() {
        if (isAgoraInitialized) {
            return;
        }

        if (!isCallPermissionsGranted) {
            return;
        }

        if (rtcAppId == null || rtcAppId.trim().isEmpty()) {
            return;
        }

        try {
            Log.d(TAG, "maybeInitAgoraEngine: appId=" + rtcAppId + ", permissionsGranted=" + isCallPermissionsGranted);
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = rtcAppId;
            config.mEventHandler = rtcEventHandler;

            rtcEngine = RtcEngine.create(config);
            rtcEngine.enableVideo();
            rtcEngine.enableAudio();
            rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);

            isAgoraInitialized = true;
            isConnected = false;
            renderConnectionState();
        } catch (Exception e) {
            hasTokenError = true;
            isAgoraInitialized = false;
            rtcEngine = null;

            Toast.makeText(
                    this,
                    e.getMessage() != null ? e.getMessage() : "Không khởi tạo được Agora engine",
                    Toast.LENGTH_SHORT
            ).show();

            renderConnectionState();
        }
    }

    private void releaseAgoraEngine() {
        if (rtcEngine == null) {
            return;
        }

        rtcEngine = null;
        isAgoraInitialized = false;

        new Thread(() -> {
            try {
                RtcEngine.destroy();
            } catch (Exception ignored) {
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQ_CALL_PERMISSIONS) {
            return;
        }

        boolean granted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }

        isCallPermissionsGranted = granted;

        if (!granted) {
            Toast.makeText(
                    this,
                    "Bạn cần cấp quyền camera và micro để bắt đầu cuộc gọi",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        maybeInitAgoraEngine();
    }

    private void setupLocalPreview() {
        if (rtcEngine == null || frameCallLocalVideo == null) {
            return;
        }

        frameCallLocalVideo.removeAllViews();

        SurfaceView surfaceView = new SurfaceView(this);
        frameCallLocalVideo.addView(surfaceView);

        rtcEngine.setupLocalVideo(new VideoCanvas(
                surfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
        ));
        rtcEngine.startPreview();

        if (txtCallLocalEmoji != null) {
            txtCallLocalEmoji.setVisibility(View.GONE);
        }
    }

    private void setupRemoteVideo(int uid) {
        if (rtcEngine == null || frameCallRemoteVideo == null) {
            return;
        }

        frameCallRemoteVideo.removeAllViews();

        SurfaceView surfaceView = new SurfaceView(this);
        frameCallRemoteVideo.addView(surfaceView);

        rtcEngine.setupRemoteVideo(new VideoCanvas(
                surfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                uid
        ));

        if (txtCallRemoteEmoji != null) {
            txtCallRemoteEmoji.setVisibility(View.GONE);
        }
    }

    private void clearRemoteVideo() {
        if (frameCallRemoteVideo != null) {
            frameCallRemoteVideo.removeAllViews();
        }

        if (txtCallRemoteEmoji != null) {
            txtCallRemoteEmoji.setVisibility(View.VISIBLE);
        }
    }

    private void maybeJoinAgoraChannel() {
        if (!isAgoraInitialized) {
            return;
        }

        if (!isCallPermissionsGranted) {
            return;
        }

        if (isJoinedChannel) {
            return;
        }

        if (rtcToken == null || rtcToken.trim().isEmpty()) {
            return;
        }

        if (rtcChannelName == null || rtcChannelName.trim().isEmpty()) {
            return;
        }

        if (currentActorUid == null || currentActorUid.trim().isEmpty()) {
            return;
        }

        setupLocalPreview();

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = true;
        options.publishMicrophoneTrack = true;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;

        Log.d(TAG, "maybeJoinAgoraChannel:");
        Log.d(TAG, "rtcChannelName=" + rtcChannelName);
        Log.d(TAG, "currentActorUid=" + currentActorUid);
        Log.d(TAG, "rtcToken.length=" + (rtcToken != null ? rtcToken.length() : -1));
        Log.d(TAG, "isAgoraInitialized=" + isAgoraInitialized);
        Log.d(TAG, "isJoinedChannel=" + isJoinedChannel);

        int result = rtcEngine.joinChannelWithUserAccount(
                rtcToken,
                rtcChannelName,
                currentActorUid,
                options
        );
        Log.d(TAG, "joinChannelWithUserAccount result=" + result);

        if (result < 0) {
            Log.e(TAG, "Join Agora thất bại ngay lập tức: " + result);
            Toast.makeText(
                    this,
                    "Join Agora thất bại: " + result,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void applySpeakerState() {
        if (rtcEngine == null || !isJoinedChannel) {
            return;
        }

        int result = rtcEngine.setEnableSpeakerphone(isSpeakerEnabled);
        if (result < 0) {
            Toast.makeText(this, "Không đổi được loa: " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private void applyMicState() {
        if (rtcEngine == null) {
            return;
        }

        int result = rtcEngine.muteLocalAudioStream(!isMicEnabled);
        if (result < 0) {
            Toast.makeText(this, "Không đổi được micro: " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private void applyCameraState() {
        if (rtcEngine == null) {
            return;
        }

        int result = rtcEngine.muteLocalVideoStream(!isCameraEnabled);
        if (result < 0) {
            Toast.makeText(this, "Không đổi được camera: " + result, Toast.LENGTH_SHORT).show();
            return;
        }

        if (frameCallLocalVideo != null) {
            frameCallLocalVideo.setVisibility(isCameraEnabled ? View.VISIBLE : View.INVISIBLE);
        }

        if (txtCallLocalEmoji != null) {
            txtCallLocalEmoji.setVisibility(isCameraEnabled ? View.GONE : View.VISIBLE);
        }
    }

    private void switchCameraLens() {
        if (rtcEngine == null || !isCameraEnabled) {
            return;
        }

        int result = rtcEngine.switchCamera();
        if (result < 0) {
            Toast.makeText(this, "Không đổi được camera: " + result, Toast.LENGTH_SHORT).show();
            return;
        }

        isFrontCamera = !isFrontCamera;
    }

    private void leaveAgoraChannelIfNeeded() {
        if (rtcEngine == null || !isJoinedChannel) {
            return;
        }

        try {
            rtcEngine.leaveChannel();
            rtcEngine.stopPreview();
        } catch (Exception ignored) {
        }

        isJoinedChannel = false;
        remoteAgoraUid = -1;
        isConnected = false;

        clearRemoteVideo();

        if (frameCallLocalVideo != null) {
            frameCallLocalVideo.removeAllViews();
            frameCallLocalVideo.setVisibility(View.VISIBLE);
        }

        if (txtCallLocalEmoji != null) {
            txtCallLocalEmoji.setVisibility(View.VISIBLE);
        }

        renderConnectionState();
        renderControlStates();
    }

    private void startConsultationCallListener() {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        stopConsultationCallListener();

        consultationCallListener = firestore.collection("consultations")
                .document(sessionId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    handleConsultationCallSnapshot(snapshot);
                });
    }

    private void stopConsultationCallListener() {
        if (consultationCallListener != null) {
            consultationCallListener.remove();
            consultationCallListener = null;
        }
    }

    private void handleConsultationCallSnapshot(@NonNull DocumentSnapshot snapshot) {
        String remoteCallStatus = safeText(snapshot.getString("call_status"), "").toUpperCase(Locale.ROOT);
        String consultationStatus = safeText(snapshot.getString("status"), "").toUpperCase(Locale.ROOT);

        if ("IN_CALL".equals(remoteCallStatus)) {
            isConnected = true;
            renderConnectionState();
        }

        boolean shouldForceClose =
                "ENDED".equals(remoteCallStatus)
                        || "COMPLETED".equals(consultationStatus)
                        || "CANCELLED".equals(consultationStatus)
                        || "CANCELED".equals(consultationStatus);

        if (!shouldForceClose) {
            return;
        }

        if (hasMarkedEnded) {
            return;
        }

        hasMarkedEnded = true;
        isRemoteEndingFlow = true;

        leaveAgoraChannelIfNeeded();

        Toast.makeText(
                this,
                "Phiên gọi đã kết thúc",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }

    private void updateConsultationCallState(@NonNull String callStatus, boolean markConsultationCompleted) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("call_status", callStatus);

        if (callChannelId != null && !callChannelId.trim().isEmpty()) {
            updates.put("call_channel_id", callChannelId);
        }

        if ("IN_CALL".equals(callStatus)) {
            updates.put("status", "ONGOING");
        }

        if ("ENDED".equals(callStatus)) {
            updates.put("call_ended_at", FieldValue.serverTimestamp());
            if (markConsultationCompleted) {
                updates.put("status", "COMPLETED");
            }
        }

        firestore.collection("consultations")
                .document(sessionId)
                .update(updates);
    }

    private void endCallAndSync(boolean markConsultationCompleted) {
        if (hasMarkedEnded) {
            finish();
            return;
        }

        hasMarkedEnded = true;

        updateConsultationCallState("ENDED", markConsultationCompleted);
        leaveAgoraChannelIfNeeded();

        Toast.makeText(
                this,
                "Đã kết thúc cuộc gọi",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }

    private void loadLocalCallNoteDraft() {
        SharedPreferences prefs = getSharedPreferences(CALL_NOTE_PREFS, MODE_PRIVATE);
        String raw = safeText(prefs.getString(buildLocalCallNoteKey(), ""), "");

        if (raw.isEmpty()) {
            localCallNoteDraft = "";
            return;
        }

        try {
            JSONObject json = new JSONObject(raw);
            localCallNoteDraft = safeText(json.optString("note", ""), "");
        } catch (JSONException e) {
            // fallback cho dữ liệu cũ nếu trước đó chỉ lưu plain text
            localCallNoteDraft = raw;
        }
    }

    private void saveLocalCallNoteDraft(@NonNull String value) {
        localCallNoteDraft = value.trim();

        try {
            JSONObject json = new JSONObject();
            json.put("sessionId", safeText(sessionId, ""));
            json.put("role", safeText(role, ROLE_USER));
            json.put("partnerName", safeText(partnerName, ""));
            json.put("formatType", safeText(formatType, "CALL"));
            json.put("note", localCallNoteDraft);
            json.put("updatedAt", System.currentTimeMillis());

            SharedPreferences prefs = getSharedPreferences(CALL_NOTE_PREFS, MODE_PRIVATE);
            prefs.edit()
                    .putString(buildLocalCallNoteKey(), json.toString())
                    .apply();
        } catch (JSONException e) {
            Toast.makeText(
                    this,
                    "Không lưu được note",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @NonNull
    private String buildLocalCallNoteKey() {
        return "call_note_" + safeText(sessionId, "unknown_session") + "_" + safeText(role, "UNKNOWN_ROLE");
    }

    private boolean hasLocalCallNote() {
        SharedPreferences prefs = getSharedPreferences(CALL_NOTE_PREFS, MODE_PRIVATE);
        String raw = safeText(prefs.getString(buildLocalCallNoteKey(), ""), "");
        return !raw.isEmpty();
    }

    @Nullable
    private JSONObject getLocalCallNoteRecord() {
        SharedPreferences prefs = getSharedPreferences(CALL_NOTE_PREFS, MODE_PRIVATE);
        String raw = safeText(prefs.getString(buildLocalCallNoteKey(), ""), "");

        if (raw.isEmpty()) {
            return null;
        }

        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    private void openInCallNoteDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_call_note_heami, null);
        dialog.setContentView(view);

        TextView txtTitle = view.findViewById(R.id.txtCallNoteTitle);
        TextView txtSubtitle = view.findViewById(R.id.txtCallNoteSubtitle);
        EditText edtNote = view.findViewById(R.id.edtCallNote);
        TextView btnDelete = view.findViewById(R.id.btnDeleteCallNote);
        TextView btnClose = view.findViewById(R.id.btnCloseCallNote);
        TextView btnSave = view.findViewById(R.id.btnSaveCallNote);

        txtTitle.setText("Note cuộc gọi");
        txtSubtitle.setText(
                ROLE_DOCTOR.equals(role)
                        ? "Ghi chú riêng tư của bác sĩ trên thiết bị này • Có thể xem lại sau cuộc gọi"
                        : "Ghi chú riêng tư của bạn trên thiết bị này • Có thể xem lại sau cuộc gọi"
        );

        edtNote.setText(localCallNoteDraft);

        btnDelete.setOnClickListener(v -> {
            saveLocalCallNoteDraft("");
            Toast.makeText(this, "Đã xóa note", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            saveLocalCallNoteDraft(edtNote.getText().toString());
            Toast.makeText(this, "Đã lưu note", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    @Override
    protected void onDestroy() {
        stopConsultationCallListener();

        if (!isRemoteEndingFlow) {
            leaveAgoraChannelIfNeeded();
        }

        releaseAgoraEngine();
        super.onDestroy();
    }
}