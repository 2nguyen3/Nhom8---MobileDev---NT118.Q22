package com.example.heami.ui.consultation;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.heami.R;
import com.example.heami.data.models.BookingModel;
import com.example.heami.data.models.ConsultationModel;
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
    private String lastSessionId; // Lưu lại sessionId để Step 3 hiển thị

    // Stepper Views
    private TextView step1Number, step2Number, step3Number;
    private View step1Divider, step2Divider;
    private TextView step1Label, step2Label, step3Label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_flow);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Lấy dữ liệu booking từ intent
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

        // Stepper numbers
        step1Number = findViewById(R.id.step1_number);
        step2Number = findViewById(R.id.step2_number);
        step3Number = findViewById(R.id.step3_number);
        
        // Dividers
        step1Divider = findViewById(R.id.step1_divider);
        btnBackFlow = findViewById(R.id.btnBackFlow);
        step2Divider = findViewById(R.id.step2_divider);
        
        // Labels
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
                // Thực hiện lưu dữ liệu vào Firestore thay vì chuyển step ngay lập tức
                saveConsultationToFirestore();
            });
        }

        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> finish());
        }
    }

    private void saveConsultationToFirestore() {
        if (bookingModel == null) return;
        
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo ConsultationModel từ BookingModel
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
        
        // Giả lập transaction id cho đến khi tích hợp VNPay thật
        consultation.setTransactionId("VNP_" + System.currentTimeMillis());

        // Parse price: "250.000đ" -> 250000.0
        try {
            String priceStr = bookingModel.getPrice().replaceAll("[^\\d]", "");
            consultation.setPrice(Double.parseDouble(priceStr));
        } catch (Exception e) {
            consultation.setPrice(0.0);
        }

        // Parse startTime và tính toán endTime
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date startDate = sdf.parse(bookingModel.getDate() + " " + bookingModel.getTime());
            if (startDate != null) {
                consultation.setStartTime(new Timestamp(startDate));
                
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);
                
                String packageType = bookingModel.getPackageType().toLowerCase();
                if (packageType.contains("15")) {
                    calendar.add(Calendar.MINUTE, 15);
                } else if (packageType.contains("30")) {
                    calendar.add(Calendar.MINUTE, 30);
                } else if (packageType.contains("7")) {
                    calendar.add(Calendar.DAY_OF_YEAR, 7);
                } else {
                    calendar.add(Calendar.MINUTE, 30); // Mặc định
                }
                consultation.setEndTime(new Timestamp(calendar.getTime()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Lưu vào Firestore collection "consultations"
        db.collection("consultations")
                .add(consultation)
                .addOnSuccessListener(documentReference -> {
                    // Sau khi lưu thành công, cập nhật sessionId bằng chính document ID
                    String docId = documentReference.getId();
                    lastSessionId = docId; // Lưu lại ID để Step 3 hiển thị
                    db.collection("consultations").document(docId).update("sessionId", docId);
                    
                    // Chuyển sang Bước 3 (Hoàn tất)
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
        int colorActive = Color.parseColor("#00BFA5"); // Teal đậm
        int colorInactive = Color.parseColor("#BDBDBD"); // Xám đậm
        
        int bgActive = R.drawable.bg_light_teal_circle;
        int bgInactive = R.drawable.bg_light_gray_circle;

        // Reset Steps
        updateStepView(step1Number, step1Label, true, bgActive, Color.parseColor("#4A9292"), Color.parseColor("#2D1B47"));
        updateStepView(step2Number, step2Label, step >= 2, step >= 2 ? bgActive : bgInactive, step >= 2 ? Color.parseColor("#4A9292") : colorInactive, step >= 2 ? Color.parseColor("#2D1B47") : colorInactive);
        updateStepView(step3Number, step3Label, step >= 3, step >= 3 ? bgActive : bgInactive, step >= 3 ? Color.parseColor("#4A9292") : colorInactive, step >= 3 ? Color.parseColor("#2D1B47") : colorInactive);

        // Dividers
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
