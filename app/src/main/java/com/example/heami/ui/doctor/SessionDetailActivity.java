package com.example.heami.ui.doctor;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SessionDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvPatientName, tvStatusBadge, tvDuration, tvTime, tvType, tvMoodTitle, tvMoodStats, tvMoodQuote, tvMoodEmoji;
    private EditText etDoctorNotes;
    private LinearLayout btnChat, btnCall, btnComplete;

    private FirebaseFirestore db;
    private String slotId;
    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        db = FirebaseFirestore.getInstance();

        // 🌟 FIX ĐỒNG BỘ: Nhận khóa chữ thường khớp 100% với DoctorHomeActivity bắn sang, bọc lót thêm chữ HOA nếu có
        slotId = getIntent().getStringExtra("slot_id");
        if (slotId == null || slotId.isEmpty()) {
            slotId = getIntent().getStringExtra("SLOT_ID");
        }

        sessionId = getIntent().getStringExtra("session_id");
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = getIntent().getStringExtra("SESSION_ID");
        }

        initViews();
        loadSessionData();
        setupActionListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvStatusBadge = findViewById(R.id.tv_status_badge);
        tvDuration = findViewById(R.id.tv_duration);
        tvTime = findViewById(R.id.tv_time);
        tvType = findViewById(R.id.tv_type);
        tvMoodTitle = findViewById(R.id.tv_mood_title);
        tvMoodEmoji = findViewById(R.id.tv_mood_emoji);
        tvMoodStats = findViewById(R.id.tv_mood_stats);
        tvMoodQuote = findViewById(R.id.tv_mood_quote);
        etDoctorNotes = findViewById(R.id.et_doctor_notes);

        btnChat = findViewById(R.id.btn_action_chat);
        btnCall = findViewById(R.id.btn_action_call);
        btnComplete = findViewById(R.id.btn_action_complete);
    }

    private void loadSessionData() {

        if (slotId == null || slotId.isEmpty()) {
            Toast.makeText(this, "Thiếu slot_id!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("lich_hen")
                .whereEqualTo("slot_id", slotId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        Toast.makeText(this,
                                "Thông tin phiên khám không tồn tại!",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    DocumentSnapshot doc = query.getDocuments().get(0);

                    // Lưu lại Document ID thật
                    slotId = doc.getId();

                    bindLichHenData(doc);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void bindLichHenData(DocumentSnapshot doc) {
        String status = doc.getString("status");
        String notes = doc.getString("doctor_notes");

        if (status != null) {
            tvStatusBadge.setText("booked".equalsIgnoreCase(status) ? "Sắp diễn ra" : "Hoàn thành");
        }

        if (notes != null && etDoctorNotes.getText().toString().isEmpty()) {
            etDoctorNotes.setText(notes);
        }

        Timestamp startTime = doc.getTimestamp("start_time");
        if (startTime != null) {
            SimpleDateFormat displayFmt = new SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault());
            tvTime.setText(displayFmt.format(startTime.toDate()));
        }

        // Nếu Intent chưa truyền session_id thì lấy trực tiếp từ trường liên kết của document lich_hen
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = doc.getString("session_id");
        }

        Log.d("HEAMI_DETAIL", "Đang xử lý với SESSION_ID = " + sessionId);

        if (sessionId != null && !sessionId.isEmpty()) {
            fetchConsultationDetails(sessionId);
        } else {
            Toast.makeText(this, "Lịch hẹn hiện chưa được liên kết phiên tư vấn!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchConsultationDetails(String targetSessionId) {
        db.collection("consultations")
                .document(targetSessionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (!doc.exists()) {
                        Log.e("HEAMI_DETAIL", "Không tìm thấy tài liệu ID [" + targetSessionId + "] trong bảng consultations");
                        return;
                    }

                    String name = doc.getString("patient_name");
                    String moodText = doc.getString("mood_text");
                    String moodEmoji = doc.getString("mood_emoji");
                    String duration = doc.getString("duration");
                    String method = doc.getString("method_text");
                    String quote = doc.getString("mood_quote");

                    String energy = doc.contains("energy") ? String.valueOf(doc.get("energy")) : "35%";
                    String bpm = doc.contains("bpm") ? String.valueOf(doc.get("bpm")) : "78";

                    if (name != null) tvPatientName.setText(name);
                    if (moodText != null) tvMoodTitle.setText(moodText);
                    if (moodEmoji != null) tvMoodEmoji.setText(moodEmoji);
                    if (duration != null) tvDuration.setText(duration);
                    if (method != null) tvType.setText(method);

                    if (quote != null && !quote.isEmpty()) {
                        tvMoodQuote.setText("\"" + quote + "\"");
                    } else {
                        tvMoodQuote.setText("\"Cảm thấy mệt mỏi và không có động lực làm gì cả\"");
                    }

                    tvMoodStats.setText("Energy: " + energy + " · BPM: " + bpm);
                    Log.d("HEAMI_DETAIL", "Đồng bộ hoàn tất! Phiên khám của bệnh nhân: " + name);
                })
                .addOnFailureListener(e -> Log.e("HEAMI_DETAIL", "Lỗi truy vấn bảng consultations: " + e.getMessage()));
    }

    private void setupActionListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnChat.setOnClickListener(v -> Toast.makeText(this, "Đang mở hộp thoại Chat...", Toast.LENGTH_SHORT).show());
        btnCall.setOnClickListener(v -> Toast.makeText(this, "Đang khởi tạo cuộc gọi tư vấn...", Toast.LENGTH_SHORT).show());

        btnComplete.setOnClickListener(v -> {
            if (slotId == null || slotId.isEmpty()) return;

            String updatedNotes = etDoctorNotes.getText().toString().trim();

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "completed");
            updateData.put("doctor_notes", updatedNotes);

            db.collection("lich_hen")
                    .document(slotId)
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật trạng thái hoàn thành phiên tư vấn!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}