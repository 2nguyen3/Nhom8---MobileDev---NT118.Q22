package com.example.heami.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent; // Thêm dòng này
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton; // Thêm dòng này
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;


public class TherapyActivity extends AppCompatActivity {

    private TextView btnTatCa, btnAmThanh, btnHitTho, btnNhatKy, btnKeHoach;
    private LinearLayout layoutAmThanh, layoutHitTho, layoutNhatKy, layoutKeHoach;
    private ImageView imgCloud;
    private ImageButton btnOpenMusic; // Khai báo ở đây
    private ImageButton btnOpenNatureSound;

    private ImageButton btnOpen1Breath;

    private ImageButton btnOpen3Breath;
    private ImageButton btnOpen5Breath;

    private ImageButton btnOpenDoctor;

    private ImageButton btnOpenPodcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_therapy);

        initViews();
        setupFilterListeners();

        // GỌI HÀM MỞ NHẠC Ở ĐÂY
        setupMusicButton();

        setupNatureButton();

        setup1BreathButton();

        setup3BreathButton();

        setup5BreathButton();

        setupDoctorButton();

        setupPodcast();

        startCloudAnimation();


        BottomNavManager.setup(this, BottomNavManager.TAB_THERAPY);
    }

    private void initViews() {
        btnTatCa = findViewById(R.id.btnTatCa);
        btnAmThanh = findViewById(R.id.btnAmThanh);
        btnHitTho = findViewById(R.id.btnHitTho);
        btnNhatKy = findViewById(R.id.btnNhatKy);
        btnKeHoach = findViewById(R.id.btnKeHoach);

        layoutAmThanh = findViewById(R.id.layoutAmThanh);
        layoutHitTho = findViewById(R.id.layoutHitTho);
        layoutNhatKy = findViewById(R.id.layoutNhatKy);
        layoutKeHoach = findViewById(R.id.layoutKeHoach);

        imgCloud = findViewById(R.id.imgCloud);

        btnOpenMusic = findViewById(R.id.btnOpenMusic);

        btnOpenNatureSound = findViewById(R.id.btnOpenNatureSound);

        btnOpen1Breath = findViewById(R.id.btnOpen1Breath);

        btnOpen3Breath = findViewById(R.id.btnOpen3Breath);

        btnOpen5Breath = findViewById(R.id.btnOpen5Breath);

        btnOpenDoctor = findViewById(R.id.btnOpenDoctor);

        btnOpenPodcast = findViewById(R.id.btnOpenPodcast);
    }

    private void setupMusicButton() {
        if (btnOpenMusic != null) {
            btnOpenMusic.setOnClickListener(v -> {
                Intent intent = new Intent(TherapyActivity.this, MusicPlayerActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void setupNatureButton() {
        if (btnOpenNatureSound != null) {
            btnOpenNatureSound.setOnClickListener(v -> {
                Intent intent = new Intent(TherapyActivity.this, NatureSoundActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void setup1BreathButton() {
        if (btnOpen1Breath != null) {
            btnOpen1Breath.setOnClickListener(v -> {
                Intent intent = new Intent(TherapyActivity.this, BreathingActivity.class);
                intent.putExtra("TARGET_TIME", 1); // Gửi số 1
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void setup3BreathButton() {
        if (btnOpen3Breath != null) {
            btnOpen3Breath.setOnClickListener(v -> {
                Intent intent = new Intent(TherapyActivity.this, BreathingActivity.class);
                intent.putExtra("TARGET_TIME", 3); // Gửi số 3
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void setup5BreathButton() {
        if (btnOpen5Breath != null) {
            btnOpen5Breath.setOnClickListener(v -> {
                Intent intent = new Intent(TherapyActivity.this, BreathingActivity.class);
                intent.putExtra("TARGET_TIME", 5); // Gửi số 5
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void setupDoctorButton() {
        if (btnOpenDoctor != null) {
            btnOpenDoctor.setOnClickListener(v -> {
                Intent intent = new Intent(TherapyActivity.this, DoctorActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void setupPodcast() {
        if (btnOpenPodcast != null) {
            btnOpenPodcast.setOnClickListener(v -> {
                Intent intent = new Intent(TherapyActivity.this, PodcastActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void startCloudAnimation() {
        if (imgCloud != null) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(imgCloud, "translationY", 0f, -25f);
            animator.setDuration(1200);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        }
    }

    private void setupFilterListeners() {
        btnTatCa.setOnClickListener(v -> applyFilter("all", btnTatCa));
        btnAmThanh.setOnClickListener(v -> applyFilter("audio", btnAmThanh));
        btnHitTho.setOnClickListener(v -> applyFilter("breath", btnHitTho));
        btnNhatKy.setOnClickListener(v -> applyFilter("journal", btnNhatKy));
        btnKeHoach.setOnClickListener(v -> applyFilter("plan", btnKeHoach));
    }

    private void applyFilter(String category, TextView selectedButton) {
        View[] layouts = {layoutAmThanh, layoutHitTho, layoutNhatKy, layoutKeHoach};

        for (View layout : layouts) {
            if (layout != null) layout.animate().alpha(0f).setDuration(100).start();
        }

        selectedButton.postDelayed(() -> {
            if(layoutAmThanh != null) layoutAmThanh.setVisibility(View.GONE);
            if(layoutHitTho != null) layoutHitTho.setVisibility(View.GONE);
            if(layoutNhatKy != null) layoutNhatKy.setVisibility(View.GONE);
            if(layoutKeHoach != null) layoutKeHoach.setVisibility(View.GONE);

            switch (category) {
                case "all":
                    showWithAnimation(layoutAmThanh);
                    showWithAnimation(layoutHitTho);
                    showWithAnimation(layoutNhatKy);
                    showWithAnimation(layoutKeHoach);
                    break;
                case "audio":
                    showWithAnimation(layoutAmThanh);
                    break;
                case "breath":
                    showWithAnimation(layoutHitTho);
                    break;
                case "journal":
                    showWithAnimation(layoutNhatKy);
                    break;
                case "plan":
                    showWithAnimation(layoutKeHoach);
                    break;
            }
            updateChipStyles(selectedButton);
        }, 120);
    }

    private void showWithAnimation(View view) {
        if (view == null) return;
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(30f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
    }

    private void updateChipStyles(TextView selectedButton) {
        TextView[] allButtons = {btnTatCa, btnAmThanh, btnHitTho, btnNhatKy, btnKeHoach};
        for (TextView btn : allButtons) {
            if (btn == null) continue;
            if (btn == selectedButton) {
                // Giữ nguyên logic màu sắc của các bạn
                if (btn == btnAmThanh) {
                    btn.setBackgroundResource(R.drawable.bg_chip_active_teal);
                    btn.setTextColor(Color.parseColor("#33ABA0"));
                } else if (btn == btnHitTho) {
                    btn.setBackgroundResource(R.drawable.bg_chip_active_purple);
                    btn.setTextColor(Color.parseColor("#B15DCA"));
                } else if (btn == btnNhatKy) {
                    btn.setBackgroundResource(R.drawable.bg_chip_active_yellow);
                    btn.setTextColor(Color.parseColor("#FFB300"));
                } else if (btn == btnKeHoach) {
                    btn.setBackgroundResource(R.drawable.bg_chip_active_green);
                    btn.setTextColor(Color.parseColor("#7CB342"));
                } else {
                    btn.setBackgroundResource(R.drawable.bg_chip_active);
                    btn.setTextColor(Color.parseColor("#E86FA0"));
                }
            } else {
                btn.setBackgroundResource(R.drawable.bg_chip_inactive);
                btn.setTextColor(Color.parseColor("#7D8BB7"));
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}