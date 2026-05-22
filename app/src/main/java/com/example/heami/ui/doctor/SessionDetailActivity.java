package com.example.heami.ui.doctor;

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
    private String currentSlotId = "slot_default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        db = FirebaseFirestore.getInstance();

        if (getIntent().hasExtra("session_id")) {
            currentSlotId = getIntent().getStringExtra("session_id");
        } else if (getIntent().hasExtra("slot_id")) {
            currentSlotId = getIntent().getStringExtra("slot_id");
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
        Log.d("HEAMI_CHECK", "Khởi chạy truy vấn an toàn với Intent ID: " + currentSlotId);

        db.collection("lich_hen")
                .whereEqualTo("slot_id", currentSlotId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        currentSlotId = documentSnapshot.getId();
                        Log.d("HEAMI_DETAIL", "Tìm thấy tài liệu lịch hẹn! ID thật trên Firestore: " + currentSlotId);

                        bindLichHenData(documentSnapshot);
                    } else {
                        fetchLichHenDirectly(currentSlotId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HEAMI_DETAIL", "Lỗi bảng lich_hen: " + e.getMessage());
                    Toast.makeText(this, "Không thể kết nối cơ sở dữ liệu!", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchLichHenDirectly(String docId) {
        db.collection("lich_hen")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        bindLichHenData(doc);
                    } else {
                        Log.e("HEAMI_DETAIL", "Thất bại! Không tìm thấy phiên khám ở cả 2 phương thức.");
                        Toast.makeText(this, "Thông tin phiên khám không tồn tại!", Toast.LENGTH_SHORT).show();
                    }
                });
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
            java.util.Date date = startTime.toDate();
            // 🌟 ĐÃ FIX: Thêm dấu nháy đơn bọc chuỗi 'Hôm nay' để không bị văng app
            SimpleDateFormat displayFmt = new SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault());
            tvTime.setText(displayFmt.format(date));
        }

        String sessionId = doc.getString("session_id");
        Log.d("HEAMI_DIAGNOSE", "Mã liên kết session_id trích xuất thành công: [" + sessionId + "]");

        if (sessionId != null && !sessionId.isEmpty()) {
            fetchConsultationDetails(sessionId);
        } else {
            Log.e("HEAMI_DETAIL", "Cảnh báo: Trường session_id của tài liệu này đang trống!");
            Toast.makeText(this, "Lịch hẹn chưa được liên kết với thông tin bệnh nhân!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchConsultationDetails(String sessionId) {
        db.collection("consultations")
                .document(sessionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.e("HEAMI_DETAIL", "Không tồn tại tài liệu [" + sessionId + "] trong bộ consultations");
                        return;
                    }

                    String name = doc.getString("customer_name");
                    String moodText = doc.getString("mood_text");
                    String moodEmoji = doc.getString("mood_emoji");
                    String duration = doc.getString("duration");
                    String method = doc.getString("method_text");
                    String quote = doc.getString("mood_quote");

                    String energy = doc.contains("energy") ? doc.get("energy").toString() : "35%";
                    String bpm = doc.contains("bpm") ? doc.get("bpm").toString() : "78";

                    if (name != null) tvPatientName.setText(name);
                    if (moodText != null) tvMoodTitle.setText(moodText);
                    if (moodEmoji != null) tvMoodEmoji.setText(moodEmoji);
                    if (duration != null) tvDuration.setText(duration);
                    if (method != null) tvType.setText(method);

                    if (quote != null) {
                        tvMoodQuote.setText("\"" + quote + "\"");
                    } else {
                        tvMoodQuote.setText("\"Cảm thấy mệt mỏi và không có động lực làm gì cả\"");
                    }

                    tvMoodStats.setText("Energy: " + energy + " · BPM: " + bpm);
                    Log.d("HEAMI_DETAIL", "Đã đồng bộ thành công! Chào mừng bệnh nhân: " + name);
                })
                .addOnFailureListener(e -> Log.e("HEAMI_DETAIL", "Lỗi bảng consultations: " + e.getMessage()));
    }

    private void setupActionListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnChat.setOnClickListener(v -> Toast.makeText(this, "Đang mở hộp thoại Chat...", Toast.LENGTH_SHORT).show());
        btnCall.setOnClickListener(v -> Toast.makeText(this, "Đang khởi tạo cuộc gọi tư vấn...", Toast.LENGTH_SHORT).show());

        btnComplete.setOnClickListener(v -> {
            String updatedNotes = etDoctorNotes.getText().toString().trim();

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "completed");
            updateData.put("doctor_notes", updatedNotes);

            db.collection("lich_hen")
                    .document(currentSlotId)
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã hoàn thành phiên tư vấn này!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}