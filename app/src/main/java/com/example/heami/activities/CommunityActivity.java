package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class CommunityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        BottomNavManager.setup(this, BottomNavManager.TAB_COMMUNITY);

        allowOverflow();
        setupActions();
        setupFilters();
        startCommunityAnimations();
    }

    private void allowOverflow() {
        setClipChildrenRecursive(findViewById(R.id.communityRoot), false);
        setClipChildrenRecursive(findViewById(R.id.layoutMoodMatchCard), false);
        setClipChildrenRecursive(findViewById(R.id.layoutMoodMatchInner), false);
        setClipChildrenRecursive(findViewById(R.id.layoutMoodTop), false);
        setClipChildrenRecursive(findViewById(R.id.layoutShuffleWrap), false);
    }

    private void setClipChildrenRecursive(View view, boolean clip) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup vg = (ViewGroup) view;
        vg.setClipChildren(clip);
        vg.setClipToPadding(clip);
    }

    private void setupActions() {
        View btnBack = findViewById(R.id.btnBackCommunity);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupFilters() {
        View all = findViewById(R.id.filterTabAll);
        View happy = findViewById(R.id.filterTabHappy);
        View sad = findViewById(R.id.filterTabSad);
        View stress = findViewById(R.id.filterTabStress);
        View fear = findViewById(R.id.filterTabFear);
        View disgust = findViewById(R.id.filterTabDisgust);
        View angry = findViewById(R.id.filterTabAngry);

        if (all != null) all.setOnClickListener(v -> applyFilter("all"));
        if (happy != null) happy.setOnClickListener(v -> applyFilter("happy"));
        if (sad != null) sad.setOnClickListener(v -> applyFilter("sad"));
        if (stress != null) stress.setOnClickListener(v -> applyFilter("stress"));
        if (fear != null) fear.setOnClickListener(v -> applyFilter("fear"));
        if (disgust != null) disgust.setOnClickListener(v -> applyFilter("disgust"));
        if (angry != null) angry.setOnClickListener(v -> applyFilter("angry"));

        applyFilter("all");
    }

    private void applyFilter(String filter) {
        updateFilterTabStyles(filter);

        View post1 = findViewById(R.id.postCard1); // sad
        View post2 = findViewById(R.id.postCard2); // stress
        View post3 = findViewById(R.id.postCard3); // happy
        View post4 = findViewById(R.id.postCard4); // sad
        View post5 = findViewById(R.id.postCard5); // happy
        View post6 = findViewById(R.id.postCard6); // stress

        setPostVisible(post1, filter.equals("all") || filter.equals("sad"));
        setPostVisible(post2, filter.equals("all") || filter.equals("stress"));
        setPostVisible(post3, filter.equals("all") || filter.equals("happy"));
        setPostVisible(post4, filter.equals("all") || filter.equals("sad"));
        setPostVisible(post5, filter.equals("all") || filter.equals("happy"));
        setPostVisible(post6, filter.equals("all") || filter.equals("stress"));
    }

    private void setPostVisible(View view, boolean visible) {
        if (view == null) return;
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateFilterTabStyles(String activeFilter) {
        updateSingleFilterTab(R.id.filterTabAll, activeFilter.equals("all"));
        updateSingleFilterTab(R.id.filterTabHappy, activeFilter.equals("happy"));
        updateSingleFilterTab(R.id.filterTabSad, activeFilter.equals("sad"));
        updateSingleFilterTab(R.id.filterTabStress, activeFilter.equals("stress"));
        updateSingleFilterTab(R.id.filterTabFear, activeFilter.equals("fear"));
        updateSingleFilterTab(R.id.filterTabDisgust, activeFilter.equals("disgust"));
        updateSingleFilterTab(R.id.filterTabAngry, activeFilter.equals("angry"));
    }

    private void updateSingleFilterTab(int viewId, boolean isActive) {
        TextView tab = findViewById(viewId);
        if (tab == null) return;

        if (isActive) {
            tab.setBackgroundResource(R.drawable.bg_filter_tab_active);
            tab.setTextColor(0xFFFFFFFF);
        } else {
            tab.setBackgroundResource(R.drawable.bg_filter_tab);
            tab.setTextColor(0xFFC0A8D0);
        }
    }

    private void startCommunityAnimations() {
        // Background blobs
        startBlobFloat(findViewById(R.id.blobRoseTop), -10f, 8f, 1.04f, 8000, 0);
        startBlobFloat(findViewById(R.id.blobVioletMid), 8f, -10f, 1.05f, 9000, 400);
        startBlobFloat(findViewById(R.id.blobCyanMid), -8f, 10f, 1.05f, 8600, 700);

        // Online badge
        startSubtleFloat(findViewById(R.id.layoutOnlineBadge), 1.5f, 4200, 0);
        startPulseDot(findViewById(R.id.viewOnlineDot), 0.92f, 1.18f, 1900);

        // Mood match card
        startPulseFadeExpand(findViewById(R.id.viewShuffleRingOuter), 0.82f, 1.32f, 0.22f, 0f, 1800, 0);
        startPulseFadeExpand(findViewById(R.id.viewShuffleRingInner), 0.90f, 1.22f, 0.30f, 0f, 1350, 120);

        startFloatRotateScale(findViewById(R.id.layoutShuffleIconBox), 3f, 3f, 1.05f, 1900, 0);
        startPulseDot(findViewById(R.id.viewMoodActiveDot), 0.92f, 1.25f, 1150);

        // Sparkles
        startSparkle(findViewById(R.id.imgSparkle1), 1.7f, 0);
        startSparkle(findViewById(R.id.imgSparkle2), 1.7f, 200);
        startSparkle(findViewById(R.id.imgSparkle3), 1.7f, 450);
        startSparkle(findViewById(R.id.imgSparkle4), 1.7f, 700);

        // Stat chips
        startStatBreath(findViewById(R.id.statChip1), 4400, 0);
        startStatBreath(findViewById(R.id.statChip2), 4400, 350);
        startStatBreath(findViewById(R.id.statChip3), 4400, 700);
    }

    private void startBlobFloat(View view, float dxDp, float dyDp, float scaleTo, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator moveX = ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_X,
                view.getTranslationX(),
                view.getTranslationX() + dp(dxDp),
                view.getTranslationX()
        );
        ObjectAnimator moveY = ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_Y,
                view.getTranslationY(),
                view.getTranslationY() + dp(dyDp),
                view.getTranslationY()
        );
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, scaleTo, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, scaleTo, 1f);

        moveX.setDuration(duration);
        moveY.setDuration(duration);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);

        moveX.setStartDelay(delay);
        moveY.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);

        moveX.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        moveX.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveX, moveY, scaleX, scaleY);
        set.start();
    }

    private void startSubtleFloat(View view, float distanceDp, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -dp(distanceDp), 0f);
        moveY.setDuration(duration);
        moveY.setStartDelay(delay);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.start();
    }

    private void startPulseDot(View view, float fromScale, float toScale, long duration) {
        if (view == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, fromScale, toScale, fromScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, fromScale, toScale, fromScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.55f, 1f, 0.55f);

        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);

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

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, 1.08f, endScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, 1.08f, endScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, startAlpha, 0.58f, endAlpha);

        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);

        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

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

    private void startFloatRotateScale(View view, float floatDp, float rotateDeg, float scaleTo, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -dp(floatDp), 0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, rotateDeg, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, scaleTo, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, scaleTo, 1f);

        moveY.setDuration(duration);
        rotate.setDuration(duration);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);

        moveY.setStartDelay(delay);
        rotate.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);

        moveY.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveY, rotate, scaleX, scaleY);
        set.start();
    }

    private void startSparkle(View view, float durationSeconds, long delay) {
        if (view == null) return;

        long duration = (long) (durationSeconds * 1000);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.15f, 0.55f, 1f, 0.45f, 0.15f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.72f, 0.90f, 1.08f, 0.85f, 0.72f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.72f, 0.90f, 1.08f, 0.85f, 0.72f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 8f, 18f, 6f, 0f);

        alpha.setDuration(duration);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        rotate.setDuration(duration);

        alpha.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        rotate.setStartDelay(delay);

        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);

        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY, rotate);
        set.start();
    }

    private void startStatBreath(View view, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -dp(1f), 0f);
        moveY.setDuration(duration);
        moveY.setStartDelay(delay);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}