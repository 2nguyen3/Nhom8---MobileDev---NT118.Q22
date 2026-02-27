package com.example.heami.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.heami.R;
import com.google.android.material.button.MaterialButton;

public class PermissionActivity extends AppCompatActivity {
    private MaterialButton btnAllowCam, btnAllowNoti, btnSkipNoti, btnAllowLocation, btnSkipLocation, btnContinueSetup;
    private TextView tvProgressStatus, btnSkipAll, tvSkipNoteNoti, tvSkipNoteLocation;
    private int actionCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        initViews();

        // Sự kiện Cho phép
        btnAllowCam.setOnClickListener(v -> markAsGranted(btnAllowCam, "#FF4D8D", "#FFF5F8"));
        btnAllowNoti.setOnClickListener(v -> markAsGranted(btnAllowNoti, "#4DB6AC", "#EAF8F7"));
        btnAllowLocation.setOnClickListener(v -> markAsGranted(btnAllowLocation, "#B197E2", "#F3EBF9"));

        // Sự kiện Bỏ qua
        btnSkipNoti.setOnClickListener(v -> markAsSkipped(btnSkipNoti, tvSkipNoteNoti));
        btnSkipLocation.setOnClickListener(v -> markAsSkipped(btnSkipLocation, tvSkipNoteLocation));

        // Điều hướng
        btnContinueSetup.setOnClickListener(v -> goToNextStep());
        btnSkipAll.setOnClickListener(v -> goToNextStep());
    }

    private void initViews() {
        btnAllowCam = findViewById(R.id.btnAllowCam);
        btnAllowNoti = findViewById(R.id.btnAllowNoti);
        btnSkipNoti = findViewById(R.id.btnSkipNoti);
        btnAllowLocation = findViewById(R.id.btnAllowLocation);
        btnSkipLocation = findViewById(R.id.btnSkipLocation);
        btnContinueSetup = findViewById(R.id.btnContinueSetup);
        tvProgressStatus = findViewById(R.id.tvProgressStatus);
        btnSkipAll = findViewById(R.id.btnSkipAll);
        tvSkipNoteNoti = findViewById(R.id.tvSkipNoteNoti);
        tvSkipNoteLocation = findViewById(R.id.tvSkipNoteLocation);
    }

    private void markAsGranted(MaterialButton btn, String btnColor, String cardBg) {
        if (!btn.isEnabled()) return;

        actionCount++;
        btn.setText("✓ Đã cấp quyền");
        btn.setEnabled(false);
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(btnColor)));
        btn.setTextColor(Color.WHITE);

        // --- CHÍNH SỬA TẠI ĐÂY: Ẩn nút Bỏ qua ngay lập tức ---
        if (btn == btnAllowNoti && btnSkipNoti != null) {
            btnSkipNoti.setVisibility(View.GONE);
        } else if (btn == btnAllowLocation && btnSkipLocation != null) {
            btnSkipLocation.setVisibility(View.GONE);
        }

        try {
            View parent = (View) btn.getParent();
            CardView card;

            // Camera thường nằm trực tiếp trong layout của Card
            // Noti/Location thường nằm trong một LinearLayout/ConstraintLayout chứa cả nút Bỏ qua
            if (btn == btnAllowCam) {
                card = (CardView) parent.getParent();
            } else {
                // Duyệt ngược lên để tìm CardView chứa cả cụm nút
                card = (CardView) parent.getParent().getParent();
            }

            card.setCardBackgroundColor(Color.parseColor(cardBg));
            card.setAlpha(1.0f);
        } catch (Exception ignored) {
            // Nếu cấu trúc View XML của bạn phức tạp hơn, có thể dùng findViewById từ parent để tìm Card
        }

        updateUI();
    }

    private void markAsSkipped(MaterialButton btnSkip, TextView tvNote) {
        if (btnSkip.getVisibility() == View.GONE) return;

        actionCount++;
        try {
            View layoutActions = (View) btnSkip.getParent();
            CardView card = (CardView) layoutActions.getParent().getParent();

            card.setAlpha(0.5f);
            layoutActions.setVisibility(View.GONE); // Ẩn cả cụm nút (bao gồm cả nút Cho phép)
            if (tvNote != null) tvNote.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {}

        updateUI();
    }

    private void updateUI() {
        tvProgressStatus.setText(actionCount + "/3 hoàn thành");

        // Hiện nút "Tiếp tục thiết lập hồ sơ" nếu đã xử lý ít nhất 1 quyền
        if (actionCount >= 1) {
            btnContinueSetup.setVisibility(View.VISIBLE);
        }

        // --- CẬP NHẬT TẠI ĐÂY: Ẩn nút "Bỏ qua tất cả" khi đã hoàn thành 3/3 ---
        if (actionCount >= 3) {
            if (btnSkipAll != null) {
                btnSkipAll.setVisibility(View.GONE);
            }
        }
    }

    private void goToNextStep() {
        Intent intent = new Intent(this, SetupAvatarActivity.class);
        //Intent intent = new Intent(this, ProfileSetupActivity.class);
        startActivity(intent);
        finish();
    }
}
