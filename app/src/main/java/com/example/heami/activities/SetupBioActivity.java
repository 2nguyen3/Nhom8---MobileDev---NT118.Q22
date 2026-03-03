package com.example.heami.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.heami.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SetupBioActivity extends AppCompatActivity {

    private TextView tvPreviewAvatar, tvNicknameCount, tvBioCount;
    private EditText etNickname, etBio;
    private MaterialButton btnNextStep2;
    private ImageButton btnBack;

    private String currentAvatar;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_bio);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        loadUserData();

        // LẤY DỮ LIỆU TỪ SHAREDPREFERENCES (Đã được lưu ở bước chọn Avatar)
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        currentAvatar = prefs.getString("user_avatar_emoji", "🌸");

        if (tvPreviewAvatar != null) {
            tvPreviewAvatar.setText(currentAvatar);
        }

        updateButtonState(false);
        setupFocusEffects();

        // Theo dõi nhập Nickname
        etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvNicknameCount.setText(length + "/20");
                updateButtonState(length >= 2);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Theo dõi nhập Bio
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

        btnBack.setOnClickListener(v -> finish());

        // Nút Tiếp theo: Lưu Nickname và Bio vào database
        btnNextStep2.setOnClickListener(v -> saveBioAndNavigate());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mở khóa lại nút khi quay lại từ trang tiếp theo
        if (btnNextStep2 != null) {
            String nickname = etNickname.getText().toString().trim();
            updateButtonState(nickname.length() >= 2);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
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

    private void loadUserData() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nickname = documentSnapshot.getString("nickname");
                            if (nickname != null) {
                                etNickname.setText(nickname);
                                tvNicknameCount.setText(nickname.length() + "/20");
                                updateButtonState(nickname.length() >= 2);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Không thể tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void saveBioAndNavigate() {
        String newNickname = etNickname.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (currentUser != null) {
            btnNextStep2.setEnabled(false);
            Map<String, Object> updates = new HashMap<>();
            updates.put("nickname", newNickname);
            updates.put("motto", bio);

            db.collection("users").document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Intent intent = new Intent(SetupBioActivity.this, SetupGoalsActivity.class);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        btnNextStep2.setEnabled(true);
                        Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupFocusEffects() {
        if (etNickname.getParent() != null && etNickname.getParent().getParent() instanceof CardView) {
            final CardView cardNickname = (CardView) etNickname.getParent().getParent();
            etNickname.setOnFocusChangeListener((v, hasFocus) -> cardNickname.setCardElevation(hasFocus ? 15f : 2f));
        }

        if (etBio.getParent() != null && etBio.getParent().getParent() instanceof CardView) {
            final CardView cardBio = (CardView) etBio.getParent().getParent();
            etBio.setOnFocusChangeListener((v, hasFocus) -> cardBio.setCardElevation(hasFocus ? 15f : 2f));
        }
    }

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
