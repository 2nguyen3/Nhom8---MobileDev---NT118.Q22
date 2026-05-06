package com.example.heami.ui.community;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.data.repositories.MoodMatchRepository;
import com.example.heami.ui.checkin.CheckInAiActivity;
import com.google.firebase.Timestamp;

public class MoodMatchActivity extends AppCompatActivity {

    private static final long ANALYZING_DELAY_MS = 2200L;
    private static final long POLLING_INTERVAL_MS = 1200L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private View layoutMoodMatchSheet;
    private View layoutStateSearching;
    private View layoutStateAnalyzing;
    private View layoutStateSuccess;
    private View viewMoodHeroRingOuter;
    private View viewMoodHeroRingInner;

    private View progressMoodSearching;
    private View progressMoodAnalyzing;

    private TextView txtSearchingTitle;
    private TextView txtSearchingSubtitle;
    private TextView txtAnalyzingTitle;
    private TextView txtAnalyzingSubtitle;
    private TextView txtSuccessTitle;

    private Button btnMoodMatchCTA;
    private TextView txtBackToCommunity;
    private View moodMatchRoot;

    private MoodMatchRepository moodMatchRepository;

    private String currentMoodTag = "stress";
    private String currentRequestId = "";
    private String currentMatchId = "";
    private String currentRoomId = "";
    private String matchedUserId = "";
    private String matchedUserName = "";
    private String matchedUserAvatar = "";

    private Timestamp currentExpiresAt = null;

    private boolean isSearchingActive = false;
    private boolean isMatched = false;
    private boolean isRetryMode = false;
    private boolean isCheckInRequiredMode = false;
    private boolean isProvisioningRoom = false;
    private boolean hasOpenedChat = false;

