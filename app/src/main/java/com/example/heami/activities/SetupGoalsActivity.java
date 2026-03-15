package com.example.heami.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.heami.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class SetupGoalsActivity extends AppCompatActivity {

    private GridLayout gridGoals;
    private TextView tvCount, btnSkip;
    private MaterialButton btnFinishSetup;
    private ImageButton btnBack;
    private View step1, step2, step3;
    private List<MaterialCardView> selectedGoals = new ArrayList<>();
    private final int MAX_GOALS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_goals);

        initViews();
        setupClickListeners();
        updateUI();
    }

    private void initViews() {
        gridGoals = findViewById(R.id.gridGoals);
        tvCount = findViewById(R.id.tvCount);
        btnFinishSetup = findViewById(R.id.btnFinishSetup);
        btnSkip = findViewById(R.id.btnSkipSetup);
        btnBack = findViewById(R.id.btnBack); // Nút mũi tên quay lại mới thêm

        // 3 vạch tiến trình trong Card hướng dẫn
        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
    }

    private void setupClickListeners() {
        // 1. Gán sự kiện cho các Card mục tiêu trong Grid
        for (int i = 0; i < gridGoals.getChildCount(); i++) {
            View child = gridGoals.getChildAt(i);
            if (child instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) child;
                card.setOnClickListener(v -> toggleGoalSelection(card));
            }
        }

        // 2. Nút Quay lại (Mũi tên trên cùng)
        btnBack.setOnClickListener(v -> {
            onBackPressed(); // Trở về Activity trước đó (ví dụ: SetupAvatarActivity)
        });

        // 3. Nút Bỏ qua
        btnSkip.setOnClickListener(v -> navigateToHome());

        // 4. Nút Hoàn tất
        btnFinishSetup.setOnClickListener(v -> {
            btnFinishSetup.setEnabled(false);
            btnFinishSetup.setText("Đang lưu hồ sơ...");

            // Giả lập thời gian lưu dữ liệu 1.5 giây trước khi vào Home
            new Handler().postDelayed(this::navigateToHome, 1500);
        });
    }

    private void toggleGoalSelection(MaterialCardView card) {
        if (selectedGoals.contains(card)) {
            selectedGoals.remove(card);
            deselectCard(card);
        } else if (selectedGoals.size() < MAX_GOALS) {
            selectedGoals.add(card);
            selectCard(card);
        }
        updateUI();
    }

    private void selectCard(MaterialCardView card) {
        // Đổi màu viền và nền khi chọn
        card.setStrokeColor(Color.parseColor("#E86FA0"));
        card.setStrokeWidth(4);
        card.setCardBackgroundColor(Color.parseColor("#FFF0F5")); // Hồng nhạt
    }

    private void deselectCard(MaterialCardView card) {
        // Trả về trạng thái trắng ban đầu
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(Color.WHITE);
    }

    private void updateUI() {
        int count = selectedGoals.size();
        tvCount.setText(count + "/" + MAX_GOALS);

        // Cập nhật 3 vạch màu hồng tùy theo số lượng mục tiêu đã chọn
        step1.setBackgroundColor(count >= 1 ? Color.parseColor("#E86FA0") : Color.parseColor("#E0E0E0"));
        step2.setBackgroundColor(count >= 2 ? Color.parseColor("#E86FA0") : Color.parseColor("#E0E0E0"));
        step3.setBackgroundColor(count >= 3 ? Color.parseColor("#E86FA0") : Color.parseColor("#E0E0E0"));

        // Chỉ cho phép bấm nút "Hoàn tất" nếu đã chọn ít nhất 1 mục tiêu
        boolean hasSelected = count >= 1;
        btnFinishSetup.setEnabled(hasSelected);
        btnFinishSetup.setAlpha(hasSelected ? 1.0f : 0.5f);
    }

    private void navigateToHome() {
        // Sửa HomeActivity.class thành ProfileActivity.class
        Intent intent = new Intent(this, HomeActivity.class);

        // Giữ nguyên các Flag này để xóa lịch sử các trang setup cũ
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);

        // Hiệu ứng chuyển cảnh mờ dần
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        finish();
    }
}