package com.example.heami.ui.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.TimeSlotsModel;
import com.example.heami.utils.ExitDialogHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DoctorScheduleManagementActivity extends AppCompatActivity {

    private RecyclerView        rvAppointments;
    private WeekScheduleAdapter weekAdapter;
    private MonthCalendarView   calendarViewScheduleManagement;
    private TextView            tvTotalSessions;
    private TextView            tvWeekRange;

    private final List<TimeSlotsModel> fullFirebaseList = new ArrayList<>();
    private FirebaseFirestore db;
    private String doctorId = "doc_001";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_schedule_management);

        db = FirebaseFirestore.getInstance();
        resolveDoctor();
        initViews();
        setupRecyclerView();
        listenToTimeSlots();

        try {
            DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_APPOINTMENTS);
            ExitDialogHelper.registerExitHandler(this);
        } catch (Exception e) {
            Log.e("HEAMI_CRASH", "Lỗi bottom nav: " + e.getMessage());
        }
    }

    /** Xác định doctorId từ SharedPreferences hoặc FirebaseAuth */
    private void resolveDoctor() {
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);
        if (isDoctor) {
            doctorId = prefs.getString("doctor_id", "doc_001");
        } else if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    private void initViews() {
        rvAppointments                 = findViewById(R.id.rv_appointments);
        tvTotalSessions                = findViewById(R.id.tv_total_sessions);
        tvWeekRange                    = findViewById(R.id.tvWeekRange);
        calendarViewScheduleManagement = findViewById(R.id.calendarViewScheduleManagement);

        updateWeekRangeLabel();

        // Nút thêm gói dịch vụ
        View btnAddServicePackage = findViewById(R.id.btnAddServicePackage);
        if (btnAddServicePackage != null) {
            btnAddServicePackage.setOnClickListener(v ->
                Toast.makeText(this, "Chức năng tạo gói tư vấn mới đang phát triển", Toast.LENGTH_SHORT).show());
        }

        // Nút chuyển đến màn hình thiết lập lịch trình
        View btnNavigateToSchedule = findViewById(R.id.btnNavigateToSchedule);
        if (btnNavigateToSchedule != null) {
            btnNavigateToSchedule.setOnClickListener(v -> {
                Intent intent = new Intent(this, DoctorScheduleActivity.class);
                startActivity(intent);
            });
        }
    }

    private void updateWeekRangeLabel() {
        if (tvWeekRange == null) return;
        Calendar mon = getMonday();
        Calendar sun = (Calendar) mon.clone();
        sun.add(Calendar.DAY_OF_YEAR, 6);
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM", Locale.getDefault());
        tvWeekRange.setText(fmt.format(mon.getTime()) + " – " + fmt.format(sun.getTime()));
    }

    private void setupRecyclerView() {
        if (rvAppointments == null) return;
        weekAdapter = new WeekScheduleAdapter();
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvAppointments.setAdapter(weekAdapter);
    }

    // ──────────────────────────────────────────────────────────────
    //  Firebase — pipeline 2 bước
    // ──────────────────────────────────────────────────────────────

    /**
     * Bước 1: Lắng nghe real-time time_slots của bác sĩ.
     * Mỗi khi thay đổi, gọi tiếp loadConsultationsAndMerge() để
     * bổ sung các ca đã booked từ collection consultations.
     */
    private void listenToTimeSlots() {
        db.collection("doctors").document(doctorId)
                .collection("time_slots")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("HEAMI_FIREBASE", "Lỗi time_slots: " + error.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    List<TimeSlotsModel> timeSlots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            TimeSlotsModel slot = doc.toObject(TimeSlotsModel.class);
                            if (slot != null) {
                                if (slot.getSlot_id() == null || slot.getSlot_id().isEmpty()) {
                                    slot.setSlot_id(doc.getId());
                                }
                                timeSlots.add(slot);
                            }
                        } catch (Exception e) {
                            Log.e("HEAMI_PARSING", "Lỗi ánh xạ TimeSlotsModel: " + e.getMessage());
                        }
                    }

                    loadConsultationsAndMerge(timeSlots);
                });
    }

    /**
     * Bước 2: Query consultations BOOKED của bác sĩ.
     * Chuyển mỗi consultation thành TimeSlotsModel(status="booked")
     * rồi merge với danh sách time_slots, deduplicate theo yyyyMMdd_HHmm.
     */
    private void loadConsultationsAndMerge(List<TimeSlotsModel> timeSlots) {
        db.collection("consultations")
                .whereEqualTo("doctor_id", doctorId)
                .whereEqualTo("status", "BOOKED")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    SimpleDateFormat dedupeKey = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());

                    // Bắt đầu bằng time_slots (available)
                    Map<String, TimeSlotsModel> mergedMap = new LinkedHashMap<>();
                    for (TimeSlotsModel s : timeSlots) {
                        if (s.getStart_time() != null) {
                            mergedMap.put(dedupeKey.format(s.getStart_time().toDate()), s);
                        }
                    }

                    // Ghi đè / thêm các slot booked từ consultations
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Timestamp tStart = doc.getTimestamp("start_time");
                            Timestamp tEnd   = doc.getTimestamp("end_time");
                            if (tStart == null) continue;

                            TimeSlotsModel booked = new TimeSlotsModel();
                            booked.setSlot_id(doc.getId());
                            booked.setDoctor_id(doctorId);
                            booked.setStart_time(tStart);
                            booked.setEnd_time(tEnd);
                            booked.setStatus("booked");
                            booked.setSession_id(doc.getId());

                            mergedMap.put(dedupeKey.format(tStart.toDate()), booked);
                        }
                    }

                    fullFirebaseList.clear();
                    fullFirebaseList.addAll(mergedMap.values());
                    refreshUI();
                })
                .addOnFailureListener(e ->
                        Log.e("HEAMI_FIREBASE", "Lỗi consultations: " + e.getMessage()));
    }

    /**
     * Sau khi fullFirebaseList đã được merge:
     * 1. Đếm ca bận trong tuần → badge tvTotalSessions
     * 2. Tập hợp ngày có booked → MonthCalendarView dot đỏ
     * 3. Feed toàn bộ list → WeekScheduleAdapter
     */
    private void refreshUI() {
        Calendar mon = getMonday();
        Calendar sun = (Calendar) mon.clone();
        sun.add(Calendar.DAY_OF_YEAR, 7); // exclusive end

        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String monKey = keyFmt.format(mon.getTime());
        String sunKey = keyFmt.format(sun.getTime());

        int bookedThisWeek = 0;
        Set<String> bookedDayKeys = new HashSet<>();

        for (TimeSlotsModel slot : fullFirebaseList) {
            if (slot == null || slot.getStart_time() == null) continue;
            String slotKey = keyFmt.format(slot.getStart_time().toDate());

            if ("booked".equalsIgnoreCase(slot.getStatus())) {
                bookedDayKeys.add(slotKey);
                if (slotKey.compareTo(monKey) >= 0 && slotKey.compareTo(sunKey) < 0) {
                    bookedThisWeek++;
                }
            }
        }

        // 1. Badge số ca bận tuần này
        if (tvTotalSessions != null) {
            tvTotalSessions.setText(bookedThisWeek + " ca bận");
        }

        // 2. Dot đỏ trên MonthCalendarView
        if (calendarViewScheduleManagement != null) {
            calendarViewScheduleManagement.setBookedDays(bookedDayKeys);
        }

        // 3. Feed available + booked vào adapter lịch tuần
        if (weekAdapter != null) {
            weekAdapter.setSlots(fullFirebaseList);
        }
    }

    /** Thứ 2 của tuần hiện tại lúc 00:00:00 */
    private Calendar getMonday() {
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SUNDAY) {
            c.add(Calendar.DAY_OF_YEAR, -6);
        } else {
            c.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - dow);
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }
}