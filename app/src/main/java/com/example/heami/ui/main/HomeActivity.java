package com.example.heami.ui.main;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.ui.checkin.CheckInAiActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

public class HomeActivity extends AppCompatActivity {

    private TextView txtGreetingLabel, txtGreetingTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ExitDialogHelper.registerExitHandler(this);
        NotificationScheduler.scheduleDailyCheckIn(this);

        initViews();
        BottomNavManager.setup(this, BottomNavManager.TAB_HOME);

        loadUserData();
        applyStaticStyles();
        startHomeAnimations();
        setupActions();
        loadDoctorsFromFirestore();
        loadUpcomingAppointmentsFromFirestore();
    }

    private void initViews() {
        txtGreetingLabel = findViewById(R.id.txtGreetingLabel);
        txtGreetingTitle = findViewById(R.id.txtGreetingTitle);
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Lấy nickname từ Firestore
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nickname = documentSnapshot.getString("nickname");
                            if (nickname != null && !nickname.isEmpty()) {
                                txtGreetingTitle.setText(nickname + " ơi!");
                            }
                        }
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

    private void applyStaticStyles() {
        TextView txtSchedule1 = findViewById(R.id.txtSchedule1);
        if (txtSchedule1 != null) {
            txtSchedule1.setPaintFlags(
                    txtSchedule1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }
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
        View btnStartAi = findViewById(R.id.btnStartAi);
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

                            Glide.with(this)
                                    .load(model.getDoctorAvatar())
                                    .placeholder(R.drawable.ic_avatar_placeholder)
                                    .error(R.drawable.ic_avatar_placeholder)
                                    .into(imgAvatar);

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
}
