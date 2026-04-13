package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashSet;
import java.util.Set;

import com.example.heami.models.MoodHistoryModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin_result);

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
        moodName = getIntent().getStringExtra("mood_name");
        moodEmoji = getIntent().getStringExtra("mood_emoji");
        moodDesc = getIntent().getStringExtra("mood_desc");
        moodPercent = getIntent().getIntExtra("mood_percent", 87);

        source = getIntent().getStringExtra("source");

        if (source == null || source.trim().isEmpty()) {
            source = "unknown";
        }

        rawEmotionLabel = getIntent().getStringExtra("raw_emotion_label");
        aiConfidence = getIntent().getFloatExtra("ai_confidence", 0f);
        modelName = getIntent().getStringExtra("model_name");
        modelVersion = getIntent().getStringExtra("model_version");
        confidenceLevel = getConfidenceLevel(aiConfidence);
        moodDesc = getSoftMoodDescIfLowConfidence(moodName, moodDesc);

        if (rawEmotionLabel == null || rawEmotionLabel.trim().isEmpty()) {
            rawEmotionLabel = "unknown";
        }

        if (modelName == null || modelName.trim().isEmpty()) {
            modelName = "unknown";
        }

        if (modelVersion == null || modelVersion.trim().isEmpty()) {
            modelVersion = "unknown";
        }

        if (moodName == null || moodName.trim().isEmpty()) {
            moodName = "Căng thẳng";
        }

        if (moodEmoji == null || moodEmoji.trim().isEmpty()) {
            moodEmoji = "😤";
        }

        if (moodDesc == null || moodDesc.trim().isEmpty()) {
            moodDesc = "Hơi nhiều áp lực hôm nay...";
        }

        if (moodPercent < 0) {
            moodPercent = 0;
        }

        if (moodPercent > 100) {
            moodPercent = 100;
        }

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

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    progressWidth,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );

            viewResultMoodProgress.setLayoutParams(params);
        });
    }

    private String getConfidenceLevel(float confidence) {
        if (confidence >= 0.75f) {
            return "high";
        }

        if (confidence >= 0.50f) {
            return "medium";
        }

        return "low";
    }

    private String getHeamiMessage(String moodName) {
        switch (moodName) {
            case "Căng thẳng":
                return "Heami thấy bạn đang mang nhiều áp lực hôm nay. Hãy để Heami cùng bạn thở nhẹ một chút nhé";

            case "Sợ hãi":
                return "Heami cảm nhận bạn đang cần một cảm giác an toàn hơn. Mình cứ đi chậm lại một chút thôi nhé";

            case "Vui vẻ":
                return "Heami thấy năng lượng của bạn hôm nay rất tươi sáng. Hãy lưu lại khoảnh khắc này nha";

            case "Buồn":
                return "Heami thấy hôm nay bạn có vẻ hơi nặng lòng. Bạn không cần phải ổn ngay lập tức đâu";

            case "Ghê tởm":
                return "Heami cảm nhận cơ thể và tâm trí bạn đang cần được nghỉ ngơi. Hãy nhẹ nhàng với bản thân hơn nhé";

            case "Tức giận":
                return "Heami thấy bên trong bạn đang có nhiều điều bị dồn nén. Mình thử hít thở chậm lại trước nha";

            default:
                return "Heami đã ghi nhận cảm xúc của bạn hôm nay. Cảm ơn bạn vì đã lắng nghe chính mình";
        }
    }

    private String getSoftMoodDescIfLowConfidence(String moodName, String originalDesc) {
        if (!source.contains("ai_camera_tflite")) {
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

            case "Sợ hãi":
                return "Heami chưa thật sự chắc chắn, nhưng thấy bạn có vẻ cần thêm cảm giác an toàn.";

            case "Ghê tởm":
                return "Heami chưa thật sự chắc chắn, nhưng cảm nhận cơ thể bạn có vẻ đang không thoải mái.";

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

    private void saveCheckInToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    CheckInResultActivity.this,
                    "Bạn cần đăng nhập để lưu check-in nha",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (btnSaveCheckInResult != null) {
            btnSaveCheckInResult.setEnabled(false);
            btnSaveCheckInResult.setAlpha(0.65f);
        }

        String note = "";
        if (edtResultNote != null) {
            note = edtResultNote.getText().toString().trim();
        }

        ArrayList<String> causes = getSelectedCauses();
        ArrayList<String> recommendations = getTherapyRecommendations();

        String recordId = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(new Date());

        Timestamp now = Timestamp.now();

        MoodHistoryModel moodHistory = new MoodHistoryModel(
                recordId,
                user.getUid(),
                moodName,
                moodEmoji,
                moodDesc,
                moodPercent,
                moodPercent,
                source,
                source != null && source.contains("ai"),
                causes,
                note,
                now,
                now
        );

        moodHistory.setRecommendations(recommendations);

        moodHistory.setRaw_emotion_label(rawEmotionLabel);
        moodHistory.setAi_confidence(aiConfidence);
        moodHistory.setModel_name(modelName);
        moodHistory.setModel_version(modelVersion);
        moodHistory.setConfidence_level(confidenceLevel);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("mood_history")
                .document(recordId)
                .set(moodHistory)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            CheckInResultActivity.this,
                            "Heami đã lưu check-in hôm nay 💗",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(CheckInResultActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (btnSaveCheckInResult != null) {
                        btnSaveCheckInResult.setEnabled(true);
                        btnSaveCheckInResult.setAlpha(1f);
                    }

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

            case "Sợ hãi":
                musicTitle = "Âm thanh an toàn";
                musicDesc = "6 phút · Grounding";

                breathTitle = "Thở neo tâm";
                breathDesc = "3 phút · 5-4-3-2-1";

                journalTitle = "Điều mình kiểm soát";
                journalDesc = "Viết 3 điều nhỏ";
                break;

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