package com.example.heami.ui.consultation;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.heami.R;
import com.example.heami.data.models.BookingModel;
import com.example.heami.data.models.ConsultationModel;
import com.example.heami.utils.VNPAYHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingFlowActivity extends AppCompatActivity {

    private ImageButton btnBackFlow;
    private View btnMainAction;
    private View layoutBtnPayVnpay;
    private View btnComplete;
    private int currentStep = 1;
    private BookingModel bookingModel;
    private FirebaseFirestore db;
    private String lastSessionId;
    private String lastTransactionId;
    private String vnp_TransactionNo = "";

    // Stepper Views
    private TextView step1Number, step2Number, step3Number;
    private View step1Divider, step2Divider;
    private TextView step1Label, step2Label, step3Label;

    // Countdown Timer
    private TextView txtBookingTimer;
    private android.os.CountDownTimer countDownTimer;
    private long remainingTimeMs = 15 * 60 * 1000; // Thời gian còn lại (mặc định 15 phút)

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        android.util.Log.d("BookingFlow", "onNewIntent called. Action: " + intent.getAction() + ", Data: " + intent.getData());
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            android.util.Log.d("BookingFlow", "handleDeepLink: intent or data is null");
            return;
        }
        
        Uri uri = intent.getData();
        String urlStr = uri.toString();
        android.util.Log.d("BookingFlow", "handleDeepLink intercepted: " + urlStr);
        android.util.Log.d("BookingFlow", "Current bookingModel state in handleDeepLink: " + (bookingModel != null ? "EXISTS" : "NULL"));

        if (urlStr.toLowerCase().contains("vnpay_return")) {
            String responseCode = uri.getQueryParameter("vnp_ResponseCode");
            String transactionNo = uri.getQueryParameter("vnp_TransactionNo");

            android.util.Log.d("BookingFlow", "responseCode=" + responseCode + " txNo=" + transactionNo);

            if ("00".equals(responseCode)) {
                vnp_TransactionNo = (transactionNo != null) ? transactionNo : "";
                lastTransactionId = vnp_TransactionNo;

                // Hiển thị Toast thông báo thành công
                Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();

                // Lưu Firestore ở background
                saveConsultationToFirestore();

                // Đồng thời chuyển sang Step 3
                if (!isFinishing() && !isDestroyed()) {
                    updateStepUI(3);
                }
            } else {
                String msg = (responseCode == null || responseCode.isEmpty())
                        ? "Đã huỷ thanh toán."
                        : "Thanh toán không thành công (mã: " + responseCode + "). Vui lòng thử lại.";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                // Tiếp tục đếm ngược từ thời điểm đã dừng (không đếm lại từ đầu)
                startCountdownTimer(remainingTimeMs);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_flow);

        android.util.Log.d("BookingFlow", "onCreate called. TaskId: " + getTaskId() + ", savedInstanceState is " + (savedInstanceState == null ? "NULL" : "NOT NULL"));
        android.util.Log.d("BookingFlow", "onCreate Intent Data: " + getIntent().getData());

        db = FirebaseFirestore.getInstance();
        if (savedInstanceState != null) {
            bookingModel = (BookingModel) savedInstanceState.getSerializable("booking_model");
            currentStep = savedInstanceState.getInt("current_step", 1);
            android.util.Log.d("BookingFlow", "onCreate: Restored bookingModel from savedInstanceState: " + (bookingModel != null ? "EXISTS" : "NULL"));
        } else {
            bookingModel = (BookingModel) getIntent().getSerializableExtra("booking_model");
            android.util.Log.d("BookingFlow", "onCreate: Extracted bookingModel from intent: " + (bookingModel != null ? "EXISTS" : "NULL"));
        }

        initViews();
        setupListeners();
        setupBackNavigation();
        updateStepUI(currentStep);

        // Xử lý deep link nếu được mở từ ban đầu (mặc dù trường hợp này hiếm hơn onNewIntent)
        handleDeepLink(getIntent());
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        android.util.Log.d("BookingFlow", "onSaveInstanceState called");
        if (bookingModel != null) {
            outState.putSerializable("booking_model", bookingModel);
        }
        outState.putInt("current_step", currentStep);
    }

    @Override
    protected void onDestroy() {
        android.util.Log.d("BookingFlow", "onDestroy called");
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }

    // ─── Public getters dùng cho BookingStep3Fragment ───────────────────────

    public BookingModel getBookingModel() { return bookingModel; }
    public String getLastSessionId()      { return lastSessionId; }
    public String getLastTransactionId()  { return lastTransactionId; }

    // ─── Views & Listeners ──────────────────────────────────────────────────

    private void initViews() {
        btnMainAction     = findViewById(R.id.btnMainAction);
        layoutBtnPayVnpay = findViewById(R.id.layoutBtnPayVnpay);
        btnComplete       = findViewById(R.id.btnComplete);
        step1Number       = findViewById(R.id.step1_number);
        step2Number       = findViewById(R.id.step2_number);
        step3Number       = findViewById(R.id.step3_number);
        step1Divider      = findViewById(R.id.step1_divider);
        btnBackFlow       = findViewById(R.id.btnBackFlow);
        step2Divider      = findViewById(R.id.step2_divider);
        step1Label        = findViewById(R.id.step1_label);
        step2Label        = findViewById(R.id.step2_label);
        step3Label        = findViewById(R.id.step3_label);
        txtBookingTimer   = findViewById(R.id.txtBookingTimer);
        startCountdownTimer();
    }

    private void startCountdownTimer() {
        startCountdownTimer(remainingTimeMs);
    }

    private void startCountdownTimer(long durationMs) {
        if (countDownTimer != null) countDownTimer.cancel();
        remainingTimeMs = durationMs;
        countDownTimer = new android.os.CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeMs = millisUntilFinished; // Lưu lại thời gian còn lại
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                if (txtBookingTimer != null) {
                    txtBookingTimer.setText(String.format(Locale.getDefault(),
                            "Giữ lịch hẹn trong %02d:%02d", minutes, seconds));
                }
            }

            @Override
            public void onFinish() {
                if (currentStep >= 3) return; // Đã thanh toán thành công, bỏ qua
                if (txtBookingTimer != null) {
                    txtBookingTimer.setText("Lịch hẹn đã hết hạn giữ!");
                    txtBookingTimer.setTextColor(Color.parseColor("#E86FA0"));
                }
                if (btnMainAction != null) { btnMainAction.setEnabled(false); btnMainAction.setAlpha(0.5f); }
                if (layoutBtnPayVnpay != null) { layoutBtnPayVnpay.setEnabled(false); layoutBtnPayVnpay.setAlpha(0.5f); }
                if (!isFinishing() && !isDestroyed()) {
                    new AlertDialog.Builder(BookingFlowActivity.this)
                            .setTitle("Hết hạn giữ lịch ⏰")
                            .setMessage("Đã quá thời gian giữ chỗ ưu tiên. Vui lòng quay lại chọn lịch hẹn khác!")
                            .setCancelable(false)
                            .setPositiveButton("Quay lại", (d, w) -> finish())
                            .show();
                }
            }
        };
        countDownTimer.start();
    }

    private void setupListeners() {
        btnBackFlow.setOnClickListener(v -> handleBackAction());

        if (btnMainAction != null) {
            btnMainAction.setOnClickListener(v -> {
                if (currentStep == 1) {
                    Fragment currentFrag = getSupportFragmentManager()
                            .findFragmentById(R.id.booking_nav_host);
                    if (currentFrag instanceof BookingStep1Fragment) {
                        String note = ((BookingStep1Fragment) currentFrag).getBookingNote();
                        if (bookingModel != null) bookingModel.setNote(note);
                    }
                    updateStepUI(2);
                }
            });
        }

        if (layoutBtnPayVnpay != null) {
            layoutBtnPayVnpay.setOnClickListener(v -> startVNPAYPayment());
        }

        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> {
                Intent intent = new Intent(this, ConsultationsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    // ─── VNPAY Payment ──────────────────────────────────────────────────────

    private void startVNPAYPayment() {
        if (bookingModel == null) return;

        // Hủy bộ đếm ngược để tránh hết hạn trong lúc người dùng đang ở trang thanh toán VNPAY
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        long amount = 0;
        try {
            String priceStr = bookingModel.getPrice().replaceAll("[^\\d]", "");
            amount = Long.parseLong(priceStr);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi định dạng giá tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        String orderInfo = "Thanh toan lich hen bac si " + bookingModel.getDoctorName();
        String paymentUrl = VNPAYHelper.createPaymentUrl(orderInfo, amount, "113.160.92.202");

        if (paymentUrl.startsWith("error:")) {
            if (paymentUrl.contains("missing_config")) {
                Toast.makeText(this, "Lỗi: Chưa cấu hình VNPAY trong local.properties", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Lỗi khởi tạo cổng thanh toán VNPAY", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Intent intent = new Intent(this, VNPAYWebViewActivity.class);
        intent.putExtra(VNPAYWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl);
        startActivity(intent);
    }

    // ─── Firebase ───────────────────────────────────────────────────────────

    private void saveConsultationToFirestore() {
        if (bookingModel == null) return;

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        ConsultationModel consultation = new ConsultationModel();
        consultation.setUserId(userId);
        consultation.setDoctorId(bookingModel.getDoctorId());
        consultation.setDoctorName(bookingModel.getDoctorName());
        consultation.setDoctorAvatar(bookingModel.getDoctorAvatar());
        consultation.setPackageType(bookingModel.getPackageType());
        consultation.setFormatType(bookingModel.getFormatType());
        consultation.setStatus("BOOKED");
        consultation.setBookedAt(Timestamp.now());
        consultation.setNote(bookingModel.getNote());

        final String gatewayId = (!vnp_TransactionNo.isEmpty())
                ? vnp_TransactionNo
                : "VNP_" + System.currentTimeMillis();
        consultation.setTransactionId(gatewayId);

        try {
            String priceStr = bookingModel.getPrice().replaceAll("[^\\d]", "");
            consultation.setPrice(Double.parseDouble(priceStr));
        } catch (Exception e) {
            consultation.setPrice(0.0);
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date startDate = sdf.parse(bookingModel.getDate() + " " + bookingModel.getTime());
            if (startDate != null) {
                consultation.setStartTime(new Timestamp(startDate));
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                String pkg = bookingModel.getPackageType().toLowerCase();
                if (pkg.contains("15"))      cal.add(Calendar.MINUTE, 15);
                else if (pkg.contains("30")) cal.add(Calendar.MINUTE, 30);
                else if (pkg.contains("7"))  cal.add(Calendar.DAY_OF_YEAR, 7);
                else                          cal.add(Calendar.MINUTE, 30);
                consultation.setEndTime(new Timestamp(cal.getTime()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.collection("consultations")
                .add(consultation)
                .addOnSuccessListener(documentReference -> {
                    if (isFinishing() || isDestroyed()) return;
                    String docId = documentReference.getId();
                    lastSessionId = docId;
                    db.collection("consultations").document(docId).update("sessionId", docId);

                    Map<String, Object> txData = new HashMap<>();
                    txData.put("session_id",             docId);
                    txData.put("amount",                 consultation.getPrice());
                    txData.put("status",                 "SUCCESS");
                    txData.put("user_id",                userId);
                    txData.put("doctor_id",              consultation.getDoctorId());
                    txData.put("created_at",             Timestamp.now());
                    txData.put("gateway_transaction_id", gatewayId);
                    db.collection("transactions").document(gatewayId).set(txData);
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("BookingFlow", "Lưu lịch hẹn thất bại: " + e.getMessage()));
    }

    // ─── Navigation ─────────────────────────────────────────────────────────

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { handleBackAction(); }
        });
    }

    private void handleBackAction() {
        if (currentStep == 2) {
            updateStepUI(1);
        } else if (currentStep == 3) {
            // Không cho back khi đã thanh toán thành công
        } else {
            finish();
        }
    }

    private void updateStepUI(int step) {
        currentStep = step;
        android.util.Log.d("BookingFlow", "updateStepUI → step=" + step);
        Fragment fragment;

        switch (step) {
            case 2:
                fragment = new BookingStep2Fragment();
                if (btnMainAction     != null) btnMainAction.setVisibility(View.GONE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.VISIBLE);
                if (btnComplete       != null) btnComplete.setVisibility(View.GONE);
                break;

            case 3:
                if (countDownTimer != null) countDownTimer.cancel();
                fragment = new BookingStep3Fragment();
                if (btnMainAction     != null) btnMainAction.setVisibility(View.GONE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.GONE);
                if (btnComplete       != null) btnComplete.setVisibility(View.VISIBLE);
                if (findViewById(R.id.layoutBookingTimerRow) != null)
                    findViewById(R.id.layoutBookingTimerRow).setVisibility(View.GONE);
                break;

            case 1:
            default:
                fragment = new BookingStep1Fragment();
                if (btnMainAction     != null) btnMainAction.setVisibility(View.VISIBLE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.GONE);
                if (btnComplete       != null) btnComplete.setVisibility(View.GONE);
                if (findViewById(R.id.layoutBookingTimerRow) != null)
                    findViewById(R.id.layoutBookingTimerRow).setVisibility(View.VISIBLE);
                break;
        }

        replaceFragment(fragment);
        updateStepperGraphics(step);
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.booking_nav_host, fragment)
                .commitAllowingStateLoss();
    }

    private void updateStepperGraphics(int step) {
        int colorActive   = Color.parseColor("#00BFA5");
        int colorInactive = Color.parseColor("#BDBDBD");
        int bgActive   = R.drawable.bg_light_teal_circle;
        int bgInactive = R.drawable.bg_light_gray_circle;
        updateStepView(step1Number, step1Label, true,       bgActive,   Color.parseColor("#4A9292"), Color.parseColor("#2D1B47"));
        updateStepView(step2Number, step2Label, step >= 2,  step >= 2 ? bgActive : bgInactive, step >= 2 ? Color.parseColor("#4A9292") : colorInactive, step >= 2 ? Color.parseColor("#2D1B47") : colorInactive);
        updateStepView(step3Number, step3Label, step >= 3,  step >= 3 ? bgActive : bgInactive, step >= 3 ? Color.parseColor("#4A9292") : colorInactive, step >= 3 ? Color.parseColor("#2D1B47") : colorInactive);
        if (step1Divider != null) step1Divider.setBackgroundColor(step >= 2 ? colorActive : Color.parseColor("#E0E0E0"));
        if (step2Divider != null) step2Divider.setBackgroundColor(step >= 3 ? colorActive : Color.parseColor("#E0E0E0"));
    }

    private void updateStepView(TextView number, TextView label, boolean active,
                                int bgRes, int textColor, int labelColor) {
        if (number != null) { number.setBackgroundResource(bgRes); number.setTextColor(textColor); }
        if (label  != null) label.setTextColor(labelColor);
    }
}
