package com.example.heami.ui.consultation;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import java.util.Locale;

public class BookingFlowActivity extends AppCompatActivity {

    private ImageButton btnBackFlow;
    private View btnMainAction;
    private View layoutBtnPayVnpay;
    private View btnComplete;
    private int currentStep = 1;
    private BookingModel bookingModel;
    private FirebaseFirestore db;
    private String lastSessionId; // ID này sẽ được dùng để hiển thị ở Step 3
    private String vnp_TransactionNo = "";

    // Stepper Views
    private TextView step1Number, step2Number, step3Number;
    private View step1Divider, step2Divider;
    private TextView step1Label, step2Label, step3Label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_flow);

        db = FirebaseFirestore.getInstance();
        bookingModel = (BookingModel) getIntent().getSerializableExtra("booking_model");

        initViews();
        setupListeners();
        setupBackNavigation();
        
        updateStepUI(1);
    }

    public BookingModel getBookingModel() {
        return bookingModel;
    }

    public String getLastSessionId() {
        return lastSessionId;
    }

    private void initViews() {
        btnMainAction = findViewById(R.id.btnMainAction);
        layoutBtnPayVnpay = findViewById(R.id.layoutBtnPayVnpay);
        btnComplete = findViewById(R.id.btnComplete);
        step1Number = findViewById(R.id.step1_number);
        step2Number = findViewById(R.id.step2_number);
        step3Number = findViewById(R.id.step3_number);
        step1Divider = findViewById(R.id.step1_divider);
        btnBackFlow = findViewById(R.id.btnBackFlow);
        step2Divider = findViewById(R.id.step2_divider);
        step1Label = findViewById(R.id.step1_label);
        step2Label = findViewById(R.id.step2_label);
        step3Label = findViewById(R.id.step3_label);
    }

    private void setupListeners() {
        btnBackFlow.setOnClickListener(v -> handleBackAction());

        if (btnMainAction != null) {
            btnMainAction.setOnClickListener(v -> {
                if (currentStep == 1) updateStepUI(2);
            });
        }

        if (layoutBtnPayVnpay != null) {
            layoutBtnPayVnpay.setOnClickListener(v -> {
                startVNPAYPayment();
            });
        }

        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> finish());
        }
    }

    private void startVNPAYPayment() {
        if (bookingModel == null) return;

        long amount = 0;
        try {
            String priceStr = bookingModel.getPrice().replaceAll("[^\\d]", "");
            amount = Long.parseLong(priceStr);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi định dạng giá tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        String orderInfo = "Thanh toan lich hen bac si: " + bookingModel.getDoctorName();
//        String paymentUrl = VNPAYHelper.createPaymentUrl(orderInfo, amount, "127.0.0.1");
        String paymentUrl = VNPAYHelper.createPaymentUrl(orderInfo, amount, "113.160.92.213");

        showVNPAYWebView(paymentUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showVNPAYWebView(String url) {
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(webView)
                .create();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String currentUrl = request.getUrl().toString();
                return checkReturnUrl(currentUrl, dialog);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                checkReturnUrl(url, dialog);
                super.onPageStarted(view, url, favicon);
            }

            private boolean checkReturnUrl(String url, AlertDialog dialog) {
                if (url.startsWith("heami://vnpay_return")) {
                    handleVNPAYCallback(url);
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl(url);
        dialog.show();
    }

    private void handleVNPAYCallback(String url) {
        Uri uri = Uri.parse(url);
        String responseCode = uri.getQueryParameter("vnp_ResponseCode");
        
        if ("00".equals(responseCode)) {
            vnp_TransactionNo = uri.getQueryParameter("vnp_TransactionNo");
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
            saveConsultationToFirestore();
        } else {
            Toast.makeText(this, "Thanh toán không thành công. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
        }
    }

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
        consultation.setTransactionId(vnp_TransactionNo != null ? vnp_TransactionNo : "VNP_" + System.currentTimeMillis());

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
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);
                String packageType = bookingModel.getPackageType().toLowerCase();
                if (packageType.contains("15")) calendar.add(Calendar.MINUTE, 15);
                else if (packageType.contains("30")) calendar.add(Calendar.MINUTE, 30);
                else if (packageType.contains("7")) calendar.add(Calendar.DAY_OF_YEAR, 7);
                else calendar.add(Calendar.MINUTE, 30);
                consultation.setEndTime(new Timestamp(calendar.getTime()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.collection("consultations")
                .add(consultation)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    lastSessionId = docId; // Gán ID thực tế từ DB
                    db.collection("consultations").document(docId).update("sessionId", docId);
                    updateStepUI(3);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu lịch hẹn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBackNavigation() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackAction();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void handleBackAction() {
        if (currentStep == 2) updateStepUI(1);
        else finish();
    }

    private void updateStepUI(int step) {
        currentStep = step;
        Fragment fragment;

        switch (step) {
            case 2:
                fragment = new BookingStep2Fragment();
                if (btnMainAction != null) btnMainAction.setVisibility(View.GONE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.VISIBLE);
                if (btnComplete != null) btnComplete.setVisibility(View.GONE);
                break;
            case 3:
                fragment = new BookingStep3Fragment();
                if (btnMainAction != null) btnMainAction.setVisibility(View.GONE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.GONE);
                if (btnComplete != null) btnComplete.setVisibility(View.VISIBLE);
                
                if (findViewById(R.id.layoutBookingTimerRow) != null) 
                    findViewById(R.id.layoutBookingTimerRow).setVisibility(View.GONE);
                break;
            case 1:
            default:
                fragment = new BookingStep1Fragment();
                if (btnMainAction != null) btnMainAction.setVisibility(View.VISIBLE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.GONE);
                if (btnComplete != null) btnComplete.setVisibility(View.GONE);

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
            .commit();
    }

    private void updateStepperGraphics(int step) {
        int colorActive = Color.parseColor("#00BFA5");
        int colorInactive = Color.parseColor("#BDBDBD");
        int bgActive = R.drawable.bg_light_teal_circle;
        int bgInactive = R.drawable.bg_light_gray_circle;

        updateStepView(step1Number, step1Label, true, bgActive, Color.parseColor("#4A9292"), Color.parseColor("#2D1B47"));
        updateStepView(step2Number, step2Label, step >= 2, step >= 2 ? bgActive : bgInactive, step >= 2 ? Color.parseColor("#4A9292") : colorInactive, step >= 2 ? Color.parseColor("#2D1B47") : colorInactive);
        updateStepView(step3Number, step3Label, step >= 3, step >= 3 ? bgActive : bgInactive, step >= 3 ? Color.parseColor("#4A9292") : colorInactive, step >= 3 ? Color.parseColor("#2D1B47") : colorInactive);

        if (step1Divider != null) step1Divider.setBackgroundColor(step >= 2 ? colorActive : Color.parseColor("#E0E0E0"));
        if (step2Divider != null) step2Divider.setBackgroundColor(step >= 3 ? colorActive : Color.parseColor("#E0E0E0"));
    }

    private void updateStepView(TextView number, TextView label, boolean active, int bgRes, int textColor, int labelColor) {
        if (number != null) {
            number.setBackgroundResource(bgRes);
            number.setTextColor(textColor);
        }
        if (label != null) {
            label.setTextColor(labelColor);
        }
    }
}
