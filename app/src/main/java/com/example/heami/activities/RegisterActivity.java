package com.example.heami.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
    private AlertDialog otpDialog;
    private CountDownTimer countDownTimer;
    private long currentMillisUntilFinished = 60000;

    private EditText edtNickname, edtPhoneOrEmail, edtPass;
    private CheckBox cbTerms;
    private TextView txtErrorNickname, txtErrorAccount, txtErrorPassword, txtErrorTerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupLogic();
    }

    private void initViews() {
        edtNickname = findViewById(R.id.edtNickname);
        edtPhoneOrEmail = findViewById(R.id.edtRegisterEmail);
        edtPass = findViewById(R.id.edtRegisterPassword);
        ImageView imgShowHide = findViewById(R.id.imgRegShowHide);
        cbTerms = findViewById(R.id.cbTerms);
        TextView txtTerms = findViewById(R.id.txtTerms);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView txtLoginLink = findViewById(R.id.txtLoginLink);
        TextView txtForgotPassReg = findViewById(R.id.txtForgotPassReg);

        txtErrorNickname = findViewById(R.id.txtErrorNickname);
        txtErrorAccount = findViewById(R.id.txtErrorAccount);
        txtErrorPassword = findViewById(R.id.txtErrorPassword);
        txtErrorTerms = findViewById(R.id.txtErrorTerms);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupLoginLink(txtLoginLink);
        setupTermsText(txtTerms);

        edtPass.setOnFocusChangeListener((v, hasFocus) -> {
            int color = ContextCompat.getColor(this, hasFocus ? R.color.orange : R.color.text_hint);
            imgShowHide.setColorFilter(color);
        });

        imgShowHide.setOnClickListener(v -> {
            android.graphics.Typeface currentTypeface = edtPass.getTypeface();
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                edtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgShowHide.setImageResource(R.drawable.ic_visibility_off);
            } else {
                edtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgShowHide.setImageResource(R.drawable.ic_visibility);
            }
            edtPass.setTypeface(currentTypeface);
            edtPass.setSelection(edtPass.getText().length());
        });

        btnRegister.setOnClickListener(v -> validateAndRegister());
        txtLoginLink.setOnClickListener(v -> finish());
        txtForgotPassReg.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, ForgotPasswordActivity.class)));
    }

    private void setupLogic() {
        authViewModel.getAuthStatus().observe(this, status -> {
            if (status == null) return;

            if (status.startsWith("SUCCESS_REGISTER_EMAIL")) {
                Toast.makeText(this, status.replace("SUCCESS_REGISTER_EMAIL:", ""), Toast.LENGTH_LONG).show();
                finish();
            } else if (status.startsWith("SUCCESS_REGISTER_PHONE")) {
                if (otpDialog != null && otpDialog.isShowing()) otpDialog.dismiss();
                Toast.makeText(this, status.replace("SUCCESS_REGISTER_PHONE:", ""), Toast.LENGTH_LONG).show();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }, 500);
            } else if (status.startsWith("OTP_SENT:")) {
                showCustomOTPDialog();
            } else if (status.startsWith("ERROR:")) {
                String errorMsg = status.replace("ERROR:", "");
                // Phân loại lỗi: Nếu là lỗi kỹ thuật sâu thì đẩy vào Logcat, ngược lại hiện Toast
                if (errorMsg.contains("[") || errorMsg.contains("internal")) {
                    Log.e("AuthError", "Chi tiết lỗi Firebase: " + errorMsg);
                    Toast.makeText(this, "Đã xảy ra sự cố hệ thống, vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void validateAndRegister() {
        resetErrors();

        String nickname = edtNickname.getText().toString().trim();
        String account = edtPhoneOrEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        boolean termsChecked = cbTerms.isChecked();

        boolean isValid = true;

        if (nickname.isEmpty()) {
            showInputError(txtErrorNickname, "Vui lòng nhập biệt danh của bạn");
            isValid = false;
        }

        if (account.isEmpty()) {
            showInputError(txtErrorAccount, "Vui lòng nhập email hoặc số điện thoại");
            isValid = false;
        } else if (!isPhoneNumber(account) && !isValidEmail(account)) {
            showInputError(txtErrorAccount, "Định dạng email hoặc số điện thoại không hợp lệ");
            isValid = false;
        }

        if (pass.isEmpty()) {
            showInputError(txtErrorPassword, "Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (!isValidPassword(pass)) {
            showInputError(txtErrorPassword, "Mật khẩu ít nhất 6 ký tự, gồm chữ và số");
            isValid = false;
        }

        if (!termsChecked) {
            showInputError(txtErrorTerms, "Bạn cần đồng ý với điều khoản dịch vụ");
            isValid = false;
        }

        if (isValid) {
            authViewModel.register(account, pass, nickname, true, this);
        }
    }

    private void showInputError(TextView errorText, String message) {
        if (errorText == null) return;
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void resetErrors() {
        TextView[] errors = {txtErrorNickname, txtErrorAccount, txtErrorPassword, txtErrorTerms};
        for (TextView tv : errors) {
            if (tv != null) tv.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void showCustomOTPDialog() {
        if (otpDialog != null && otpDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_otp, null);
        builder.setView(view);

        ImageView btnClose = view.findViewById(R.id.btnClose);
        TextView txtSubtitle = view.findViewById(R.id.txtSubtitle);
        TextView txtResend = view.findViewById(R.id.txtResend);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        EditText[] otpBoxes = new EditText[]{
                view.findViewById(R.id.otp1), view.findViewById(R.id.otp2),
                view.findViewById(R.id.otp3), view.findViewById(R.id.otp4),
                view.findViewById(R.id.otp5), view.findViewById(R.id.otp6)
        };

        String phone = edtPhoneOrEmail.getText().toString().trim();
        String masked;
        if (phone.length() > 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < phone.length() - 3; i++) sb.append("*");
            sb.append(phone.substring(phone.length() - 3));
            masked = sb.toString();
        } else {
            masked = phone;
        }

        txtSubtitle.setText("Vui lòng nhập mã OTP đã được gửi đến số điện thoại\n" + masked);

        setupOTPInputs(otpBoxes);

        otpDialog = builder.create();
        if (otpDialog.getWindow() != null) {
            otpDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        otpDialog.setCancelable(false);

        btnClose.setOnClickListener(v -> {
            otpDialog.dismiss();
            authViewModel.cancelLoading();
        });

        btnConfirm.setOnClickListener(v -> {
            StringBuilder code = new StringBuilder();
            for (EditText box : otpBoxes) code.append(box.getText().toString());
            if (code.length() == 6) {
                authViewModel.verifyOTP(code.toString());
            } else {
                Toast.makeText(RegisterActivity.this, "Vui lòng nhập đủ 6 số OTP!", Toast.LENGTH_SHORT).show();
            }
        });

        otpDialog.show();

        if (countDownTimer == null) {
            startResendTimer();
        } else {
            updateTimerUI(txtResend, currentMillisUntilFinished);
        }
    }

    private void startResendTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                currentMillisUntilFinished = millisUntilFinished;
                if (otpDialog != null && otpDialog.isShowing()) {
                    TextView resend = otpDialog.findViewById(R.id.txtResend);
                    updateTimerUI(resend, millisUntilFinished);
                }
            }

            public void onFinish() {
                currentMillisUntilFinished = 0;
                if (otpDialog != null && otpDialog.isShowing()) {
                    TextView resend = otpDialog.findViewById(R.id.txtResend);
                    if (resend != null) {
                        String finishText = "Gửi lại mã ngay";
                        SpannableString ss = new SpannableString(finishText);
                        ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(RegisterActivity.this, R.color.orange)), 0, finishText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, finishText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        resend.setText(ss);
                        resend.setEnabled(true);
                        resend.setOnClickListener(v -> {
                            authViewModel.resendOTP(RegisterActivity.this);
                            startResendTimer();
                        });
                    }
                }
                countDownTimer = null;
            }
        }.start();
    }

    private void updateTimerUI(TextView tvResend, long millisUntilFinished) {
        if (tvResend == null) return;
        tvResend.setEnabled(false);

        String timeVal = String.valueOf(millisUntilFinished / 1000);
        String fullText = "Mã OTP có hạn sử dụng trong " + timeVal + "s. Gửi lại mã";
        SpannableString ss = new SpannableString(fullText);

        ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.grey)), 0, fullText.indexOf(timeVal), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int startOrange = fullText.indexOf(timeVal);
        if (startOrange != -1) {
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.orange)), startOrange, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int startBold = fullText.indexOf("Gửi lại mã");
        if(startBold != -1) {
            ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startBold, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvResend.setText(ss);
    }

    private void setupOTPInputs(EditText[] otpBoxes) {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int index = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpBoxes.length - 1) otpBoxes[index + 1].requestFocus();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            otpBoxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpBoxes[index].getText().toString().isEmpty() && index > 0) {
                        otpBoxes[index - 1].requestFocus();
                        otpBoxes[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private boolean isPhoneNumber(String input) {
        return input != null && input.matches("^[0-9]{9,11}$");
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return java.util.regex.Pattern.compile(emailPattern).matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}$";
        return java.util.regex.Pattern.compile(passwordRegex).matcher(password).matches();
    }

    private void setupTermsText(TextView textView) {
        String fullText = textView.getText().toString();
        SpannableString ss = new SpannableString(fullText);
        String target = "Điều khoản & Chính sách bảo mật";
        int start = fullText.indexOf(target);
        if (start != -1) {
            int end = start + target.length();
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_blue)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(ss);
        }
    }

    private void setupLoginLink(TextView textView) {
        String text = "Đăng nhập";
        SpannableString ss = new SpannableString(text);
        ss.setSpan(new UnderlineSpan(), 0, text.length(), 0);
        textView.setText(ss);
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
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}