    private final Runnable showAnalyzingRunnable = this::showAnalyzingStateAnimated;

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            pollMoodMatchStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_match);

        bindViews();
        initData();
        prepareSheetIntro();
        setupActions();
        setupBackPressHandler();
        startHeroIconPulse();
        startSheetIntroAnimation();

        if (currentMoodTag.isEmpty()) {
            showCheckInRequiredState();
        } else {
            startRealMoodMatchFlow();
        }
    }

    private void bindViews() {
        layoutMoodMatchSheet = findViewById(R.id.layoutMoodMatchSheet);
        layoutStateSearching = findViewById(R.id.layoutStateSearching);
        layoutStateAnalyzing = findViewById(R.id.layoutStateAnalyzing);
        layoutStateSuccess = findViewById(R.id.layoutStateSuccess);
        viewMoodHeroRingOuter = findViewById(R.id.viewMoodHeroRingOuter);
        viewMoodHeroRingInner = findViewById(R.id.viewMoodHeroRingInner);

        progressMoodSearching = findViewById(R.id.progressMoodSearching);
        progressMoodAnalyzing = findViewById(R.id.progressMoodAnalyzing);

        txtSearchingTitle = findViewById(R.id.txtSearchingTitle);
        txtSearchingSubtitle = findViewById(R.id.txtSearchingSubtitle);
        txtAnalyzingTitle = findViewById(R.id.txtAnalyzingTitle);
        txtAnalyzingSubtitle = findViewById(R.id.txtAnalyzingSubtitle);
        txtSuccessTitle = findViewById(R.id.txtSuccessTitle);

        btnMoodMatchCTA = findViewById(R.id.btnMoodMatchCTA);
        txtBackToCommunity = findViewById(R.id.txtBackToCommunity);
        moodMatchRoot = findViewById(R.id.moodMatchRoot);
    }

    private void initData() {
        moodMatchRepository = new MoodMatchRepository();
        currentMoodTag = resolveMoodTagFromIntent();
    }

    private void prepareSheetIntro() {
        if (layoutMoodMatchSheet != null) {
            layoutMoodMatchSheet.setTranslationY(1400f);
            layoutMoodMatchSheet.setAlpha(1f);
        }
    }

    private void startSheetIntroAnimation() {
        if (layoutMoodMatchSheet != null) {
            layoutMoodMatchSheet.animate()
                    .translationY(0f)
                    .setDuration(420)
                    .start();
        }
    }

    private void setupActions() {
        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setEnabled(false);
            btnMoodMatchCTA.setAlpha(0.6f);
            btnMoodMatchCTA.setOnClickListener(v -> {
                if (!btnMoodMatchCTA.isEnabled()) return;

                if (isCheckInRequiredMode) {
                    startActivity(new Intent(MoodMatchActivity.this, CheckInAiActivity.class));
                    return;
                }

                if (isMatched && !currentRoomId.isEmpty()) {
                    openMoodMatchChat();
                    return;
                }

                if (isRetryMode) {
                    startRealMoodMatchFlow();
                }
            });
        }

        if (txtBackToCommunity != null) {
            txtBackToCommunity.setOnClickListener(v -> handleExitRequested());
        }

        if (moodMatchRoot != null) {
            moodMatchRoot.setOnClickListener(v -> handleExitRequested());
        }
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExitRequested();
            }
        });
    }

    private void handleExitRequested() {
        if (isProvisioningRoom || hasOpenedChat) {
            finish();
            return;
        }

        if (isSearchingActive && !currentRequestId.isEmpty() && !isMatched) {
            moodMatchRepository.cancelMoodMatchRequestSafely(
                    currentRequestId,
                    new MoodMatchRepository.SimpleActionListener() {
                        @Override
                        public void onSuccess() {
                            finish();
                        }

                        @Override
                        public void onFailure(@NonNull String errorMessage) {
                            finish();
                        }
                    }
            );
            return;
        }

        finish();
    }

    private void startRealMoodMatchFlow() {
        clearScheduledTasks();

        isSearchingActive = false;
        isMatched = false;
        isRetryMode = false;
        isCheckInRequiredMode = false;
        isProvisioningRoom = false;
        hasOpenedChat = false;

        currentRequestId = "";
        currentMatchId = "";
        currentRoomId = "";
        matchedUserId = "";
        matchedUserName = "";
        matchedUserAvatar = "";
        currentExpiresAt = null;

        showSearchingStateImmediate();

        moodMatchRepository.startMoodMatch(currentMoodTag, new MoodMatchRepository.StartMoodMatchListener() {
            @Override
            public void onSearching(@NonNull MoodMatchRepository.MoodMatchSessionInfo sessionInfo) {
                currentRequestId = sessionInfo.getRequestId();
                currentMatchId = sessionInfo.getMatchId();
                currentRoomId = sessionInfo.getRoomId();
                currentExpiresAt = sessionInfo.getExpiresAt();

                isSearchingActive = true;
                isMatched = false;
                isRetryMode = false;
                isCheckInRequiredMode = false;

                showSearchingStateImmediate();
                scheduleAnalyzingState();
                scheduleNextPoll();
            }

            @Override
            public void onMatched(@NonNull MoodMatchRepository.MoodMatchSessionInfo sessionInfo) {
                handleMatchedSession(sessionInfo);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                showFailureState(errorMessage);
            }
        });
    }

    private void scheduleAnalyzingState() {
        handler.removeCallbacks(showAnalyzingRunnable);
        handler.postDelayed(showAnalyzingRunnable, ANALYZING_DELAY_MS);
    }

    private void scheduleNextPoll() {
        handler.removeCallbacks(pollingRunnable);
        handler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS);
    }

    private void pollMoodMatchStatus() {
        if (!isSearchingActive || currentRequestId.isEmpty()) {
            return;
        }

        if (isExpired(currentExpiresAt)) {
            moodMatchRepository.markRequestTimeout(currentRequestId, new MoodMatchRepository.SimpleActionListener() {
                @Override
                public void onSuccess() {
                    showTimeoutState();
                }

                @Override
                public void onFailure(@NonNull String errorMessage) {
                    showTimeoutState();
                }
            });
            return;
        }

        moodMatchRepository.checkExistingMatchedRequest(
                currentRequestId,
                new MoodMatchRepository.CheckMatchStatusListener() {
                    @Override
                    public void onSearching(@NonNull MoodMatchRepository.MoodMatchSessionInfo sessionInfo) {
                        currentExpiresAt = sessionInfo.getExpiresAt();
                        scheduleNextPoll();
                    }

                    @Override
                    public void onMatched(@NonNull MoodMatchRepository.MoodMatchSessionInfo sessionInfo) {
                        handleMatchedSession(sessionInfo);
                    }

                    @Override
                    public void onTimeout() {
                        showTimeoutState();
                    }

                    @Override
                    public void onCancelled() {
                        showFailureState("Phiên tìm kiếm đã được hủy");
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        showFailureState(errorMessage);
                    }
                }
        );
    }

    private void handleMatchedSession(@NonNull MoodMatchRepository.MoodMatchSessionInfo sessionInfo) {
        clearScheduledTasks();

        currentRequestId = sessionInfo.getRequestId();
        currentMatchId = sessionInfo.getMatchId();
        currentExpiresAt = sessionInfo.getExpiresAt();

        matchedUserId = sessionInfo.getMatchedUserId();
        matchedUserName = sessionInfo.getMatchedUserName();
        matchedUserAvatar = sessionInfo.getMatchedUserAvatar();

        isSearchingActive = false;
        isMatched = true;
        isRetryMode = false;
        isCheckInRequiredMode = false;

        provisionRoomAndShowSuccess();
    }

    private void provisionRoomAndShowSuccess() {
        if (isProvisioningRoom || hasOpenedChat) {
            return;
        }

        if (currentMatchId.isEmpty() || matchedUserId.isEmpty()) {
            showFailureState("Thiếu thông tin ghép cặp để tạo phòng chat");
            return;
        }

        isProvisioningRoom = true;

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setEnabled(false);
            btnMoodMatchCTA.setAlpha(0.6f);
            btnMoodMatchCTA.setText("Đang tạo phòng chat...");
        }

        moodMatchRepository.ensureMoodMatchChatRoom(
                currentMatchId,
                matchedUserId,
                currentMoodTag,
                new MoodMatchRepository.EnsureRoomListener() {
                    @Override
                    public void onSuccess(@NonNull String roomId) {
                        isProvisioningRoom = false;
                        currentRoomId = roomId;
                        showSuccessStateAnimated();
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        isProvisioningRoom = false;
                        showFailureState(errorMessage);
                    }
                }
        );
    }

    private void showSearchingStateImmediate() {
        if (layoutStateSearching != null) {
            layoutStateSearching.setVisibility(View.VISIBLE);
            layoutStateSearching.setAlpha(1f);
            layoutStateSearching.setScaleX(1f);
            layoutStateSearching.setScaleY(1f);
        }

        if (layoutStateAnalyzing != null) {
            layoutStateAnalyzing.setVisibility(View.GONE);
            layoutStateAnalyzing.setAlpha(0f);
        }

        if (layoutStateSuccess != null) {
            layoutStateSuccess.setVisibility(View.GONE);
            layoutStateSuccess.setAlpha(0f);
        }

        if (progressMoodSearching != null) {
            progressMoodSearching.setVisibility(View.VISIBLE);
        }

        if (txtSearchingTitle != null) {
            txtSearchingTitle.setText("Đang tìm người cùng tần số...");
        }

        if (txtSearchingSubtitle != null) {
            txtSearchingSubtitle.setText("Heami đang tìm một người phù hợp\nđể trò chuyện cùng bạn");
        }

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setText("Vào phòng trò chuyện");
            btnMoodMatchCTA.setEnabled(false);
            btnMoodMatchCTA.setAlpha(0.6f);
        }
    }

    private void showAnalyzingStateAnimated() {
        if (!isSearchingActive || isMatched) return;

        if (txtAnalyzingTitle != null) {
            txtAnalyzingTitle.setText("Đang phân tích mood tương thích...");
        }

        if (txtAnalyzingSubtitle != null) {
            txtAnalyzingSubtitle.setText("Đảm bảo kết nối nhẹ nhàng, an toàn\nvà phù hợp với cảm xúc hiện tại");
        }

        if (progressMoodAnalyzing != null) {
            progressMoodAnalyzing.setVisibility(View.VISIBLE);
        }

        crossfadeState(layoutStateSearching, layoutStateAnalyzing);

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setEnabled(false);
            btnMoodMatchCTA.setAlpha(0.6f);
        }
    }

    private void showSuccessStateAnimated() {
        View fromView = (layoutStateAnalyzing != null && layoutStateAnalyzing.getVisibility() == View.VISIBLE)
                ? layoutStateAnalyzing
                : layoutStateSearching;

        crossfadeState(fromView, layoutStateSuccess);

        if (layoutStateSuccess != null) {
            layoutStateSuccess.setScaleX(0.94f);
            layoutStateSuccess.setScaleY(0.94f);
            layoutStateSuccess.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .start();
        }

        if (txtSuccessTitle != null) {
            String partnerName = matchedUserName.trim().isEmpty()
                    ? "Bạn đã được kết nối!"
                    : "Đã ghép với " + matchedUserName + "!";
            txtSuccessTitle.setText(partnerName);
        }

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setText("Vào phòng trò chuyện");
            btnMoodMatchCTA.setEnabled(true);
            btnMoodMatchCTA.animate().alpha(1f).setDuration(220).start();
        }
    }

    private void showTimeoutState() {
        clearScheduledTasks();

        isSearchingActive = false;
        isMatched = false;
        isRetryMode = true;
        isProvisioningRoom = false;

        if (layoutStateSearching != null) {
            layoutStateSearching.setVisibility(View.VISIBLE);
            layoutStateSearching.setAlpha(1f);
        }

        if (layoutStateAnalyzing != null) {
            layoutStateAnalyzing.setVisibility(View.GONE);
            layoutStateAnalyzing.setAlpha(0f);
        }

        if (layoutStateSuccess != null) {
            layoutStateSuccess.setVisibility(View.GONE);
            layoutStateSuccess.setAlpha(0f);
        }

        if (progressMoodSearching != null) {
            progressMoodSearching.setVisibility(View.GONE);
        }

        if (txtSearchingTitle != null) {
            txtSearchingTitle.setText("Chưa tìm thấy người phù hợp");
        }

        if (txtSearchingSubtitle != null) {
            txtSearchingSubtitle.setText("Heami chưa tìm được người phù hợp lúc này.\nBạn có thể thử lại ngay bây giờ.");
        }

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setText("Thử lại");
            btnMoodMatchCTA.setEnabled(true);
            btnMoodMatchCTA.setAlpha(1f);
        }
    }

    private void showFailureState(@NonNull String errorMessage) {
        clearScheduledTasks();

        isSearchingActive = false;
        isMatched = false;
        isRetryMode = true;
        isProvisioningRoom = false;

        if (layoutStateSearching != null) {
            layoutStateSearching.setVisibility(View.VISIBLE);
            layoutStateSearching.setAlpha(1f);
        }

        if (layoutStateAnalyzing != null) {
            layoutStateAnalyzing.setVisibility(View.GONE);
            layoutStateAnalyzing.setAlpha(0f);
        }

        if (layoutStateSuccess != null) {
            layoutStateSuccess.setVisibility(View.GONE);
            layoutStateSuccess.setAlpha(0f);
        }

        if (progressMoodSearching != null) {
            progressMoodSearching.setVisibility(View.GONE);
        }

        if (txtSearchingTitle != null) {
            txtSearchingTitle.setText("Mood Match đang gặp trục trặc");
        }

        if (txtSearchingSubtitle != null) {
            txtSearchingSubtitle.setText(errorMessage);
        }

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setText("Thử lại");
            btnMoodMatchCTA.setEnabled(true);
            btnMoodMatchCTA.setAlpha(1f);
        }
    }

    private void openMoodMatchChat() {
        if (currentRoomId.isEmpty()) {
            return;
        }

        Intent intent = new Intent(MoodMatchActivity.this, MoodMatchChatActivity.class);
        intent.putExtra("room_id", currentRoomId);
        intent.putExtra("match_id", currentMatchId);
        intent.putExtra("matched_user_id", matchedUserId);
        intent.putExtra("matched_user_name", matchedUserName);
        intent.putExtra("matched_user_avatar", matchedUserAvatar);
        intent.putExtra("mood_tag", currentMoodTag);
        intent.putExtra("room_status", "ACTIVE");

        hasOpenedChat = true;
        startActivity(intent);
        finish();
    }

    @NonNull
    private String resolveMoodTagFromIntent() {
        android.content.SharedPreferences prefs = getSharedPreferences("heami_prefs", MODE_PRIVATE);

        String savedMoodTag = prefs.getString("latest_mood_tag", "");
        String savedMoodDate = prefs.getString("latest_mood_date", "");

        String todayKey = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        boolean isTodayMood = todayKey.equals(savedMoodDate);

        if (isTodayMood && savedMoodTag != null && !savedMoodTag.trim().isEmpty()) {
            return savedMoodTag.trim().toLowerCase();
        }

        return "";
    }

    private void showCheckInRequiredState() {
        clearScheduledTasks();

        isSearchingActive = false;
        isMatched = false;
        isRetryMode = false;
        isCheckInRequiredMode = true;
        isProvisioningRoom = false;

        if (layoutStateSearching != null) {
            layoutStateSearching.setVisibility(View.VISIBLE);
            layoutStateSearching.setAlpha(1f);
        }

        if (layoutStateAnalyzing != null) {
            layoutStateAnalyzing.setVisibility(View.GONE);
            layoutStateAnalyzing.setAlpha(0f);
        }

        if (layoutStateSuccess != null) {
            layoutStateSuccess.setVisibility(View.GONE);
            layoutStateSuccess.setAlpha(0f);
        }

        if (progressMoodSearching != null) {
            progressMoodSearching.setVisibility(View.GONE);
        }

        if (txtSearchingTitle != null) {
            txtSearchingTitle.setText("Bạn cần check-in cảm xúc hôm nay");
        }

        if (txtSearchingSubtitle != null) {
            txtSearchingSubtitle.setText(
                    "Heami cần cảm xúc đã xác nhận trong hôm nay,\nđể kết nối bạn với một người phù hợp hơn."
            );
        }

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setText("Check-in ngay");
            btnMoodMatchCTA.setEnabled(true);
            btnMoodMatchCTA.setAlpha(1f);
        }
    }

    private boolean isExpired(Timestamp expiresAt) {
        if (expiresAt == null) return false;
        return expiresAt.toDate().getTime() <= System.currentTimeMillis();
    }

    private void clearScheduledTasks() {
        handler.removeCallbacks(showAnalyzingRunnable);
        handler.removeCallbacks(pollingRunnable);
    }

    private void crossfadeState(View from, View to) {
        if (to != null) {
            to.setVisibility(View.VISIBLE);
            to.setAlpha(0f);
            to.animate().alpha(1f).setDuration(300).start();
        }

        if (from != null) {
            from.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(() -> from.setVisibility(View.GONE))
                    .start();
        }
    }

    private void startHeroIconPulse() {
        startPulseFadeExpand(viewMoodHeroRingOuter, 0.82f, 1.18f, 0.20f, 0f, 1800, 0);
        startPulseFadeExpand(viewMoodHeroRingInner, 0.90f, 1.12f, 0.28f, 0f, 1350, 120);
    }

    private void startPulseFadeExpand(
            View view,
            float startScale,
            float endScale,
            float startAlpha,
            float endAlpha,
            long duration,
            long delay
    ) {
        if (view == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, 1.06f, endScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, 1.06f, endScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, startAlpha, 0.55f, endAlpha);

        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);

        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        scaleX.setInterpolator(interpolator);
        scaleY.setInterpolator(interpolator);
        alpha.setInterpolator(interpolator);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearScheduledTasks();
    }
}