package com.example.heami.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.heami.R;
import com.example.heami.ui.auth.SetupAvatarActivity;
import com.google.android.material.button.MaterialButton;

public class PermissionActivity extends AppCompatActivity {
    private MaterialButton btnAllowCam, btnAllowNoti, btnSkipNoti, btnAllowLocation, btnSkipLocation, btnContinueSetup;
    private TextView tvProgressStatus, btnSkipAll, tvSkipNoteNoti, tvSkipNoteLocation;
    private int actionCount = 0;

    private static final int REQ_CAMERA = 101;
    private static final int REQ_LOCATION = 102;
    private static final int REQ_NOTIFICATION = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        initViews();

        // Sự kiện Cho phép
        btnAllowCam.setOnClickListener(v -> requestCameraPermission());
        btnAllowNoti.setOnClickListener(v -> requestNotificationPermission());
        btnAllowLocation.setOnClickListener(v -> requestLocationPermission());

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

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            markAsGranted(btnAllowCam, "#FF4D8D", "#FFF5F8");
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        } else {
            markAsGranted(btnAllowLocation, "#B197E2", "#F3EBF9");
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
            } else {
                markAsGranted(btnAllowNoti, "#4DB6AC", "#EAF8F7");
            }
        } else {
            // Android < 13 mặc định có quyền notification
            markAsGranted(btnAllowNoti, "#4DB6AC", "#EAF8F7");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        switch (requestCode) {
            case REQ_CAMERA:
                if (granted) markAsGranted(btnAllowCam, "#FF4D8D", "#FFF5F8");
                else Toast.makeText(this, "Quyền Camera bị từ chối", Toast.LENGTH_SHORT).show();
                break;
            case REQ_LOCATION:
                if (granted) markAsGranted(btnAllowLocation, "#B197E2", "#F3EBF9");
                else Toast.makeText(this, "Quyền Vị trí bị từ chối", Toast.LENGTH_SHORT).show();
                break;
            case REQ_NOTIFICATION:
                if (granted) markAsGranted(btnAllowNoti, "#4DB6AC", "#EAF8F7");
                else Toast.makeText(this, "Quyền Thông báo bị từ chối", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void markAsGranted(MaterialButton btn, String btnColor, String cardBg) {
        if (!btn.isEnabled()) return;

        actionCount++;
        btn.setText("✓ Đã cấp quyền");
        btn.setEnabled(false);
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(btnColor)));
        btn.setTextColor(Color.WHITE);

        if (btn == btnAllowNoti && btnSkipNoti != null) {
            btnSkipNoti.setVisibility(View.GONE);
        } else if (btn == btnAllowLocation && btnSkipLocation != null) {
            btnSkipLocation.setVisibility(View.GONE);
        }

        try {
            View parent = (View) btn.getParent();
            CardView card;
            if (btn == btnAllowCam) {
                card = (CardView) parent.getParent();
            } else {
                card = (CardView) parent.getParent().getParent();
            }
            card.setCardBackgroundColor(Color.parseColor(cardBg));
            card.setAlpha(1.0f);
        } catch (Exception ignored) {}

        updateUI();
    }

    private void markAsSkipped(MaterialButton btnSkip, TextView tvNote) {
        if (btnSkip.getVisibility() == View.GONE) return;

        actionCount++;
        try {
            View layoutActions = (View) btnSkip.getParent();
            CardView card = (CardView) layoutActions.getParent().getParent();
            card.setAlpha(0.5f);
            layoutActions.setVisibility(View.GONE);
            if (tvNote != null) tvNote.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {}

        updateUI();
    }

    private void updateUI() {
        tvProgressStatus.setText(actionCount + "/3 hoàn thành");
        if (actionCount >= 1) {
            btnContinueSetup.setVisibility(View.VISIBLE);
        }
        if (actionCount >= 3) {
            if (btnSkipAll != null) {
                btnSkipAll.setVisibility(View.GONE);
            }
        }
    }

    private void goToNextStep() {
        Intent intent = new Intent(this, SetupAvatarActivity.class);
        startActivity(intent);
        finish();
    }
}
