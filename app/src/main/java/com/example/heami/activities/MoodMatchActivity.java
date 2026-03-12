package com.example.heami.activities;

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

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class MoodMatchActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private View layoutMoodMatchSheet;
    private View layoutStateSearching;
    private View layoutStateAnalyzing;
    private View layoutStateSuccess;
    private View viewMoodHeroRingOuter;
    private View viewMoodHeroRingInner;

    private Button btnMoodMatchCTA;
    private TextView txtBackToCommunity;

    private View moodMatchRoot;

    private final Runnable showAnalyzingRunnable = this::showAnalyzingStateAnimated;
    private final Runnable showSuccessRunnable = this::showSuccessStateAnimated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_match);

        bindViews();
        prepareSheetIntro();
        setupActions();
        showSearchingStateImmediate();
        startHeroIconPulse();
        startSheetIntroAnimation();
        startMoodMatchFlow();
    }

    private void bindViews() {
        layoutMoodMatchSheet = findViewById(R.id.layoutMoodMatchSheet);
        layoutStateSearching = findViewById(R.id.layoutStateSearching);
        layoutStateAnalyzing = findViewById(R.id.layoutStateAnalyzing);
        layoutStateSuccess = findViewById(R.id.layoutStateSuccess);
        viewMoodHeroRingOuter = findViewById(R.id.viewMoodHeroRingOuter);
        viewMoodHeroRingInner = findViewById(R.id.viewMoodHeroRingInner);
        btnMoodMatchCTA = findViewById(R.id.btnMoodMatchCTA);
        txtBackToCommunity = findViewById(R.id.txtBackToCommunity);
        moodMatchRoot = findViewById(R.id.moodMatchRoot);
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

                Intent intent = new Intent(MoodMatchActivity.this, MoodMatchChatActivity.class);
                startActivity(intent);
            });
        }

        if (txtBackToCommunity != null) {
            txtBackToCommunity.setOnClickListener(v -> finish());
        }

        if (moodMatchRoot != null) {
            moodMatchRoot.setOnClickListener(v -> finish());
        }
    }

    private void startMoodMatchFlow() {
        handler.postDelayed(showAnalyzingRunnable, 2200);
        handler.postDelayed(showSuccessRunnable, 4300);
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

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setEnabled(false);
            btnMoodMatchCTA.setAlpha(0.6f);
        }
    }

    private void showAnalyzingStateAnimated() {
        crossfadeState(layoutStateSearching, layoutStateAnalyzing);

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.setEnabled(false);
            btnMoodMatchCTA.setAlpha(0.6f);
        }
    }

    private void showSuccessStateAnimated() {
        crossfadeState(layoutStateAnalyzing, layoutStateSuccess);

        if (layoutStateSuccess != null) {
            layoutStateSuccess.setScaleX(0.94f);
            layoutStateSuccess.setScaleY(0.94f);
            layoutStateSuccess.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .start();
        }

        if (btnMoodMatchCTA != null) {
            btnMoodMatchCTA.animate()
                    .alpha(1f)
                    .setDuration(220)
                    .start();
            btnMoodMatchCTA.setEnabled(true);
        }
    }

    private void crossfadeState(View from, View to) {
        if (to != null) {
            to.setVisibility(View.VISIBLE);
            to.setAlpha(0f);
            to.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
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
        handler.removeCallbacks(showAnalyzingRunnable);
        handler.removeCallbacks(showSuccessRunnable);
    }
}