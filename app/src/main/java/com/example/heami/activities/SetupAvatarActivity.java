package com.example.heami.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.android.material.button.MaterialButton;

public class SetupAvatarActivity extends AppCompatActivity {

    private TextView tvSelectedAvatar;
    private MaterialButton btnNextStep1;
    private GridLayout gridLayout;
    private String selectedAvatar = "🌸"; // Giá trị mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_avatar); // Tên file XML của bạn

        // 1. Ánh xạ view
        tvSelectedAvatar = findViewById(R.id.tvSelectedAvatar);
        btnNextStep1 = findViewById(R.id.btnNextStep1);
        gridLayout = findViewById(R.id.gridAvatar); // Bạn nhớ thêm android:id="@+id/gridAvatar" vào thẻ GridLayout trong XML nhé

        // 2. Thiết lập chọn Avatar từ GridLayout
        setupAvatarSelection();

        // 3. Xử lý khi bấm nút "Tiếp theo"
        btnNextStep1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển sang BioActivity (Step 2)
                Intent intent = new Intent(SetupAvatarActivity.this, SetupBioActivity.class);
                // Gửi kèm avatar đã chọn để trang sau hiển thị lại
                intent.putExtra("selected_avatar", selectedAvatar);
                startActivity(intent);
            }
        });
    }

    private void setupAvatarSelection() {
        // Lặp qua tất cả các con của GridLayout (các TextView chứa emoji)
        int childCount = gridLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = gridLayout.getChildAt(i);

            if (child instanceof TextView) {
                final TextView tvEmoji = (TextView) child;
                tvEmoji.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Lấy emoji từ TextView được click
                        selectedAvatar = tvEmoji.getText().toString();

                        // Cập nhật lên Avatar xem trước ở phía trên
                        tvSelectedAvatar.setText(selectedAvatar);
                    }
                });
            }
        }
    }
}
