package com.example.heami.ui.doctor;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.TimeSlotsModel;
import com.example.heami.utils.ExitDialogHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DoctorAppointmentsActivity extends AppCompatActivity {

    private RecyclerView rvAppointments;
    private TimeSlotsAdapter adapter;
    private List<TimeSlotsModel> displayList;
    private List<TimeSlotsModel> fullFirebaseList;
    private List<TimeSlotsModel> allSlotsFromFirebase;

    private TextView tvTotalSessions;
    private TextView tabAll, tabUpcoming, tabOngoing;
    private ImageButton btnBack;
    private TextView btnOpenScheduleSetup;

    private FirebaseFirestore db;
    private String currentTabFilter = "Tất cả";
    private final String currentDoctorId = "DOC_001";

    private List<String> fixedWorkingShifts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_management);

        db = FirebaseFirestore.getInstance();

        generateWorkingShifts();
        initViews();
        setupRecyclerView();
        setupTabListeners();
        listenToTimeSlots();

        try {
            DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_APPOINTMENTS);
            ExitDialogHelper.registerExitHandler(this);
        } catch (Exception e) {
            Log.e("HEAMI_CRASH", "Lỗi bottom nav: " + e.getMessage());
        }
    }

    private void generateWorkingShifts() {
        fixedWorkingShifts = new ArrayList<>();
        fixedWorkingShifts.add("09:00 - 10:00");
        fixedWorkingShifts.add("10:30 - 11:30");
        fixedWorkingShifts.add("12:00 - 13:00");
        fixedWorkingShifts.add("13:30 - 14:30");
        fixedWorkingShifts.add("15:00 - 16:00");
        fixedWorkingShifts.add("16:30 - 17:30");
    }

    private void initViews() {
        rvAppointments = findViewById(R.id.rv_appointments);
        tvTotalSessions = findViewById(R.id.tv_total_sessions);
        btnBack = findViewById(R.id.btn_back);

        tabAll = findViewById(R.id.tab_all);
        tabUpcoming = findViewById(R.id.tab_upcoming);
        tabOngoing = findViewById(R.id.tab_ongoing);
        btnOpenScheduleSetup = findViewById(R.id.btn_open_schedule_setup);

        // 🌟 ĐÃ CẬP NHẬT: Nhấn nút back điều hướng an toàn về DoctorHomeActivity
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateToDoctorHome());
        }

        if (btnOpenScheduleSetup != null) {
            btnOpenScheduleSetup.setOnClickListener(v -> openCalendarDialog());
        }
    }

    // ĐIỀU HƯỚNG VỀ HOME DOCTOR VÀ LÀM SẠCH STACK
    private void navigateToDoctorHome() {
        try {
            Intent intent = new Intent(this, DoctorHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("HEAMI_NAV", "Lỗi điều hướng Home: " + e.getMessage());
            finish();
        }
    }

    private void openCalendarDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calendar_picker, null);
        CalendarView dialogCalendarView = dialogView.findViewById(R.id.calendarView);

        AlertDialog calendarDialog = new AlertDialog.Builder(this).setView(dialogView).create();

        if (calendarDialog.getWindow() != null) {
            calendarDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (dialogCalendarView != null) {
            dialogCalendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                calendarDialog.dismiss();

                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(year, month, dayOfMonth, 0, 0, 0);
                selectedCal.set(Calendar.MILLISECOND, 0);

                String dateLabel = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, (month + 1), year);
                openSetupScheduleDialog(selectedCal, dateLabel);
            });
        }

        calendarDialog.show();
    }

    private void openSetupScheduleDialog(Calendar selectedCal, String dateLabel) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_available_slots, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_date_title);
        ImageButton btnCloseDialog = dialogView.findViewById(R.id.btn_close_dialog);
        RecyclerView rvHours = dialogView.findViewById(R.id.rv_dialog_setup_hours);
        Button btnSave = dialogView.findViewById(R.id.btn_save_schedule);

        tvDialogTitle.setText("Thiết lập ngày " + dateLabel);

        List<String> activeAvailableShifts = new ArrayList<>();
        Map<String, String> shiftToDocIdMap = new HashMap<>(); // Lưu ID tài liệu hiện tại để xử lý nếu bị loại bỏ

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String selectedDayStr = dayFmt.format(selectedCal.getTime());

        // Quét tìm các khung giờ trống đã được cấu hình từ trước
        for (TimeSlotsModel slot : allSlotsFromFirebase) {
            if (slot == null || slot.getStart_time() == null || slot.getEnd_time() == null) continue;
            String slotDayStr = dayFmt.format(slot.getStart_time().toDate());

            if (selectedDayStr.equals(slotDayStr) && "available".equalsIgnoreCase(slot.getStatus())) {
                String rangeText = timeFmt.format(slot.getStart_time().toDate()) + " - " + timeFmt.format(slot.getEnd_time().toDate());
                activeAvailableShifts.add(rangeText);

                // Ghi nhớ slot_id/id tài liệu để xử lý đồng bộ
                if (slot.getSlot_id() != null) {
                    shiftToDocIdMap.put(rangeText, slot.getSlot_id());
                }
            }
        }

        rvHours.setLayoutManager(new GridLayoutManager(this, 2));
        SetupHoursAdapter setupAdapter = new SetupHoursAdapter(this, fixedWorkingShifts, activeAvailableShifts);
        rvHours.setAdapter(setupAdapter);

        AlertDialog hoursDialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (hoursDialog.getWindow() != null) {
            hoursDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnSave.setOnClickListener(v -> {
            // 🛠️ XỬ LÝ ĐỒNG BỘ: Quét dọn các ca đã bị BÁC SĨ BỎ CHỌN trực tiếp trên Firebase
            for (String fixedShift : fixedWorkingShifts) {
                if (!activeAvailableShifts.contains(fixedShift) && shiftToDocIdMap.containsKey(fixedShift)) {
                    String targetSlotId = shiftToDocIdMap.get(fixedShift);
                    db.collection("lich_hen")
                            .whereEqualTo("slot_id", targetSlotId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().delete();
                                }
                            });
                }
            }

            // LƯU THÊM MỚI các ca vừa được bác sĩ tích chọn thêm
            for (String shift : activeAvailableShifts) {
                String[] times = shift.split(" - ");
                String[] startParts = times[0].split(":");
                String[] endParts = times[1].split(":");

                Calendar startCal = (Calendar) selectedCal.clone();
                startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startParts[0]));
                startCal.set(Calendar.MINUTE, Integer.parseInt(startParts[1]));

                Calendar endCal = (Calendar) selectedCal.clone();
                endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endParts[0]));
                endCal.set(Calendar.MINUTE, Integer.parseInt(endParts[1]));

                boolean isExist = false;
                for (TimeSlotsModel currentSlot : allSlotsFromFirebase) {
                    if (currentSlot.getStart_time() != null &&
                            currentSlot.getStart_time().toDate().getTime() == startCal.getTimeInMillis()) {
                        isExist = true;
                        break;
                    }
                }

                if (!isExist) {
                    Map<String, Object> newSlot = new HashMap<>();
                    newSlot.put("doctor_id", currentDoctorId);
                    newSlot.put("status", "available");
                    newSlot.put("session_id", "");
                    newSlot.put("slot_id", "slot_" + startCal.getTimeInMillis());
                    newSlot.put("start_time", new Timestamp(startCal.getTime()));
                    newSlot.put("end_time", new Timestamp(endCal.getTime()));

                    db.collection("lich_hen").add(newSlot);
                }
            }

            Toast.makeText(this, "Đã cập nhật lịch nhận khách ngày " + dateLabel, Toast.LENGTH_SHORT).show();
            hoursDialog.dismiss();
        });

        btnCloseDialog.setOnClickListener(v -> hoursDialog.dismiss());
        hoursDialog.show();
    }

    private void setupRecyclerView() {
        displayList = new ArrayList<>();
        fullFirebaseList = new ArrayList<>();
        allSlotsFromFirebase = new ArrayList<>();

        if (rvAppointments != null) {
            rvAppointments.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TimeSlotsAdapter(this, displayList);
            rvAppointments.setAdapter(adapter);
        }
    }

    private void setupTabListeners() {
        if (tabAll != null) tabAll.setOnClickListener(v -> updateTabSelection("Tất cả", tabAll));
        if (tabUpcoming != null) tabUpcoming.setOnClickListener(v -> updateTabSelection("booked", tabUpcoming));
        if (tabOngoing != null) tabOngoing.setOnClickListener(v -> updateTabSelection("completed", tabOngoing));
    }

    private void updateTabSelection(String filterStatus, TextView selectedTab) {
        currentTabFilter = filterStatus;

        TextView[] allTabs = {tabAll, tabUpcoming, tabOngoing};
        for (TextView tab : allTabs) {
            if (tab != null) {
                tab.setTextColor(Color.parseColor("#718096"));
                tab.setBackgroundResource(R.drawable.bg_appointment_tab_normal);
            }
        }

        if (selectedTab != null) {
            selectedTab.setTextColor(Color.parseColor("#319795"));
            selectedTab.setBackgroundResource(R.drawable.bg_appointment_tab_active);
        }

        executeFilter();
    }

    private void listenToTimeSlots() {
        db.collection("lich_hen")
                .whereEqualTo("doctor_id", currentDoctorId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("HEAMI_FIREBASE", "Lỗi Firestore: " + error.getMessage());
                        return;
                    }

                    if (snapshots != null) {
                        fullFirebaseList.clear();
                        allSlotsFromFirebase.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            try {
                                TimeSlotsModel slot = document.toObject(TimeSlotsModel.class);
                                if (slot != null) {
                                    fullFirebaseList.add(slot);
                                    allSlotsFromFirebase.add(slot);
                                }
                            } catch (Exception e) {
                                Log.e("HEAMI_PARSING", "Lỗi ánh xạ Model: " + e.getMessage());
                            }
                        }
                        executeFilter();
                    }
                });
    }

    private void executeFilter() {
        displayList.clear();

        for (TimeSlotsModel slot : fullFirebaseList) {
            if (slot == null || "available".equalsIgnoreCase(slot.getStatus())) {
                continue;
            }

            if ("Tất cả".equals(currentTabFilter)) {
                displayList.add(slot);
            } else {
                if (slot.getStatus() != null && slot.getStatus().equalsIgnoreCase(currentTabFilter)) {
                    displayList.add(slot);
                }
            }
        }

        if (tvTotalSessions != null) {
            tvTotalSessions.setText(displayList.size() + " phiên tư vấn");
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}