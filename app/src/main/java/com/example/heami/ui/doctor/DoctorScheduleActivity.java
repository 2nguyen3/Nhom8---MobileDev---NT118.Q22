package com.example.heami.ui.doctor;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.data.models.TimeSlotsModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DoctorScheduleActivity extends AppCompatActivity {

    private ImageButton btnBackSchedule;
    private MonthCalendarView calendarViewSchedule;
    private TextView txtSelectedDateTitle;
    private MaterialButton btnAddTimeSlot;
    private LinearLayout layoutScheduleSlotsContainer;
    private TextView txtScheduleEmpty;

    private final List<TimeSlotsModel> fullFirebaseList = new ArrayList<>();

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
        listenToAllTimeSlots();
        loadSlotsForSelectedDate();
    }

    private void initViews() {
        btnBackSchedule = findViewById(R.id.btnBackSchedule);
        calendarViewSchedule = findViewById(R.id.calendarViewSchedule);
        txtSelectedDateTitle = findViewById(R.id.txtSelectedDateTitle);
        btnAddTimeSlot = findViewById(R.id.btnAddTimeSlot);
        layoutScheduleSlotsContainer = findViewById(R.id.layoutScheduleSlotsContainer);
        txtScheduleEmpty = findViewById(R.id.txtScheduleEmpty);

        updateTitle();
        if (calendarViewSchedule != null) {
            calendarViewSchedule.setSelectedDay(selectedDay);
        }
    }

    private void setupEvents() {
        btnBackSchedule.setOnClickListener(v -> finish());

        if (calendarViewSchedule != null) {
            calendarViewSchedule.setOnDateClickListener((year, month, day) -> {
                selectedDay.set(Calendar.YEAR, year);
                selectedDay.set(Calendar.MONTH, month);
                selectedDay.set(Calendar.DAY_OF_MONTH, day);
                updateTitle();
                loadSlotsForSelectedDate();
            });
        }

        btnAddTimeSlot.setOnClickListener(v -> showCustomTimePickerDialog());
    }

    private void listenToAllTimeSlots() {
        db.collection("doctors").document(doctorId)
                .collection("time_slots")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("HEAMI_FIREBASE", "Lỗi Firestore: " + error.getMessage());
                        return;
                    }
                    if (snapshots != null) {
                        fullFirebaseList.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            try {
                                TimeSlotsModel slot = document.toObject(TimeSlotsModel.class);
                                if (slot != null) {
                                    if (slot.getSlot_id() == null || slot.getSlot_id().isEmpty()) {
                                        slot.setSlot_id(document.getId());
                                    }
                                    fullFirebaseList.add(slot);
                                }
                            } catch (Exception e) {
                                Log.e("HEAMI_PARSING", "Lỗi ánh xạ: " + e.getMessage());
                            }
                        }
                        updateCalendarDecorator();
                    }
                });
    }

    private void updateCalendarDecorator() {
        if (calendarViewSchedule == null) return;

        Set<String> bookedDayKeys = new HashSet<>();
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        for (TimeSlotsModel slot : fullFirebaseList) {
            if (slot == null || slot.getStart_time() == null) continue;
            if ("booked".equalsIgnoreCase(slot.getStatus())) {
                String key = keyFmt.format(slot.getStart_time().toDate());
                bookedDayKeys.add(key);
            }
        }

        calendarViewSchedule.setBookedDays(bookedDayKeys);
    }

    private void updateTitle() {
        if (txtSelectedDateTitle == null) return;
        txtSelectedDateTitle.setText(titleDateFormat.format(selectedDay.getTime()));

        // So sánh selectedDay với ngày hôm nay
        Calendar today = Calendar.getInstance();
        boolean isToday = (selectedDay.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && selectedDay.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && selectedDay.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH));

        boolean isPast = selectedDay.before(today) && !isToday;

        if (btnAddTimeSlot != null) {
            if (isToday || isPast) {
                // Khóa nút thêm giờ đối với hôm nay hoặc các ngày quá khứ
                btnAddTimeSlot.setVisibility(View.GONE);
            } else {
                btnAddTimeSlot.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showCustomTimePickerDialog() {
        // Tạo danh sách giờ (mỗi ca cách nhau 30 phút từ 06:00 đến 22:00)
        List<String> timeList = new ArrayList<>();
        for (int h = 6; h <= 22; h++) {
            timeList.add(String.format(Locale.getDefault(), "%02d:00", h));
            if (h < 22) {
                timeList.add(String.format(Locale.getDefault(), "%02d:30", h));
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_time_slot, null);
        builder.setView(dialogView);

        Spinner spinnerStart = dialogView.findViewById(R.id.spinnerStartTime);
        Spinner spinnerEnd = dialogView.findViewById(R.id.spinnerEndTime);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmDialog);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStart.setAdapter(adapter);
        spinnerEnd.setAdapter(adapter);

        // Đặt mặc định giờ bắt đầu là 08:00, giờ kết thúc là 09:00 cho tiện dụng
        spinnerStart.setSelection(timeList.indexOf("08:00") >= 0 ? timeList.indexOf("08:00") : 0);
        spinnerEnd.setSelection(timeList.indexOf("09:00") >= 0 ? timeList.indexOf("09:00") : 0);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String startTime = spinnerStart.getSelectedItem().toString();
            String endTime = spinnerEnd.getSelectedItem().toString();

            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMin = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMin = Integer.parseInt(endParts[1]);

            if (endHour < startHour || (endHour == startHour && endMin <= startMin)) {
                Toast.makeText(this, "Thời gian kết thúc phải lớn hơn thời gian bắt đầu!", Toast.LENGTH_LONG).show();
                return;
            }

            saveTimeSlotToFirestore(startHour, startMin, endHour, endMin);
            dialog.dismiss();
        });

        dialog.show();
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

                            // Kiểm tra ngày hôm nay hoặc quá khứ
                            Calendar today = Calendar.getInstance();
                            boolean isToday = (selectedDay.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                                    && selectedDay.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                                    && selectedDay.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH));
                            boolean isPast = selectedDay.before(today) && !isToday;

                            if ("booked".equals(status)) {
                                txtStatus.setText("Đã đặt");
                                txtStatus.setTextColor(Color.parseColor("#FF5A5F"));
                                btnDelete.setVisibility(View.INVISIBLE); // Không cho xóa slot đã đặt
                            } else {
                                txtStatus.setText("Sẵn sàng");
                                txtStatus.setTextColor(Color.parseColor("#09A38C"));
                                
                                if (isToday || isPast) {
                                    // Ẩn nút xóa đối với hôm nay và quá khứ
                                    btnDelete.setVisibility(View.INVISIBLE);
                                } else {
                                    btnDelete.setVisibility(View.VISIBLE);
                                    btnDelete.setOnClickListener(v -> deleteTimeSlot(doc.getId()));
                                }
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
