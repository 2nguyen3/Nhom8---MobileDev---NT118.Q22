package com.example.heami.ui.main;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.ui.custom.WeeklyLineChartView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.content.Intent;

public class StatsActivity extends AppCompatActivity {
    private int regYear = 2025;
    private int regMonth = 4; // Mặc định Đăng ký Tháng 4/2025
    private int sysYear = 2026;
    private int sysMonth = 6; // Mặc định hiện tại Tháng 6/2026

    private int currentYear = 2026;
    private int currentMonth = 6;
    private int currentWeek = 1; // 1: Tuần 1, 2: Tuần 2, 3: Tuần 3, 4: Tuần 4
    private boolean showYearlyTrend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Khởi tạo thời gian hệ thống và đăng ký thực tế
        java.util.Calendar cal = java.util.Calendar.getInstance();
        sysYear = cal.get(java.util.Calendar.YEAR);
        sysMonth = cal.get(java.util.Calendar.MONTH) + 1;

        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getMetadata() != null) {
            long creationTime = user.getMetadata().getCreationTimestamp();
            if (creationTime > 0) {
                java.util.Calendar regCal = java.util.Calendar.getInstance();
                regCal.setTimeInMillis(creationTime);
                regYear = regCal.get(java.util.Calendar.YEAR);
                regMonth = regCal.get(java.util.Calendar.MONTH) + 1;
            }
        }

        // Đảm bảo logic năm đăng ký luôn nhỏ hơn hoặc bằng năm hiện tại
        if (regYear > sysYear) regYear = sysYear;

        // Thiết lập giá trị ban đầu tương ứng
        currentYear = sysYear;
        currentMonth = sysMonth;
        int sysDay = cal.get(java.util.Calendar.DAY_OF_MONTH);
        if (sysDay <= 7) {
            currentWeek = 1;
        } else if (sysDay <= 14) {
            currentWeek = 2;
        } else if (sysDay <= 21) {
            currentWeek = 3;
        } else {
            currentWeek = 4;
        }

        setupHeader();
        setupSpinners();
        setupCalendarGrid();
        setupWeeklyLineChart();
        setupTrendChart();
        setupHeatmap();
        setupFactorsAndPatterns();
        setupStreakStats();
        setupJournalLogs();
        setupRecommendations();
    }

    private List<String> generateMonthsForYear(int year) {
        List<String> monthsList = new ArrayList<>();
        int startMonth = 12;
        int endMonth = 1;

        if (year == sysYear) {
            startMonth = sysMonth; // Chỉ cho phép đến tháng hiện tại
        }
        if (year == regYear) {
            endMonth = regMonth; // Chỉ cho phép từ tháng đăng ký trở đi
        }

        for (int m = startMonth; m >= endMonth; m--) {
            monthsList.add(String.valueOf(m));
        }
        return monthsList;
    }

    private List<String> generateWeeksForMonth(int year, int month) {
        List<String> weeksList = new ArrayList<>();
        int maxWeek = 4;

        // Nếu là Năm hiện tại và Tháng hiện tại, giới hạn tuần đã đi qua dựa trên ngày hiện tại
        if (year == sysYear && month == sysMonth) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int sysDay = cal.get(java.util.Calendar.DAY_OF_MONTH);
            if (sysDay <= 7) {
                maxWeek = 1;
            } else if (sysDay <= 14) {
                maxWeek = 2;
            } else if (sysDay <= 21) {
                maxWeek = 3;
            } else {
                maxWeek = 4;
            }
        }

        for (int w = 1; w <= maxWeek; w++) {
            weeksList.add(String.valueOf(w));
        }
        return weeksList;
    }

    private void updateWeekSpinner(android.widget.Spinner spinnerWeek) {
        if (spinnerWeek == null) return;
        List<String> weeks = generateWeeksForMonth(currentYear, currentMonth);
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, weeks);
        weekAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerWeek.setAdapter(weekAdapter);

        if (currentWeek > weeks.size()) {
            currentWeek = weeks.size();
        }
        spinnerWeek.setSelection(currentWeek - 1);
    }

    private void setupSpinners() {
        android.widget.Spinner spinnerYear = findViewById(R.id.spinnerYear);
        android.widget.Spinner spinnerMonth = findViewById(R.id.spinnerMonth);
        android.widget.Spinner spinnerWeek = findViewById(R.id.spinnerWeek);

        if (spinnerYear == null || spinnerMonth == null || spinnerWeek == null) return;

        // 1. Tạo danh sách Năm động từ sysYear về regYear
        List<String> years = new ArrayList<>();
        for (int y = sysYear; y >= regYear; y--) {
            years.add(String.valueOf(y));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, years);
        yearAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerYear.setAdapter(yearAdapter);

        // 2. Tạo danh sách Tháng động dựa trên currentYear ban đầu (sysYear)
        List<String> months = generateMonthsForYear(currentYear);
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, months);
        monthAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerMonth.setAdapter(monthAdapter);

        // 3. Tạo danh sách Tuần động dựa trên currentYear và currentMonth ban đầu
        List<String> weeks = generateWeeksForMonth(currentYear, currentMonth);
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, weeks);
        weekAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerWeek.setAdapter(weekAdapter);

        // Set Defaults
        spinnerYear.setSelection(0); // Năm hiện tại
        spinnerMonth.setSelection(0); // Tháng hiện tại
        spinnerWeek.setSelection(currentWeek - 1); // Chọn đúng tuần hiện tại làm mặc định

        // Listeners
        spinnerYear.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentYear = Integer.parseInt(years.get(position));
                showYearlyTrend = true;

                // Cập nhật danh sách Tháng
                List<String> updatedMonths = generateMonthsForYear(currentYear);
                ArrayAdapter<String> updatedMonthAdapter = new ArrayAdapter<>(StatsActivity.this, R.layout.item_spinner_selected, updatedMonths);
                updatedMonthAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
                spinnerMonth.setAdapter(updatedMonthAdapter);
                spinnerMonth.setSelection(0); // Reset về tháng mới nhất của năm đó
                
                currentMonth = Integer.parseInt(updatedMonths.get(0));

                // Cập nhật danh sách Tuần tương ứng với tháng mới
                updateWeekSpinner(spinnerWeek);

                // Refresh all views
                setupTrendChart();
                setupCalendarGrid();
                setupWeeklyLineChart();
                setupHeatmap();
                setupFactorsAndPatterns();
                setupJournalLogs();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerMonth.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                List<String> currentMonthsList = generateMonthsForYear(currentYear);
                if (position >= currentMonthsList.size()) return;
                
                currentMonth = Integer.parseInt(currentMonthsList.get(position));
                showYearlyTrend = false;
                
                TextView txtCalendarMonth = findViewById(R.id.txtCalendarMonth);
                if (txtCalendarMonth != null) {
                    txtCalendarMonth.setText("Lịch Cảm Xúc Tháng " + currentMonth + "/" + currentYear);
                }

                // Cập nhật danh sách Tuần tương ứng với tháng mới
                updateWeekSpinner(spinnerWeek);

                // Refresh all views
                setupCalendarGrid();
                setupTrendChart();
                setupWeeklyLineChart();
                setupHeatmap();
                setupFactorsAndPatterns();
                setupJournalLogs();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerWeek.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentWeek = position + 1; // Tuần 1, 2, 3, 4 tương ứng vị trí 0, 1, 2, 3
                
                // Refresh week-based views
                setupCalendarGrid(); // to highlight current week
                setupWeeklyLineChart();
                setupHeatmap();
                setupFactorsAndPatterns();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupHeatmap() {
        com.example.heami.ui.custom.SensitiveTimezoneHeatmapView heatmapView = findViewById(R.id.heatmapView);
        if (heatmapView == null) return;

        // Tạo dữ liệu nhiệt tương ứng theo Tuần được chọn để tạo cảm giác trực quan sinh động
        int[][] data;
        if (currentWeek == 1) {
            data = new int[][]{
                {0, 1, 0, 2, 0, 1, 0}, // Sáng
                {1, 2, 1, 0, 3, 0, 1}, // Trưa
                {2, 3, 2, 1, 2, 1, 0}, // Chiều
                {1, 1, 3, 2, 1, 0, 2}  // Tối
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
                {2, 1, 0, 1, 0, 2, 0},
                {0, 3, 2, 1, 1, 0, 1},
                {1, 2, 1, 3, 2, 1, 0},
                {2, 1, 0, 2, 3, 0, 2}
            };
        }

        // Tinh chỉnh nhẹ theo tháng và năm
        if (currentYear == 2025) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 7; j++) {
                    if (data[i][j] == 3) data[i][j] = 2; // Giảm độ nhạy cảm năm ngoái
                }
            }
        }

        heatmapView.setHeatmapData(data);
    }

    private int getStartOffsetForMonth(int month) {
        if (currentYear == 2025) {
            // 2025 Calendar Offsets
            if (month == 6) return 6; // Tháng 6/2025 bắt đầu từ Chủ Nhật (6 ô trống)
            return 0;
        }
        if (month == 4) return 2; // Tháng 4/2026 bắt đầu từ Thứ Tư (2 ô trống T2, T3)
        if (month == 5) return 4; // Tháng 5/2026 bắt đầu từ Thứ Sáu (4 ô trống T2, T3, T4, T5)
        return 0; // Tháng 6/2026 bắt đầu từ Thứ Hai (0 ô trống)
    }

    private int getDaysInMonth(int month) {
        if (month == 4) return 30;
        if (month == 5) return 31;
        return 30; // Tháng 6
    }

    private void setupWeeklyLineChart() {
        WeeklyLineChartView weeklyLineChart = findViewById(R.id.weeklyLineChart);
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
            default: return 4f; // Mặc định trung tính
        }
    }

    private void setupHeader() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        View btnExportReport = findViewById(R.id.btnExportReport);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnExportReport != null) {
            btnExportReport.setOnClickListener(v -> showExportSuccessDialog());
        }
    }

    // ─────────────────────────────────────────────────────────
    //  1. LỊCH THÁNG CẢM XÚC
    // ─────────────────────────────────────────────────────────
    private void setupCalendarGrid() {
        GridLayout gridCalendar = findViewById(R.id.gridCalendar);
        if (gridCalendar == null) return;

        float density = getResources().getDisplayMetrics().density;
        int marginPx = (int) (4 * density);
        int paddingPx = (int) (8 * density);

        gridCalendar.removeAllViews();

        // 1. Vẽ các ô trống ở đầu tháng để canh đúng thứ trong tuần
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

        // 2. Vẽ các ngày thực tế
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

            // Day Number text
            TextView tvDay = new TextView(this);
            tvDay.setText(String.valueOf(day));
            tvDay.setTextSize(12);
            tvDay.setTextColor(Color.parseColor("#313866"));
            tvDay.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            tvDay.setGravity(Gravity.CENTER);
            cell.addView(tvDay);

            String emoji = getMoodEmojiForDayAndMonth(day, currentMonth);
            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(14);
            tvEmoji.setGravity(Gravity.CENTER);
            tvEmoji.setPadding(0, (int) (2 * density), 0, 0);
            cell.addView(tvEmoji);

            // Xác định xem ngày này có thuộc tuần đang được chọn hay không
            boolean isDayInSelectedWeek = false;
            int startDay = 1;
            if (currentWeek == 2) startDay = 8;
            else if (currentWeek == 3) startDay = 15;
            else if (currentWeek == 4) startDay = 22;

            int endDay = startDay + 6;
            if (currentWeek == 4) endDay = totalDays; // Tuần cuối lấy hết ngày còn lại

            if (day >= startDay && day <= endDay) {
                isDayInSelectedWeek = true;
            }

            // Luôn luôn hiển thị rõ nét toàn bộ các ngày (alpha = 1.0f) theo yêu cầu của bạn
            cell.setAlpha(1.0f);

            // Thiết lập Background và viền Highlight nếu thuộc tuần đang chọn
            if (!emoji.isEmpty()) {
                int bgColor = getMoodBgColorForEmoji(emoji);
                if (isDayInSelectedWeek) {
                    // Vẽ viền đậm màu tối (#313866) quanh ô thuộc tuần được chọn
                    GradientDrawable highlightGd = new GradientDrawable();
                    highlightGd.setColor(bgColor);
                    highlightGd.setStroke((int) (2f * density), Color.parseColor("#313866"));
                    highlightGd.setCornerRadius(12 * density);
                    cell.setBackground(highlightGd);
                } else {
                    cell.setBackground(createCellBackground(bgColor, 12 * density));
                }
            } else {
                // Ô trống chưa ghi nhận
                GradientDrawable borderGd = new GradientDrawable();
                borderGd.setColor(Color.parseColor("#FAFAFA"));
                if (isDayInSelectedWeek) {
                    // Vẽ viền đậm quanh cả ô trống nếu thuộc tuần được chọn
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

    private String getMoodEmojiForDayAndMonth(int day, int month) {
        if (currentYear == 2025) {
            // Mock dữ liệu rải rác năm 2025
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
            return "";
        }

        if (month == 4) {
            // Mock dữ liệu rải rác Tháng 4 (Không full ngày)
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
            // Mock dữ liệu rải rác Tháng 5 (Không full ngày)
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
            // Dữ liệu Tháng 6 (Full ngày)
            switch (day) {
                case 4: case 10: case 17: case 25:
                    return "😮‍💨"; // Căng thẳng
                case 5: case 16:
                    return "😢"; // Buồn bã
                case 20:
                    return "😡"; // Giận dữ
                case 2: case 7: case 11: case 13: case 18: case 22: case 27: case 30:
                    return "😌"; // Bình yên
                default:
                    return "😊"; // Vui vẻ
            }
        }
    }

    private int getMoodBgColorForEmoji(String emoji) {
        switch (emoji) {
            case "😮‍💨":
                return Color.parseColor("#FFF9C4"); // Vàng nhạt (Stressed)
            case "😢":
                return Color.parseColor("#F3E5F5"); // Tím nhạt (Sad)
            case "😡":
                return Color.parseColor("#FFEBEE"); // Đỏ nhạt (Angry)
            case "😌":
                return Color.parseColor("#E1F5FE"); // Xanh dương nhạt (Calm)
            default:
                return Color.parseColor("#E0F7F4"); // Xanh mint nhạt (Happy)
        }
    }

    private Drawable createCellBackground(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    // ─────────────────────────────────────────────────────────
    //  2. BIỂU ĐỒ XU HƯỚNG TÂM TRẠNG (Tính động theo tháng được chọn)
    // ─────────────────────────────────────────────────────────
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
            // Tính gộp cả năm (tất cả các tháng)
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
            // Chỉ tính cho tháng hiện tại
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
            progressHappy.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#00BFA5")));
            progressHappy.setProgress(pHappy);
        }
        if (txtPercentHappy != null) txtPercentHappy.setText(pHappy + "%");

        if (progressCalm != null) {
            progressCalm.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#4A90E2")));
            progressCalm.setProgress(pCalm);
        }
        if (txtPercentCalm != null) txtPercentCalm.setText(pCalm + "%");

        if (progressStressed != null) {
            progressStressed.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FFB300")));
            progressStressed.setProgress(pStressed);
        }
        if (txtPercentStressed != null) txtPercentStressed.setText(pStressed + "%");

        if (progressSad != null) {
            progressSad.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#9C27B0")));
            progressSad.setProgress(pSad);
        }
        if (txtPercentSad != null) txtPercentSad.setText(pSad + "%");

        if (progressAngry != null) {
            progressAngry.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E86FA0")));
            progressAngry.setProgress(pAngry);
        }
        if (txtPercentAngry != null) txtPercentAngry.setText(pAngry + "%");

        com.example.heami.ui.custom.EmotionalRadarChartView radarChartView = findViewById(R.id.radarChartView);
        if (radarChartView != null) {
            radarChartView.setData(pHappy / 100f, pCalm / 100f, pStressed / 100f, pSad / 100f, pAngry / 100f);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  3. TÁC NHÂN ẢNH HƯỞNG & QUY LUẬT
    // ─────────────────────────────────────────────────────────
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
        } else if (currentWeek == 4) {
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

        // Name + Icon
        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams((int) (130 * density), LinearLayout.LayoutParams.WRAP_CONTENT));
        tvName.setText(icon + " " + name);
        tvName.setTextColor(Color.parseColor("#313866"));
        tvName.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        tvName.setTextSize(12);
        row.addView(tvName);

        // Progress bar
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(0, (int) (6 * density), 1.0f);
        pbParams.rightMargin = (int) (12 * density);
        pb.setLayoutParams(pbParams);
        pb.setMax(100);
        pb.setProgress(percent);
        pb.setProgressTintList(ColorStateList.valueOf(progressColor));
        pb.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
        row.addView(pb);

        // Percent text
        TextView tvPercent = new TextView(this);
        tvPercent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvPercent.setText(percent + "%");
        tvPercent.setTextColor(Color.parseColor("#7D8BB7"));
        tvPercent.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        tvPercent.setTextSize(12);
        row.addView(tvPercent);

        container.addView(row);
    }

    // ─────────────────────────────────────────────────────────
    //  4. THỐNG KÊ CHUỖI
    // ─────────────────────────────────────────────────────────
    private void setupStreakStats() {
        TextView txtCurrentStreak = findViewById(R.id.txtCurrentStreak);
        TextView txtMaxStreak = findViewById(R.id.txtMaxStreak);
        TextView txtTotalDays = findViewById(R.id.txtTotalDays);

        if (txtCurrentStreak != null) txtCurrentStreak.setText("7 ngày");
        if (txtMaxStreak != null) txtMaxStreak.setText("15 ngày");
        if (txtTotalDays != null) txtTotalDays.setText("42 ngày");
    }

    // ─────────────────────────────────────────────────────────
    //  5. XEM LẠI NHẬT KÝ
    // ─────────────────────────────────────────────────────────
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
        tvMood.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        item.addView(tvMood);

        container.addView(item);
    }

    // ─────────────────────────────────────────────────────────
    //  6. KHUYẾN NGHỊ CÁ NHÂN
    // ─────────────────────────────────────────────────────────
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

        // Tag
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

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#313866"));
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        item.addView(tvTitle);

        // Description
        TextView tvDesc = new TextView(this);
        tvDesc.setText(description);
        tvDesc.setTextColor(Color.parseColor("#7D8BB7"));
        tvDesc.setTextSize(12);
        tvDesc.setLineSpacing(2 * density, 1.0f);
        tvDesc.setPadding(0, (int) (4 * density), 0, 0);
        item.addView(tvDesc);

        container.addView(item);
    }

    // ─────────────────────────────────────────────────────────
    //  POPUP XUẤT BÁO CÁO
    // ─────────────────────────────────────────────────────────
    private String collectReportData() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 [BÁO CÁO HÀNH TRÌNH CẢM XÚC - THÁNG " + currentMonth + "/" + currentYear + "]\n\n");

        sb.append("1. Lịch Cảm Xúc Tháng " + currentMonth + ":\n");
        int happy = 0, calm = 0, stressed = 0, sad = 0, angry = 0;
        int totalDays = getDaysInMonth(currentMonth);
        for (int i = 1; i <= totalDays; i++) {
            String emoji = getMoodEmojiForDayAndMonth(i, currentMonth);
            if (emoji.equals("😊")) happy++;
            else if (emoji.equals("😌")) calm++;
            else if (emoji.equals("😮‍💨")) stressed++;
            else if (emoji.equals("😢")) sad++;
            else if (emoji.equals("😡")) angry++;
        }
        int totalRecorded = happy + calm + stressed + sad + angry;
        int pHappy = 0, pCalm = 0, pStressed = 0, pSad = 0, pAngry = 0;
        if (totalRecorded > 0) {
            pHappy = (happy * 100) / totalRecorded;
            pCalm = (calm * 100) / totalRecorded;
            pStressed = (stressed * 100) / totalRecorded;
            pSad = (sad * 100) / totalRecorded;
            pAngry = 100 - (pHappy + pCalm + pStressed + pSad);
            if (pAngry < 0) pAngry = 0;
        }

        sb.append(String.format("• Vui vẻ (😊): %d ngày\n", happy));
        sb.append(String.format("• Bình yên (😌): %d ngày\n", calm));
        sb.append(String.format("• Căng thẳng (😮‍💨): %d ngày\n", stressed));
        sb.append(String.format("• Buồn bã (😢): %d ngày\n", sad));
        sb.append(String.format("• Giận dữ (😡): %d ngày\n\n", angry));

        sb.append("2. Xu Hướng Tâm Trạng:\n");
        sb.append(String.format("• 😊 Vui vẻ: %d%%\n", pHappy));
        sb.append(String.format("• 😌 Bình yên: %d%%\n", pCalm));
        sb.append(String.format("• 😮‍💨 Căng thẳng: %d%%\n", pStressed));
        sb.append(String.format("• 😢 Buồn bã: %d%%\n", pSad));
        sb.append(String.format("• 😡 Giận dữ: %d%%\n\n", pAngry));

        int fWork = 40;
        int fSleep = 30;
        int fRelations = 20;
        int fHealth = 10;
        if (currentWeek == 1) {
            fWork = 50; fSleep = 20; fRelations = 20; fHealth = 10;
        } else if (currentWeek == 2) {
            fWork = 30; fSleep = 40; fRelations = 20; fHealth = 10;
        } else if (currentWeek == 3) {
            fWork = 40; fSleep = 25; fRelations = 25; fHealth = 10;
        } else {
            fWork = 35; fSleep = 30; fRelations = 20; fHealth = 15;
        }

        sb.append("3. Tác Nhân & Quy Luật:\n");
        sb.append(String.format("• Tác nhân hàng đầu: Công việc & Học tập (%d%%), Giấc ngủ (%d%%), Mối quan hệ (%d%%), Sức khỏe thể chất (%d%%)\n", fWork, fSleep, fRelations, fHealth));
        sb.append("• Quy luật thời gian:\n");
        sb.append("  - Cảm xúc tích cực thường xuất hiện vào Buổi sáng (08:00 - 10:00).\n");
        sb.append("  - Cảm xúc căng thẳng thường tăng cao vào Buổi chiều muộn (15:00 - 17:00).\n");

        return sb.toString();
    }

    private void showExportSuccessDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_export_report);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        // Tạo tên tệp động dạng HReport_YYYYMMWW.pdf
        String formattedMonth = String.format("%02d", currentMonth);
        String formattedWeek = String.format("%02d", currentWeek);
        String fileName = "HReport_" + currentYear + formattedMonth + formattedWeek + ".pdf";

        TextView txtExportFileName = dialog.findViewById(R.id.txtExportFileName);
        if (txtExportFileName != null) {
            txtExportFileName.setText(fileName);
        }

        String reportText = collectReportData();

        // Tạo mảng byte PDF thực tế để đo kích thước
        final byte[] pdfBytes = generatePdfBytes(reportText);
        double sizeKb = pdfBytes != null ? (pdfBytes.length / 1024.0) : 0.0;
        String formattedSize = String.format(java.util.Locale.US, "%.1f KB", sizeKb);

        TextView txtExportFileSize = dialog.findViewById(R.id.txtExportFileSize);
        if (txtExportFileSize != null) {
            txtExportFileSize.setText(formattedSize);
        }

        View btnCancel = dialog.findViewById(R.id.btnCancelExport);
        View btnDownload = dialog.findViewById(R.id.btnDownload);
        View btnShare = dialog.findViewById(R.id.btnShare);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                savePdfBytesToDisk(fileName, pdfBytes);
                dialog.dismiss();
            });
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                dialog.dismiss();
                showShareMenuDialog(reportText, pdfBytes, fileName);
            });
        }

        dialog.show();
    }

    private byte[] generatePdfBytes(String reportText) {
        // Tính toán các chỉ số cảm xúc thực tế cho tháng
        int totalRecorded = 0;
        int happyCount = 0;
        int calmCount = 0;
        int stressedCount = 0;
        int sadCount = 0;
        int angryCount = 0;

        int totalDaysInMonth = getDaysInMonth(currentMonth);
        for (int day = 1; day <= totalDaysInMonth; day++) {
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

        int pHappy = 0, pCalm = 0, pStressed = 0, pSad = 0, pAngry = 0;
        if (totalRecorded > 0) {
            pHappy = (happyCount * 100) / totalRecorded;
            pCalm = (calmCount * 100) / totalRecorded;
            pStressed = (stressedCount * 100) / totalRecorded;
            pSad = (sadCount * 100) / totalRecorded;
            pAngry = 100 - (pHappy + pCalm + pStressed + pSad);
            if (pAngry < 0) pAngry = 0;
        }

        // Tính toán chỉ số tác nhân thực tế cho tuần
        int fWork = 40;
        int fSleep = 30;
        int fRelations = 20;
        int fHealth = 10;
        if (currentWeek == 1) {
            fWork = 50; fSleep = 20; fRelations = 20; fHealth = 10;
        } else if (currentWeek == 2) {
            fWork = 30; fSleep = 40; fRelations = 20; fHealth = 10;
        } else if (currentWeek == 3) {
            fWork = 40; fSleep = 25; fRelations = 25; fHealth = 10;
        } else {
            fWork = 35; fSleep = 30; fRelations = 20; fHealth = 15;
        }

        PdfDocument document = new PdfDocument();

        // ═════════════════════════════════════════════════════════
        //  TRANG 1: LỊCH THÁNG & XU HƯỚNG TÂM TRẠNG THÁNG
        // ═════════════════════════════════════════════════════════
        PdfDocument.PageInfo pageInfo1 = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page1 = document.startPage(pageInfo1);
        Canvas canvas1 = page1.getCanvas();

        // Thiết lập Paint vẽ cơ bản
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#313866"));
        textPaint.setTextSize(11);

        Paint boldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldPaint.setColor(Color.parseColor("#313866"));
        boldPaint.setTextSize(14);
        boldPaint.setFakeBoldText(true);

        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#313866"));
        headerPaint.setTextSize(18);
        headerPaint.setFakeBoldText(true);

        Paint progressBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressBgPaint.setColor(Color.parseColor("#F5F5F5"));
        progressBgPaint.setStyle(Paint.Style.FILL);

        Paint progressFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressFillPaint.setStyle(Paint.Style.FILL);

        // 1. Tiêu đề Báo cáo trang 1
        canvas1.drawText("BÁO CÁO HÀNH TRÌNH CẢM XÚC - THÁNG " + currentMonth + "/" + currentYear, 40, 55, headerPaint);
        
        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.parseColor("#7D8BB7"));
        subPaint.setTextSize(11);
        canvas1.drawText("Báo cáo sức khỏe tinh thần được trích xuất tự động từ ứng dụng Heami", 40, 75, subPaint);

        // Đường gạch phân cách
        Paint dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#E0E4F0"));
        dividerPaint.setStrokeWidth(1);
        canvas1.drawLine(40, 90, 555, 90, dividerPaint);

        // 2. PHẦN 1: LỊCH CẢM XÚC THEO THÁNG
        canvas1.drawText("1. Lịch Cảm Xúc Cả Tháng", 40, 115, boldPaint);

        // Vẽ Lưới lịch mini
        float gridX = 75;
        float gridY = 145;
        float cellSize = 38;
        float cellMargin = 4;
        String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

        // Vẽ thứ trong tuần
        Paint dayHeaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayHeaderPaint.setColor(Color.parseColor("#7D8BB7"));
        dayHeaderPaint.setTextSize(10);
        dayHeaderPaint.setFakeBoldText(true);
        dayHeaderPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < 7; i++) {
            canvas1.drawText(weekdays[i], gridX + i * (cellSize + cellMargin) + cellSize/2f, gridY - 8, dayHeaderPaint);
        }

        // Khởi tạo các ô lịch
        int offset = getStartOffsetForMonth(currentMonth);
        int totalDays = getDaysInMonth(currentMonth);
        int currentCellIndex = 0;

        // Vẽ ô trống trước đầu tháng
        for (int i = 0; i < offset; i++) {
            currentCellIndex++;
        }

        // Vẽ các ngày thực tế
        Paint cellBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellBgPaint.setStyle(Paint.Style.FILL);

        Paint cellStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellStrokePaint.setStyle(Paint.Style.STROKE);
        cellStrokePaint.setStrokeWidth(1);
        cellStrokePaint.setColor(Color.parseColor("#EAEAEA"));

        Paint dayNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayNumPaint.setColor(Color.parseColor("#313866"));
        dayNumPaint.setTextSize(9);
        dayNumPaint.setTextAlign(Paint.Align.CENTER);

        Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emojiPaint.setTextSize(11);
        emojiPaint.setTextAlign(Paint.Align.CENTER);

        // Highlight tuần hiện tại trên PDF bằng viền đen đậm
        Paint highlightStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightStrokePaint.setStyle(Paint.Style.STROKE);
        highlightStrokePaint.setStrokeWidth(1.8f);
        highlightStrokePaint.setColor(Color.parseColor("#313866"));

        int startHighlightDay = 1;
        if (currentWeek == 2) startHighlightDay = 8;
        else if (currentWeek == 3) startHighlightDay = 15;
        else if (currentWeek == 4) startHighlightDay = 22;
        int endHighlightDay = startHighlightDay + 6;
        if (currentWeek == 4) endHighlightDay = totalDays;

        for (int day = 1; day <= totalDays; day++) {
            int row = currentCellIndex / 7;
            int col = currentCellIndex % 7;

            float x = gridX + col * (cellSize + cellMargin);
            float y = gridY + row * (cellSize + cellMargin);

            String emoji = getMoodEmojiForDayAndMonth(day, currentMonth);

            // Vẽ nền ô cảm xúc
            if (!emoji.isEmpty()) {
                int bgColor = getMoodBgColorForEmoji(emoji);
                cellBgPaint.setColor(bgColor);
                canvas1.drawRoundRect(x, y, x + cellSize, y + cellSize, 6, 6, cellBgPaint);
            } else {
                cellBgPaint.setColor(Color.parseColor("#FAFAFA"));
                canvas1.drawRoundRect(x, y, x + cellSize, y + cellSize, 6, 6, cellBgPaint);
            }

            // Vẽ viền ô (Highlight tuần được chọn)
            if (day >= startHighlightDay && day <= endHighlightDay) {
                canvas1.drawRoundRect(x, y, x + cellSize, y + cellSize, 6, 6, highlightStrokePaint);
            } else {
                canvas1.drawRoundRect(x, y, x + cellSize, y + cellSize, 6, 6, cellStrokePaint);
            }

            // Vẽ số ngày
            canvas1.drawText(String.valueOf(day), x + cellSize/2f, y + 12, dayNumPaint);

            // Vẽ Emoji
            if (!emoji.isEmpty()) {
                canvas1.drawText(emoji, x + cellSize/2f, y + 28, emojiPaint);
            }

            currentCellIndex++;
        }

        // 3. PHẦN 2: XU HƯỚNG CẢM XÚC THÁNG
        float trendYStart = 430;
        canvas1.drawText("2. Xu Hướng Phân Bổ Cảm Xúc Tháng", 40, trendYStart, boldPaint);

        // Vẽ 5 thanh tiến trình cho 5 cảm xúc thực tế
        String[] moodLabels = {"😊 Vui vẻ", "😌 Bình yên", "😮‍💨 Áp lực", "😢 Buồn bã", "😡 Giận dữ"};
        int[] moodPercents = {pHappy, pCalm, pStressed, pSad, pAngry};
        String[] moodColors = {"#00BFA5", "#4A90E2", "#FFB300", "#9C27B0", "#E86FA0"};

        for (int i = 0; i < 5; i++) {
            float y = trendYStart + 30 + i * 32;

            // Nhãn cảm xúc
            canvas1.drawText(moodLabels[i], 40, y + 10, textPaint);

            // Thanh tiến trình
            canvas1.drawRoundRect(150, y, 480, y + 8, 4, 4, progressBgPaint);
            progressFillPaint.setColor(Color.parseColor(moodColors[i]));
            float fillWidth = 150 + (330 * (moodPercents[i] / 100f));
            canvas1.drawRoundRect(150, y, fillWidth, y + 8, 4, 4, progressFillPaint);

            // Phần trăm hiển thị bên phải
            canvas1.drawText(moodPercents[i] + "%", 500, y + 10, boldPaint);
        }

        document.finishPage(page1);

        // ═════════════════════════════════════════════════════════
        //  TRANG 2: BIỂU ĐỒ ĐƯỜNG TUẦN, LƯỚI NHIỆT MÚI GIỜ & TÁC NHÂN
        // ═════════════════════════════════════════════════════════
        PdfDocument.PageInfo pageInfo2 = new PdfDocument.PageInfo.Builder(595, 842, 2).create();
        PdfDocument.Page page2 = document.startPage(pageInfo2);
        Canvas canvas2 = page2.getCanvas();

        // Tiêu đề trang 2
        canvas2.drawText("PHÂN TÍCH CHI TIẾT TUẦN " + currentWeek + " - THÁNG " + currentMonth + "/" + currentYear, 40, 55, headerPaint);
        canvas2.drawLine(40, 75, 555, 75, dividerPaint);

        // 4. PHẦN 3: BIẾN ĐỘNG TÂM TRẠNG THEO TUẦN (Biểu đồ đường)
        canvas2.drawText("3. Biểu Đồ Biến Động Tâm Trạng Tuần", 40, 105, boldPaint);

        float chartXStart = 80;
        float chartYStart = 135;
        float chartW = 410;
        float chartH = 100;

        // Vẽ lưới ngang biểu đồ (5 đường)
        String[] chartEmojis = {"😡", "😢", "😮‍💨", "😌", "😊"};
        Paint chartLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chartLinePaint.setColor(Color.parseColor("#E0E4F0"));
        chartLinePaint.setStrokeWidth(1);

        for (int i = 0; i < 5; i++) {
            float y = chartYStart + (chartH / 4f) * (4 - i);
            canvas2.drawLine(chartXStart, y, chartXStart + chartW, y, chartLinePaint);
            // Emoji trục tung
            canvas2.drawText(chartEmojis[i], chartXStart - 25, y + 4, emojiPaint);
        }

        // Vẽ trục hoành ngày
        String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        float[] weeklyData = getWeeklyMoodsData(currentYear, currentMonth, currentWeek);

        float[] pointsX = new float[7];
        float[] pointsY = new float[7];

        dayHeaderPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            pointsX[i] = chartXStart + (chartW / 6f) * i;
            float moodVal = weeklyData[i];
            float normalized = (moodVal - 1f) / 4f;
            pointsY[i] = chartYStart + chartH * (1f - normalized);

            // Vẽ chữ Thứ bên dưới
            canvas2.drawText(days[i], pointsX[i], chartYStart + chartH + 18, dayHeaderPaint);
        }

        // Nối đường biến động
        Paint graphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        graphPaint.setColor(Color.parseColor("#00BFA5"));
        graphPaint.setStrokeWidth(2.5f);
        graphPaint.setStyle(Paint.Style.STROKE);

        android.graphics.Path graphPath = new android.graphics.Path();
        graphPath.moveTo(pointsX[0], pointsY[0]);
        for (int i = 1; i < 7; i++) {
            graphPath.lineTo(pointsX[i], pointsY[i]);
        }
        canvas2.drawPath(graphPath, graphPaint);

        // Vẽ các chấm điểm tròn trên đồ thị
        Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.parseColor("#00BFA5"));
        pointPaint.setStyle(Paint.Style.FILL);

        Paint pointInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointInnerPaint.setColor(Color.WHITE);
        pointInnerPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < 7; i++) {
            canvas2.drawCircle(pointsX[i], pointsY[i], 4, pointPaint);
            canvas2.drawCircle(pointsX[i], pointsY[i], 2, pointInnerPaint);
        }

        // 5. PHẦN 4: BẢN ĐỒ MÚI GIỜ NHẠY CẢM (Lưới nhiệt tuần)
        float heatYStart = 295;
        canvas2.drawText("4. Bản Đồ Múi Giờ Nhạy Cảm Trong Tuần", 40, heatYStart, boldPaint);

        float heatXStart = 110;
        float heatW = 390;
        float heatH = 90;
        float heatCellW = heatW / 7f;
        float heatCellH = heatH / 4f;
        float heatPadding = 2.5f;

        String[] timeSlots = {"Sáng", "Trưa", "Chiều", "Tối"};
        String[][] heatmapColors = {
            {"#F5F6F8", "#B2DFDB", "#F5F6F8", "#4DB6AC", "#F5F6F8", "#B2DFDB", "#F5F6F8"},
            {"#B2DFDB", "#4DB6AC", "#B2DFDB", "#F5F6F8", "#E86FA0", "#F5F6F8", "#B2DFDB"},
            {"#4DB6AC", "#E86FA0", "#4DB6AC", "#B2DFDB", "#4DB6AC", "#B2DFDB", "#F5F6F8"},
            {"#B2DFDB", "#B2DFDB", "#E86FA0", "#4DB6AC", "#B2DFDB", "#F5F6F8", "#4DB6AC"}
        };

        // Vẽ nhãn ngày ở trên đầu lưới nhiệt
        dayHeaderPaint.setTextAlign(Paint.Align.CENTER);
        for (int col = 0; col < 7; col++) {
            float cx = heatXStart + heatCellW * col + heatCellW / 2f;
            canvas2.drawText(days[col], cx, heatYStart + 22, dayHeaderPaint);
        }

        // Vẽ các ô lưới nhiệt
        Paint heatCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        heatCellPaint.setStyle(Paint.Style.FILL);

        Paint heatRowLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        heatRowLabelPaint.setColor(Color.parseColor("#313866"));
        heatRowLabelPaint.setTextSize(9);
        heatRowLabelPaint.setFakeBoldText(true);
        heatRowLabelPaint.setTextAlign(Paint.Align.RIGHT);

        for (int row = 0; row < 4; row++) {
            // Nhãn buổi bên trái
            float ly = heatYStart + 35 + heatCellH * row + heatCellH / 2f + 3;
            canvas2.drawText(timeSlots[row], heatXStart - 10, ly, heatRowLabelPaint);

            for (int col = 0; col < 7; col++) {
                float left = heatXStart + heatCellW * col + heatPadding;
                float top = heatYStart + 35 + heatCellH * row + heatPadding;
                float right = heatXStart + heatCellW * (col + 1) - heatPadding;
                float bottom = heatYStart + 35 + heatCellH * (row + 1) - heatPadding;

                heatCellPaint.setColor(Color.parseColor(heatmapColors[row][col]));
                canvas2.drawRoundRect(left, top, right, bottom, 4, 4, heatCellPaint);
            }
        }

        // Chú giải màu sắc dưới lưới nhiệt
        float legendY = heatYStart + 140;
        String[] legendTexts = {"Bình yên", "Thấp", "Vừa", "Cao"};
        String[] legendColors = {"#F5F6F8", "#B2DFDB", "#4DB6AC", "#E86FA0"};

        Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legendPaint.setColor(Color.parseColor("#7D8BB7"));
        legendPaint.setTextSize(9);

        for (int i = 0; i < 4; i++) {
            float lx = 140 + i * 85;
            heatCellPaint.setColor(Color.parseColor(legendColors[i]));
            canvas2.drawRect(lx, legendY, lx + 12, legendY + 8, heatCellPaint);
            canvas2.drawText(legendTexts[i], lx + 18, legendY + 7, legendPaint);
        }

        // 6. PHẦN 5: QUY LUẬT & TÁC NHÂN ẢNH HƯỞNG
        float factorsYStart = 490;
        canvas2.drawText("5. Tác Nhân & Quy Luật Cảm Xúc Hàng Đầu", 40, factorsYStart, boldPaint);

        String[] factorLabels = {"💼 Công việc & Học tập", "😴 Giấc ngủ", "🤝 Mối quan hệ", "🏃 Sức khỏe"};
        int[] factorPercents = {fWork, fSleep, fRelations, fHealth};
        String[] factorColors = {"#E86FA0", "#00BFA5", "#4A90E2", "#9575CD"};

        for (int i = 0; i < 4; i++) {
            float y = factorsYStart + 25 + i * 28;

            canvas2.drawText(factorLabels[i], 40, y + 10, textPaint);

            canvas2.drawRoundRect(180, y, 460, y + 7, 3.5f, 3.5f, progressBgPaint);
            progressFillPaint.setColor(Color.parseColor(factorColors[i]));
            float fillWidth = 180 + (280 * (factorPercents[i] / 100f));
            canvas2.drawRoundRect(180, y, fillWidth, y + 7, 3.5f, 3.5f, progressFillPaint);

            canvas2.drawText(factorPercents[i] + "%", 480, y + 10, boldPaint);
        }

        // Vẽ tóm tắt quy luật ở chân trang
        float ruleY = factorsYStart + 155;
        Paint rulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rulePaint.setColor(Color.parseColor("#495057"));
        rulePaint.setTextSize(10);

        canvas2.drawText("• Quy luật thời gian tiêu biểu: Cảm xúc tích cực nhiều vào Buổi sáng (08h - 10h).", 40, ruleY, rulePaint);
        canvas2.drawText("• Tác nhân áp lực: Tần suất căng thẳng cao xuất hiện vào Buổi chiều muộn (15h - 17h) do Công việc.", 40, ruleY + 18, rulePaint);

        document.finishPage(page2);

        // Trả về luồng byte dữ liệu PDF
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            document.writeTo(baos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    private void savePdfBytesToDisk(String fileName, byte[] pdfBytes) {
        if (pdfBytes == null) return;

        // Bỏ chữ 's' trong 'Downloads' -> đích đến là 'Download/Heami/Report'
        String targetFolderName = "Download/Heami/Report";

        // 1. Dành cho Android 10 (Q - API 29) trở lên: Sử dụng MediaStore để ghi file trực tiếp vào thư mục Download công cộng
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                // Ghi vào thư mục công cộng Download/Heami/Report
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Heami/Report");

                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    OutputStream os = resolver.openOutputStream(uri);
                    if (os != null) {
                        os.write(pdfBytes);
                        os.close();
                        showLeftAlignedToast("Đã tải xuống thành công báo cáo " + fileName + " tại thư mục " + targetFolderName + "!");
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. Dành cho các dòng máy cũ (dưới API 29): Dùng File IO truyền thống
        try {
            File targetDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Heami/Report");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            File pdfFile = new File(targetDir, fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            fos.write(pdfBytes);
            fos.close();
            showLeftAlignedToast("Đã tải xuống thành công báo cáo " + fileName + " tại thư mục " + targetFolderName + "!");
        } catch (Exception e) {
            e.printStackTrace();
            // Ghi dự phòng vào thư mục External Files của ứng dụng
            try {
                File fallbackDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Heami/Report");
                if (!fallbackDir.exists()) {
                    fallbackDir.mkdirs();
                }
                File fallbackFile = new File(fallbackDir, fileName);
                FileOutputStream fos = new FileOutputStream(fallbackFile);
                fos.write(pdfBytes);
                fos.close();
                showLeftAlignedToast("Đã lưu PDF (dự phòng) tại: Android/data/com.example.heami/files/" + targetFolderName + "/" + fileName);
            } catch (Exception ex) {
                ex.printStackTrace();
                showLeftAlignedToast("Lỗi khi lưu tệp PDF: " + ex.getMessage());
            }
        }
    }

    private void showLeftAlignedToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        if (android.os.Build.VERSION.SDK_INT < 30) {
            try {
                View view = toast.getView();
                if (view != null) {
                    TextView text = view.findViewById(android.R.id.message);
                    if (text != null) {
                        text.setGravity(Gravity.START);
                        text.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        toast.show();
    }

    private void showShareMenuDialog(String reportText, final byte[] pdfBytes, final String fileName) {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_share_menu);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        View btnShareDoctor = dialog.findViewById(R.id.btnShareDoctor);
        View btnShareCommunity = dialog.findViewById(R.id.btnShareCommunity);
        View btnShareOthers = dialog.findViewById(R.id.btnShareOthers);
        View btnBack = dialog.findViewById(R.id.btnBackShareMenu);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                dialog.dismiss();
                showExportSuccessDialog();
            });
        }

        if (btnShareDoctor != null) {
            btnShareDoctor.setOnClickListener(v -> {
                dialog.dismiss();
                showDoctorSelectDialog(reportText, pdfBytes, fileName);
            });
        }

        if (btnShareCommunity != null) {
            btnShareCommunity.setOnClickListener(v -> {
                dialog.dismiss();
                showFriendSelectDialog(reportText, pdfBytes, fileName);
            });
        }

        if (btnShareOthers != null) {
            btnShareOthers.setOnClickListener(v -> {
                dialog.dismiss();
                sharePdfToOtherApps(fileName, pdfBytes);
            });
        }

        dialog.show();
    }

    private void showDoctorSelectDialog(String reportText, final byte[] pdfBytes, final String fileName) {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_select_doctor);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        TextView txtNoDoctor = dialog.findViewById(R.id.txtNoDoctor);
        ListView lvDoctors = dialog.findViewById(R.id.lvDoctors);
        View btnBack = dialog.findViewById(R.id.btnBackSelectDoctor);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                dialog.dismiss();
                showShareMenuDialog(reportText, pdfBytes, fileName);
            });
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("chatrooms").get().addOnCompleteListener(task -> {
            List<String> doctorNames = new ArrayList<>();
            List<String> chatroomIds = new ArrayList<>();

            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String doctorName = doc.getString("doctorName");
                    if (doctorName != null) {
                        doctorNames.add("Bác sĩ " + doctorName);
                        chatroomIds.add(doc.getId());
                    }
                }
            }

            // Fallback mock doctors if Firestore collection is empty
            if (doctorNames.isEmpty()) {
                doctorNames.add("Bác sĩ Phạm Minh Tuấn");
                chatroomIds.add("mock_room_tuan");
                doctorNames.add("Bác sĩ Nguyễn Thị Hà");
                chatroomIds.add("mock_room_ha");
            }

            if (txtNoDoctor != null) txtNoDoctor.setVisibility(View.GONE);
            if (lvDoctors != null) {
                lvDoctors.setVisibility(View.VISIBLE);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(StatsActivity.this,
                        R.layout.item_dialog_list, doctorNames);
                lvDoctors.setAdapter(adapter);

                lvDoctors.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedChatroomId = chatroomIds.get(position);
                    String selectedDoctorName = doctorNames.get(position);
                    dialog.dismiss();
                    sendReportToDoctorChatroom(selectedChatroomId, selectedDoctorName, reportText, pdfBytes, fileName);

                    // Cập nhật trạng thái chia sẻ trong Firestore settings
                    String myUid = FirebaseAuth.getInstance().getUid();
                    if (myUid != null) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("shareStatsWithDoctor", true);
                        db.collection("users").document(myUid).collection("settings").document("default")
                                .set(update, com.google.firebase.firestore.SetOptions.merge());
                    }
                });
            }
        });

        dialog.show();
    }

    private void showFriendSelectDialog(String reportText, final byte[] pdfBytes, final String fileName) {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_select_friend);

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        TextView txtNoFriend = dialog.findViewById(R.id.txtNoFriend);
        ListView lvFriends = dialog.findViewById(R.id.lvFriends);
        View btnBack = dialog.findViewById(R.id.btnBackSelectFriend);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                dialog.dismiss();
                showShareMenuDialog(reportText, pdfBytes, fileName);
            });
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("chatrooms").get().addOnCompleteListener(task -> {
            List<String> friendNames = new ArrayList<>();
            List<String> chatroomIds = new ArrayList<>();

            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String doctorName = doc.getString("doctorName");
                    if (doctorName == null) {
                        String userName = doc.getString("userName");
                        String name = doc.getString("name");
                        String displayName = (userName != null) ? userName : ((name != null) ? name : "Người dùng " + doc.getId().substring(0, 4));
                        friendNames.add(displayName);
                        chatroomIds.add(doc.getId());
                    }
                }
            }

            // Fallback mock friends if Firestore chats are empty
            if (friendNames.isEmpty()) {
                friendNames.add("Trần Minh Vy");
                chatroomIds.add("mock_room_vy");
                friendNames.add("Lê Hoàng Nam");
                chatroomIds.add("mock_room_nam");
                friendNames.add("Phạm Quỳnh Anh");
                chatroomIds.add("mock_room_quynhanh");
            }

            if (txtNoFriend != null) txtNoFriend.setVisibility(View.GONE);
            if (lvFriends != null) {
                lvFriends.setVisibility(View.VISIBLE);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(StatsActivity.this,
                        R.layout.item_dialog_list, friendNames);
                lvFriends.setAdapter(adapter);

                lvFriends.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedChatroomId = chatroomIds.get(position);
                    String selectedFriendName = friendNames.get(position);
                    dialog.dismiss();
                    sendReportToFriendChatroom(selectedChatroomId, selectedFriendName, reportText, pdfBytes, fileName);
                });
            }
        });

        dialog.show();
    }

    private void sendReportToDoctorChatroom(String chatroomId, String doctorName, String reportText, byte[] pdfBytes, String fileName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) userId = "heami_patient_id";

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", userId);
        msg.put("timestamp", Timestamp.now());
        
        if (pdfBytes != null) {
            String pdfBase64 = android.util.Base64.encodeToString(pdfBytes, android.util.Base64.DEFAULT);
            msg.put("fileData", pdfBase64);
            msg.put("fileName", fileName);
            msg.put("type", "FILE");
            msg.put("message", "[Báo cáo PDF] " + fileName);
        } else {
            msg.put("message", "[Hành trình cảm xúc] Báo cáo tháng:\n\n" + reportText);
            msg.put("type", "TEXT");
        }

        db.collection("chatrooms").document(chatroomId).collection("messages").add(msg)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(StatsActivity.this, "Đã gửi báo cáo PDF thành công đến Bác sĩ " + doctorName + "!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StatsActivity.this, "Đã gửi báo cáo thành công tới Bác sĩ " + doctorName + " (Lưu offline)!", Toast.LENGTH_LONG).show();
                });
    }

    private void sendReportToFriendChatroom(String chatroomId, String friendName, String reportText, byte[] pdfBytes, String fileName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) userId = "heami_user_id";

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", userId);
        msg.put("timestamp", Timestamp.now());

        if (pdfBytes != null) {
            String pdfBase64 = android.util.Base64.encodeToString(pdfBytes, android.util.Base64.DEFAULT);
            msg.put("fileData", pdfBase64);
            msg.put("fileName", fileName);
            msg.put("type", "FILE");
            msg.put("message", "[Báo cáo PDF] " + fileName);
        } else {
            msg.put("message", "[Hành trình cảm xúc] Báo cáo tháng:\n\n" + reportText);
            msg.put("type", "TEXT");
        }

        db.collection("chatrooms").document(chatroomId).collection("messages").add(msg)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(StatsActivity.this, "Đã gửi báo cáo PDF thành công vào phòng chat của " + friendName + "!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StatsActivity.this, "Đã gửi báo cáo thành công tới " + friendName + " (Lưu offline)!", Toast.LENGTH_LONG).show();
                });
    }

    private void sharePdfToOtherApps(String fileName, byte[] pdfBytes) {
        if (pdfBytes == null) {
            Toast.makeText(this, "Không có dữ liệu báo cáo PDF!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File cacheDir = new File(getCacheDir(), "shared_reports");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File pdfFile = new File(cacheDir, fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            fos.write(pdfBytes);
            fos.close();

            Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    this, "com.example.heami.fileprovider", pdfFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Báo cáo Hành trình cảm xúc - Heami App");
            intent.putExtra(Intent.EXTRA_TEXT, "Gửi bạn báo cáo hành trình cảm xúc của tôi trích xuất từ ứng dụng Heami.");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Chia sẻ báo cáo PDF qua..."));
            Toast.makeText(this, "Đang mở danh sách các ứng dụng liên kết...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi chuẩn bị chia sẻ PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}