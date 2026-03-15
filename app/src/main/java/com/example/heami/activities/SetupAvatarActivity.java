package com.example.heami.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;

import com.example.heami.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SetupAvatarActivity extends AppCompatActivity {

    private TextView tvSelectedAvatar;
    private MaterialButton btnNextStep1;
    private GridLayout gridLayout;
    private String selectedAvatar = "🌸"; // Giá trị mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_avatar);

        // 1. Ánh xạ view
        tvSelectedAvatar = findViewById(R.id.tvSelectedAvatar);
        btnNextStep1 = findViewById(R.id.btnNextStep1);
        gridLayout = findViewById(R.id.gridAvatar);

        // 2. Thiết lập chọn Avatar từ GridLayout
        setupAvatarSelection();

        // 3. Xử lý khi bấm nút "Tiếp theo"
        btnNextStep1.setOnClickListener(v -> {
            String avatarToPass = tvSelectedAvatar.getText().toString();

            // LƯU VÀO BỘ NHỚ MÁY
            SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
            prefs.edit().putString("user_avatar_emoji", avatarToPass).apply();

            // LƯU VÀO FIREBASE FIRESTORE
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                btnNextStep1.setEnabled(false); // Khóa nút để tránh spam
                FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                        .update("avatar_url", avatarToPass)
                        .addOnSuccessListener(aVoid -> {
                            // Chuyển sang Bio sau khi lưu thành công
                            Intent intent = new Intent(SetupAvatarActivity.this, SetupBioActivity.class);
                            startActivity(intent);
                        })
                        .addOnFailureListener(e -> {
                            btnNextStep1.setEnabled(true);
                            Toast.makeText(SetupAvatarActivity.this, "Lỗi lưu avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mở khóa lại nút khi quay lại từ trang Bio
        if (btnNextStep1 != null) {
            btnNextStep1.setEnabled(true);
        }
    }

    private void setupAvatarSelection() {
        int childCount = gridLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = gridLayout.getChildAt(i);
            if (child instanceof TextView) {
                final TextView tvEmoji = (TextView) child;
                tvEmoji.setOnClickListener(v -> {
                    selectedAvatar = tvEmoji.getText().toString();
                    tvSelectedAvatar.setText(selectedAvatar);
                });
            }
        }
    }
}
