package com.example.heami.ui.doctor;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.heami.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorPatientDetailActivity extends AppCompatActivity {

    private ImageButton btnPatientDetailBack;
    private TextView txtPatientBookingNote;
    
    // Editable Causes
    private TextView btnEditCauses;
    private TextView txtPatientCausesDisplay;
    private EditText edtPatientCausesInput;
    private boolean isEditingCauses = false;

    // Editable Consultation Notes (removed)

    // Stats Section
    private View layoutStatsHeaderToggle;
    private ImageView imgStatsExpandToggle;
    private LinearLayout layoutStatsLockedMessage;
    private LinearLayout layoutStatsSharedContent;
    private boolean isStatsExpanded = false;
    private boolean isStatsShared = false; // Default to false, can be queried from database

    // Time filter stats variables
    private int regYear = 2025;
    private int regMonth = 4;
    private int sysYear = 2026;
    private int sysMonth = 6;
    private int currentYear = 2026;
    private int currentMonth = 6;
    private int currentWeek = 1;
    private boolean showYearlyTrend = false;

    private LinearLayout btnPatientDetailNotes;
    private LinearLayout btnPatientDetailCreatePlan;
    private LinearLayout btnPatientDetailHistory;

    private String patientUserId = "";
    private String latestConsultationDocId = "";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patient_detail);

        db = FirebaseFirestore.getInstance();

        // Khởi tạo thời gian hệ thống
        java.util.Calendar cal = java.util.Calendar.getInstance();
        sysYear = cal.get(java.util.Calendar.YEAR);
        sysMonth = cal.get(java.util.Calendar.MONTH) + 1;
        currentYear = sysYear;
        currentMonth = sysMonth;
        int sysDay = cal.get(java.util.Calendar.DAY_OF_MONTH);
        if (sysDay <= 7) currentWeek = 1;
        else if (sysDay <= 14) currentWeek = 2;
        else if (sysDay <= 21) currentWeek = 3;
        else currentWeek = 4;

        initViews();
        setupActions();
        setupSpinners();
        setupCalendarGrid();
        setupWeeklyLineChart();
        setupTrendChart();
        setupHeatmap();
        setupFactorsAndPatterns();
        setupStreakStats();
        setupJournalLogs();
        setupRecommendations();
        loadPatientData();
    }

    private void initViews() {
        btnPatientDetailBack = findViewById(R.id.btnPatientDetailBack);
        txtPatientBookingNote = findViewById(R.id.txtPatientBookingNote);

        btnEditCauses = findViewById(R.id.btnEditCauses);
        txtPatientCausesDisplay = findViewById(R.id.txtPatientCausesDisplay);
        edtPatientCausesInput = findViewById(R.id.edtPatientCausesInput);

        // Consultation notes views removed

        layoutStatsHeaderToggle = findViewById(R.id.layoutStatsHeaderToggle);
        imgStatsExpandToggle = findViewById(R.id.imgStatsExpandToggle);
        layoutStatsLockedMessage = findViewById(R.id.layoutStatsLockedMessage);
        layoutStatsSharedContent = findViewById(R.id.layoutStatsSharedContent);

        btnPatientDetailNotes = findViewById(R.id.btnPatientDetailNotes);
        btnPatientDetailCreatePlan = findViewById(R.id.btnPatientDetailCreatePlan);
        btnPatientDetailHistory = findViewById(R.id.btnPatientDetailHistory);
    }

    private void setupActions() {
        btnPatientDetailBack.setOnClickListener(v -> finish());

        // Toggle edit Causes
        btnEditCauses.setOnClickListener(v -> toggleEditCauses());

        // Toggle Stats Expand/Collapse
        layoutStatsHeaderToggle.setOnClickListener(v -> toggleStatsExpand());

        btnPatientDetailNotes.setOnClickListener(v -> showConsultationNotesDialog());

        btnPatientDetailCreatePlan.setOnClickListener(v -> 
            Toast.makeText(this, "Chức năng tạo kế hoạch mới...", Toast.LENGTH_SHORT).show()
        );

        btnPatientDetailHistory.setOnClickListener(v -> 
            Toast.makeText(this, "Đang hiển thị lịch sử tư vấn...", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadPatientData() {
        String directUserId = getIntent().getStringExtra("patient_user_id");
        if (directUserId != null && !directUserId.isEmpty()) {
            patientUserId = directUserId;
            db.collection("users").document(patientUserId).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String nickname = userDoc.getString("nickname");
                            TextView txtPatientDetailName = findViewById(R.id.txtPatientDetailName);
                            if (txtPatientDetailName != null && nickname != null) {
                                txtPatientDetailName.setText(nickname);
                            }
                        }
                    });
            checkStatsSharingSettings(patientUserId);
            loadLatestConsultation(patientUserId);
            return;
        }

        String targetPrefix = getIntent().getStringExtra("PATIENT_NICKNAME_PREFIX");
        final String prefix = (targetPrefix != null && !targetPrefix.isEmpty()) ? targetPrefix : "Bướm";
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String nickname = doc.getString("nickname");
                            if (nickname != null && nickname.contains(prefix)) {
                                patientUserId = doc.getId();
                                checkStatsSharingSettings(patientUserId);
                                loadLatestConsultation(patientUserId);
                                break;
                            }
                        }
                    }
                    if (patientUserId.isEmpty()) {
                        // Fallback if no matching user found
                        txtPatientBookingNote.setText("Không tìm thấy thông tin bệnh nhân trong hệ thống.");
                    }
                });
    }

    private void checkStatsSharingSettings(String userId) {
        // Query settings to check if stats sharing is permitted
        db.collection("users").document(userId).collection("settings").document("default")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean shared = documentSnapshot.getBoolean("shareStatsWithDoctor");
                        if (shared != null) {
                            isStatsShared = shared;
                        } else {
                            isStatsShared = false; // Default false
                        }
                    } else {
                        isStatsShared = false; // Default false
                    }
                    
                    // Update layout badge based on sharing permission status
                    TextView txtPermissionStatus = findViewById(R.id.txtPermissionStatus);
                    ImageView imgPermissionShield = findViewById(R.id.imgPermissionShield);
                    LinearLayout layoutPermissionBadge = findViewById(R.id.layoutPermissionBadge);
                    
                    if (txtPermissionStatus != null && imgPermissionShield != null && layoutPermissionBadge != null) {
                        if (isStatsShared) {
                            txtPermissionStatus.setText("Đã cấp quyền xem dữ liệu");
                            txtPermissionStatus.setTextColor(0xFF09A38C);
                            imgPermissionShield.setColorFilter(0xFF09A38C);
                            layoutPermissionBadge.setBackgroundResource(R.drawable.bg_stat_icon_green);
                        } else {
                            txtPermissionStatus.setText("Quyền xem dữ liệu bị hạn chế");
                            txtPermissionStatus.setTextColor(0xFF7F8C8D);
                            imgPermissionShield.setColorFilter(0xFF7F8C8D);
                            layoutPermissionBadge.setBackgroundResource(R.drawable.bg_chip_inactive);
                        }
                    }
                });
    }

    private void loadLatestConsultation(String userId) {
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);
        String doctorUid = "doc_001";
        if (isDoctor) {
            doctorUid = prefs.getString("doctor_id", "doc_001");
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        db.collection("consultations")
                .whereEqualTo("doctor_id", doctorUid)
                .whereEqualTo("user_id", userId)
                .orderBy("booked_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        latestConsultationDocId = doc.getId();

                        // Load Booking Note
                        String bookingNote = doc.getString("note");
                        if (bookingNote != null && !bookingNote.isEmpty()) {
                            txtPatientBookingNote.setText(bookingNote);
                        } else {
                            txtPatientBookingNote.setText("Bệnh nhân không để lại ghi chú cho cuộc hẹn này.");
                        }

                        // Load Doctor Consultation Notes (moved to bottom dialog)

                        // Load Causes
                        List<String> causesList = (List<String>) doc.get("commonCauses");
                        if (causesList != null && !causesList.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < causesList.size(); i++) {
                                sb.append(causesList.get(i));
                                if (i < causesList.size() - 1) {
                                    sb.append(", ");
                                }
                            }
                            txtPatientCausesDisplay.setText(sb.toString());
                            edtPatientCausesInput.setText(sb.toString());
                        } else {
                            txtPatientCausesDisplay.setText("Chưa ghi nhận nguyên nhân.");
                            edtPatientCausesInput.setText("");
                        }
                    } else {
                        txtPatientBookingNote.setText("Chưa tìm thấy cuộc hẹn nào giữa bạn và bệnh nhân này.");
                    }
                });
    }

    private void toggleEditCauses() {
        if (!isEditingCauses) {
            // Switch to Edit Mode
            isEditingCauses = true;
            btnEditCauses.setText("Lưu");
            txtPatientCausesDisplay.setVisibility(View.GONE);
            edtPatientCausesInput.setVisibility(View.VISIBLE);
        } else {
            // Save & Switch back to Display Mode
            isEditingCauses = false;
            btnEditCauses.setText("Sửa");
            String input = edtPatientCausesInput.getText().toString().trim();
            txtPatientCausesDisplay.setText(input.isEmpty() ? "Trống" : input);
            txtPatientCausesDisplay.setVisibility(View.VISIBLE);
            edtPatientCausesInput.setVisibility(View.GONE);

            // Save to Firestore
            if (!latestConsultationDocId.isEmpty()) {
                List<String> causesList = new ArrayList<>();
                if (!input.isEmpty()) {
                    String[] tokens = input.split(",");
                    for (String t : tokens) {
                        causesList.add(t.trim());
                    }
                }
                db.collection("consultations").document(latestConsultationDocId)
                        .update("commonCauses", causesList)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã cập nhật nguyên nhân!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }



    private void showConsultationNotesDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_patient_consultation_notes, null);
        dialog.setContentView(sheetView);

        ImageButton btnClose = sheetView.findViewById(R.id.btnCloseNotesDialog);
        TextView txtNoConsultations = sheetView.findViewById(R.id.txtNoConsultationsMessage);
        LinearLayout layoutNotesContainer = sheetView.findViewById(R.id.layoutNotesContainer);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        String doctorUid = "doc_001";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            doctorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (layoutNotesContainer != null) {
            layoutNotesContainer.removeAllViews();
        }

        db.collection("consultations")
                .whereEqualTo("doctorId", doctorUid)
                .whereEqualTo("userId", patientUserId)
                .orderBy("bookedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        if (txtNoConsultations != null) txtNoConsultations.setVisibility(View.GONE);
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            addConsultationNoteItem(layoutNotesContainer, doc);
                        }
                    } else {
                        if (txtNoConsultations != null) txtNoConsultations.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lấy dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (txtNoConsultations != null) {
                        txtNoConsultations.setText("Lỗi kết nối mạng.");
                        txtNoConsultations.setVisibility(View.VISIBLE);
                    }
                });

        dialog.show();
    }

    private void addConsultationNoteItem(LinearLayout container, DocumentSnapshot doc) {
        if (container == null) return;
        String docId = doc.getId();
        String packageType = doc.getString("package_type");
        if (packageType == null) packageType = "Thường";
        String formatType = doc.getString("format_type");
        if (formatType == null) formatType = "Online";
        
        com.google.firebase.Timestamp timeStamp = doc.getTimestamp("bookedAt");
        String dateStr = "Chưa rõ thời gian";
        if (timeStamp != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            dateStr = sdf.format(timeStamp.toDate());
        }
        
        String initialNotes = doc.getString("doctor_notes");
        if (initialNotes == null) initialNotes = "";

        float density = getResources().getDisplayMetrics().density;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadius(16 * density);
        gd.setStroke((int)(1 * density), Color.parseColor("#E1E5EC"));
        card.setBackground(gd);
        card.setPadding((int)(16*density), (int)(16*density), (int)(16*density), (int)(16*density));
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = (int)(12 * density);
        card.setLayoutParams(cardParams);

        // Header Info
        TextView tvHeader = new TextView(this);
        tvHeader.setText("📅 " + dateStr + " (" + packageType + " • " + formatType + ")");
        tvHeader.setTextColor(Color.parseColor("#313866"));
        tvHeader.setTextSize(13);
        tvHeader.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        card.addView(tvHeader);

        // Display note text
        TextView tvNoteDisplay = new TextView(this);
        tvNoteDisplay.setText(initialNotes.isEmpty() ? "Chưa có ghi chú từ bác sĩ cho phiên này." : initialNotes);
        tvNoteDisplay.setTextColor(Color.parseColor("#5A6B82"));
        tvNoteDisplay.setTextSize(12);
        tvNoteDisplay.setPadding(0, (int)(8*density), 0, (int)(8*density));
        card.addView(tvNoteDisplay);

        // Edit text input (hidden by default)
        EditText etNoteInput = new EditText(this);
        etNoteInput.setText(initialNotes);
        etNoteInput.setHint("Nhập ghi chú kết quả tư vấn...");
        etNoteInput.setTextColor(Color.parseColor("#2D1B47"));
        etNoteInput.setTextSize(13);
        etNoteInput.setPadding((int)(10*density), (int)(10*density), (int)(10*density), (int)(10*density));
        etNoteInput.setBackgroundResource(R.drawable.bg_bio_box);
        etNoteInput.setGravity(Gravity.TOP | Gravity.START);
        etNoteInput.setLines(3);
        etNoteInput.setVisibility(View.GONE);
        card.addView(etNoteInput);

        // Button row
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.topMargin = (int)(6 * density);
        buttonRow.setLayoutParams(btnRowParams);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("Sửa");
        btnEdit.setTextColor(Color.parseColor("#09A38C"));
        btnEdit.setTextSize(13);
        btnEdit.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        btnEdit.setPadding((int)(12*density), (int)(6*density), (int)(12*density), (int)(6*density));
        buttonRow.addView(btnEdit);
        card.addView(buttonRow);

        final boolean[] isEditing = {false};
        btnEdit.setOnClickListener(v -> {
            if (!isEditing[0]) {
                isEditing[0] = true;
                btnEdit.setText("Lưu");
                tvNoteDisplay.setVisibility(View.GONE);
                etNoteInput.setVisibility(View.VISIBLE);
                etNoteInput.requestFocus();
            } else {
                isEditing[0] = false;
                btnEdit.setText("Sửa");
                String inputNote = etNoteInput.getText().toString().trim();
                tvNoteDisplay.setText(inputNote.isEmpty() ? "Chưa có ghi chú từ bác sĩ cho phiên này." : inputNote);
                tvNoteDisplay.setVisibility(View.VISIBLE);
                etNoteInput.setVisibility(View.GONE);

                // Update in Firestore
                db.collection("consultations").document(docId)
                        .update("doctorNotes", inputNote)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã cập nhật ghi chú phiên này!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        container.addView(card);
    }

    private void toggleStatsExpand() {
        if (!isStatsExpanded) {
            isStatsExpanded = true;
            if (imgStatsExpandToggle != null) {
                imgStatsExpandToggle.animate().rotation(90f).setDuration(250).start();
            }
            if (isStatsShared) {
                layoutStatsSharedContent.setVisibility(View.VISIBLE);
                layoutStatsLockedMessage.setVisibility(View.GONE);
            } else {
                layoutStatsSharedContent.setVisibility(View.GONE);
                layoutStatsLockedMessage.setVisibility(View.VISIBLE);
            }
        } else {
            isStatsExpanded = false;
            if (imgStatsExpandToggle != null) {
                imgStatsExpandToggle.animate().rotation(0f).setDuration(250).start();
            }
            layoutStatsSharedContent.setVisibility(View.GONE);
            layoutStatsLockedMessage.setVisibility(View.GONE);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  STATS SETUP & HELPER METHODS (Dùng chung bộ logic như StatsActivity)
    // ─────────────────────────────────────────────────────────
    private void setupSpinners() {
        Spinner spinnerYear = findViewById(R.id.spinnerYear);
        Spinner spinnerMonth = findViewById(R.id.spinnerMonth);
        Spinner spinnerWeek = findViewById(R.id.spinnerWeek);

        if (spinnerYear == null || spinnerMonth == null || spinnerWeek == null) return;

        List<String> years = new ArrayList<>();
        for (int y = sysYear; y >= regYear; y--) {
            years.add(String.valueOf(y));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, years);
        yearAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerYear.setAdapter(yearAdapter);

        List<String> months = generateMonthsForYear(currentYear);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, months);
        monthAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerMonth.setAdapter(monthAdapter);

        List<String> weeks = generateWeeksForMonth(currentYear, currentMonth);
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, weeks);
        weekAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerWeek.setAdapter(weekAdapter);

        spinnerYear.setSelection(0);
        spinnerMonth.setSelection(0);
        spinnerWeek.setSelection(currentWeek - 1);

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentYear = Integer.parseInt(years.get(position));
                showYearlyTrend = true;

                List<String> updatedMonths = generateMonthsForYear(currentYear);
                ArrayAdapter<String> updatedMonthAdapter = new ArrayAdapter<>(DoctorPatientDetailActivity.this, R.layout.item_spinner_selected, updatedMonths);
                updatedMonthAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
                spinnerMonth.setAdapter(updatedMonthAdapter);
                spinnerMonth.setSelection(0);
                
                currentMonth = Integer.parseInt(updatedMonths.get(0));
                updateWeekSpinner(spinnerWeek);

                setupTrendChart();
                setupCalendarGrid();
                setupWeeklyLineChart();
                setupHeatmap();
                setupFactorsAndPatterns();
                setupJournalLogs();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                List<String> currentMonthsList = generateMonthsForYear(currentYear);
                if (position >= currentMonthsList.size()) return;
                
                currentMonth = Integer.parseInt(currentMonthsList.get(position));
                showYearlyTrend = false;
                
                TextView txtCalendarMonth = findViewById(R.id.txtCalendarMonth);
                if (txtCalendarMonth != null) {
                    txtCalendarMonth.setText("Lịch Cảm Xúc Tháng " + currentMonth + "/" + currentYear);
                }

                updateWeekSpinner(spinnerWeek);

                setupCalendarGrid();
                setupTrendChart();
                setupWeeklyLineChart();
                setupHeatmap();
                setupFactorsAndPatterns();
                setupJournalLogs();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentWeek = position + 1;
                setupWeeklyLineChart();
                setupHeatmap();
                setupFactorsAndPatterns();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private List<String> generateMonthsForYear(int year) {
        List<String> monthsList = new ArrayList<>();
        int startMonth = 12;
        int endMonth = 1;
        if (year == sysYear) startMonth = sysMonth;
        if (year == regYear) endMonth = regMonth;
        for (int m = startMonth; m >= endMonth; m--) {
            monthsList.add(String.valueOf(m));
        }
        return monthsList;
    }

    private List<String> generateWeeksForMonth(int year, int month) {
        List<String> weeksList = new ArrayList<>();
        int maxWeek = 4;
        if (year == sysYear && month == sysMonth) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int sysDay = cal.get(java.util.Calendar.DAY_OF_MONTH);
            if (sysDay <= 7) maxWeek = 1;
            else if (sysDay <= 14) maxWeek = 2;
            else if (sysDay <= 21) maxWeek = 3;
            else maxWeek = 4;
        }
        for (int w = 1; w <= maxWeek; w++) {
            weeksList.add(String.valueOf(w));
        }
        return weeksList;
    }

    private void updateWeekSpinner(Spinner spinnerWeek) {
        List<String> updatedWeeks = generateWeeksForMonth(currentYear, currentMonth);
        ArrayAdapter<String> updatedWeekAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, updatedWeeks);
        updatedWeekAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerWeek.setAdapter(updatedWeekAdapter);
        if (currentWeek > updatedWeeks.size()) {
            currentWeek = updatedWeeks.size();
        }
        spinnerWeek.setSelection(currentWeek - 1);
    }

    private void setupCalendarGrid() {
        GridLayout gridCalendar = findViewById(R.id.gridCalendar);
        if (gridCalendar == null) return;

        float density = getResources().getDisplayMetrics().density;
        int marginPx = (int) (4 * density);
        int paddingPx = (int) (8 * density);

        gridCalendar.removeAllViews();

        int offset = getStartOffsetForMonth(currentMonth);
        for (int i = 0; i < offset; i++) {
            View emptyView = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (40 * density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            emptyView.setLayoutParams(params);
            gridCalendar.addView(emptyView);
        }

        int totalDays = getDaysInMonth(currentMonth);
        for (int day = 1; day <= totalDays; day++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            cell.setLayoutParams(params);

            TextView tvDay = new TextView(this);
            tvDay.setText(String.valueOf(day));
            tvDay.setTextSize(12);
            tvDay.setTextColor(Color.parseColor("#313866"));
            tvDay.setGravity(Gravity.CENTER);
            cell.addView(tvDay);

            String emoji = getMoodEmojiForDayAndMonth(day, currentMonth);
            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(14);
            tvEmoji.setGravity(Gravity.CENTER);
            tvEmoji.setPadding(0, (int) (2 * density), 0, 0);
            cell.addView(tvEmoji);

            boolean isDayInSelectedWeek = false;
            int startDay = 1;
            if (currentWeek == 2) startDay = 8;
            else if (currentWeek == 3) startDay = 15;
            else if (currentWeek == 4) startDay = 22;

            int endDay = startDay + 6;
            if (currentWeek == 4) endDay = totalDays;

            if (day >= startDay && day <= endDay) {
                isDayInSelectedWeek = true;
            }

            cell.setAlpha(1.0f);

            if (!emoji.isEmpty()) {
                int bgColor = getMoodBgColorForEmoji(emoji);
                if (isDayInSelectedWeek) {
                    GradientDrawable highlightGd = new GradientDrawable();
                    highlightGd.setColor(bgColor);
                    highlightGd.setStroke((int) (2f * density), Color.parseColor("#313866"));
                    highlightGd.setCornerRadius(12 * density);
                    cell.setBackground(highlightGd);
                } else {
                    cell.setBackground(createCellBackground(bgColor, 12 * density));
                }
            } else {
                GradientDrawable borderGd = new GradientDrawable();
                borderGd.setColor(Color.parseColor("#FAFAFA"));
                if (isDayInSelectedWeek) {
                    borderGd.setStroke((int) (2f * density), Color.parseColor("#313866"));
                } else {
                    borderGd.setStroke((int) (1 * density), Color.parseColor("#EAEAEA"));
                }
                borderGd.setCornerRadius(12 * density);
                cell.setBackground(borderGd);
            }

            gridCalendar.addView(cell);
        }
    }

    private int getStartOffsetForMonth(int month) {
        if (month == 4) return 1;
        if (month == 5) return 4;
        return 0;
    }

    private int getDaysInMonth(int month) {
        if (month == 4) return 30;
        if (month == 5) return 31;
        return 30;
    }

    private String getMoodEmojiForDayAndMonth(int day, int month) {
        if (month == 6) {
            switch (day) {
                case 3: return "😊";
                case 7: return "😌";
                case 12: return "😮‍💨";
                case 18: return "😊";
                case 24: return "😢";
                default: return "";
            }
        }
        if (month == 4) {
            switch (day) {
                case 2: return "😊";
                case 5: return "😌";
                case 9: return "😮‍💨";
                case 14: return "😊";
                case 18: return "😢";
                case 23: return "😌";
                case 28: return "😊";
                default: return "";
            }
        } else if (month == 5) {
            switch (day) {
                case 1: return "😊";
                case 4: return "😌";
                case 8: return "😮‍💨";
                case 12: return "😊";
                case 15: return "😊";
                case 19: return "😢";
                case 22: return "😌";
                case 26: return "😊";
                case 29: return "😡";
                default: return "";
            }
        } else {
            switch (day) {
                case 4: case 10: case 17: case 25:
                    return "😮‍💨";
                case 5: case 16:
                    return "😢";
                case 20:
                    return "😡";
                case 2: case 7: case 11: case 13: case 18: case 22: case 27: case 30:
                    return "😌";
                default:
                    return "😊";
            }
        }
    }

    private int getMoodBgColorForEmoji(String emoji) {
        switch (emoji) {
            case "😮‍💨": return Color.parseColor("#FFF9C4");
            case "😢": return Color.parseColor("#F3E5F5");
            case "😡": return Color.parseColor("#FFEBEE");
            case "😌": return Color.parseColor("#E1F5FE");
            default: return Color.parseColor("#E0F7F4");
        }
    }

    private Drawable createCellBackground(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    private void setupTrendChart() {
        ProgressBar progressHappy = findViewById(R.id.progressHappy);
        ProgressBar progressCalm = findViewById(R.id.progressCalm);
        ProgressBar progressStressed = findViewById(R.id.progressStressed);
        ProgressBar progressSad = findViewById(R.id.progressSad);
        ProgressBar progressAngry = findViewById(R.id.progressAngry);

        TextView txtPercentHappy = findViewById(R.id.txtPercentHappy);
        TextView txtPercentCalm = findViewById(R.id.txtPercentCalm);
        TextView txtPercentStressed = findViewById(R.id.txtPercentStressed);
        TextView txtPercentSad = findViewById(R.id.txtPercentSad);
        TextView txtPercentAngry = findViewById(R.id.txtPercentAngry);

        int totalRecorded = 0;
        int happyCount = 0;
        int calmCount = 0;
        int stressedCount = 0;
        int sadCount = 0;
        int angryCount = 0;

        if (showYearlyTrend) {
            for (int m = 1; m <= 12; m++) {
                int totalDays = getDaysInMonth(m);
                for (int day = 1; day <= totalDays; day++) {
                    String emoji = getMoodEmojiForDayAndMonth(day, m);
                    if (!emoji.isEmpty()) {
                        totalRecorded++;
                        if (emoji.equals("😊")) happyCount++;
                        else if (emoji.equals("😌")) calmCount++;
                        else if (emoji.equals("😮‍💨")) stressedCount++;
                        else if (emoji.equals("😢")) sadCount++;
                        else if (emoji.equals("😡")) angryCount++;
                    }
                }
            }
        } else {
            int totalDays = getDaysInMonth(currentMonth);
            for (int day = 1; day <= totalDays; day++) {
                String emoji = getMoodEmojiForDayAndMonth(day, currentMonth);
                if (!emoji.isEmpty()) {
                    totalRecorded++;
                    if (emoji.equals("😊")) happyCount++;
                    else if (emoji.equals("😌")) calmCount++;
                    else if (emoji.equals("😮‍💨")) stressedCount++;
                    else if (emoji.equals("😢")) sadCount++;
                    else if (emoji.equals("😡")) angryCount++;
                }
            }
        }

        int pHappy = 0, pCalm = 0, pStressed = 0, pSad = 0, pAngry = 0;
        if (totalRecorded > 0) {
            pHappy = (happyCount * 100) / totalRecorded;
            pCalm = (calmCount * 100) / totalRecorded;
            pStressed = (stressedCount * 100) / totalRecorded;
            pSad = (sadCount * 100) / totalRecorded;
            pAngry = 100 - (pHappy + pCalm + pStressed + pSad);
            if (pAngry < 0) pAngry = 0;
        }

        if (progressHappy != null) {
            progressHappy.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#00BFA5")));
            progressHappy.setProgress(pHappy);
        }
        if (txtPercentHappy != null) txtPercentHappy.setText(pHappy + "%");

        if (progressCalm != null) {
            progressCalm.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4A90E2")));
            progressCalm.setProgress(pCalm);
        }
        if (txtPercentCalm != null) txtPercentCalm.setText(pCalm + "%");

        if (progressStressed != null) {
            progressStressed.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB300")));
            progressStressed.setProgress(pStressed);
        }
        if (txtPercentStressed != null) txtPercentStressed.setText(pStressed + "%");

        if (progressSad != null) {
            progressSad.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9C27B0")));
            progressSad.setProgress(pSad);
        }
        if (txtPercentSad != null) txtPercentSad.setText(pSad + "%");

        if (progressAngry != null) {
            progressAngry.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E86FA0")));
            progressAngry.setProgress(pAngry);
        }
        if (txtPercentAngry != null) txtPercentAngry.setText(pAngry + "%");

        com.example.heami.ui.custom.EmotionalRadarChartView radarChartView = findViewById(R.id.radarChartView);
        if (radarChartView != null) {
            radarChartView.setData(pHappy / 100f, pCalm / 100f, pStressed / 100f, pSad / 100f, pAngry / 100f);
        }
    }

    private void setupWeeklyLineChart() {
        com.example.heami.ui.custom.WeeklyLineChartView weeklyLineChart = findViewById(R.id.weeklyLineChart);
        if (weeklyLineChart != null) {
            float[] moods = getWeeklyMoodsData(currentYear, currentMonth, currentWeek);
            weeklyLineChart.setWeeklyMoods(moods);
        }
    }

    private float[] getWeeklyMoodsData(int year, int month, int week) {
        float[] moods = new float[7];
        if (year == 2025) {
            return new float[]{4f, 4f, 5f, 3f, 5f, 4f, 5f};
        }
        int startDay = 1;
        if (week == 2) startDay = 8;
        else if (week == 3) startDay = 15;
        else if (week == 4) startDay = 22;

        for (int i = 0; i < 7; i++) {
            int day = startDay + i;
            String emoji = getMoodEmojiForDayAndMonth(day, month);
            moods[i] = getMoodValueFromEmoji(emoji);
        }
        return moods;
    }

    private float getMoodValueFromEmoji(String emoji) {
        switch (emoji) {
            case "😊": return 5f;
            case "😌": return 4f;
            case "😮‍💨": return 3f;
            case "😢": return 2f;
            case "😡": return 1f;
            default: return 4f;
        }
    }

    private void setupHeatmap() {
        com.example.heami.ui.custom.SensitiveTimezoneHeatmapView heatmapView = findViewById(R.id.heatmapView);
        if (heatmapView == null) return;

        int[][] data;
        if (currentWeek == 1) {
            data = new int[][]{
                {0, 1, 0, 2, 0, 1, 0},
                {1, 2, 1, 0, 3, 0, 1},
                {2, 3, 2, 1, 2, 1, 0},
                {1, 1, 3, 2, 1, 0, 2}
            };
        } else if (currentWeek == 2) {
            data = new int[][]{
                {1, 0, 2, 1, 0, 0, 1},
                {2, 1, 0, 3, 1, 2, 0},
                {3, 2, 1, 2, 0, 1, 1},
                {1, 3, 2, 1, 2, 0, 0}
            };
        } else if (currentWeek == 3) {
            data = new int[][]{
                {0, 2, 1, 0, 1, 1, 0},
                {1, 0, 3, 2, 2, 0, 1},
                {2, 1, 2, 1, 3, 1, 0},
                {3, 0, 1, 0, 2, 2, 1}
            };
        } else {
            data = new int[][]{
                {1, 1, 0, 2, 1, 0, 1},
                {0, 2, 1, 1, 3, 1, 0},
                {2, 3, 2, 0, 1, 2, 1},
                {2, 1, 2, 3, 1, 0, 2}
            };
        }
        heatmapView.setHeatmapData(data);
    }

    private void setupFactorsAndPatterns() {
        LinearLayout layoutFactors = findViewById(R.id.layoutFactors);
        if (layoutFactors == null) return;
        layoutFactors.removeAllViews();

        int fWork = 40;
        int fSleep = 30;
        int fRelations = 20;
        int fHealth = 10;

        if (currentWeek == 1) {
            fWork = 50; fSleep = 20; fRelations = 20; fHealth = 10;
        } else if (currentWeek == 2) {
            fWork = 30; fSleep = 40; fRelations = 20; fHealth = 10;
        } else if (currentWeek == 3) {
            fWork = 45; fSleep = 25; fRelations = 15; fHealth = 15;
        } else {
            fWork = 35; fSleep = 35; fRelations = 25; fHealth = 5;
        }

        addFactor(layoutFactors, "Công việc & Học tập", "💼", fWork, Color.parseColor("#E86FA0"));
        addFactor(layoutFactors, "Giấc ngủ", "😴", fSleep, Color.parseColor("#00BFA5"));
        addFactor(layoutFactors, "Mối quan hệ", "🤝", fRelations, Color.parseColor("#4A90E2"));
        addFactor(layoutFactors, "Sức khỏe thể chất", "🏃", fHealth, Color.parseColor("#9575CD"));
    }

    private void addFactor(LinearLayout container, String name, String icon, int percent, int progressColor) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = (int) (10 * density);
        row.setLayoutParams(rowParams);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams((int) (130 * density), LinearLayout.LayoutParams.WRAP_CONTENT));
        tvName.setText(icon + " " + name);
        tvName.setTextColor(Color.parseColor("#313866"));
        tvName.setTextSize(12);
        row.addView(tvName);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(0, (int) (6 * density), 1.0f);
        pbParams.rightMargin = (int) (12 * density);
        pb.setLayoutParams(pbParams);
        pb.setMax(100);
        pb.setProgress(percent);
        pb.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));
        pb.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
        row.addView(pb);

        TextView tvPercent = new TextView(this);
        tvPercent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvPercent.setText(percent + "%");
        tvPercent.setTextColor(Color.parseColor("#7D8BB7"));
        tvPercent.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        tvPercent.setTextSize(12);
        row.addView(tvPercent);

        container.addView(row);
    }

    private void setupStreakStats() {
        TextView txtCurrentStreak = findViewById(R.id.txtCurrentStreak);
        TextView txtMaxStreak = findViewById(R.id.txtMaxStreak);
        TextView txtTotalDays = findViewById(R.id.txtTotalDays);

        if (txtCurrentStreak != null) txtCurrentStreak.setText("7 ngày");
        if (txtMaxStreak != null) txtMaxStreak.setText("15 ngày");
        if (txtTotalDays != null) txtTotalDays.setText("42 ngày");
    }

    private void setupJournalLogs() {
        LinearLayout layoutJournalLogs = findViewById(R.id.layoutJournalLogs);
        if (layoutJournalLogs == null) return;
        layoutJournalLogs.removeAllViews();

        if (currentMonth == 6) {
            addJournalLog(layoutJournalLogs,
                    "Ngày mới tràn đầy năng lượng!",
                    "Hôm nay chạy bộ buổi sáng lúc 6h, hít thở không khí trong lành giúp cả ngày làm việc tập trung hơn rất nhiều. Sẽ duy trì thói quen này.",
                    "😊 Vui vẻ · 28/06/2026 08:30",
                    "#00BFA5");

            addJournalLog(layoutJournalLogs,
                    "Hơi quá tải buổi chiều",
                    "Nhiều deadline dồn dập khiến đầu óc căng thẳng và có chút bực dọc. Đã tự nhắc bản thân dừng lại thở sâu 3 nhịp để cân bằng cảm xúc.",
                    "😮‍💨 Căng thẳng · 27/06/2026 15:45",
                    "#FFB300");

            addJournalLog(layoutJournalLogs,
                    "Đọc sách buổi tối ấm áp",
                    "Buổi tối không bấm điện thoại nữa, dành ra 30 phút đọc sách và uống trà hoa cúc ấm trước khi đi ngủ. Thấy tâm trí rất nhẹ nhõm.",
                    "😌 Bình yên · 25/06/2026 22:15",
                    "#4A90E2");
        } else if (currentMonth == 5) {
            addJournalLog(layoutJournalLogs,
                    "Uống trà sữa chiều cùng đồng nghiệp",
                    "Buổi chiều làm việc hơi uể oải nhưng mọi người rủ nhau gọi trà sữa. Nói chuyện vui vẻ giúp giải tỏa căng thẳng hẳn.",
                    "😊 Vui vẻ · 26/05/2026 16:00",
                    "#00BFA5");

            addJournalLog(layoutJournalLogs,
                    "Buổi tối tĩnh lặng",
                    "Nghe một bản nhạc không lời nhẹ nhàng và thiền 10 phút. Cảm giác bình yên bao trùm sau ngày làm việc bận rộn.",
                    "😌 Bình yên · 22/05/2026 21:30",
                    "#4A90E2");
        } else if (currentMonth == 4) {
            addJournalLog(layoutJournalLogs,
                    "Hoàn thành dự án lớn",
                    "Dành cả ngày thuyết trình và bàn giao sản phẩm. Nhận được phản hồi rất tốt từ sếp và khách hàng, hạnh phúc khôn tả!",
                    "😊 Vui vẻ · 28/04/2026 17:30",
                    "#00BFA5");

            addJournalLog(layoutJournalLogs,
                    "Mưa lạnh đầu mùa",
                    "Trời chuyển mưa giông lạnh ẩm ướt, giao thông ùn tắc khiến việc đi lại rất mệt mỏi. Chỉ muốn về nhà đắp chăn đi ngủ.",
                    "😢 Buồn bã · 18/04/2026 18:20",
                    "#9C27B0");
        }
    }

    private void addJournalLog(LinearLayout container, String title, String content, String moodInfo, String moodColorHex) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#F8F9FA"));
        gd.setCornerRadius(16 * density);
        item.setBackground(gd);

        item.setPadding((int) (16 * density), (int) (16 * density), (int) (16 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = (int) (12 * density);
        item.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#313866"));
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        item.addView(tvTitle);

        TextView tvContent = new TextView(this);
        tvContent.setText(content);
        tvContent.setTextColor(Color.parseColor("#7D8BB7"));
        tvContent.setTextSize(12);
        tvContent.setLineSpacing(2 * density, 1.0f);
        tvContent.setPadding(0, (int) (6 * density), 0, (int) (8 * density));
        item.addView(tvContent);

        TextView tvMood = new TextView(this);
        tvMood.setText(moodInfo);
        tvMood.setTextColor(Color.parseColor(moodColorHex));
        tvMood.setTextSize(11);
        item.addView(tvMood);

        container.addView(item);
    }

    private void setupRecommendations() {
        LinearLayout layoutRecommendations = findViewById(R.id.layoutRecommendations);
        if (layoutRecommendations == null) return;
        layoutRecommendations.removeAllViews();

        addRecommendation(layoutRecommendations,
                "Thiền hơi thở lúc 15:00",
                "Lịch sử ghi nhận bạn hay căng thẳng vào cuối chiều. Hãy cài đặt chuông nhắc thở 3 phút tại thời điểm này để cân bằng hệ thần kinh.",
                "🧘 Trị liệu hơi thở",
                "#9C27B0",
                "#F3E5F5");
    }

    private void addRecommendation(LinearLayout container, String title, String description, String tag, String textColor, String tagBgColor) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#F9FBFB"));
        gd.setCornerRadius(16 * density);
        gd.setStroke((int)(1 * density), Color.parseColor("#E0F2F1"));
        item.setBackground(gd);

        item.setPadding((int) (16 * density), (int) (16 * density), (int) (16 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = (int) (12 * density);
        item.setLayoutParams(params);

        TextView tvTag = new TextView(this);
        tvTag.setText(tag);
        tvTag.setTextColor(Color.parseColor(textColor));
        tvTag.setTextSize(10);
        tvTag.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        tvTag.setPadding((int)(8*density), (int)(4*density), (int)(8*density), (int)(4*density));

        GradientDrawable tagGd = new GradientDrawable();
        tagGd.setColor(Color.parseColor(tagBgColor));
        tagGd.setCornerRadius(12 * density);
        tvTag.setBackground(tagGd);

        LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tagParams.bottomMargin = (int)(6 * density);
        tvTag.setLayoutParams(tagParams);
        item.addView(tvTag);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#313866"));
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        item.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(description);
        tvDesc.setTextColor(Color.parseColor("#7D8BB7"));
        tvDesc.setTextSize(12);
        tvDesc.setLineSpacing(2 * density, 1.0f);
        tvDesc.setPadding(0, (int) (4 * density), 0, 0);
        item.addView(tvDesc);

        container.addView(item);
    }
}
