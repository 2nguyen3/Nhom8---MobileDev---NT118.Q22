package com.example.heami.activities;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.heami.R;
import com.example.heami.viewmodels.AuthViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private EditText edtEmail;
    private TextView txtErrorEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Ánh xạ View
        edtEmail = findViewById(R.id.edtForgotEmail);
        txtErrorEmail = findViewById(R.id.txtErrorForgotEmail);

        View btnBack = findViewById(R.id.btnBack);
        Button btnSend = findViewById(R.id.btnSendResetLink);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Lắng nghe kết quả từ AuthViewModel
        authViewModel.getAuthStatus().observe(this, status -> {
            if (status == null) return;
            if (status.startsWith("SUCCESS_RESET")) {
                Toast.makeText(this, status.replace("SUCCESS_RESET:", ""), Toast.LENGTH_LONG).show();
                finish();
            } else if (status.startsWith("ERROR:")) {
                Toast.makeText(this, status.replace("ERROR:", ""), Toast.LENGTH_LONG).show();
            }
        });

        // Xử lý sự kiện click
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> validateAndReset());
    }

    // Logic kiểm tra dữ liệu đầu vào và thực hiện yêu cầu đổi mật khẩu
    private void validateAndReset() {
        txtErrorEmail.setVisibility(View.GONE);
        String email = edtEmail.getText().toString().trim();

        if (email.isEmpty()) {
            showInputError("Vui lòng nhập email của bạn");
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showInputError("Định dạng email không hợp lệ");
        } else {
            authViewModel.resetPassword(email);
        }
    }

    private void showInputError(String message) {
        txtErrorEmail.setText(message);
        txtErrorEmail.setVisibility(View.VISIBLE);
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
                    findViewById(android.R.id.content).requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
