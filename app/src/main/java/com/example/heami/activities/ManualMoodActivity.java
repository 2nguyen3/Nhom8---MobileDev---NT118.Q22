package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.checkin.CheckInConstants;

public class ManualMoodActivity extends AppCompatActivity {

    private ImageButton btnBackManualMood;

    private View glowManualMoodTopLeft;
    private View glowManualMoodBottomRight;

    private View layoutManualMoodHeader;
    private View layoutManualMoodCloudWrap;
    private View imgManualMoodCloud;
    private View layoutManualMoodGrid;

    private View cardMoodStress;
    private View cardMoodFear;
    private View cardMoodHappy;
    private View cardMoodSad;
    private View cardMoodDisgust;
    private View cardMoodAngry;

    private View selectedMoodCard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_mood);

        bindViews();
        setupActions();
        startManualMoodAnimations();
    }

    private void bindViews() {
        btnBackManualMood = findViewById(R.id.btnBackManualMood);

        glowManualMoodTopLeft = findViewById(R.id.glowManualMoodTopLeft);
        glowManualMoodBottomRight = findViewById(R.id.glowManualMoodBottomRight);

        layoutManualMoodHeader = findViewById(R.id.layoutManualMoodHeader);
        layoutManualMoodCloudWrap = findViewById(R.id.layoutManualMoodCloudWrap);
        imgManualMoodCloud = findViewById(R.id.imgManualMoodCloud);
        layoutManualMoodGrid = findViewById(R.id.layoutManualMoodGrid);

        cardMoodStress = findViewById(R.id.cardMoodStress);
        cardMoodFear = findViewById(R.id.cardMoodFear);
        cardMoodHappy = findViewById(R.id.cardMoodHappy);
        cardMoodSad = findViewById(R.id.cardMoodSad);
        cardMoodDisgust = findViewById(R.id.cardMoodDisgust);
        cardMoodAngry = findViewById(R.id.cardMoodAngry);
    }

    private void setupActions() {
        if (btnBackManualMood != null) {
            btnBackManualMood.setOnClickListener(v -> finish());
        }

        setupMoodClick(cardMoodStress, "Căng thẳng", CheckInConstants.MOOD_STRESS);
        setupMoodClick(cardMoodFear, "Sợ hãi", CheckInConstants.MOOD_FEAR);
        setupMoodClick(cardMoodHappy, "Vui vẻ", CheckInConstants.MOOD_HAPPY);
        setupMoodClick(cardMoodSad, "Buồn", CheckInConstants.MOOD_SAD);
        setupMoodClick(cardMoodDisgust, "Ghê tởm", CheckInConstants.MOOD_DISGUST);
        setupMoodClick(cardMoodAngry, "Tức giận", CheckInConstants.MOOD_ANGRY);
    }

    private void setupMoodClick(View card, String moodName, String moodTag) {
        if (card == null) return;

        card.setOnClickListener(v -> {
            selectMoodCard(card);

            Intent intent = new Intent(ManualMoodActivity.this, CheckInResultActivity.class);
            intent.putExtra("mood_name", moodName);
            intent.putExtra("mood_emoji", getMoodEmoji(moodName));
            intent.putExtra("mood_desc", getMoodDescription(moodName));
            intent.putExtra("mood_percent", 100);
            intent.putExtra(CheckInConstants.EXTRA_MOOD_TAG, moodTag);
            intent.putExtra(CheckInConstants.EXTRA_CONFIDENCE, 1.0);
            intent.putExtra(CheckInConstants.EXTRA_AI_ANALYSIS, "Bạn đã chọn cảm xúc thủ công.");
            intent.putExtra(CheckInConstants.EXTRA_SOURCE, CheckInConstants.SOURCE_MANUAL);

            startActivity(intent);
        });
    }

    private void selectMoodCard(View selectedCard) {
        selectedMoodCard = selectedCard;

        resetMoodCard(cardMoodStress);
        resetMoodCard(cardMoodFear);
        resetMoodCard(cardMoodHappy);
        resetMoodCard(cardMoodSad);
        resetMoodCard(cardMoodDisgust);
        resetMoodCard(cardMoodAngry);

        selectedCard.setAlpha(1f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(selectedCard, View.SCALE_X, 1f, 1.04f, 1.02f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(selectedCard, View.SCALE_Y, 1f, 1.04f, 1.02f);

        scaleX.setDuration(220);
        scaleY.setDuration(220);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.start();
    }

    private void resetMoodCard(View card) {
        if (card == null) return;

        if (card == selectedMoodCard) return;

        card.animate()
                .alpha(0.76f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .start();
    }

    private void startManualMoodAnimations() {
        startGlowBreath(glowManualMoodTopLeft, 0.48f, 0.72f, 5200, 0);
        startGlowBreath(glowManualMoodBottomRight, 0.42f, 0.66f, 5600, 700);

        startFloatY(imgManualMoodCloud, 5f, 4200, 0);

        startEntranceFadeUp(layoutManualMoodHeader, 0);
        startEntranceFadeUp(layoutManualMoodCloudWrap, 130);
        startEntranceFadeUp(layoutManualMoodGrid, 240);

        startMoodCardEntrance(cardMoodStress, 260);
        startMoodCardEntrance(cardMoodFear, 330);
        startMoodCardEntrance(cardMoodHappy, 400);
        startMoodCardEntrance(cardMoodSad, 470);
        startMoodCardEntrance(cardMoodDisgust, 540);
        startMoodCardEntrance(cardMoodAngry, 610);
    }

    private void startMoodCardEntrance(View view, long delay) {
        if (view == null) return;

        float distancePx = 14f * getResources().getDisplayMetrics().density;

        view.setAlpha(0f);
        view.setTranslationY(distancePx);
        view.setScaleX(0.96f);
        view.setScaleY(0.96f);

        ObjectAnimator fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
        ObjectAnimator move = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, distancePx, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.96f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.96f, 1f);

        fade.setDuration(430);
        move.setDuration(430);
        scaleX.setDuration(430);
        scaleY.setDuration(430);

        fade.setStartDelay(delay);
        move.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);

        fade.setInterpolator(new AccelerateDecelerateInterpolator());
        move.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fade, move, scaleX, scaleY);
        set.start();
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

    private String getMoodEmoji(String moodName) {
        switch (moodName) {
            case "Căng thẳng":
                return "😤";
            case "Sợ hãi":
                return "😭";
            case "Vui vẻ":
                return "😊";
            case "Buồn":
                return "🥲";
            case "Ghê tởm":
                return "🤢";
            case "Tức giận":
                return "🤬";
            default:
                return "😤";
        }
    }

    private String getMoodDescription(String moodName) {
        switch (moodName) {
            case "Căng thẳng":
                return "Hơi nhiều áp lực hôm nay...";
            case "Sợ hãi":
                return "Năng lượng nhẹ nhàng, dễ chịu";
            case "Vui vẻ":
                return "Heami thấy bạn đang rất ổn!";
            case "Buồn":
                return "Hôm nay có gì nặng lòng không?";
            case "Ghê tởm":
                return "Cơ thể đang cần nghỉ ngơi...";
            case "Tức giận":
                return "Có điều gì đó đang bất an...";
            default:
                return "Hơi nhiều áp lực hôm nay...";
        }
    }
}
