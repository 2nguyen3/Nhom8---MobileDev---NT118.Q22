package com.example.heami.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.heami.R;
import com.example.heami.viewmodels.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Ánh xạ View
        EditText edtNickname = findViewById(R.id.edtNickname);
        EditText edtEmail = findViewById(R.id.edtRegisterEmail);
        EditText edtPass = findViewById(R.id.edtRegisterPassword);
        ImageView imgShowHide = findViewById(R.id.imgRegShowHide);
        CheckBox cbTerms = findViewById(R.id.cbTerms);
        TextView txtTerms = findViewById(R.id.txtTerms);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView txtLoginLink = findViewById(R.id.txtLoginLink);
        setupLoginLink(txtLoginLink);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 2. Xử lý Spannable cho phần Điều khoản
        setupTermsText(txtTerms);

        // 3. Logic ẩn/hiện mật khẩu
        imgShowHide.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                edtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgShowHide.setImageResource(R.drawable.ic_visibility_off);
            } else {
                edtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgShowHide.setImageResource(R.drawable.ic_visibility);
            }
            edtPass.setTypeface(android.graphics.Typeface.DEFAULT); // Giữ font Quicksand
            edtPass.setSelection(edtPass.getText().length());
        });

        // 4. Lắng nghe kết quả từ ViewModel (BT03)
        authViewModel.getAuthStatus().observe(this, status -> {
            if (status != null) {
                if (status.startsWith("SUCCESS_REGISTER")) {
                    Toast.makeText(this, "Tạo tài khoản thành công! 🌿", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại trang Login
                } else {
                    Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 5. Xử lý sự kiện nút Tạo tài khoản
        btnRegister.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPass.getText().toString().trim();
            String nickname = edtNickname.getText().toString().trim();

            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Vui lòng đồng ý với điều khoản", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(email, pass, nickname);
        });

        // Chuyển về trang Login
        txtLoginLink.setOnClickListener(v -> finish());
    }

    private void setupTermsText(TextView textView) {
        // 1. Lấy chuỗi trực tiếp từ XML
        String fullText = textView.getText().toString();
        SpannableString ss = new SpannableString(fullText);

        // 2. Xác định chính xác cụm từ cần "lên màu"
        String target = "Điều khoản & Chính sách bảo mật";
        int start = fullText.indexOf(target);

        // Nếu tìm thấy cụm từ (start != -1)
        if (start != -1) {
            int end = start + target.length();

            // Đổi màu xanh
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_blue)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Gạch chân
            ss.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            textView.setText(ss);
        }
    }

    private void setupLoginLink(TextView textView) {
        String text = "Đăng nhập";
        SpannableString ss = new SpannableString(text);

        // Gạch chân toàn bộ chữ "Đăng nhập"
        ss.setSpan(new android.text.style.UnderlineSpan(), 0, text.length(), 0);

        textView.setText(ss);
    }
}