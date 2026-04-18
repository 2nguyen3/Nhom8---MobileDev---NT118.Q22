package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.checkin.CheckInConstants;

public class CheckInAiActivity extends AppCompatActivity {

    private ImageButton btnBackCheckInAi;

    private View imgCheckInHeami;
    private View cardCheckInBubble;
    private View cardCameraPreview;
    private View imgCheckInCameraPreview;

    private View viewCheckInScanLine;
    private View viewCheckInScanGlow;

    private View glowCheckInTopLeft;
    private View glowCheckInBottomRight;

    private View decorCheckInFlowerTopLeft;
    private View decorCheckInFlowerTopRight;
    private View decorCheckInFlowerMiddleLeft;
    private View decorCheckInFlowerMiddleRight;
    private View decorCheckInFlowerBottomRight;

    private LinearLayout btnManualMood;
    private TextView txtCheckInAiLabel;
    private TextView txtCheckInAiTitle;
    private TextView txtCheckInInstruction;
    private TextView txtManualMoodHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin_ai);

        bindViews();
        setupActions();
        startCheckInAnimations();
    }

    private void bindViews() {
        btnBackCheckInAi = findViewById(R.id.btnBackCheckInAi);

        imgCheckInHeami = findViewById(R.id.imgCheckInHeami);
        cardCheckInBubble = findViewById(R.id.cardCheckInBubble);
        cardCameraPreview = findViewById(R.id.cardCameraPreview);
        imgCheckInCameraPreview = findViewById(R.id.imgCheckInCameraPreview);

        viewCheckInScanLine = findViewById(R.id.viewCheckInScanLine);
        viewCheckInScanGlow = findViewById(R.id.viewCheckInScanGlow);

        glowCheckInTopLeft = findViewById(R.id.glowCheckInTopLeft);
        glowCheckInBottomRight = findViewById(R.id.glowCheckInBottomRight);

        decorCheckInFlowerTopLeft = findViewById(R.id.decorCheckInFlowerTopLeft);
        decorCheckInFlowerTopRight = findViewById(R.id.decorCheckInFlowerTopRight);
        decorCheckInFlowerMiddleLeft = findViewById(R.id.decorCheckInFlowerMiddleLeft);
        decorCheckInFlowerMiddleRight = findViewById(R.id.decorCheckInFlowerMiddleRight);
        decorCheckInFlowerBottomRight = findViewById(R.id.decorCheckInFlowerBottomRight);

        btnManualMood = findViewById(R.id.btnManualMood);
        txtCheckInAiLabel = findViewById(R.id.txtCheckInAiLabel);
        txtCheckInAiTitle = findViewById(R.id.txtCheckInAiTitle);
        txtCheckInInstruction = findViewById(R.id.txtCheckInInstruction);
        txtManualMoodHint = findViewById(R.id.txtManualMoodHint);
    }

    private void setupActions() {
        if (btnBackCheckInAi != null) {
            btnBackCheckInAi.setOnClickListener(v -> finish());
        }

        if (btnManualMood != null) {
            btnManualMood.setOnClickListener(v -> {
                Intent intent = new Intent(CheckInAiActivity.this, ManualMoodActivity.class);
                startActivity(intent);
            });
        }

        if (cardCameraPreview != null) {
            cardCameraPreview.setOnClickListener(v -> openResultFromAiScan());
        }

        if (imgCheckInCameraPreview != null) {
            imgCheckInCameraPreview.setOnClickListener(v -> openResultFromAiScan());
        }
    }

    private void startCheckInAnimations() {
        startFloatY(imgCheckInHeami, 5f, 4200, 0);
        startFloatY(cardCheckInBubble, 3f, 4600, 250);

        startCameraBreath(cardCameraPreview);
        startFloatY(imgCheckInCameraPreview, 3f, 4200, 300);

        startScan(viewCheckInScanLine, viewCheckInScanGlow);

        startGlowBreath(glowCheckInTopLeft, 0.55f, 0.78f, 5200, 0);
        startGlowBreath(glowCheckInBottomRight, 0.48f, 0.72f, 5600, 800);

        startFlowerFloat(decorCheckInFlowerTopLeft, 7f, 10f, 5200, 0);
        startFlowerFloat(decorCheckInFlowerTopRight, 6f, -8f, 5000, 500);
        startFlowerFloat(decorCheckInFlowerMiddleLeft, 8f, 12f, 5600, 900);
        startFlowerFloat(decorCheckInFlowerMiddleRight, 6f, -10f, 5300, 1300);
        startFlowerFloat(decorCheckInFlowerBottomRight, 7f, 8f, 5400, 1800);

        startSubtleButtonBreath(btnManualMood);
        startAlphaBreath(txtManualMoodHint, 0.72f, 1.0f, 2400);

        startEntranceFadeUp(txtCheckInAiLabel, 0);
        startEntranceFadeUp(txtCheckInAiTitle, 80);
        startEntranceFadeUp(cardCheckInBubble, 160);
        startEntranceFadeUp(cardCameraPreview, 240);
        startEntranceFadeUp(txtCheckInInstruction, 320);
        startEntranceFadeUp(btnManualMood, 420);
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

    private void startCameraBreath(View view) {
        if (view == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.015f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.015f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.96f, 1.0f, 0.96f);

        scaleX.setDuration(2800);
        scaleY.setDuration(2800);
        alpha.setDuration(2800);

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

    private void startScan(View scanLine, View scanGlow) {
        if (scanLine == null || scanGlow == null) return;

        float distancePx = 95f * getResources().getDisplayMetrics().density;

        ObjectAnimator lineMove = ObjectAnimator.ofFloat(
                scanLine,
                View.TRANSLATION_Y,
                -distancePx,
                0f,
                distancePx
        );
        ObjectAnimator lineAlpha = ObjectAnimator.ofFloat(
                scanLine,
                View.ALPHA,
                0f,
                0.95f,
                0.95f,
                0f
        );

        ObjectAnimator glowMove = ObjectAnimator.ofFloat(
                scanGlow,
                View.TRANSLATION_Y,
                -distancePx,
                0f,
                distancePx
        );
        ObjectAnimator glowAlpha = ObjectAnimator.ofFloat(
                scanGlow,
                View.ALPHA,
                0f,
                0.65f,
                0.65f,
                0f
        );

        lineMove.setDuration(3600);
        lineAlpha.setDuration(3600);
        glowMove.setDuration(3600);
        glowAlpha.setDuration(3600);

        lineMove.setRepeatCount(ValueAnimator.INFINITE);
        lineAlpha.setRepeatCount(ValueAnimator.INFINITE);
        glowMove.setRepeatCount(ValueAnimator.INFINITE);
        glowAlpha.setRepeatCount(ValueAnimator.INFINITE);

        lineMove.setInterpolator(new AccelerateDecelerateInterpolator());
        lineAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
        glowMove.setInterpolator(new AccelerateDecelerateInterpolator());
        glowAlpha.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(lineMove, lineAlpha, glowMove, glowAlpha);
        set.start();
    }

    private void startGlowBreath(View view, float fromAlpha, float toAlpha, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, fromAlpha, toAlpha, fromAlpha);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.98f, 1.05f, 0.98f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.98f, 1.05f, 0.98f);

        alpha.setDuration(duration);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);

        alpha.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);

        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY);
        set.start();
    }

    private void startFlowerFloat(View view, float dpDistance, float rotationDeg, long duration, long delay) {
        if (view == null) return;

        float distancePx = dpDistance * getResources().getDisplayMetrics().density;

        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -distancePx, 0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, rotationDeg, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.65f, 1.0f, 0.65f);

        moveY.setDuration(duration);
        rotate.setDuration(duration);
        alpha.setDuration(duration);

        moveY.setStartDelay(delay);
        rotate.setStartDelay(delay);
        alpha.setStartDelay(delay);

        moveY.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveY, rotate, alpha);
        set.start();
    }

    private void startSubtleButtonBreath(View view) {
        if (view == null) return;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.94f, 1.0f, 0.94f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.012f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.012f, 1.0f);

        alpha.setDuration(2200);
        scaleX.setDuration(2200);
        scaleY.setDuration(2200);

        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY);
        set.start();
    }

    private void startAlphaBreath(View view, float fromAlpha, float toAlpha, long duration) {
        if (view == null) return;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, fromAlpha, toAlpha, fromAlpha);
        alpha.setDuration(duration);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.start();
    }

    private void startEntranceFadeUp(View view, long delay) {
        if (view == null) return;

        float distancePx = 10f * getResources().getDisplayMetrics().density;

        view.setAlpha(0f);
        view.setTranslationY(distancePx);

        ObjectAnimator fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
        ObjectAnimator move = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, distancePx, 0f);

        fade.setDuration(420);
        move.setDuration(420);

        fade.setStartDelay(delay);
        move.setStartDelay(delay);

        fade.setInterpolator(new AccelerateDecelerateInterpolator());
        move.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fade, move);
        set.start();
    }

    private void openResultFromAiScan() {
        Intent intent = new Intent(CheckInAiActivity.this, CheckInResultActivity.class);

        String moodTag = CheckInConstants.MOOD_STRESS;
        double confidence = 0.87;
        String aiAnalysis = "AI nhận thấy bạn có dấu hiệu căng thẳng nhẹ hôm nay.";

        intent.putExtra("mood_name", "Căng thẳng");
        intent.putExtra("mood_emoji", "😤");
        intent.putExtra("mood_desc", "Hơi nhiều áp lực hôm nay...");
        intent.putExtra("mood_percent", (int) (confidence * 100));
        intent.putExtra(CheckInConstants.EXTRA_MOOD_TAG, moodTag);
        intent.putExtra(CheckInConstants.EXTRA_CONFIDENCE, confidence);
        intent.putExtra(CheckInConstants.EXTRA_AI_ANALYSIS, aiAnalysis);
        intent.putExtra(CheckInConstants.EXTRA_SOURCE, CheckInConstants.SOURCE_AI);

        startActivity(intent);
    }
}
