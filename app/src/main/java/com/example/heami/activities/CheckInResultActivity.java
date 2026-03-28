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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin_result);

        bindViews();
        bindResultData();
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
    }

    private void bindResultData() {
        moodName = getIntent().getStringExtra("mood_name");
        moodEmoji = getIntent().getStringExtra("mood_emoji");
        moodDesc = getIntent().getStringExtra("mood_desc");
        moodPercent = getIntent().getIntExtra("mood_percent", 87);

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
            btnSaveCheckInResult.setOnClickListener(v -> {
                Toast.makeText(
                        CheckInResultActivity.this,
                        "Heami đã lưu check-in hôm nay 💗",
                        Toast.LENGTH_SHORT
                ).show();

                Intent intent = new Intent(CheckInResultActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
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
}