package com.example.heami.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.example.heami.R;
import com.example.heami.viewmodels.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private boolean isPasswordVisible = false; // Biến theo dõi trạng thái ẩn/hiện

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ các View từ XML
        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPass = findViewById(R.id.edtPassword);
        ImageView imgShowHide = findViewById(R.id.imgShowHidePassword); // ID của ic_visibility
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoogle = findViewById(R.id.btnGoogle);
        TextView txtCreateAccount = findViewById(R.id.txtCreateAccount);
        setupCreateAccountLink(txtCreateAccount);
        View layoutLoading = findViewById(R.id.layoutLoading);
        LottieAnimationView lottieView = findViewById(R.id.loadingView);

        // 2. Kết nối với ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 3. Logic Ẩn/Hiện mật khẩu
        imgShowHide.setOnClickListener(v -> {
            // Đảo ngược trạng thái
            isPasswordVisible = !isPasswordVisible;

            if (isPasswordVisible) {
                // TRẠNG THÁI HIỆN: Hiển thị chữ, đổi icon sang ẩn
                edtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgShowHide.setImageResource(R.drawable.ic_visibility_off);
            } else {
                // TRẠNG THÁI ẨN: Hiển thị dấu chấm, đổi icon sang xem
                edtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgShowHide.setImageResource(R.drawable.ic_visibility);
            }

            // Lấy font của ô Email áp sang cho đồng bộ
            edtPass.setTypeface(edtEmail.getTypeface());

            // Đưa con trỏ về cuối văn bản
            edtPass.setSelection(edtPass.getText().length());
        });

        // 4. Lắng nghe kết quả từ ViewModel
        authViewModel.getAuthStatus().observe(this, status -> {
            if (status != null && status.startsWith("SUCCESS")) {
                // Sử dụng Handler chuẩn để đợi Animation chạy thêm một chút
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {

                    // 1. Chuyển sang trang chính
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);

                    // 2. Sau đó mới đóng trang Đăng nhập
                    finish();

                    // Thêm hiệu ứng chuyển cảnh mượt mà
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                }, 500);
            } else if (status != null) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            }
        });

        // 4.1 Lắng nghe trạng thái Loading
        authViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                if (isLoading) {
                    layoutLoading.setVisibility(View.VISIBLE);
                    lottieView.playAnimation();
                    btnLogin.setEnabled(false);
                } else {
                    // Khi load xong (isLoading = false), ta kiểm tra kết quả
                    String status = authViewModel.getAuthStatus().getValue();
                    // NẾU THẤT BẠI: Tắt ngay lớp phủ để người dùng nhập lại
                    if (status == null || !status.startsWith("SUCCESS")) {
                        layoutLoading.setVisibility(View.GONE);
                        lottieView.pauseAnimation();
                        btnLogin.setEnabled(true);
                    }
                    // NẾU THÀNH CÔNG: để phần authStatus bên dưới xử lý
                }
            }
        });

        // 5. Xử lý các sự kiện Click
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPass.getText().toString().trim();

            authViewModel.login(email, pass);
        });

        btnGoogle.setOnClickListener(v -> Toast.makeText(this, "Tính năng Đăng nhập Google đang được phát triển!", Toast.LENGTH_SHORT).show());
    }

    private void setupCreateAccountLink(TextView textView) {
        String fullText = "Người mới? Tạo tài khoản ngay ✨";
        SpannableString ss = new SpannableString(fullText);

        // Xác định cụm từ cần tô màu và gắn link
        String target = "Tạo tài khoản ngay";
        int start = fullText.indexOf(target);
        int end = start + target.length();

        // Tô màu Teal
        ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.teal)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ss.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Gắn sự kiện Click để chuyển sang trang Đăng ký
        android.text.style.ClickableSpan clickableSpan = new android.text.style.ClickableSpan() {
            @Override
            public void onClick(@androidx.annotation.NonNull android.view.View widget) {
                // Lệnh chuyển trang
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@androidx.annotation.NonNull android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                // Đảm bảo màu sắc không bị đổi về màu mặc định của hệ thống
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.teal));
            }
        };

        ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Cấu hình để TextView có thể nhấn được
        textView.setText(ss);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        textView.setHighlightColor(android.graphics.Color.TRANSPARENT); // Bỏ màu nền khi nhấn vào
    }
}