package com.example.heami.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Thêm import này

import com.example.heami.R;
import com.google.android.material.button.MaterialButton;

public class SetupBioActivity extends AppCompatActivity {

    private TextView tvPreviewAvatar, tvNicknameCount, tvBioCount;
    private EditText etNickname, etBio;
    private MaterialButton btnNextStep2;
    private ImageButton btnBack;

    private String currentAvatar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_bio);

        initViews();

        // LẤY DỮ LIỆU TỪ SHAREDPREFERENCES
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        currentAvatar = prefs.getString("user_avatar_emoji", "🌸"); // Mặc định là hoa nếu chưa có

        if (tvPreviewAvatar != null) {
            tvPreviewAvatar.setText(currentAvatar);
        }

        // 3. Khởi tạo trạng thái nút (Ban đầu bị khóa)
        updateButtonState(false);

        // --- PHẦN THÊM MỚI: HIỆU ỨNG KHI NHẤP VÀO Ô NHẬP LIỆU ---
        setupFocusEffects();
        // -------------------------------------------------------

        // 4. Bắt sự kiện nhập Nickname để mở khóa nút & đếm số
        etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvNicknameCount.setText(length + "/20");

                if (length >= 2) {
                    updateButtonState(true);
                } else {
                    updateButtonState(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 5. Bắt sự kiện nhập Bio để đếm số
        etBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvBioCount.setText(s.length() + "/60");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 6. Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // 7. Nút Tiếp theo (Chuyển sang Step 3)
        btnNextStep2.setOnClickListener(v -> {
            Intent intent = new Intent(SetupBioActivity.this, SetupGoalsActivity.class);
            startActivity(intent);
        });
    }

    private void initViews() {
        tvPreviewAvatar = findViewById(R.id.tvPreviewAvatar);
        etNickname = findViewById(R.id.etNickname);
        etBio = findViewById(R.id.etBio);
        tvNicknameCount = findViewById(R.id.tvNicknameCount);
        tvBioCount = findViewById(R.id.tvBioCount);
        btnNextStep2 = findViewById(R.id.btnNextStep2);
        btnBack = findViewById(R.id.btnBack);
    }

    // --- HÀM THÊM MỚI ĐỂ XỬ LÝ HIỆU ỨNG ---
    private void setupFocusEffects() {
        // Hiệu ứng cho ô Nickname
        final CardView cardNickname = (CardView) etNickname.getParent().getParent();
        etNickname.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cardNickname.setCardElevation(15f); // Nổi lên cao khi nhấn vào
            } else {
                cardNickname.setCardElevation(2f);  // Trở về bình thường khi bỏ chọn
            }
        });

        // Hiệu ứng cho ô Bio
        final CardView cardBio = (CardView) etBio.getParent().getParent();
        etBio.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cardBio.setCardElevation(15f); // Nổi lên cao khi nhấn vào
            } else {
                cardBio.setCardElevation(2f);  // Trở về bình thường khi bỏ chọn
            }
        });
    }

    // Hàm thay đổi trạng thái nút Tiếp theo
    private void updateButtonState(boolean isEnabled) {
        btnNextStep2.setEnabled(isEnabled);
        if (isEnabled) {
            btnNextStep2.setAlpha(1.0f);
            btnNextStep2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E86FA0")));
        } else {
            btnNextStep2.setAlpha(0.5f);
            btnNextStep2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F4B4CF")));
        }
    }
}