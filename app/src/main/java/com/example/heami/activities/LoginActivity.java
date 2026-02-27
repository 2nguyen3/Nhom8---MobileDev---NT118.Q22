package com.example.heami.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.CancellationSignal;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import com.airbnb.lottie.LottieAnimationView;
import com.example.heami.R;
import com.example.heami.viewmodels.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private boolean isPasswordVisible = false;
    private CredentialManager credentialManager;

    // Đợi hệ thống Android thêm tài khoản xong rồi tự động chạy tiếp
    private final ActivityResultLauncher<Intent> addAccountLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> signInWithGoogleNewAPI()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ View
        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPass = findViewById(R.id.edtPassword);
        ImageView imgShowHide = findViewById(R.id.imgShowHidePassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        View btnGoogle = findViewById(R.id.btnGoogle);
        TextView txtCreateAccount = findViewById(R.id.txtCreateAccount);
        View layoutLoading = findViewById(R.id.layoutLoading);
        LottieAnimationView lottieView = findViewById(R.id.loadingView);
        TextView txtForgotPass = findViewById(R.id.txtForgotPass);

        // hiết lập ban đầu
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        credentialManager = CredentialManager.create(this);
        setupCreateAccountLink(txtCreateAccount);

        // Link Quên mật khẩu
        txtForgotPass.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        // Đổi màu ic_visibility khi active/focus mật khẩu
        edtPass.setOnFocusChangeListener((v, hasFocus) -> {
            int color = ContextCompat.getColor(this, hasFocus ? R.color.teal : R.color.text_hint);
            imgShowHide.setColorFilter(color);
        });

        // Quan sát trạng thái Auth
        authViewModel.getAuthStatus().observe(this, status -> {
            if (status != null && status.startsWith("SUCCESS")) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }, 500);
            } else if (status != null) {
                // Tắt loading nếu có lỗi
                layoutLoading.setVisibility(View.GONE);
                lottieView.pauseAnimation();
                btnLogin.setEnabled(true);
                // CHỈ HIỂN THỊ LỖI QUAN TRỌNG CHO NGƯỜI DÙNG
                Toast.makeText(this, status.replace("ERROR:", ""), Toast.LENGTH_SHORT).show();
            }
        });

        // Quan sát trạng thái Loading
        authViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                layoutLoading.setVisibility(View.VISIBLE);
                lottieView.playAnimation();
                btnLogin.setEnabled(false);
            }
        });

        // Xử lý sự kiện Click
        imgShowHide.setOnClickListener(v -> togglePasswordVisibility(edtEmail, edtPass, imgShowHide));

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPass.getText().toString().trim();
            authViewModel.login(email, pass);
        });

        btnGoogle.setOnClickListener(v -> signInWithGoogleNewAPI());
    }

    /**
     * Logic đăng nhập Google với Credential Manager
     */
    private void signInWithGoogleNewAPI() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        androidx.credentials.Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential &&
                                credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            try {
                                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                                String idToken = googleIdTokenCredential.getIdToken();
                                authViewModel.signInWithGoogle(idToken);
                            } catch (Exception e) {
                                Log.e("GoogleAuthError", "Lỗi xử lý dữ liệu Google: " + e.getMessage());
                            }
                        } else {
                            Log.w("GoogleAuthError", "Loại tài khoản không được hỗ trợ");
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) {
                            Log.d("GoogleAuthError", "Người dùng đã hủy chọn tài khoản Google.");
                        } else if (e instanceof NoCredentialException) {
                            Toast.makeText(LoginActivity.this, "Vui lòng đăng nhập Google trên máy để tiếp tục!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
                            intent.putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"});
                            addAccountLauncher.launch(intent);
                        } else {
                            Log.e("GoogleAuthError", "Lỗi xác thực sâu: ", e);
                        }
                    }
                }
        );
    }

    private void togglePasswordVisibility(EditText edtEmail, EditText edtPass, ImageView imgShowHide) {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            edtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imgShowHide.setImageResource(R.drawable.ic_visibility_off);
        } else {
            edtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imgShowHide.setImageResource(R.drawable.ic_visibility);
        }
        edtPass.setTypeface(edtEmail.getTypeface());
        edtPass.setSelection(edtPass.getText().length());
    }

    private void setupCreateAccountLink(TextView textView) {
        String fullText = "Người mới? Tạo tài khoản ngay";
        SpannableString ss = new SpannableString(fullText);

        String target = "Tạo tài khoản ngay";
        int start = fullText.indexOf(target);
        int end = start + target.length();

        ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.teal)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
            @Override
            public void updateDrawState(@NonNull android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.teal));
            }
        };

        ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(ss);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        textView.setHighlightColor(android.graphics.Color.TRANSPARENT);
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

                    View rootView = findViewById(android.R.id.content);
                    rootView.setFocusableInTouchMode(true);
                    rootView.requestFocus();

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}