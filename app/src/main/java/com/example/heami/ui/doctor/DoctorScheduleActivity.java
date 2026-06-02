package com.example.heami.ui.doctor;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DoctorScheduleActivity extends AppCompatActivity {

    private ImageButton btnBackSchedule;
    private CalendarView calendarViewSchedule;
    private TextView txtSelectedDateTitle;
    private MaterialButton btnAddTimeSlot;
    private LinearLayout layoutScheduleSlotsContainer;
    private TextView txtScheduleEmpty;

    private FirebaseFirestore db;
    private String doctorId = "doc_001";
    private Calendar selectedDay;
    private SimpleDateFormat titleDateFormat;
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_schedule);

        db = FirebaseFirestore.getInstance();
        titleDateFormat = new SimpleDateFormat("'Ngày' dd/MM/yyyy", new Locale("vi", "VN"));
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Đọc doctor_id đã đăng nhập
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);
        if (isDoctor) {
            doctorId = prefs.getString("doctor_id", "doc_001");
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Khởi tạo ngày chọn mặc định là hôm nay
        selectedDay = Calendar.getInstance();

        initViews();
        setupEvents();
        loadSlotsForSelectedDate();
    }

    private void initViews() {
        btnBackSchedule = findViewById(R.id.btnBackSchedule);
        calendarViewSchedule = findViewById(R.id.calendarViewSchedule);
        txtSelectedDateTitle = findViewById(R.id.txtSelectedDateTitle);
        btnAddTimeSlot = findViewById(R.id.btnAddTimeSlot);
        layoutScheduleSlotsContainer = findViewById(R.id.layoutScheduleSlotsContainer);
        txtScheduleEmpty = findViewById(R.id.txtScheduleEmpty);

        // Giới hạn lịch tháng: Cho chọn từ hôm nay trở đi
        calendarViewSchedule.setMinDate(System.currentTimeMillis() - 1000);
        updateTitle();
    }

    private void setupEvents() {
        btnBackSchedule.setOnClickListener(v -> finish());

        calendarViewSchedule.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDay.set(Calendar.YEAR, year);
            selectedDay.set(Calendar.MONTH, month);
            selectedDay.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateTitle();
            loadSlotsForSelectedDate();
        });

        btnAddTimeSlot.setOnClickListener(v -> showTimePickerDialogs());
    }

    private void updateTitle() {
        txtSelectedDateTitle.setText(titleDateFormat.format(selectedDay.getTime()));
    }

    private void showTimePickerDialogs() {
        Calendar helper = Calendar.getInstance();
        
        // Chọn giờ bắt đầu
        new TimePickerDialog(this, (viewStart, startHour, startMin) -> {
            // Sau khi chọn giờ bắt đầu, chọn tiếp giờ kết thúc
            new TimePickerDialog(this, (viewEnd, endHour, endMin) -> {
                
                if (endHour < startHour || (endHour == startHour && endMin <= startMin)) {
                    Toast.makeText(this, "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!", Toast.LENGTH_LONG).show();
                    return;
                }

                saveTimeSlotToFirestore(startHour, startMin, endHour, endMin);

            }, helper.get(Calendar.HOUR_OF_DAY) + 1, 0, true).show();
        }, helper.get(Calendar.HOUR_OF_DAY), 0, true).show();
    }

    private void saveTimeSlotToFirestore(int startHour, int startMin, int endHour, int endMin) {
        Calendar startCal = (Calendar) selectedDay.clone();
        startCal.set(Calendar.HOUR_OF_DAY, startHour);
        startCal.set(Calendar.MINUTE, startMin);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = (Calendar) selectedDay.clone();
        endCal.set(Calendar.HOUR_OF_DAY, endHour);
        endCal.set(Calendar.MINUTE, endMin);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        Map<String, Object> slot = new HashMap<>();
        slot.put("doctor_id", doctorId);
        slot.put("start_time", new Timestamp(startCal.getTime()));
        slot.put("end_time", new Timestamp(endCal.getTime()));
        slot.put("status", "available");
        slot.put("session_id", "");

        db.collection("doctors").document(doctorId)
                .collection("time_slots")
                .add(slot)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Thêm khung giờ thành công!", Toast.LENGTH_SHORT).show();
                    loadSlotsForSelectedDate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSlotsForSelectedDate() {
        layoutScheduleSlotsContainer.removeAllViews();
        layoutScheduleSlotsContainer.addView(txtScheduleEmpty);
        txtScheduleEmpty.setVisibility(View.VISIBLE);

        // Xác định khoảng bắt đầu & kết thúc của ngày được chọn
        Calendar startDay = (Calendar) selectedDay.clone();
        startDay.set(Calendar.HOUR_OF_DAY, 0);
        startDay.set(Calendar.MINUTE, 0);
        startDay.set(Calendar.SECOND, 0);
        startDay.set(Calendar.MILLISECOND, 0);

        Calendar endDay = (Calendar) selectedDay.clone();
        endDay.set(Calendar.HOUR_OF_DAY, 23);
        endDay.set(Calendar.MINUTE, 59);
        endDay.set(Calendar.SECOND, 59);
        endDay.set(Calendar.MILLISECOND, 999);

        Timestamp tsStart = new Timestamp(startDay.getTime());
        Timestamp tsEnd = new Timestamp(endDay.getTime());

        db.collection("doctors").document(doctorId)
                .collection("time_slots")
                .whereGreaterThanOrEqualTo("start_time", tsStart)
                .whereLessThanOrEqualTo("start_time", tsEnd)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        txtScheduleEmpty.setVisibility(View.GONE);
                        layoutScheduleSlotsContainer.removeView(txtScheduleEmpty);

                        LayoutInflater inflater = LayoutInflater.from(this);
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            View row = inflater.inflate(R.layout.item_doctor_schedule_slot, layoutScheduleSlotsContainer, false);
                            
                            TextView txtTimeRange = row.findViewById(R.id.txtSlotTimeRange);
                            TextView txtStatus = row.findViewById(R.id.txtSlotStatus);
                            ImageButton btnDelete = row.findViewById(R.id.btnDeleteSlot);

                            Timestamp tStart = doc.getTimestamp("start_time");
                            Timestamp tEnd = doc.getTimestamp("end_time");
                            String status = doc.getString("status");

                            if (tStart != null && tEnd != null) {
                                String timeRange = timeFormat.format(tStart.toDate()) + " - " + timeFormat.format(tEnd.toDate());
                                txtTimeRange.setText(timeRange);
                            }

                            if ("booked".equals(status)) {
                                txtStatus.setText("Đã đặt");
                                txtStatus.setTextColor(Color.parseColor("#FF5A5F"));
                                btnDelete.setVisibility(View.INVISIBLE); // Không cho xóa slot đã đặt
                            } else {
                                txtStatus.setText("Sẵn sàng");
                                txtStatus.setTextColor(Color.parseColor("#09A38C"));
                                btnDelete.setVisibility(View.VISIBLE);
                                btnDelete.setOnClickListener(v -> deleteTimeSlot(doc.getId()));
                            }

                            layoutScheduleSlotsContainer.addView(row);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteTimeSlot(String slotId) {
        db.collection("doctors").document(doctorId)
                .collection("time_slots")
                .document(slotId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa khung giờ!", Toast.LENGTH_SHORT).show();
                    loadSlotsForSelectedDate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
