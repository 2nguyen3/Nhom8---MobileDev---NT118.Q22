package com.example.heami.ui.therapy;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.DiaryModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiaryActivity extends AppCompatActivity {

    private RecyclerView rvMonths, rvDiaries;
    private MonthAdapter monthAdapter;
    private DiaryAdapter diaryAdapter;

    private List<String> monthList;
    private List<DiaryModel> diaryList;        // Danh sách hiển thị sau khi lọc trên màn hình
    private List<DiaryModel> fullFirebaseList; // 🌟 Tổng kho dữ liệu tải về từ Firebase để lọc offline liên hoàn siêu tốc

    private ImageButton btnBack, btnSearch, btnList;
    private EditText edtSearchHashtag;
    private FloatingActionButton fabAdd;
    private TextView tvTotalEntries, tvSubtitleDate, tvAverageScore;

    private int currentSelectedMonth;
    private String currentSearchKeyword = "";          // Lưu từ khóa tìm kiếm hashtag hiện tại
    private String currentSelectedMoodFilter = "Tất cả"; // Lưu bộ lọc cảm xúc hiện tại

    // Biến kết nối Firebase Firestore
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);

        Calendar calendar = Calendar.getInstance();
        currentSelectedMonth = calendar.get(Calendar.MONTH) + 1;

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Ánh xạ các View trên layout chính
        rvMonths = findViewById(R.id.rv_months);
        rvDiaries = findViewById(R.id.rv_diaries);
        fabAdd = findViewById(R.id.fab_add);
        btnBack = findViewById(R.id.btn_back);
        btnSearch = findViewById(R.id.btn_search);
        btnList = findViewById(R.id.btn_list);
        edtSearchHashtag = findViewById(R.id.edt_search_hashtag);
        tvTotalEntries = findViewById(R.id.tv_total_entries);
        tvSubtitleDate = findViewById(R.id.tv_subtitle_date);
        tvAverageScore = findViewById(R.id.tv_average_score);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvSubtitleDate != null) {
            tvSubtitleDate.setText("Tháng " + currentSelectedMonth + " · 2026");
        }

        // 🌟 NÚT 1: XỬ LÝ LOGIC ẨN/HIỆN VÀ TÌM KIẾM ĐỘNG THEO HASHTAG
        if (btnSearch != null && edtSearchHashtag != null) {
            btnSearch.setOnClickListener(v -> {
                if (edtSearchHashtag.getVisibility() == View.GONE) {
                    edtSearchHashtag.setVisibility(View.VISIBLE);
                    edtSearchHashtag.requestFocus();
                } else {
                    edtSearchHashtag.setVisibility(View.GONE);
                    edtSearchHashtag.setText(""); // Xóa chữ khi đóng thanh tìm kiếm
                    currentSearchKeyword = "";
                    executeMasterFilter();        // Chạy lại bộ lọc tổng để khôi phục danh sách
                }
            });

            // Gán TextWatcher lắng nghe từng ký tự gõ vào ô tìm kiếm để lọc real-time
            edtSearchHashtag.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchKeyword = s.toString().trim().toLowerCase();
                    executeMasterFilter(); // Thực thi lọc ngay khi gõ
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // 🌟 NÚT 2: XỬ LÝ LOGIC BẤM VÀO PLAYLIST ĐỂ HIỂN THỊ DIALOG LỌC CẢM XÚC
        if (btnList != null) {
            btnList.setOnClickListener(v -> showFilterMoodDialog());
        }

        // Khởi tạo thanh chọn tháng
        monthList = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            monthList.add("T" + i);
        }

        rvMonths.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        monthAdapter = new MonthAdapter(monthList, currentSelectedMonth, monthNumber -> {
            currentSelectedMonth = monthNumber;
            if (tvSubtitleDate != null) {
                tvSubtitleDate.setText("Tháng " + currentSelectedMonth + " · 2026");
            }
            executeMasterFilter(); // Chạy bộ lọc khi chuyển tháng
        });
        rvMonths.setAdapter(monthAdapter);

        // Khởi tạo các mảng dữ liệu nhật ký
        diaryList = new ArrayList<>();
        fullFirebaseList = new ArrayList<>();
        rvDiaries.setLayoutManager(new LinearLayoutManager(this));

        diaryAdapter = new DiaryAdapter(this, diaryList);
        rvDiaries.setAdapter(diaryAdapter);

        // Bắt đầu lắng nghe kho dữ liệu tổng từ Cloud về
        listenToDiaryChanges();

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddDiaryDialog());
        }
    }

    /**
     * HÀM LẮNG NGHE DỮ LIỆU TOÀN BỘ TỪ FIREBASE (Không gom cứng theo tháng ở bước này)
     */
    private void listenToDiaryChanges() {
        db.collection("nhat_ky")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(DiaryActivity.this, "Lỗi tải dữ liệu thời gian thực!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        fullFirebaseList.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            try {
                                DiaryModel diary = document.toObject(DiaryModel.class);
                                long createdAt = 0;
                                if (document.get("created_at") instanceof Long) {
                                    createdAt = document.getLong("created_at");
                                } else if (document.get("created_at") instanceof String) {
                                    createdAt = Long.parseLong(document.getString("created_at"));
                                }
                                diary.setCreated_at(createdAt);

                                if (diary.getCreated_at() > 0) {
                                    fullFirebaseList.add(diary);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // Chạy hàm lọc đa điều kiện tổng lực để hiển thị lên màn hình
                        executeMasterFilter();
                    }
                });
    }

    /**
     * 🌟 BỘ LỌC TỔNG LIÊN HOÀN (MASTER FILTER): KIỂM TRA THÁNG + HASHTAG + CẢM XÚC CÙNG LÚC
     */
    private void executeMasterFilter() {
        diaryList.clear();
        Calendar cal = Calendar.getInstance();

        for (DiaryModel diary : fullFirebaseList) {
            // ĐIỀU KIỆN 1: Lọc trùng tháng đang chọn
            cal.setTimeInMillis(diary.getCreated_at());
            int diaryMonth = cal.get(Calendar.MONTH) + 1;
            String monthStr = "Tháng " + currentSelectedMonth;

            boolean isSameMonth = (diaryMonth == currentSelectedMonth) ||
                    (diary.getDate() != null && diary.getDate().contains(monthStr));
            if (!isSameMonth) {
                continue; // Sai tháng bỏ qua
            }

            // ĐIỀU KIỆN 2: Lọc trùng Cảm xúc đang chọn từ Bottom Sheet
            if (!currentSelectedMoodFilter.equals("Tất cả")) {
                if (diary.getMood() == null || !diary.getMood().trim().contains(currentSelectedMoodFilter)) {
                    continue; // Sai cảm xúc bỏ qua
                }
            }

            // ĐIỀU KIỆN 3: Lọc trùng Hashtag đang gõ trong EditText
            if (!currentSearchKeyword.isEmpty()) {
                boolean hasMatchingTag = false;
                if (diary.getTags() != null) {
                    for (String tag : diary.getTags()) {
                        if (tag.toLowerCase().contains(currentSearchKeyword)) {
                            hasMatchingTag = true;
                            break;
                        }
                    }
                }
                if (!hasMatchingTag) {
                    continue; // Không trùng hashtag bỏ qua
                }
            }

            // Vượt qua cả 3 chốt chặn trên thì thêm bài viết vào danh sách hiển thị
            diaryList.add(diary);
        }

        if (tvTotalEntries != null) {
            tvTotalEntries.setText(diaryList.size() + " mục nhật ký");
        }

        // Cập nhật lại điểm trung bình dựa trên list dữ liệu sau lọc
        updateAverageScore();

        if (diaryAdapter != null) {
            diaryAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 🌟 HÀM HIỂN THỊ DIALOG LỌC CẢM XÚC - ĐỔI MÀU NỀN ACTIVE VÀ HIỂN THỊ DẤU TÍCH DỘNG
     */
    private void showFilterMoodDialog() {
        BottomSheetDialog filterDialog = new BottomSheetDialog(DiaryActivity.this);
        View view = getLayoutInflater().inflate(R.layout.dialog_filter_mood, null);
        filterDialog.setContentView(view);

        ImageButton btnClose = view.findViewById(R.id.btn_close_filter);
        if (btnClose != null) btnClose.setOnClickListener(v -> filterDialog.dismiss());

        // Ánh xạ các LinearLayout hàng bấm chọn
        LinearLayout btnAll = view.findViewById(R.id.btn_filter_all);
        LinearLayout btnHappy = view.findViewById(R.id.btn_filter_happy);
        LinearLayout btnPeace = view.findViewById(R.id.btn_filter_peace);
        LinearLayout btnSad = view.findViewById(R.id.btn_filter_sad);
        LinearLayout btnStress = view.findViewById(R.id.btn_filter_stress);
        LinearLayout btnTired = view.findViewById(R.id.btn_filter_tired);

        // Ánh xạ các TextView chữ bên trong để đổi màu tương phản
        TextView tvAll = (TextView) btnAll.getChildAt(0);
        TextView tvHappy = (TextView) btnHappy.getChildAt(0);
        TextView tvPeace = (TextView) btnPeace.getChildAt(0);
        TextView tvSad = (TextView) btnSad.getChildAt(0);
        TextView tvStress = (TextView) btnStress.getChildAt(0);
        TextView tvTired = (TextView) btnTired.getChildAt(0);

        // Ánh xạ các TextView dấu tích (✓)
        TextView tvCheckAll = view.findViewById(R.id.tv_check_all);
        TextView tvCheckHappy = view.findViewById(R.id.tv_check_happy);
        TextView tvCheckPeace = view.findViewById(R.id.tv_check_peace);
        TextView tvCheckSad = view.findViewById(R.id.tv_check_sad);
        TextView tvCheckStress = view.findViewById(R.id.tv_check_stress);
        TextView tvCheckTired = view.findViewById(R.id.tv_check_tired);

        // 1. Reset toàn bộ các hàng về trạng thái bình thường (Nền trắng viền xám nhạt, ẩn dấu tích, chữ xám đậm)
        btnAll.setBackgroundResource(R.drawable.bg_filter_item_normal);
        btnHappy.setBackgroundResource(R.drawable.bg_filter_item_normal);
        btnPeace.setBackgroundResource(R.drawable.bg_filter_item_normal);
        btnSad.setBackgroundResource(R.drawable.bg_filter_item_normal);
        btnStress.setBackgroundResource(R.drawable.bg_filter_item_normal);
        btnTired.setBackgroundResource(R.drawable.bg_filter_item_normal);

        int normalColor = android.graphics.Color.parseColor("#4A5568");
        tvAll.setTextColor(normalColor);
        tvHappy.setTextColor(normalColor);
        tvPeace.setTextColor(normalColor);
        tvSad.setTextColor(normalColor);
        tvStress.setTextColor(normalColor);
        tvTired.setTextColor(normalColor);

        tvCheckAll.setVisibility(View.GONE);
        tvCheckHappy.setVisibility(View.GONE);
        tvCheckPeace.setVisibility(View.GONE);
        tvCheckSad.setVisibility(View.GONE);
        tvCheckStress.setVisibility(View.GONE);
        tvCheckTired.setVisibility(View.GONE);

        // 2. Kích hoạt bừng sáng ô Active dựa vào bộ lọc hiện tại (Nền hồng nhạt viền hồng, hiện dấu ✓, chữ hồng đậm)
        int activeColor = android.graphics.Color.parseColor("#E86FA0");
        switch (currentSelectedMoodFilter) {
            case "Tất cả":
                btnAll.setBackgroundResource(R.drawable.bg_filter_item_active);
                tvAll.setTextColor(activeColor);
                tvCheckAll.setVisibility(View.VISIBLE);
                break;
            case "Vui vẻ":
                btnHappy.setBackgroundResource(R.drawable.bg_filter_item_active);
                tvHappy.setTextColor(activeColor);
                tvCheckHappy.setVisibility(View.VISIBLE);
                break;
            case "Bình yên":
                btnPeace.setBackgroundResource(R.drawable.bg_filter_item_active);
                tvPeace.setTextColor(activeColor);
                tvCheckPeace.setVisibility(View.VISIBLE);
                break;
            case "Buồn":
                btnSad.setBackgroundResource(R.drawable.bg_filter_item_active);
                tvSad.setTextColor(activeColor);
                tvCheckSad.setVisibility(View.VISIBLE);
                break;
            case "Căng thẳng":
                btnStress.setBackgroundResource(R.drawable.bg_filter_item_active);
                tvStress.setTextColor(activeColor);
                tvCheckStress.setVisibility(View.VISIBLE);
                break;
            case "Mệt mỏi":
                btnTired.setBackgroundResource(R.drawable.bg_filter_item_active);
                tvTired.setTextColor(activeColor);
                tvCheckTired.setVisibility(View.VISIBLE);
                break;
        }

        // 3. Cài đặt sự kiện nhấn chọn cảm xúc mới cho từng khối
        btnAll.setOnClickListener(v -> { currentSelectedMoodFilter = "Tất cả"; executeMasterFilter(); filterDialog.dismiss(); });
        btnHappy.setOnClickListener(v -> { currentSelectedMoodFilter = "Vui vẻ"; executeMasterFilter(); filterDialog.dismiss(); });
        btnPeace.setOnClickListener(v -> { currentSelectedMoodFilter = "Bình yên"; executeMasterFilter(); filterDialog.dismiss(); });
        btnSad.setOnClickListener(v -> { currentSelectedMoodFilter = "Buồn"; executeMasterFilter(); filterDialog.dismiss(); });
        btnStress.setOnClickListener(v -> { currentSelectedMoodFilter = "Căng thẳng"; executeMasterFilter(); filterDialog.dismiss(); });
        btnTired.setOnClickListener(v -> { currentSelectedMoodFilter = "Mệt mỏi"; executeMasterFilter(); filterDialog.dismiss(); });

        filterDialog.show();
    }

    /**
     * HÀM MỞ BOTTOM SHEET DIALOG GHI NHẬT KÝ MỚI
     */
    private void showAddDiaryDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(DiaryActivity.this);
        bottomSheetDialog.setContentView(R.layout.dialog_add_diary);

        TextView tvDate = bottomSheetDialog.findViewById(R.id.tv_date);
        ImageButton btnClose = bottomSheetDialog.findViewById(R.id.btn_close);
        ChipGroup chipGroupMood = bottomSheetDialog.findViewById(R.id.chip_group_mood);
        ChipGroup chipGroupTags = bottomSheetDialog.findViewById(R.id.chip_group_tags);
        EditText edtContent = bottomSheetDialog.findViewById(R.id.edt_content);
        AppCompatButton btnSave = bottomSheetDialog.findViewById(R.id.btn_save);

        long currentTimestamp = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d 'Tháng' M", new Locale("vi", "VN"));
        if (tvDate != null) {
            tvDate.setText(dateFormat.format(new Date(currentTimestamp)) + " · 2026");
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(vClose -> bottomSheetDialog.dismiss());
        }

        if (chipGroupMood != null) {
            Chip defaultChip = bottomSheetDialog.findViewById(R.id.chip_peaceful);
            if (defaultChip != null && !defaultChip.getText().toString().contains("✓")) {
                defaultChip.setText(defaultChip.getText().toString() + " ✓");
            }

            chipGroupMood.setOnCheckedStateChangeListener((group, checkedIds) -> {
                for (int i = 0; i < group.getChildCount(); i++) {
                    Chip chip = (Chip) group.getChildAt(i);
                    String text = chip.getText().toString();
                    if (text.contains("✓")) {
                        chip.setText(text.replace(" ✓", "").trim());
                    }
                }
                if (!checkedIds.isEmpty()) {
                    int selectedId = checkedIds.get(0);
                    Chip selectedChip = bottomSheetDialog.findViewById(selectedId);
                    if (selectedChip != null) {
                        String currentText = selectedChip.getText().toString();
                        if (!currentText.contains("✓")) {
                            selectedChip.setText(currentText + " ✓");
                        }
                    }
                }
            });
        }

        if (edtContent != null && btnSave != null) {
            edtContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.toString().trim().length() > 0) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Lưu vào dòng thời gian");
                    } else {
                        btnSave.setEnabled(false);
                        btnSave.setText("Hãy viết gì đó nhé...");
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(vSave -> {
                String contentText = edtContent != null ? edtContent.getText().toString().trim() : "";

                String finalMood = "Bình yên";
                if (chipGroupMood != null) {
                    int selectedMoodId = chipGroupMood.getCheckedChipId();
                    Chip selectedChip = bottomSheetDialog.findViewById(selectedMoodId);
                    if (selectedChip != null) {
                        String rawMoodText = selectedChip.getText().toString();
                        rawMoodText = rawMoodText.replace("✓", "").trim();
                        if (rawMoodText.length() > 3) {
                            finalMood = rawMoodText.substring(2).trim();
                        } else {
                            finalMood = rawMoodText;
                        }
                    }
                }

                List<String> selectedTags = new ArrayList<>();
                if (chipGroupTags != null) {
                    for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
                        Chip chip = (Chip) chipGroupTags.getChildAt(i);
                        if (chip.isChecked()) {
                            selectedTags.add(chip.getText().toString());
                        }
                    }
                }
                if (selectedTags.isEmpty()) {
                    selectedTags.add("#NhậtKý");
                }

                String documentId = "diary_" + System.currentTimeMillis();
                String formattedDate = tvDate != null ? tvDate.getText().toString() : "Thứ Ba, 30 Tháng 6";

                DiaryModel newDiary = new DiaryModel(
                        documentId,
                        formattedDate,
                        contentText,
                        finalMood,
                        selectedTags,
                        true,
                        currentTimestamp
                );

                db.collection("nhat_ky")
                        .document(documentId)
                        .set(newDiary)
                        .addOnSuccessListener(aVoid -> {
                            bottomSheetDialog.dismiss();
                            Toast.makeText(DiaryActivity.this, "Đã lưu nhật ký lên Cloud! ✨", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(DiaryActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }

        bottomSheetDialog.show();
    }

    /**
     * HÀM TÍNH ĐIỂM TRUNG BÌNH CHUẨN XÁC DỰA TRÊN MỨC ĐỘ CẢM XÚC
     */
    private void updateAverageScore() {
        if (tvAverageScore == null) return;

        if (diaryList.isEmpty()) {
            tvAverageScore.setText("0.0");
            return;
        }

        double totalScore = 0;
        for (DiaryModel diary : diaryList) {
            String mood = diary.getMood();
            if (mood == null) {
                totalScore += 7.5;
                continue;
            }

            String cleanMood = mood.trim();

            if (cleanMood.contains("Vui vẻ")) {
                totalScore += 9.0;
            } else if (cleanMood.contains("Căng thẳng")) {
                totalScore += 4.0;
            } else if (cleanMood.contains("Buồn")) {
                totalScore += 4.5;
            } else if (cleanMood.contains("Mệt mỏi")) {
                totalScore += 5.0;
            } else {
                totalScore += 7.5;
            }
        }

        double avg = totalScore / diaryList.size();
        tvAverageScore.setText(String.format(Locale.US, "%.1f", avg));
    }
}