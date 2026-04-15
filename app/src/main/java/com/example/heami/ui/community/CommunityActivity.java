package com.example.heami.ui.community;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.ui.main.BottomNavManager;

public class CommunityActivity extends AppCompatActivity {

    private LinearLayout layoutPostsContainer;

    private View btnHugPost1;
    private TextView txtHugCountPost1;
    private boolean isHuggedPost1 = false;
    private int hugCountPost1 = 24;

    private View btnCommentPost1;
    private TextView txtCommentCountPost1;

    private View btnEmpathyPost1;
    private boolean isEmpathyPost1Active = false;

    private View btnReportPost1;

    private View btnCommunityChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        BottomNavManager.setup(this, BottomNavManager.TAB_COMMUNITY);

        bindViews();
        allowOverflow();
        setupActions();
        setupFilters();
        setupHugActions();
        setupCommentActions();
        setupEmpathyActions();
        setupReportActions();
        handleSharedPostIntent();
        startCommunityAnimations();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSharedPostIntent();
    }

    private void bindViews() {
        layoutPostsContainer = findViewById(R.id.layoutPostsContainer);

        btnHugPost1 = findViewById(R.id.btnHugPost1);
        txtHugCountPost1 = findViewById(R.id.txtHugCountPost1);

        btnCommentPost1 = findViewById(R.id.btnCommentPost1);
        txtCommentCountPost1 = findViewById(R.id.txtCommentCountPost1);

        btnEmpathyPost1 = findViewById(R.id.btnEmpathyPost1);

        btnReportPost1 = findViewById(R.id.btnReportPost1);

        btnCommunityChat = findViewById(R.id.btnCommunityChat);
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
        View moodMatchCard = findViewById(R.id.layoutMoodMatchCard);
        if (moodMatchCard != null) {
            moodMatchCard.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, MoodMatchActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        View btnShareYourFeeling = findViewById(R.id.btnShareYourFeeling);
        if (btnShareYourFeeling != null) {
            btnShareYourFeeling.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, ShareFeelingActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        if (btnCommunityChat != null) {
            btnCommunityChat.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, CommunityChatListActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
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

    private void handleSharedPostIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        boolean fromShareFeeling = intent.getBooleanExtra("from_share_feeling", false);
        if (!fromShareFeeling) return;

        String content = intent.getStringExtra("new_post_content");
        String mood = intent.getStringExtra("new_post_mood");

        if (content == null || content.trim().isEmpty()) return;

        addNewSharedPost(content, mood);

        intent.removeExtra("from_share_feeling");
        intent.removeExtra("new_post_content");
        intent.removeExtra("new_post_mood");
    }

    private void addNewSharedPost(String content, String mood) {
        if (layoutPostsContainer == null) return;

        View postView = getLayoutInflater().inflate(R.layout.item_post_shared, layoutPostsContainer, false);

        View accent = postView.findViewById(R.id.viewPostAccent);
        View avatarStatus = postView.findViewById(R.id.viewPostAvatarStatus);

        TextView txtAvatarEmoji = postView.findViewById(R.id.txtPostAvatarEmoji);
        TextView txtUserName = postView.findViewById(R.id.txtPostUserName);
        TextView txtMoodTag = postView.findViewById(R.id.txtPostMoodTag);
        TextView txtPostTime = postView.findViewById(R.id.txtPostTime);
        TextView txtPostContent = postView.findViewById(R.id.txtPostContent);
        TextView txtPostHugCount = postView.findViewById(R.id.txtPostHugCount);
        TextView txtPostCommentCount = postView.findViewById(R.id.txtPostCommentCount);

        txtUserName.setText(getAnonymousNameByMood(mood));
        txtMoodTag.setText(getMoodLabel(mood));
        txtMoodTag.setBackgroundResource(getMoodTagBackground(mood));
        txtMoodTag.setTextColor(getMoodTextColor(mood));
        txtPostTime.setText("· Vừa xong");
        txtPostContent.setText(content);
        txtPostHugCount.setText("Ôm 0");
        txtPostCommentCount.setText("0");

        applyPostMoodStyle(mood, accent, txtAvatarEmoji, avatarStatus);

        layoutPostsContainer.addView(postView, 0);
    }

    private void applyPostMoodStyle(String mood, View accent, TextView avatarEmoji, View avatarStatus) {
        if (accent == null || avatarEmoji == null || avatarStatus == null) return;

        if (mood == null) mood = "stress";

        switch (mood) {
            case "happy":
                accent.setBackgroundResource(R.drawable.bg_post_accent_orange);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_orange);
                avatarEmoji.setText("🌻");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_orange);
                break;

            case "sad":
                accent.setBackgroundResource(R.drawable.bg_post_accent_blue);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_blue);
                avatarEmoji.setText("🌧️");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_blue);
                break;

            case "stress":
                accent.setBackgroundResource(R.drawable.bg_post_accent_purple);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_purple);
                avatarEmoji.setText("🌪️");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_purple);
                break;

            case "fear":
                accent.setBackgroundResource(R.drawable.bg_post_accent_mint);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_mint);
                avatarEmoji.setText("😰");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_mint);
                break;

            case "disgust":
                accent.setBackgroundResource(R.drawable.bg_post_accent_green);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_green);
                avatarEmoji.setText("🤢");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_green);
                break;

            case "angry":
                accent.setBackgroundResource(R.drawable.bg_post_accent_red);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_red);
                avatarEmoji.setText("😠");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_red);
                break;

            default:
                accent.setBackgroundResource(R.drawable.bg_post_accent_purple);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_purple);
                avatarEmoji.setText("🌪️");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_purple);
                break;
        }
    }

    private String getAnonymousNameByMood(String mood) {
        if (mood == null) return "Người bạn ẩn danh";

        switch (mood) {
            case "happy":
                return "Hoa nắng nhỏ";
            case "sad":
                return "Mây chiều xanh";
            case "stress":
                return "Lá dương xỉ";
            case "fear":
                return "Sương đêm mỏng";
            case "disgust":
                return "Chiếc lá lặng";
            case "angry":
                return "Đốm lửa nhỏ";
            default:
                return "Người bạn ẩn danh";
        }
    }

    private String getMoodLabel(String mood) {
        if (mood == null) return "Cảm xúc";

        switch (mood) {
            case "happy":
                return "Đang vui";
            case "sad":
                return "Đang buồn";
            case "stress":
                return "Đang stress";
            case "fear":
                return "Đang sợ hãi";
            case "disgust":
                return "Đang ghê tởm";
            case "angry":
                return "Đang tức giận";
            default:
                return "Cảm xúc";
        }
    }

    private int getMoodTagBackground(String mood) {
        if (mood == null) return R.drawable.bg_mood_tag_stress;

        switch (mood) {
            case "happy":
                return R.drawable.bg_mood_tag_happy;
            case "sad":
                return R.drawable.bg_mood_tag_sad;
            case "stress":
                return R.drawable.bg_mood_tag_stress;
            case "fear":
                return R.drawable.bg_mood_tag_fear;
            case "disgust":
                return R.drawable.bg_mood_tag_disgust;
            case "angry":
                return R.drawable.bg_mood_tag_angry;
            default:
                return R.drawable.bg_mood_tag_stress;
        }
    }

    private int getMoodTextColor(String mood) {
        if (mood == null) return 0xFFB06ED8;

        switch (mood) {
            case "happy":
                return 0xFFF5A623;
            case "sad":
                return 0xFF6B9EE8;
            case "stress":
                return 0xFFB06ED8;
            case "fear":
                return 0xFF4BBBAD;
            case "disgust":
                return 0xFF7FA56A;
            case "angry":
                return 0xFFE49797;
            default:
                return 0xFFB06ED8;
        }
    }

    private void startCommunityAnimations() {
        startBlobFloat(findViewById(R.id.blobRoseTop), -10f, 8f, 1.04f, 8000, 0);
        startBlobFloat(findViewById(R.id.blobVioletMid), 8f, -10f, 1.05f, 9000, 400);
        startBlobFloat(findViewById(R.id.blobCyanMid), -8f, 10f, 1.05f, 8600, 700);

        startPulseFadeExpand(findViewById(R.id.viewShuffleRingOuter), 0.82f, 1.32f, 0.22f, 0f, 1800, 0);
        startPulseFadeExpand(findViewById(R.id.viewShuffleRingInner), 0.90f, 1.22f, 0.30f, 0f, 1350, 120);

        startFloatRotateScale(findViewById(R.id.layoutShuffleIconBox), 3f, 3f, 1.05f, 1900, 0);
        startPulseDot(findViewById(R.id.viewMoodActiveDot), 0.92f, 1.25f, 1150);

        startSparkle(findViewById(R.id.imgSparkle1), 1.7f, 0);
        startSparkle(findViewById(R.id.imgSparkle2), 1.7f, 200);
        startSparkle(findViewById(R.id.imgSparkle3), 1.7f, 450);
        startSparkle(findViewById(R.id.imgSparkle4), 1.7f, 700);

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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void setupHugActions() {
        if (btnHugPost1 != null) {
            btnHugPost1.setOnClickListener(v -> toggleHugPost1());
        }

        refreshHugPost1UI();
    }

    private void toggleHugPost1() {
        if (isHuggedPost1) {
            hugCountPost1--;
            isHuggedPost1 = false;
        } else {
            hugCountPost1++;
            isHuggedPost1 = true;
        }

        refreshHugPost1UI();

        if (isHuggedPost1 && btnHugPost1 != null) {
            btnHugPost1.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(120)
                    .withEndAction(() -> btnHugPost1.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start())
                    .start();
        }
    }

    private void refreshHugPost1UI() {
        if (txtHugCountPost1 != null) {
            txtHugCountPost1.setText("Ôm " + hugCountPost1);
        }

        if (btnHugPost1 != null) {
            if (isHuggedPost1) {
                btnHugPost1.setBackgroundResource(R.drawable.bg_action_btn_active);
            } else {
                btnHugPost1.setBackgroundResource(R.drawable.bg_action_btn);
            }
        }
    }

    private void setupCommentActions() {
        if (btnCommentPost1 != null) {
            btnCommentPost1.setOnClickListener(v -> openPost1Comments());
        }
    }

    private void openPost1Comments() {
        Intent intent = new Intent(CommunityActivity.this, PostCommentActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void setupEmpathyActions() {
        if (btnEmpathyPost1 != null) {
            btnEmpathyPost1.setOnClickListener(v -> toggleEmpathyPost1());
        }

        refreshEmpathyPost1UI();
    }

    private void toggleEmpathyPost1() {
        isEmpathyPost1Active = !isEmpathyPost1Active;
        refreshEmpathyPost1UI();

        if (isEmpathyPost1Active && btnEmpathyPost1 != null) {
            btnEmpathyPost1.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(120)
                    .withEndAction(() -> btnEmpathyPost1.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start())
                    .start();
        }
    }

    private void refreshEmpathyPost1UI() {
        if (btnEmpathyPost1 == null) return;

        if (isEmpathyPost1Active) {
            btnEmpathyPost1.setBackgroundResource(R.drawable.bg_empathy_btn_active);
            btnEmpathyPost1.setAlpha(1f);
        } else {
            btnEmpathyPost1.setBackgroundResource(R.drawable.bg_empathy_btn);
            btnEmpathyPost1.setAlpha(1f);
        }
    }

    private void setupReportActions() {
        if (btnReportPost1 != null) {
            btnReportPost1.setOnClickListener(v -> showReportDialog());
        }
    }

    private void showReportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_post_report, null, false);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnCancel = dialogView.findViewById(R.id.btnCancelReport);
        View btnSubmit = dialogView.findViewById(R.id.btnSubmitReport);
        android.widget.RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupReportReason);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                int checkedId = radioGroup != null ? radioGroup.getCheckedRadioButtonId() : -1;

                if (checkedId == -1) {
                    android.widget.Toast.makeText(
                            CommunityActivity.this,
                            "Bạn hãy chọn lý do báo cáo nhé",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                android.widget.Toast.makeText(
                        CommunityActivity.this,
                        "Đã gửi báo cáo. Cảm ơn bạn đã giúp giữ cộng đồng an toàn.",
                        android.widget.Toast.LENGTH_SHORT
                ).show();

                dialog.dismiss();
            });
        }

        dialog.setCancelable(true);
        dialog.show();
    }
}