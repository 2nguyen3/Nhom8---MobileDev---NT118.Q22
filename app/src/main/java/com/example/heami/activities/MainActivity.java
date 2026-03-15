package com.example.heami.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        Button btnLogout = findViewById(R.id.btnLogout);

        // Kiểm tra xem User có đang đăng nhập không
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Nếu chưa đăng nhập, quay về màn hình Login
            backToLogin();
        }

        TextView txtWelcome = findViewById(R.id.txtWelcome);
        if (currentUser != null && currentUser.getDisplayName() != null) {
            txtWelcome.setText("Chào cậu, " + currentUser.getDisplayName() + " 🌿");
        }

        // Xử lý nút Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // Đăng xuất khỏi Firebase
            Toast.makeText(this, "Hẹn gặp lại bạn sớm nhé! 👋", Toast.LENGTH_SHORT).show();
            backToLogin();
        });
    }

    private void backToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Đóng MainActivity để người dùng không bấm Back quay lại được
    }
}