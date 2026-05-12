package com.example.heami.ui.checkin;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.example.heami.R;
import com.example.heami.data.models.MoodHistoryModel;
import com.example.heami.ui.main.HomeActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class CheckInResultActivity extends AppCompatActivity {

    private ImageButton btnBackCheckInResult;

    private TextView txtResultMoodEmoji;
    private TextView txtResultMoodName;
    private TextView txtResultMoodDesc;
    private TextView txtResultMoodPercent;
    private TextView txtResultHeamiMessage;

    private FrameLayout progressResultMood;
    private View viewResultMoodProgress;

    private EditText edtResultNote;
    private LinearLayout btnSaveCheckInResult;
    private TextView txtSaveCheckInResult;

    private TextView chipCauseWork;
    private TextView chipCauseStudy;
    private TextView chipCauseFamily;
    private TextView chipCauseLove;
    private TextView chipCauseHealth;
    private TextView chipCauseFinance;
    private TextView chipCauseWeather;
    private TextView chipCauseOther;

    private final Set<TextView> selectedCauseChips = new HashSet<>();

    private View imgResultHeamiCloud;

    private String moodName;
    private String moodEmoji;
    private String moodDesc;
    private int moodPercent;

    private String source;
    private String rawEmotionLabel;
    private float aiConfidence;
    private String modelName;
    private String modelVersion;
    private String confidenceLevel;

    private TextView txtTherapyMusicTitle;
    private TextView txtTherapyMusicDesc;
    private TextView txtTherapyBreathTitle;
    private TextView txtTherapyBreathDesc;
    private TextView txtTherapyJournalTitle;
    private TextView txtTherapyJournalDesc;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin_result);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();
        bindResultData();
        bindTherapySuggestions();
        setupActions();
        setupCauseChipActions();
        updateProgressBar();
        startResultAnimations();
    }

    private void bindViews() {
        btnBackCheckInResult = findViewById(R.id.btnBackCheckInResult);

        txtResultMoodEmoji = findViewById(R.id.txtResultMoodEmoji);
        txtResultMoodName = findViewById(R.id.txtResultMoodName);
        txtResultMoodDesc = findViewById(R.id.txtResultMoodDesc);
        txtResultMoodPercent = findViewById(R.id.txtResultMoodPercent);
        txtResultHeamiMessage = findViewById(R.id.txtResultHeamiMessage);
        imgResultHeamiCloud = findViewById(R.id.imgResultHeamiCloud);

        progressResultMood = findViewById(R.id.progressResultMood);
        viewResultMoodProgress = findViewById(R.id.viewResultMoodProgress);

        edtResultNote = findViewById(R.id.edtResultNote);
        btnSaveCheckInResult = findViewById(R.id.btnSaveCheckInResult);
        txtSaveCheckInResult = findViewById(R.id.txtSaveCheckInResult);

        chipCauseWork = findViewById(R.id.chipCauseWork);
        chipCauseStudy = findViewById(R.id.chipCauseStudy);
        chipCauseFamily = findViewById(R.id.chipCauseFamily);
        chipCauseLove = findViewById(R.id.chipCauseLove);
        chipCauseHealth = findViewById(R.id.chipCauseHealth);
        chipCauseFinance = findViewById(R.id.chipCauseFinance);
        chipCauseWeather = findViewById(R.id.chipCauseWeather);
        chipCauseOther = findViewById(R.id.chipCauseOther);

        txtTherapyMusicTitle = findViewById(R.id.txtTherapyMusicTitle);
        txtTherapyMusicDesc = findViewById(R.id.txtTherapyMusicDesc);
        txtTherapyBreathTitle = findViewById(R.id.txtTherapyBreathTitle);
        txtTherapyBreathDesc = findViewById(R.id.txtTherapyBreathDesc);
        txtTherapyJournalTitle = findViewById(R.id.txtTherapyJournalTitle);
        txtTherapyJournalDesc = findViewById(R.id.txtTherapyJournalDesc);
    }

    private void bindResultData() {
        moodName = safeText(getIntent().getStringExtra("mood_name"), "Bình thường");
        moodEmoji = safeText(getIntent().getStringExtra("mood_emoji"), "😌");
        moodDesc = safeText(
                getIntent().getStringExtra("mood_desc"),
                "Heami thấy trạng thái của bạn hiện tại khá ổn định."
        );
        moodPercent = getIntent().getIntExtra("mood_percent", 60);

        source = safeText(getIntent().getStringExtra("source"), "manual");
        rawEmotionLabel = safeText(getIntent().getStringExtra("raw_emotion_label"), "unknown");
        aiConfidence = getIntent().getFloatExtra("ai_confidence", 0f);
        modelName = safeText(
                getIntent().getStringExtra("model_name"),
                isAiSource(source) ? "MediaPipe Face Landmarker" : "manual"
        );
        modelVersion = safeText(
                getIntent().getStringExtra("model_version"),
                isAiSource(source) ? "unknown" : "manual"
        );

        String intentConfidenceLevel = getIntent().getStringExtra("confidence_level");
        if (intentConfidenceLevel == null || intentConfidenceLevel.trim().isEmpty()) {
            confidenceLevel = getConfidenceLevel(aiConfidence);
        } else {
            confidenceLevel = intentConfidenceLevel;
        }

        if (moodPercent < 0) moodPercent = 0;
        if (moodPercent > 100) moodPercent = 100;

        moodDesc = getSoftMoodDescIfLowConfidence(moodName, moodDesc);

        if (txtResultMoodName != null) {
            txtResultMoodName.setText(moodName);
        }

        if (txtResultMoodEmoji != null) {
            txtResultMoodEmoji.setText(moodEmoji);
        }

        if (txtResultMoodDesc != null) {
            txtResultMoodDesc.setText(moodDesc);
        }

        if (txtResultMoodPercent != null) {
            txtResultMoodPercent.setText(moodPercent + "%");
        }

        if (txtResultHeamiMessage != null) {
            txtResultHeamiMessage.setText(getHeamiMessage(moodName));
        }
    }

    private void setupActions() {
        if (btnBackCheckInResult != null) {
            btnBackCheckInResult.setOnClickListener(v -> finish());
        }

        if (btnSaveCheckInResult != null) {
            btnSaveCheckInResult.setOnClickListener(v -> saveCheckInToFirestore());
        }
    }

    private void updateProgressBar() {
        if (progressResultMood == null || viewResultMoodProgress == null) {
            return;
        }

        progressResultMood.post(() -> {
            int totalWidth = progressResultMood.getWidth();
            int progressWidth = (int) (totalWidth * (moodPercent / 100f));

            FrameLayout .LayoutParams params = new FrameLayout.LayoutParams(
                    progressWidth,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );

            viewResultMoodProgress.setLayoutParams(params);
        });
    }

    private String getConfidenceLevel(float confidence) {
        if (confidence >= 0.75f) return "high";
        if (confidence >= 0.50f) return "medium";
        return "low";
    }

    private boolean isAiSource(String src) {
        if (src == null) return false;
        String s = src.toLowerCase(Locale.ROOT);
        return s.contains("ai") || s.contains("mediapipe");
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    private String normalizeMoodTag(String moodName, String rawEmotionLabel) {
        String raw = rawEmotionLabel == null ? "" : rawEmotionLabel.trim().toLowerCase(Locale.ROOT);

        switch (raw) {
            case "happy":
            case "sad":
            case "angry":
            case "fear":
            case "stress":
            case "neutral":
            case "disgust":
                return raw;
        }

        switch (moodName) {
            case "Vui vẻ":
                return "happy";
            case "Buồn":
                return "sad";
            case "Tức giận":
                return "angry";
            case "Lo lắng":
            case "Sợ hãi":
                return "fear";
            case "Căng thẳng":
                return "stress";
            case "Khó chịu":
            case "Ghê tởm":
                return "disgust";
            case "Bình thường":
            default:
                return "neutral";
        }
    }

    private void saveTodayConfirmedMoodLocally() {
        String normalizedMoodTag = normalizeMoodTag(moodName, rawEmotionLabel);

        if (normalizedMoodTag.trim().isEmpty()) {
            return;
        }

        String todayKey = getTodayKey();

        getSharedPreferences("heami_prefs", MODE_PRIVATE)
                .edit()
                .putString("latest_mood_tag", normalizedMoodTag)
                .putString("latest_mood_date", todayKey)
                .apply();
    }

    @NonNull
    private String getTodayKey() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    private String normalizeSource(String src) {
        if (src == null || src.trim().isEmpty()) {
            return "manual_checkin";
        }

        String s = src.trim().toLowerCase(Locale.ROOT);

        if (s.contains("mediapipe")) {
            return "ai_checkin_mediapipe";
        }

        if (s.contains("ai")) {
            return "ai_checkin";
        }

        return "manual_checkin";
    }

    private String normalizeConfidenceLevel(String currentLevel, float confidence, boolean aiAnalysis) {
        if (!aiAnalysis) {
            return "manual";
        }

        if ("high".equals(currentLevel) || "medium".equals(currentLevel) || "low".equals(currentLevel)) {
            return currentLevel;
        }

        return getConfidenceLevel(confidence);
    }

    private int getEnergyLevelForMood(String moodName) {
        switch (moodName) {
            case "Vui vẻ":
                return 85;
            case "Tức giận":
                return 72;
            case "Căng thẳng":
                return 42;
            case "Lo lắng":
            case "Sợ hãi":
                return 34;
            case "Buồn":
                return 28;
            case "Khó chịu":
            case "Ghê tởm":
                return 30;
            case "Bình thường":
            default:
                return 58;
        }
    }

    private ArrayList<String> sanitizeStringList(ArrayList<String> input) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();

        if (input != null) {
            for (String item : input) {
                if (item == null) continue;
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    cleaned.add(trimmed);
                }
            }
        }

        return new ArrayList<>(cleaned);
    }

    private String buildMoodRecordId(Timestamp timestamp) {
        return "mood_" + timestamp.getSeconds() + "_" + timestamp.getNanoseconds();
    }

    private String getHeamiMessage(String moodName) {
        switch (moodName) {
            case "Bình thường":
                return "Heami thấy cảm xúc của bạn hôm nay khá ổn định. Đây cũng là một trạng thái rất đáng trân trọng đó.";

            case "Căng thẳng":
                return "Heami thấy bạn đang mang nhiều áp lực hôm nay. Hãy để Heami cùng bạn thở nhẹ một chút nhé.";

            case "Lo lắng":
            case "Sợ hãi":
                return "Heami cảm nhận bạn đang cần một cảm giác an toàn hơn. Mình cứ đi chậm lại một chút thôi nhé.";

            case "Vui vẻ":
                return "Heami thấy năng lượng của bạn hôm nay rất tươi sáng. Hãy lưu lại khoảnh khắc này nha.";

            case "Buồn":
                return "Heami thấy hôm nay bạn có vẻ hơi nặng lòng. Bạn không cần phải ổn ngay lập tức đâu.";

            case "Khó chịu":
            case "Ghê tởm":
                return "Heami cảm nhận cơ thể và tâm trí bạn đang cần được nghỉ ngơi. Hãy nhẹ nhàng với bản thân hơn nhé.";

            case "Tức giận":
                return "Heami thấy bên trong bạn đang có nhiều điều bị dồn nén. Mình thử hít thở chậm lại trước nha.";

            default:
                return "Heami đã ghi nhận cảm xúc của bạn hôm nay. Cảm ơn bạn vì đã lắng nghe chính mình.";
        }
    }

    private String getSoftMoodDescIfLowConfidence(String moodName, String originalDesc) {
        if (!isAiSource(source)) {
            return originalDesc;
        }

        if (!"low".equals(confidenceLevel)) {
            return originalDesc;
        }

        switch (moodName) {
            case "Vui vẻ":
                return "Heami chưa thật sự chắc chắn, nhưng thấy bạn có nét năng lượng tích cực.";

            case "Buồn":
                return "Heami chưa thật sự chắc chắn, nhưng cảm nhận bạn có vẻ hơi trầm hơn hôm nay.";

            case "Tức giận":
                return "Heami chưa thật sự chắc chắn, nhưng nhận thấy bạn có vẻ đang hơi căng bên trong.";

            case "Lo lắng":
            case "Sợ hãi":
                return "Heami chưa thật sự chắc chắn, nhưng thấy bạn có vẻ cần thêm cảm giác an toàn.";

            case "Khó chịu":
            case "Ghê tởm":
                return "Heami chưa thật sự chắc chắn, nhưng cảm nhận cơ thể bạn có vẻ đang không thoải mái.";

            case "Bình thường":
                return "Heami chưa thật sự chắc chắn, nhưng thấy cảm xúc của bạn có vẻ khá ổn định.";

            case "Căng thẳng":
            default:
                return "Heami chưa thật sự chắc chắn, nhưng có vẻ hôm nay bạn hơi căng thẳng.";
        }
    }

    private void startResultAnimations() {
        startFloatY(imgResultHeamiCloud, 5f, 4200, 0);
        startCloudBreath(imgResultHeamiCloud);
    }

    private void startFloatY(View view, float dpDistance, long duration, long delay) {
        if (view == null) return;

        float distancePx = dpDistance * getResources().getDisplayMetrics().density;

        ObjectAnimator moveY = ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_Y,
                0f,
                -distancePx,
                0f
        );

        moveY.setDuration(duration);
        moveY.setStartDelay(delay);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.start();
    }

    private void startCloudBreath(View view) {
        if (view == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.025f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.025f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.94f, 1.0f, 0.94f);

        scaleX.setDuration(2600);
        scaleY.setDuration(2600);
        alpha.setDuration(2600);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.start();
    }

    private void setupCauseChipActions() {
        setupCauseChip(chipCauseWork);
        setupCauseChip(chipCauseStudy);
        setupCauseChip(chipCauseFamily);
        setupCauseChip(chipCauseLove);
        setupCauseChip(chipCauseHealth);
        setupCauseChip(chipCauseFinance);
        setupCauseChip(chipCauseWeather);
        setupCauseChip(chipCauseOther);
    }

    private void setupCauseChip(TextView chip) {
        if (chip == null) return;
        chip.setOnClickListener(v -> toggleCauseChip(chip));
    }

    private void toggleCauseChip(TextView chip) {
        boolean isSelected = selectedCauseChips.contains(chip);

        if (isSelected) {
            selectedCauseChips.remove(chip);
            setCauseChipUnselected(chip);
        } else {
            selectedCauseChips.add(chip);
            setCauseChipSelected(chip);
        }

        animateCauseChip(chip);
    }

    private void setCauseChipSelected(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_result_cause_chip_selected);
        chip.setTextColor(0xFFE86FA0);
        chip.setAlpha(1f);

        String text = chip.getText().toString();
        if (!text.endsWith("  ✓")) {
            chip.setText(text + "  ✓");
        }
    }

    private void setCauseChipUnselected(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_result_cause_chip);
        chip.setTextColor(0xFF7A8AAA);
        chip.setAlpha(0.92f);

        String text = chip.getText().toString();
        chip.setText(text.replace("  ✓", ""));
    }

    private void animateCauseChip(View chip) {
        if (chip == null) return;

        chip.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(110)
                .withEndAction(() -> chip.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }

    private ArrayList<String> getSelectedCauses() {
        ArrayList<String> causes = new ArrayList<>();

        for (TextView chip : selectedCauseChips) {
            if (chip != null) {
                String text = chip.getText().toString()
                        .replace("  ✓", "")
                        .trim();
                causes.add(text);
            }
        }

        return causes;
    }

    private void setSaveLoading(boolean isLoading) {
        if (btnSaveCheckInResult != null) {
            btnSaveCheckInResult.setEnabled(!isLoading);
            btnSaveCheckInResult.setAlpha(isLoading ? 0.65f : 1f);
        }

        if (txtSaveCheckInResult != null) {
            txtSaveCheckInResult.setText(
                    isLoading ? "Đang lưu check-in..." : "Lưu & Nhận gợi ý trị liệu"
            );
        }
    }

    private void saveCheckInToFirestore() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    CheckInResultActivity.this,
                    "Bạn cần đăng nhập để lưu check-in nha",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        setSaveLoading(true);

        String note = "";
        if (edtResultNote != null) {
            note = edtResultNote.getText().toString().trim();
        }

        ArrayList<String> causes = sanitizeStringList(getSelectedCauses());
        ArrayList<String> recommendations = sanitizeStringList(getTherapyRecommendations());

        boolean aiAnalysis = isAiSource(source);
        String normalizedSource = normalizeSource(source);
        String normalizedMoodTag = normalizeMoodTag(moodName, rawEmotionLabel);
        String normalizedRawEmotionLabel = aiAnalysis
                ? normalizeMoodTag(moodName, rawEmotionLabel)
                : normalizedMoodTag;

        float normalizedAiConfidence = aiAnalysis ? clamp01(aiConfidence) : 0f;
        String normalizedConfidenceLevel = normalizeConfidenceLevel(
                confidenceLevel,
                normalizedAiConfidence,
                aiAnalysis
        );
        String normalizedModelName = aiAnalysis ? safeText(modelName, "MediaPipe Face Landmarker") : "manual";
        String normalizedModelVersion = aiAnalysis ? safeText(modelVersion, "unknown") : "manual";

        int energyLevel = getEnergyLevelForMood(moodName);

        Timestamp now = Timestamp.now();
        String recordId = buildMoodRecordId(now);

        MoodHistoryModel moodHistory = new MoodHistoryModel(
                recordId,
                user.getUid(),
                normalizedMoodTag,
                moodEmoji,
                moodDesc,
                moodPercent,
                energyLevel,
                normalizedSource,
                aiAnalysis,
                causes,
                note,
                now,
                now
        );

        moodHistory.setRecommendations(recommendations);
        moodHistory.setRaw_emotion_label(normalizedRawEmotionLabel);
        moodHistory.setAi_confidence(normalizedAiConfidence);
        moodHistory.setModel_name(normalizedModelName);
        moodHistory.setModel_version(normalizedModelVersion);
        moodHistory.setConfidence_level(normalizedConfidenceLevel);

        firestore.collection("users")
                .document(user.getUid())
                .collection("mood_history")
                .document(recordId)
                .set(moodHistory)
                .addOnSuccessListener(unused -> {
                    saveTodayConfirmedMoodLocally();

                    Toast.makeText(
                            CheckInResultActivity.this,
                            "Heami đã lưu một check-in mới 💗",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(CheckInResultActivity.this, HomeActivity.class);
                    intent.putExtra("refresh_mood_today", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setSaveLoading(false);
                    Toast.makeText(
                            CheckInResultActivity.this,
                            "Lưu check-in thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void bindTherapySuggestions() {
        String musicTitle = "Nghe nhạc";
        String musicDesc = "5 phút · Nhạc thư\ngiãn";

        String breathTitle = "Hít thở";
        String breathDesc = "3 phút · 4-7-8";

        String journalTitle = "Nhật ký";
        String journalDesc = "Viết điều bạn cảm";

        switch (moodName) {
            case "Bình thường":
                musicTitle = "Nhạc nhẹ";
                musicDesc = "5 phút · Giữ cân bằng";

                breathTitle = "Thở duy trì";
                breathDesc = "2 phút · Nhẹ nhàng";

                journalTitle = "Nhật ký hôm nay";
                journalDesc = "Ghi lại điều nhỏ";
                break;

            case "Vui vẻ":
                musicTitle = "Nhạc tích cực";
                musicDesc = "5 phút · Giữ năng lượng";

                breathTitle = "Thở biết ơn";
                breathDesc = "2 phút · Chậm rãi";

                journalTitle = "Nhật ký vui";
                journalDesc = "Lưu khoảnh khắc đẹp";
                break;

            case "Buồn":
                musicTitle = "Nhạc chữa lành";
                musicDesc = "7 phút · Dịu tâm trí";

                breathTitle = "Thở an ủi";
                breathDesc = "3 phút · Nhẹ nhàng";

                journalTitle = "Viết ra nỗi buồn";
                journalDesc = "Không cần phải ổn ngay";
                break;

            case "Căng thẳng":
                musicTitle = "Nhạc thư giãn";
                musicDesc = "5 phút · Giảm áp lực";

                breathTitle = "Hít thở 4-7-8";
                breathDesc = "3 phút · Thả lỏng";

                journalTitle = "Gỡ rối suy nghĩ";
                journalDesc = "Viết điều đang lo";
                break;

            case "Tức giận":
                musicTitle = "Âm thanh xả giận";
                musicDesc = "5 phút · Hạ nhiệt";

                breathTitle = "Thở chậm";
                breathDesc = "3 phút · Bình tĩnh lại";

                journalTitle = "Viết không gửi";
                journalDesc = "Xả cảm xúc an toàn";
                break;

            case "Lo lắng":
            case "Sợ hãi":
                musicTitle = "Âm thanh an toàn";
                musicDesc = "6 phút · Grounding";

                breathTitle = "Thở neo tâm";
                breathDesc = "3 phút · 5-4-3-2-1";

                journalTitle = "Điều mình kiểm soát";
                journalDesc = "Viết 3 điều nhỏ";
                break;

            case "Khó chịu":
            case "Ghê tởm":
                musicTitle = "Âm thanh nghỉ ngơi";
                musicDesc = "5 phút · Làm dịu cơ thể";

                breathTitle = "Thở làm sạch";
                breathDesc = "3 phút · Buông nhẹ";

                journalTitle = "Chăm sóc bản thân";
                journalDesc = "Cơ thể cần gì?";
                break;
        }

        if (txtTherapyMusicTitle != null) txtTherapyMusicTitle.setText(musicTitle);
        if (txtTherapyMusicDesc != null) txtTherapyMusicDesc.setText(musicDesc);

        if (txtTherapyBreathTitle != null) txtTherapyBreathTitle.setText(breathTitle);
        if (txtTherapyBreathDesc != null) txtTherapyBreathDesc.setText(breathDesc);

        if (txtTherapyJournalTitle != null) txtTherapyJournalTitle.setText(journalTitle);
        if (txtTherapyJournalDesc != null) txtTherapyJournalDesc.setText(journalDesc);
    }

    private ArrayList<String> getTherapyRecommendations() {
        ArrayList<String> recommendations = new ArrayList<>();

        if (txtTherapyMusicTitle != null) {
            recommendations.add(txtTherapyMusicTitle.getText().toString());
        }

        if (txtTherapyBreathTitle != null) {
            recommendations.add(txtTherapyBreathTitle.getText().toString());
        }

        if (txtTherapyJournalTitle != null) {
            recommendations.add(txtTherapyJournalTitle.getText().toString());
        }

        return recommendations;
    }
}

