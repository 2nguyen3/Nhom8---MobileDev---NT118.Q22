package com.example.heami.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class TherapyActivity extends AppCompatActivity {

    private TextView btnTatCa, btnAmThanh, btnHitTho, btnNhatKy, btnKeHoach;
    private LinearLayout layoutAmThanh, layoutHitTho, layoutNhatKy, layoutKeHoach;
    private ImageView imgCloud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_therapy);

        initViews();
        setupFilterListeners();

        // 1. Khởi tạo đám mây nhảy múa
        startCloudAnimation();

        // 2. QUAN TRỌNG: Thiết lập Bottom Navigation ngay từ đầu
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

    /**
     * Tối ưu hàm lọc để không gây nghẽn thanh Bottom Navigation
     */
    private void applyFilter(String category, TextView selectedButton) {
        View[] layouts = {layoutAmThanh, layoutHitTho, layoutNhatKy, layoutKeHoach};

        // Bước 1: Cho tất cả mờ dần đồng loạt để tạo hiệu ứng mượt
        for (View layout : layouts) {
            layout.animate().alpha(0f).setDuration(100).start();
        }

        // Bước 2: Sử dụng postDelayed để tách biệt luồng xử lý Animation và Logic hiển thị
        // Việc này giúp thanh Bottom Nav luôn nhận được tương tác từ người dùng
        selectedButton.postDelayed(() -> {
            layoutAmThanh.setVisibility(View.GONE);
            layoutHitTho.setVisibility(View.GONE);
            layoutNhatKy.setVisibility(View.GONE);
            layoutKeHoach.setVisibility(View.GONE);

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

            // Cập nhật màu sắc cho các nút Chip
            updateChipStyles(selectedButton);

        }, 120); // Delay nhẹ 120ms
    }

    private void showWithAnimation(View view) {
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
            if (btn == selectedButton) {
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

    // Xử lý nút Back hệ thống để luôn về HomeActivity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        android.content.Intent intent = new android.content.Intent(this, HomeActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}