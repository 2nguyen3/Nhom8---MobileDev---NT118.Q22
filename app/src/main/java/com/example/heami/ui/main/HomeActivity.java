package com.example.heami.ui.main;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;

import com.example.heami.notifications.HeamiFirebaseMessagingService;
import com.example.heami.ui.community.MoodMatchChatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.ui.checkin.CheckInAiActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.graphics.Color;
import com.bumptech.glide.Glide;
import com.example.heami.data.models.DoctorModel;
import com.example.heami.ui.consultation.DoctorActivity;
import com.example.heami.ui.consultation.DoctorDetailActivity;

import com.example.heami.utils.ExitDialogHelper;
import com.example.heami.utils.NotificationScheduler;
import com.example.heami.data.models.ConsultationModel;
import com.example.heami.ui.consultation.ConsultationsActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private BroadcastReceiver chatBannerReceiver;

    private TextView txtGreetingLabel;
    private TextView txtGreetingTitle;
    private TextView txtGreetingSubtitle;

    private TextView txtHelloTitle;
    private TextView txtHelloSub;
    private TextView txtStreakCount;

    private TextView txtAiTitle;
    private TextView txtAiSubtitle;
    private TextView txtStartAi;


    private TextView txtSchedule1;
    private TextView txtSchedule2;
    private TextView txtSchedule3;
    private TextView txtSchedule4;
    private TextView txtScheduleProgress;

    private LinearLayout layoutAiReady;
    private View checkDone1;
    private View checkEmpty2;
    private View checkEmpty3;
    private View checkEmpty4;
    private View btnStartAi;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private boolean waterDone = false;
    private boolean relaxDone = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ExitDialogHelper.registerExitHandler(this);
        NotificationScheduler.scheduleDailyCheckIn(this);

        initViews();
        BottomNavManager.setup(this, BottomNavManager.TAB_HOME);

        loadUserData();
        loadTodayMoodState();

        applyStaticStyles();
        startHomeAnimations();
        setupActions();
        loadDoctorsFromFirestore();
        loadUpcomingAppointmentsFromFirestore();

        setupNotificationPermissionLauncher();
        syncFcmToken();
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreetingLabel();
        loadTodayMoodState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        updateGreetingLabel();
        loadTodayMoodState();
        handleNotificationIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerChatBannerReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterChatBannerReceiver();
    }

    private void initViews() {
        txtGreetingLabel = findViewById(R.id.txtGreetingLabel);
        txtGreetingTitle = findViewById(R.id.txtGreetingTitle);
        txtGreetingSubtitle = findViewById(R.id.txtGreetingSubtitle);

        txtHelloTitle = findViewById(R.id.txtHelloTitle);
        txtHelloSub = findViewById(R.id.txtHelloSub);
        txtStreakCount = findViewById(R.id.txtStreakCount);

        txtAiTitle = findViewById(R.id.txtAiTitle);

        txtAiSubtitle = findViewById(R.id.txtAiSubtitle);
        txtStartAi = findViewById(R.id.txtStartAi);

        txtSchedule1 = findViewById(R.id.txtSchedule1);
        txtSchedule2 = findViewById(R.id.txtSchedule2);
        txtSchedule3 = findViewById(R.id.txtSchedule3);
        txtSchedule4 = findViewById(R.id.txtSchedule4);
        txtScheduleProgress = findViewById(R.id.txtScheduleProgress);

        layoutAiReady = findViewById(R.id.txtAiReady);

        checkDone1 = findViewById(R.id.checkDone1);
        checkEmpty2 = findViewById(R.id.checkEmpty2);
        checkEmpty3 = findViewById(R.id.checkEmpty3);
        checkEmpty4 = findViewById(R.id.checkEmpty4);

        btnStartAi = findViewById(R.id.btnStartAi);
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            com.example.heami.utils.StreakManager.checkAndResetStreakIfMissed(user.getUid(), correctedStreak -> {
                firestore.collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String nickname = documentSnapshot.getString("nickname");
                                if (nickname != null && !nickname.trim().isEmpty()) {
                                    txtGreetingTitle.setText(nickname.trim() + " ơi!");
                                }
                                Long currentStreak = documentSnapshot.getLong("current_streak");
                                if (currentStreak != null && txtStreakCount != null) {
                                    txtStreakCount.setText(String.valueOf(currentStreak));
                                }
                            }
                        });
            });
        }

        updateGreetingLabel();
    }

    private void updateGreetingLabel() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;

        // Logic 4 mốc thời gian: Sáng, Trưa, Chiều, Tối
        if (hour >= 4 && hour < 10) {
            greeting = "🌅 Chào buổi sáng,";
        } else if (hour >= 10 && hour < 13) {
            greeting = "☀️ Chào buổi trưa,";
        } else if (hour >= 13 && hour < 18) {
            greeting = "🌤️ Chào buổi chiều,";
        } else {
            greeting = "🌙 Chào buổi tối,";
        }

        if (txtGreetingLabel != null) {
            txtGreetingLabel.setText(greeting);
        }
    }

    private void loadTodayMoodState() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            applyNoCheckInState();
            applyTodaySchedule(false);
            return;
        }

        Timestamp startOfToday = getStartOfToday();
        Timestamp startOfTomorrow = getStartOfTomorrow();

        firestore.collection("users")
                .document(user.getUid())
                .collection("mood_history")
                .whereGreaterThanOrEqualTo("timestamp", startOfToday)
                .whereLessThan("timestamp", startOfTomorrow)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasMoodCheckinToday = querySnapshot != null && !querySnapshot.isEmpty();

                    if (hasMoodCheckinToday) {
                        DocumentSnapshot latestSnapshot = querySnapshot.getDocuments().get(0);
                        applyTodayMoodState(latestSnapshot);
                    } else {
                        applyNoCheckInState();
                    }

                    applyTodaySchedule(hasMoodCheckinToday);
                })
                .addOnFailureListener(e -> {
                    applyNoCheckInState();
                    applyTodaySchedule(false);
                });
    }

    private Timestamp getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp(new Date(cal.getTimeInMillis()));
    }

    private Timestamp getStartOfTomorrow() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp(new Date(cal.getTimeInMillis()));
    }

    private void applyNoCheckInState() {
        if (txtGreetingSubtitle != null) {
            txtGreetingSubtitle.setText("Hôm nay là một ngày tuyệt vời");
        }

        if (txtHelloTitle != null) {
            txtHelloTitle.setText("Xin chào, bạn!");
        }

        if (txtHelloSub != null) {
            txtHelloSub.setText("Mình ở đây cùng bạn nhé!");
        }

        if (txtAiTitle != null) {
            txtAiTitle.setText("Để Heami đọc\ntâm trạng của bạn");
        }

        if (txtAiSubtitle != null) {
            txtAiSubtitle.setText("Nhận diện cảm xúc qua khuôn mặt\nchính xác — chỉ trong 3 giây");
        }

        setAiReadyText("AI đang sẵn sàng nhận diện");

        if (txtStartAi != null) {
            txtStartAi.setText("Bắt đầu nhận diện ngay");
        }
    }

    private void applyTodayMoodState(DocumentSnapshot snapshot) {
        String moodTag = safeText(snapshot.getString("mood_tag"), "neutral");
        String moodEmoji = safeText(snapshot.getString("mood_emoji"), getMoodEmojiFallback(moodTag));
        String moodDesc = safeText(snapshot.getString("mood_desc"), getShortMoodDesc(moodTag));

        Long moodPercentLong = snapshot.getLong("mood_percent");
        int moodPercent = moodPercentLong != null ? moodPercentLong.intValue() : 60;
        if (moodPercent < 0) moodPercent = 0;
        if (moodPercent > 100) moodPercent = 100;

        String source = safeText(snapshot.getString("source"), "manual_checkin");

        String moodName = mapMoodTagToDisplayName(moodTag);
        String sourceLabel = source.contains("ai") ? "AI check-in" : "Check-in thủ công";

        if (txtGreetingSubtitle != null) {
            txtGreetingSubtitle.setText("Mood hôm nay: " + moodEmoji + " " + moodName + " · " + moodPercent + "%");
        }

        if (txtHelloTitle != null) {
            txtHelloTitle.setText("Bạn đã check-in hôm nay");
        }

        if (txtHelloSub != null) {
            txtHelloSub.setText(moodEmoji + " " + moodName + " · " + moodPercent + "%");
        }

        if (txtAiTitle != null) {
            txtAiTitle.setText("Hôm nay bạn đang\n" + moodEmoji + " " + moodName);
        }

        if (txtAiSubtitle != null) {
            txtAiSubtitle.setText(
                    "Kết quả gần nhất: " + moodPercent + "% · " + sourceLabel +
                            "\n" + buildHomeAiSubline(moodName, moodDesc)
            );
        }

        setAiReadyText("Đã check-in hôm nay");

        if (txtStartAi != null) {
            txtStartAi.setText("Check-in lại ngay");
        }
    }

    private void applyTodaySchedule(boolean hasMoodCheckinToday) {
        // Kiểm tra và reset trạng thái uống nước hàng ngày
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiDailyTasks", MODE_PRIVATE);
        String todayKey = new java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString("last_checked_date", "");
        
        if (!todayKey.equals(savedDate)) {
            // Ngày mới -> reset trạng thái uống nước & thư giãn
            waterDone = false;
            relaxDone = false;
            prefs.edit()
                 .putString("last_checked_date", todayKey)
                 .putBoolean("water_done", false)
                 .putBoolean("relax_done", false)
                 .apply();
        } else {
            waterDone = prefs.getBoolean("water_done", false);
            relaxDone = prefs.getBoolean("relax_done", false);
        }

        boolean moodCheckinDone = hasMoodCheckinToday;
        boolean breathDone = prefs.getBoolean("breath_done", false);


        setScheduleTaskState(txtSchedule1, checkDone1, "Uống 1 ly nước", waterDone);
        setScheduleTaskState(txtSchedule2, checkEmpty2, "Check-in cảm xúc buổi sáng", moodCheckinDone);
        setScheduleTaskState(txtSchedule3, checkEmpty3, "Thở sâu 1 phút", breathDone);
        setScheduleTaskState(txtSchedule4, checkEmpty4, "Thư giãn giữa buổi", relaxDone);

        int doneSteps = 0;
        if (waterDone) doneSteps++;
        if (moodCheckinDone) doneSteps++;
        if (breathDone) doneSteps++;
        if (relaxDone) doneSteps++;

        setScheduleProgress(doneSteps);
    }

    private String buildHomeAiSubline(String moodName, String moodDesc) {
        switch (moodName) {
            case "Vui vẻ":
                return "Giữ nguồn năng lượng tích cực này nhé";

            case "Buồn":
                return "Heami vẫn ở đây cùng bạn, cứ đi chậm thôi";

            case "Căng thẳng":
                return "Bạn có thể check-in lại nếu áp lực thay đổi";

            case "Tức giận":
                return "Nếu cần, mình check-in lại sau vài phút thở chậm";

            case "Lo lắng":
            case "Sợ hãi":
                return "Heami ghi nhận trạng thái hiện tại của bạn";

            case "Khó chịu":
            case "Ghê tởm":
                return "Bạn có thể check-in lại nếu cảm xúc dịu hơn";

            case "Bình thường":
            default:
                return "Bạn có thể check-in lại bất cứ lúc nào";
        }
    }

    private String mapMoodTagToDisplayName(String moodTag) {
        switch (moodTag.toLowerCase(Locale.ROOT)) {
            case "happy":
                return "Vui vẻ";
            case "sad":
                return "Buồn";
            case "angry":
                return "Tức giận";
            case "fear":
                return "Lo lắng";
            case "stress":
                return "Căng thẳng";
            case "disgust":
                return "Khó chịu";
            case "neutral":
            default:
                return "Bình thường";
        }
    }

    private String getMoodEmojiFallback(String moodTag) {
        switch (moodTag.toLowerCase(Locale.ROOT)) {
            case "happy":
                return "😊";
            case "sad":
                return "🥲";
            case "angry":
                return "😤";
            case "fear":
                return "😟";
            case "stress":
                return "😮‍💨";
            case "disgust":
                return "😣";
            case "neutral":
            default:
                return "😌";
        }
    }

    private String getShortMoodDesc(String moodTag) {
        switch (moodTag.toLowerCase(Locale.ROOT)) {
            case "happy":
                return "Heami cảm nhận bạn đang có năng lượng khá tích cực.";
            case "sad":
                return "Heami thấy hôm nay bạn có vẻ hơi nặng lòng một chút.";
            case "angry":
                return "Heami cảm nhận có điều gì đó đang khiến bạn khá khó chịu.";
            case "fear":
                return "Heami thấy bạn đang hơi lo lắng hoặc thiếu cảm giác an toàn.";
            case "stress":
                return "Heami cảm nhận bạn đang hơi căng thẳng và cần thả lỏng một chút.";
            case "disgust":
                return "Heami cảm nhận bạn đang có phản ứng khá khó chịu với điều gì đó.";
            case "neutral":
            default:
                return "Heami thấy trạng thái của bạn hiện tại khá ổn định.";
        }
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void setScheduleProgress(int doneSteps) {
        int totalSteps = 4;

        if (doneSteps < 0) doneSteps = 0;
        if (doneSteps > totalSteps) doneSteps = totalSteps;

        if (txtScheduleProgress != null) {
            txtScheduleProgress.setText(doneSteps + "/" + totalSteps + " xong");
        }
    }

    private void setScheduleTaskState(TextView taskTextView, View checkView, String label, boolean done) {
        if (taskTextView != null) {
            taskTextView.setText(label);

            int currentFlags = taskTextView.getPaintFlags();

            if (done) {
                taskTextView.setPaintFlags(currentFlags | Paint.STRIKE_THRU_TEXT_FLAG);
                taskTextView.setAlpha(0.55f);
                taskTextView.setTextColor(0xFFA7BDC2);
            } else {
                taskTextView.setPaintFlags(currentFlags & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskTextView.setAlpha(1f);
                taskTextView.setTextColor(0xFF2D3A5E);
            }
        }

        if (checkView != null) {
            checkView.setBackgroundResource(done ? R.drawable.ic_check : R.drawable.bg_check_empty);
            checkView.setAlpha(1f);

            if (checkView instanceof TextView) {
                ((TextView) checkView).setText("");
            }
        }
    }

    private void setAiReadyText(String text) {
        if (layoutAiReady == null || layoutAiReady.getChildCount() < 2) {
            return;
        }

        View child = layoutAiReady.getChildAt(1);
        if (child instanceof TextView) {
            ((TextView) child).setText(text);
        }
    }

    private void applyStaticStyles() {
        // Không hardcode task nào done ở đây nữa.
    }

    private void startHomeAnimations() {
        // Cloud
        startFloatY(findViewById(R.id.imgHeamiCloud), 6f, 4800, 0);

        // Flowers
        startFlowerFloat(findViewById(R.id.decorFlowerPinkTop), 8f, 12f, 5600, 0);
        startFlowerFloat(findViewById(R.id.decorFlowerPinkLeft), 8f, 12f, 5600, 800);
        startFlowerFloat(findViewById(R.id.decorFlowerMintMid), 8f, 12f, 5600, 1400);
        startFlowerFloat(findViewById(R.id.decorFlowerPurpleAi), 8f, 12f, 5600, 2100);

        // Bell dot
        startPulse(findViewById(R.id.viewBellDot), 1.0f, 1.18f, 2000, 0);

        // Camera orb group
        startFloatY(findViewById(R.id.layoutCameraOrb), 3f, 4800, 0);
        startPulseScaleAlpha(findViewById(R.id.viewCameraRing), 1.0f, 1.035f, 0.40f, 0.65f, 3200, 0);
        startPulseScaleAlpha(findViewById(R.id.viewCameraFocus), 1.0f, 1.045f, 0.35f, 0.72f, 2800, 0);
        startPulseScaleAlpha(findViewById(R.id.viewCameraCore), 1.0f, 1.06f, 0.95f, 1.0f, 2600, 0);

        // Scan + twinkle inside camera
        startScan(findViewById(R.id.viewScanLine), findViewById(R.id.viewScanGlow));
        startTwinkleInside(findViewById(R.id.viewTwinkle1), 2800, 200);
        startTwinkleInside(findViewById(R.id.viewTwinkle2), 2800, 1200);
        startTwinkleInside(findViewById(R.id.viewTwinkle3), 2800, 2000);

        // Leaves
        startLeafTop(findViewById(R.id.imgCameraLeafTop));
        startLeafLeft(findViewById(R.id.imgCameraLeafLeft));
        startLeafRight(findViewById(R.id.imgCameraLeafRight));

        // Twinkle stars on AI card
        startTwinkle(findViewById(R.id.starAi1), 3600, 200);
        startTwinkle(findViewById(R.id.starAi2), 3600, 1100);
        startTwinkle(findViewById(R.id.starAi3), 3600, 2000);
        startTwinkle(findViewById(R.id.starAi4), 3600, 2800);

        // CTA subtle breathing
        startSubtleButtonBreath(findViewById(R.id.btnStartAi));
        startArrowShift(findViewById(R.id.txtStartAiArrow));

        // Ready pill subtle pulse
        startAlphaBreath(findViewById(R.id.txtAiReady), 0.92f, 1.0f, 2200);
        startAiReadyDotAnimation(
                findViewById(R.id.viewAiReadyDot),
                findViewById(R.id.viewAiReadyGlow)
        );
    }

    private void startFloatY(View view, float dpDistance, long duration, long delay) {
        if (view == null) return;
        float distancePx = dpDistance * getResources().getDisplayMetrics().density;
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -distancePx, 0f);
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void startFlowerFloat(View view, float dpDistance, float rotationDeg, long duration, long delay) {
        if (view == null) return;
        float distancePx = dpDistance * getResources().getDisplayMetrics().density;
        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -distancePx, 0f);
        moveY.setDuration(duration);
        moveY.setStartDelay(delay);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, rotationDeg, 0f);
        rotate.setDuration(duration);
        rotate.setStartDelay(delay);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.70f, 1.0f, 0.70f);
        alpha.setDuration(duration);
        alpha.setStartDelay(delay);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveY, rotate, alpha);
        set.start();
    }

    private void startPulse(View view, float fromScale, float toScale, long duration, long delay) {
        if (view == null) return;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, fromScale, toScale, fromScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, fromScale, toScale, fromScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.7f, 1f);
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

    private void startPulseScaleAlpha(View view, float fromScale, float toScale, float fromAlpha, float toAlpha, long duration, long delay) {
        if (view == null) return;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, fromScale, toScale, fromScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, fromScale, toScale, fromScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, fromAlpha, toAlpha, fromAlpha);
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

    private void startTwinkle(View view, long duration, long delay) {
        if (view == null) return;
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 0.25f, 0.95f, 0.35f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.7f, 0.88f, 1.12f, 0.92f, 0.7f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.7f, 0.88f, 1.12f, 0.92f, 0.7f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 8f, 18f, 8f, 0f);
        alpha.setDuration(duration);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        rotation.setDuration(duration);
        alpha.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        rotation.setStartDelay(delay);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY, rotation);
        set.start();
    }

    private void startLeafTop(View view) {
        if (view == null) return;
        float dp8 = 8 * getResources().getDisplayMetrics().density;
        float dp9 = 9 * getResources().getDisplayMetrics().density;
        ObjectAnimator moveX = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -dp9, 0f);
        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, dp8, 0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, View.ROTATION, 18f, 8f, 18f);
        moveX.setDuration(3800);
        moveY.setDuration(3800);
        rotate.setDuration(3800);
        moveX.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        moveX.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveX, moveY, rotate);
        set.start();
    }

    private void startLeafLeft(View view) {
        if (view == null) return;
        float dp7 = 7 * getResources().getDisplayMetrics().density;
        float dp10 = 10 * getResources().getDisplayMetrics().density;
        ObjectAnimator moveX = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, dp10, 0f);
        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -dp7, 0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, View.ROTATION, -8f, 6f, -8f);
        moveX.setDuration(3400);
        moveY.setDuration(3400);
        rotate.setDuration(3400);
        moveX.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        moveX.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveX, moveY, rotate);
        set.start();
    }

    private void startLeafRight(View view) {
        if (view == null) return;
        float dp8 = 8 * getResources().getDisplayMetrics().density;
        float dp6 = 6 * getResources().getDisplayMetrics().density;
        ObjectAnimator moveX = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -dp6, 0f);
        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, dp8, 0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, -10f, 0f);
        moveX.setDuration(3600);
        moveY.setDuration(3600);
        rotate.setDuration(3600);
        moveX.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        moveX.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveX, moveY, rotate);
        set.start();
    }

    private void startSubtleButtonBreath(View view) {
        if (view == null) return;
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.96f, 1.0f, 0.96f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.01f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.01f, 1.0f);
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

    private void startAlphaBreath(View view, float from, float to, long duration) {
        if (view == null) return;
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, from, to, from);
        alpha.setDuration(duration);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.start();
    }

    private void startScan(View scanLine, View scanGlow) {
        if (scanLine == null || scanGlow == null) return;
        float dp22 = 22 * getResources().getDisplayMetrics().density;
        ObjectAnimator lineMove = ObjectAnimator.ofFloat(scanLine, View.TRANSLATION_Y, -dp22, 0f, dp22);
        lineMove.setDuration(3800);
        lineMove.setRepeatCount(ValueAnimator.INFINITE);
        lineMove.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator lineAlpha = ObjectAnimator.ofFloat(scanLine, View.ALPHA, 0f, 1f, 1f, 0f);
        lineAlpha.setDuration(3800);
        lineAlpha.setRepeatCount(ValueAnimator.INFINITE);
        lineAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator glowMove = ObjectAnimator.ofFloat(scanGlow, View.TRANSLATION_Y, -dp22, 0f, dp22);
        glowMove.setDuration(3800);
        glowMove.setRepeatCount(ValueAnimator.INFINITE);
        glowMove.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator glowAlpha = ObjectAnimator.ofFloat(scanGlow, View.ALPHA, 0f, 0.72f, 0.72f, 0f);
        glowAlpha.setDuration(3800);
        glowAlpha.setRepeatCount(ValueAnimator.INFINITE);
        glowAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playTogether(lineMove, lineAlpha, glowMove, glowAlpha);
        set.start();
    }

    private void startTwinkleInside(View view, long duration, long delay) {
        if (view == null) return;
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 0.35f, 1f, 0.4f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.7f, 0.9f, 1.15f, 0.95f, 0.7f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.7f, 0.9f, 1.15f, 0.95f, 0.7f);
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

    private void startArrowShift(View view) {
        if (view == null) return;
        float dp6 = 6 * getResources().getDisplayMetrics().density;
        ObjectAnimator shift = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, dp6, 0f);
        shift.setDuration(1400);
        shift.setRepeatCount(ValueAnimator.INFINITE);
        shift.setInterpolator(new AccelerateDecelerateInterpolator());
        shift.start();
    }

    private void startAiReadyDotAnimation(View dot, View glow) {
        if (dot == null || glow == null) return;
        ObjectAnimator dotScaleX = ObjectAnimator.ofFloat(dot, View.SCALE_X, 1.0f, 0.72f, 1.45f, 1.0f);
        ObjectAnimator dotScaleY = ObjectAnimator.ofFloat(dot, View.SCALE_Y, 1.0f, 0.72f, 1.45f, 1.0f);
        ObjectAnimator dotAlpha = ObjectAnimator.ofFloat(dot, View.ALPHA, 0.95f, 0.88f, 1.0f, 0.95f);
        dotScaleX.setDuration(1700);
        dotScaleY.setDuration(1700);
        dotAlpha.setDuration(1700);
        dotScaleX.setRepeatCount(ValueAnimator.INFINITE);
        dotScaleY.setRepeatCount(ValueAnimator.INFINITE);
        dotAlpha.setRepeatCount(ValueAnimator.INFINITE);
        dotScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        dotScaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        dotAlpha.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator glowScaleX = ObjectAnimator.ofFloat(glow, View.SCALE_X, 0.7f, 1.9f, 2.3f);
        ObjectAnimator glowScaleY = ObjectAnimator.ofFloat(glow, View.SCALE_Y, 0.7f, 1.9f, 2.3f);
        ObjectAnimator glowAlpha = ObjectAnimator.ofFloat(glow, View.ALPHA, 0.0f, 0.30f, 0.0f);
        glowScaleX.setDuration(1700);
        glowScaleY.setDuration(1700);
        glowAlpha.setDuration(1700);
        glowScaleX.setRepeatCount(ValueAnimator.INFINITE);
        glowScaleY.setRepeatCount(ValueAnimator.INFINITE);
        glowAlpha.setRepeatCount(ValueAnimator.INFINITE);
        glowScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        glowScaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        glowAlpha.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet dotSet = new AnimatorSet();
        dotSet.playTogether(dotScaleX, dotScaleY, dotAlpha);
        dotSet.start();

        AnimatorSet glowSet = new AnimatorSet();
        glowSet.playTogether(glowScaleX, glowScaleY, glowAlpha);
        glowSet.start();
    }

    private void setupActions() {
        if (btnStartAi != null) {
            btnStartAi.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, CheckInAiActivity.class);
                startActivity(intent);
            });
        }

        TextView txtExpertsMore = findViewById(R.id.txtExpertsMore);
        if (txtExpertsMore != null) {
            txtExpertsMore.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, DoctorActivity.class);
                startActivity(intent);
            });
        }

        TextView txtAppointmentsMore = findViewById(R.id.txtAppointmentsMore);
        if (txtAppointmentsMore != null) {
            txtAppointmentsMore.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ConsultationsActivity.class);
                startActivity(intent);
            });
        }

        // Section Healing (Therapy) actions
        TextView txtHealingMore = findViewById(R.id.txtHealingMore);
        if (txtHealingMore != null) {
            txtHealingMore.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, com.example.heami.ui.therapy.TherapyActivity.class);
                startActivity(intent);
            });
        }

        View cardHealMusic = findViewById(R.id.cardHealMusic);
        if (cardHealMusic != null) {
            cardHealMusic.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, com.example.heami.ui.therapy.TherapyActivity.class);
                intent.putExtra("category", "audio");
                startActivity(intent);
            });
        }

        View cardHealBreath = findViewById(R.id.cardHealBreath);
        if (cardHealBreath != null) {
            cardHealBreath.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, com.example.heami.ui.therapy.TherapyActivity.class);
                intent.putExtra("category", "breath");
                startActivity(intent);
            });
        }

        View cardHealDiary = findViewById(R.id.cardHealDiary);
        if (cardHealDiary != null) {
            cardHealDiary.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, com.example.heami.ui.therapy.TherapyActivity.class);
                intent.putExtra("category", "journal");
                startActivity(intent);
            });
        }

        View cardHealPodcast = findViewById(R.id.cardHealPodcast);
        if (cardHealPodcast != null) {
            cardHealPodcast.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, com.example.heami.ui.therapy.TherapyActivity.class);
                intent.putExtra("category", "audio"); // Podcast cũng thuộc tab Âm thanh (layoutAmThanh)
                startActivity(intent);
            });
        }

        // Handle water click check
        if (checkDone1 != null) {
            checkDone1.setOnClickListener(v -> {
                waterDone = !waterDone;
                getSharedPreferences("HeamiDailyTasks", MODE_PRIVATE)
                    .edit()
                    .putBoolean("water_done", waterDone)
                    .apply();
                
                // Cộng dồn tasks_done trên Firestore nếu tích hoàn thành
                FirebaseUser user = auth.getCurrentUser();
                if (user != null && waterDone) {
                    firestore.collection("users")
                            .document(user.getUid())
                            .update("tasks_done", com.google.firebase.firestore.FieldValue.increment(1));
                } else if (user != null && !waterDone) {
                    // Nếu bỏ tích, trừ bớt 1 nhưng không để âm
                    firestore.collection("users")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.exists()) {
                                    Long currentTasks = snapshot.getLong("tasks_done");
                                    if (currentTasks != null && currentTasks > 0) {
                                        firestore.collection("users")
                                                .document(user.getUid())
                                                .update("tasks_done", com.google.firebase.firestore.FieldValue.increment(-1));
                                    }
                                }
                            });
                }
                
                loadTodayMoodState(); // reload schedule state
            });
        }

        // Handle breathing task click
        View itemBreathGroup = findViewById(R.id.checkEmpty3);
        if (itemBreathGroup != null) {
            itemBreathGroup.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, com.example.heami.ui.therapy.TherapyActivity.class);
                intent.putExtra("category", "breath");
                startActivity(intent);
            });
        }
        if (txtSchedule3 != null) {
            txtSchedule3.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, com.example.heami.ui.therapy.TherapyActivity.class);
                intent.putExtra("category", "breath");
                startActivity(intent);
            });
        }

        // Handle relax click check
        if (checkEmpty4 != null) {
            checkEmpty4.setOnClickListener(v -> {
                relaxDone = !relaxDone;
                getSharedPreferences("HeamiDailyTasks", MODE_PRIVATE)
                    .edit()
                    .putBoolean("relax_done", relaxDone)
                    .apply();

                // Cộng dồn tasks_done trên Firestore nếu tích hoàn thành
                FirebaseUser user = auth.getCurrentUser();
                if (user != null && relaxDone) {
                    firestore.collection("users")
                            .document(user.getUid())
                            .update("tasks_done", com.google.firebase.firestore.FieldValue.increment(1));
                } else if (user != null && !relaxDone) {
                    // Nếu bỏ tích, trừ bớt 1 nhưng không để âm
                    firestore.collection("users")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.exists()) {
                                    Long currentTasks = snapshot.getLong("tasks_done");
                                    if (currentTasks != null && currentTasks > 0) {
                                        firestore.collection("users")
                                                .document(user.getUid())
                                                .update("tasks_done", com.google.firebase.firestore.FieldValue.increment(-1));
                                    }
                                }
                            });
                }

                loadTodayMoodState(); // reload schedule state
            });
        }
    }

    private void loadDoctorsFromFirestore() {
        LinearLayout container = findViewById(R.id.layoutDoctorsContainer);
        if (container == null) return;

        FirebaseFirestore.getInstance().collection("doctors")
                .limit(4)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return;

                    container.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(this);

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        DoctorModel doctor = doc.toObject(DoctorModel.class);
                        if (doctor == null) continue;

                        if (doctor.getDoctor_id() == null || doctor.getDoctor_id().isEmpty()) {
                            doctor.setDoctor_id(doc.getId());
                        }

                        View doctorView = inflater.inflate(R.layout.item_home_doctor, container, false);

                        ImageView imgAvatar = doctorView.findViewById(R.id.imgDoctorAvatar);
                        TextView txtTag = doctorView.findViewById(R.id.txtDoctorTag);
                        TextView txtStatus = doctorView.findViewById(R.id.txtDoctorStatus);
                        TextView txtName = doctorView.findViewById(R.id.txtDoctorName);
                        TextView txtSpecialty = doctorView.findViewById(R.id.txtDoctorSpecialty);
                        TextView txtRating = doctorView.findViewById(R.id.txtDoctorRating);
                        View btnBook = doctorView.findViewById(R.id.btnBookDoctor);

                        txtName.setText(doctor.getFull_name());

                        String specialtyStr = doctor.getSpecialization().isEmpty() ? "" : doctor.getSpecialization().get(0);
                        txtSpecialty.setText(specialtyStr);

                        // Ánh xạ tag danh mục bác sĩ
                        txtTag.setText(getCategoryDisplayName(doctor.getCategory_id()));
                        txtTag.setBackgroundResource(getCategoryChipBg(doctor.getCategory_id()));

                        txtRating.setText("⭐ " + doctor.getRating_avg());

                        if (doctor.isIs_online()) {
                            txtStatus.setText("Online");
                            txtStatus.setTextColor(Color.parseColor("#4DB6AC"));
                        } else {
                            txtStatus.setText("Offline");
                            txtStatus.setTextColor(Color.parseColor("#7D8BB7"));
                        }

                        Glide.with(this)
                                .load(doctor.getAvatar_url())
                                .placeholder(R.drawable.img_doctor_1)
                                .error(R.drawable.img_doctor_1)
                                .into(imgAvatar);

                        doctorView.setOnClickListener(v -> {
                            Intent intent = new Intent(HomeActivity.this, DoctorDetailActivity.class);
                            intent.putExtra("doctor_id", doctor.getDoctor_id());
                            intent.putExtra("doctor_name", doctor.getFull_name());
                            intent.putExtra("doctor_degree", doctor.getDegree());
                            intent.putExtra("doctor_specialty", specialtyStr);
                            intent.putExtra("doctor_location", doctor.getLocation());
                            intent.putExtra("doctor_rating", String.valueOf(doctor.getRating_avg()));
                            intent.putExtra("doctor_sessions", String.valueOf(doctor.getTotal_sessions()) + "+");
                            intent.putExtra("doctor_experience", doctor.getExperience_years() + " năm");
                            intent.putExtra("doctor_intro", doctor.getBio());
                            intent.putExtra("doctor_avatar", doctor.getAvatar_url());
                            startActivity(intent);
                        });

                        if (btnBook != null) {
                            btnBook.setOnClickListener(v -> {
                                Intent intent = new Intent(HomeActivity.this, DoctorDetailActivity.class);
                                intent.putExtra("doctor_id", doctor.getDoctor_id());
                                intent.putExtra("doctor_name", doctor.getFull_name());
                                intent.putExtra("doctor_degree", doctor.getDegree());
                                intent.putExtra("doctor_specialty", specialtyStr);
                                intent.putExtra("doctor_location", doctor.getLocation());
                                intent.putExtra("doctor_rating", String.valueOf(doctor.getRating_avg()));
                                intent.putExtra("doctor_sessions", String.valueOf(doctor.getTotal_sessions()) + "+");
                                intent.putExtra("doctor_experience", doctor.getExperience_years() + " năm");
                                intent.putExtra("doctor_intro", doctor.getBio());
                                intent.putExtra("doctor_avatar", doctor.getAvatar_url());
                                startActivity(intent);
                            });
                        }

                        container.addView(doctorView);
                    }

                    View moreView = inflater.inflate(R.layout.item_home_doctor_more, container, false);
                    if (moreView != null) {
                        moreView.setOnClickListener(v -> {
                            Intent intent = new Intent(HomeActivity.this, DoctorActivity.class);
                            startActivity(intent);
                        });
                        container.addView(moreView);
                    }
                });
    }

    private String getCategoryDisplayName(String categoryId) {
        if ("clinical".equals(categoryId)) return "Lâm sàng";
        if ("psychiatry".equals(categoryId)) return "Tâm thần";
        if ("therapy".equals(categoryId)) return "Trị liệu";
        if ("positive".equals(categoryId)) return "Tích cực";
        if ("care".equals(categoryId)) return "Chăm sóc";
        return "Tư vấn";
    }

    private int getCategoryChipBg(String categoryId) {
        if ("clinical".equals(categoryId)) return R.drawable.bg_chip_teal;
        if ("psychiatry".equals(categoryId)) return R.drawable.bg_chip_active_purple;
        if ("therapy".equals(categoryId)) return R.drawable.bg_chip_pink;
        if ("positive".equals(categoryId)) return R.drawable.bg_chip_active_yellow;
        if ("care".equals(categoryId)) return R.drawable.bg_chip_active_green;
        return R.drawable.bg_chip_pink;
    }

    private void loadUpcomingAppointmentsFromFirestore() {
        View section = findViewById(R.id.layoutSectionAppointments);
        LinearLayout container = findViewById(R.id.layoutAppointmentsContainer);
        if (section == null || container == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            section.setVisibility(View.GONE);
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore.getInstance().collection("consultations")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("HomeActivity", "Error loading appointments: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    List<ConsultationModel> upcomingList = new ArrayList<>();
                    Date now = new Date();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        ConsultationModel model = doc.toObject(ConsultationModel.class);
                        if (model == null) continue;
                        model.setSessionId(doc.getId());

                        if (model.getUserId() == null) model.setUserId(doc.getString("userId"));
                        if (model.getUserId() == null) model.setUserId(doc.getString("user_id"));

                        if (model.getDoctorName() == null) model.setDoctorName(doc.getString("doctorName"));
                        if (model.getDoctorName() == null) model.setDoctorName(doc.getString("doctor_name"));

                        if (model.getDoctorAvatar() == null) model.setDoctorAvatar(doc.getString("doctorAvatar"));
                        if (model.getDoctorAvatar() == null) model.setDoctorAvatar(doc.getString("doctor_avatar"));

                        if (model.getStatus() == null) model.setStatus(doc.getString("status"));
                        if (model.getStartTime() == null) model.setStartTime(doc.getTimestamp("startTime"));
                        if (model.getStartTime() == null) model.setStartTime(doc.getTimestamp("start_time"));

                        if (model.getEndTime() == null) model.setEndTime(doc.getTimestamp("endTime"));
                        if (model.getEndTime() == null) model.setEndTime(doc.getTimestamp("end_time"));

                        if (model.getPackageType() == null) model.setPackageType(doc.getString("packageType"));
                        if (model.getPackageType() == null) model.setPackageType(doc.getString("package_type"));

                        if (model.getFormatType() == null) model.setFormatType(doc.getString("formatType"));
                        if (model.getFormatType() == null) model.setFormatType(doc.getString("format_type"));

                        if (userId.equals(model.getUserId())) {
                            String status = model.getStatus() != null ? model.getStatus().toUpperCase(Locale.ROOT) : "BOOKED";

                            if ("BOOKED".equals(status) && model.getEndTime() != null && model.getEndTime().toDate().before(now)) {
                                status = "COMPLETED";
                            }

                            if ("BOOKED".equals(status)) {
                                upcomingList.add(model);

                                if (model.getStartTime() != null) {
                                    NotificationScheduler.scheduleAppointmentNotification(
                                        HomeActivity.this,
                                        model.getSessionId(),
                                        model.getStartTime().toDate().getTime(),
                                        model.getDoctorName()
                                    );
                                }
                            }
                        }
                    }

                    // Sort by start time ascending
                    upcomingList.sort((o1, o2) -> {
                        if (o1.getStartTime() == null || o2.getStartTime() == null) return 0;
                        return o1.getStartTime().compareTo(o2.getStartTime());
                    });

                    if (upcomingList.isEmpty()) {
                        section.setVisibility(View.GONE);
                    } else {
                        section.setVisibility(View.VISIBLE);
                        container.removeAllViews();
                        LayoutInflater inflater = LayoutInflater.from(this);

                        int limit = Math.min(upcomingList.size(), 2);
                        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, dd/MM/yyyy", new Locale("vi", "VN"));
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        for (int i = 0; i < limit; i++) {
                            ConsultationModel model = upcomingList.get(i);
                            View cardView = inflater.inflate(R.layout.item_home_appointment, container, false);

                            ImageView imgAvatar = cardView.findViewById(R.id.imgDoctorAvatar);
                            TextView txtDocName = cardView.findViewById(R.id.txtDoctorName);
                            TextView txtDate = cardView.findViewById(R.id.txtAppointmentDate);
                            TextView txtTime = cardView.findViewById(R.id.txtAppointmentTime);

                            View btnActionRight = cardView.findViewById(R.id.btnActionRight);
                            ImageView imgRightBtnIcon = cardView.findViewById(R.id.imgRightBtnIcon);

                            if (txtDocName != null) {
                                txtDocName.setText(model.getDoctorName() != null ? model.getDoctorName() : "Chuyên gia");
                            }

                            if (model.getStartTime() != null) {
                                if (txtDate != null) {
                                    String dateStr = dayFormat.format(model.getStartTime().toDate());
                                    if (dateStr.startsWith("Th ")) {
                                        dateStr = dateStr.replace("Th ", "Thứ ");
                                    } else if (dateStr.startsWith("CN")) {
                                        dateStr = dateStr.replace("CN", "Chủ Nhật");
                                    }
                                    txtDate.setText(dateStr);
                                }
                                if (txtTime != null) txtTime.setText(timeFormat.format(model.getStartTime().toDate()));
                            }

                            boolean isVideo = model.getFormatType() != null && model.getFormatType().toLowerCase(Locale.ROOT).contains("video");
                            if (isVideo) {
                                if (imgAvatar != null) {
                                    imgAvatar.setBackgroundColor(Color.parseColor("#FFF0F5")); // Light pink background
                                }

                                if (btnActionRight != null) btnActionRight.setBackgroundResource(R.drawable.bg_appointment_btn_pink);
                                if (imgRightBtnIcon != null) {
                                    imgRightBtnIcon.setImageResource(R.drawable.ic_video_call);
                                    imgRightBtnIcon.setColorFilter(null);
                                }
                            } else {
                                if (imgAvatar != null) {
                                    imgAvatar.setBackgroundColor(Color.parseColor("#E0F2F1")); // Light mint background
                                }

                                if (btnActionRight != null) btnActionRight.setBackgroundResource(R.drawable.bg_appointment_btn_mint);
                                if (imgRightBtnIcon != null) {
                                    imgRightBtnIcon.setImageResource(R.drawable.ic_doctor_chat);
                                    imgRightBtnIcon.setColorFilter(null);
                                }
                            }

                            // Guard against Glide crashing when Activity is destroyed in background
                            if (!isDestroyed() && !isFinishing()) {
                                Glide.with(this)
                                        .load(model.getDoctorAvatar())
                                        .placeholder(R.drawable.ic_avatar_placeholder)
                                        .error(R.drawable.ic_avatar_placeholder)
                                        .into(imgAvatar);
                            }

                            cardView.setOnClickListener(v -> {
                                Intent intent = new Intent(HomeActivity.this, ConsultationsActivity.class);
                                startActivity(intent);
                            });

                            btnActionRight.setOnClickListener(v -> {
                                Intent intent = new Intent(HomeActivity.this, ConsultationsActivity.class);
                                startActivity(intent);
                            });

                            container.addView(cardView);
                        }
                    }
                });
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    // no-op
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void syncFcmToken() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.trim().isEmpty()) return;

                    java.util.HashMap<String, Object> updates = new java.util.HashMap<>();
                    updates.put("account_id", user.getUid());
                    updates.put("fcm_token", token);

                    firestore.collection("accounts")
                            .document(user.getUid())
                            .set(updates, SetOptions.merge());
                });
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) return;

        String type = intent.getStringExtra("type");
        if (!"COMMUNITY_CHAT".equals(type)) {
            return;
        }

        String roomId = safeText(intent.getStringExtra("room_id"), "");
        String matchId = safeText(intent.getStringExtra("match_id"), "");
        String matchedUserId = safeText(intent.getStringExtra("matched_user_id"), "");
        String matchedUserName = safeText(intent.getStringExtra("matched_user_name"), "Người bạn ẩn danh");
        String moodTag = safeText(intent.getStringExtra("mood_tag"), "stress");

        if (roomId.isEmpty()) {
            return;
        }

        Intent chatIntent = new Intent(this, MoodMatchChatActivity.class);
        chatIntent.putExtra("room_id", roomId);
        chatIntent.putExtra("match_id", matchId);
        chatIntent.putExtra("matched_user_id", matchedUserId);
        chatIntent.putExtra("matched_user_name", matchedUserName);
        chatIntent.putExtra("mood_tag", moodTag);
        chatIntent.putExtra("room_status", "ACTIVE");
        startActivity(chatIntent);

        intent.removeExtra("type");
    }

    private void registerChatBannerReceiver() {
        if (chatBannerReceiver != null) return;

        chatBannerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String type = safeText(intent.getStringExtra("type"), "");
                if (!"COMMUNITY_CHAT".equals(type)) return;

                String roomId = safeText(intent.getStringExtra("room_id"), "");
                String matchId = safeText(intent.getStringExtra("match_id"), "");
                String matchedUserId = safeText(intent.getStringExtra("matched_user_id"), "");
                String matchedUserName = safeText(intent.getStringExtra("matched_user_name"), "Người bạn ẩn danh");
                String previewText = safeText(intent.getStringExtra("preview_text"), "Bạn có tin nhắn mới");
                String moodTag = safeText(intent.getStringExtra("mood_tag"), "stress");

                showHeamiChatBanner(roomId, matchId, matchedUserId, matchedUserName, previewText, moodTag);
            }
        };

        IntentFilter filter = new IntentFilter(HeamiFirebaseMessagingService.ACTION_CHAT_PUSH_BANNER);

        ContextCompat.registerReceiver(
                this,
                chatBannerReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    private void unregisterChatBannerReceiver() {
        if (chatBannerReceiver == null) return;

        try {
            unregisterReceiver(chatBannerReceiver);
        } catch (Exception ignored) {
        }

        chatBannerReceiver = null;
    }

    private void showHeamiChatBanner(
            @NonNull String roomId,
            @NonNull String matchId,
            @NonNull String matchedUserId,
            @NonNull String matchedUserName,
            @NonNull String previewText,
            @NonNull String moodTag
    ) {
        View root = findViewById(android.R.id.content);
        if (root == null) return;

        Snackbar snackbar = Snackbar.make(
                root,
                "🌸 " + matchedUserName + ": " + previewText,
                Snackbar.LENGTH_LONG
        );

        snackbar.setBackgroundTint(0xFFFDEAF1);
        snackbar.setTextColor(0xFF2D1B47);
        snackbar.setActionTextColor(0xFFE8507A);

        snackbar.setAction("Mở", v -> {
            Intent chatIntent = new Intent(this, MoodMatchChatActivity.class);
            chatIntent.putExtra("room_id", roomId);
            chatIntent.putExtra("match_id", matchId);
            chatIntent.putExtra("matched_user_id", matchedUserId);
            chatIntent.putExtra("matched_user_name", matchedUserName);
            chatIntent.putExtra("mood_tag", moodTag);
            chatIntent.putExtra("room_status", "ACTIVE");
            startActivity(chatIntent);
        });

        snackbar.show();
    }
}

