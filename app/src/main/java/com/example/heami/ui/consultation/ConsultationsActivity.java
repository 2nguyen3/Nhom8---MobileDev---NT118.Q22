package com.example.heami.ui.consultation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ConsultationModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConsultationsActivity extends AppCompatActivity {

    private static final String TAG = "HEAMI_CONSULT";

    private RecyclerView rvContent;

    // Hai list hoàn toàn tách biệt, không chia sẻ references
    private final List<ConsultationModel> upcomingList = new ArrayList<>();
    private final List<ConsultationModel> historyList  = new ArrayList<>();

    // Hai adapter tách biệt — tránh mọi ClassCastException từ getItemViewType()
    private ConsultationAdapter upcomingAdapter;
    private ConsultationAdapter historyAdapter;

    private View tabUpcoming, tabHistory;
    private TextView txtTabUpcomingCount, txtTabHistoryCount;
    private TextView txtTabUpcomingLabel, txtTabHistoryLabel;
    private TextView txtStatDone, txtStatSpent;
    private View layoutEmpty;
    private View btnNewBooking;

    private FirebaseFirestore db;
    private String userId;
    private boolean isUpcomingTab = true;
    private ListenerRegistration consultationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultations);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        initViews();
        setupAdapters();
        setupListeners();

        switchTab(true);  // mặc định tab Sắp tới
        loadData();
    }

    private void initViews() {
        rvContent           = findViewById(R.id.rvContent);
        tabUpcoming         = findViewById(R.id.tabUpcoming);
        tabHistory          = findViewById(R.id.tabHistory);
        txtTabUpcomingCount = findViewById(R.id.txtTabUpcomingCount);
        txtTabHistoryCount  = findViewById(R.id.txtTabHistoryCount);
        txtTabUpcomingLabel = findViewById(R.id.txtTabUpcomingLabel);
        txtTabHistoryLabel  = findViewById(R.id.txtTabHistoryLabel);
        txtStatDone         = findViewById(R.id.txtStatDone);
        txtStatSpent        = findViewById(R.id.txtStatSpent);
        layoutEmpty         = findViewById(R.id.layoutEmpty);
        btnNewBooking       = findViewById(R.id.btnNewBooking);

        txtStatDone.setText("0");
        txtStatSpent.setText("0đ");
        txtTabUpcomingCount.setText("0");
        txtTabHistoryCount.setText("0");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNewBooking.setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctorActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    private void setupAdapters() {
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        upcomingAdapter = new ConsultationAdapter(this, upcomingList, false);
        historyAdapter  = new ConsultationAdapter(this, historyList,  true);
    }

    private void setupListeners() {
        tabUpcoming.setOnClickListener(v -> switchTab(true));
        tabHistory.setOnClickListener(v -> {
            try {
                switchTab(false);
            } catch (Exception e) {
                Log.e("HEAMI_DEBUG", "Crash when switching tab: " + e.getMessage(), e);
                android.widget.Toast.makeText(this, "Lỗi: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  SWITCH TAB — swap adapter, KHÔNG tạo trang mới
    // ─────────────────────────────────────────────────────────
    private void switchTab(boolean upcomingSelected) {
        this.isUpcomingTab = upcomingSelected;

        int pinkBold    = Color.parseColor("#E8507A");
        int pinkLight   = Color.parseColor("#FFF0F5");
        int grayText    = Color.parseColor("#8E8E93");
        int grayBadgeBg = Color.parseColor("#F5F5F5");

        if (upcomingSelected) {
            tabUpcoming.setBackgroundResource(R.drawable.bg_payment_tab_selected);
            tabUpcoming.setBackgroundTintList(ColorStateList.valueOf(pinkLight));
            txtTabUpcomingLabel.setTextColor(pinkBold);
            txtTabUpcomingCount.setBackgroundTintList(ColorStateList.valueOf(pinkBold));
            txtTabUpcomingCount.setTextColor(Color.WHITE);

            tabHistory.setBackgroundColor(Color.TRANSPARENT);
            txtTabHistoryLabel.setTextColor(grayText);
            txtTabHistoryCount.setBackgroundTintList(ColorStateList.valueOf(grayBadgeBg));
            txtTabHistoryCount.setTextColor(grayText);

            btnNewBooking.setVisibility(View.VISIBLE);
            rvContent.setAdapter(upcomingAdapter);
            layoutEmpty.setVisibility(upcomingList.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            tabHistory.setBackgroundResource(R.drawable.bg_payment_tab_selected);
            tabHistory.setBackgroundTintList(ColorStateList.valueOf(pinkLight));
            txtTabHistoryLabel.setTextColor(pinkBold);
            txtTabHistoryCount.setBackgroundTintList(ColorStateList.valueOf(pinkBold));
            txtTabHistoryCount.setTextColor(Color.WHITE);

            tabUpcoming.setBackgroundColor(Color.TRANSPARENT);
            txtTabUpcomingLabel.setTextColor(grayText);
            txtTabUpcomingCount.setBackgroundTintList(ColorStateList.valueOf(grayBadgeBg));
            txtTabUpcomingCount.setTextColor(grayText);

            btnNewBooking.setVisibility(View.GONE);
            rvContent.setAdapter(historyAdapter);
            layoutEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  LOAD DATA từ Firestore
    //
    //  QUAN TRỌNG: Không dùng whereEqualTo() vì:
    //  1. Nếu Security Rules chặn collection query, Firestore trả về
    //     EMPTY (không phải error) → fallback không bao giờ được kích hoạt
    //     → dữ liệu không bao giờ hiển thị dù đơn đã tồn tại trong DB.
    //  2. Tên field trong Firestore có thể là "user_id" (snake_case) hoặc
    //     "userId" (camelCase) tuỳ vào lúc lưu.
    //  → Giải pháp: Đọc toàn bộ + filter thủ công (như code gốc của project)
    // ─────────────────────────────────────────────────────────
    private void loadData() {
        if (userId == null) {
            Log.w(TAG, "userId is null — không thể tải dữ liệu");
            return;
        }

        if (consultationListener != null) consultationListener.remove();

        consultationListener = db.collection("consultations")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore Error: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    // Filter thủ công: chấp nhận cả user_id (snake_case) và userId (camelCase)
                    List<DocumentSnapshot> userDocs = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        // Cách 1: đọc trực tiếp field string
                        String uid1 = doc.getString("user_id");
                        String uid2 = doc.getString("userId");
                        // Cách 2: qua model (PropertyName annotation)
                        ConsultationModel tmp = doc.toObject(ConsultationModel.class);
                        String uid3 = (tmp != null) ? tmp.getUserId() : null;

                        if (userId.equals(uid1) || userId.equals(uid2) || userId.equals(uid3)) {
                            userDocs.add(doc);
                        }
                    }

                    Log.d(TAG, "Tìm thấy " + userDocs.size() + " đơn của user " + userId);
                    processQueryResult(userDocs);
                });
    }

    // ─────────────────────────────────────────────────────────
    //  XỬ LÝ KẾT QUẢ QUERY
    // ─────────────────────────────────────────────────────────
    private void processQueryResult(List<DocumentSnapshot> docs) {
        upcomingList.clear();
        historyList.clear();

        int doneCount = 0;
        double spentTotal = 0;
        Date now = new Date();
        Map<String, String> docsToUpdate = new HashMap<>();

        for (DocumentSnapshot doc : docs) {
            ConsultationModel model = doc.toObject(ConsultationModel.class);
            if (model == null) continue;
            model.setSessionId(doc.getId());

            // Đọc giá tiền an toàn (tránh null/wrong type)
            Object priceObj = doc.get("price");
            double price = 0;
            if (priceObj instanceof Number) {
                price = ((Number) priceObj).doubleValue();
            }
            model.setPrice(price);

            // Bù trừ cho việc Firestore lưu lúc camelCase lúc snake_case
            if (model.getDoctorName() == null) model.setDoctorName(doc.getString("doctorName"));
            if (model.getDoctorAvatar() == null) model.setDoctorAvatar(doc.getString("doctorAvatar"));
            if (model.getPackageType() == null) model.setPackageType(doc.getString("packageType"));
            if (model.getFormatType() == null) model.setFormatType(doc.getString("formatType"));
            if (model.getTransactionId() == null) model.setTransactionId(doc.getString("transactionId"));
            if (model.getStatus() == null) model.setStatus(doc.getString("status"));
            
            if (model.getStartTime() == null) model.setStartTime(doc.getTimestamp("startTime"));
            if (model.getEndTime() == null) model.setEndTime(doc.getTimestamp("endTime"));
            if (model.getBookedAt() == null) model.setBookedAt(doc.getTimestamp("bookedAt"));

            String status = model.getStatus() != null
                    ? model.getStatus().toUpperCase(Locale.ROOT)
                    : "BOOKED";

            // Auto-complete nếu đã quá giờ kết thúc
            if ("BOOKED".equals(status)
                    && model.getEndTime() != null
                    && model.getEndTime().toDate().before(now)) {
                status = "COMPLETED";
                model.setStatus("COMPLETED");
                docsToUpdate.put(doc.getId(), "COMPLETED");
            }

            // Phân loại vào đúng list
            if ("COMPLETED".equals(status)) {
                historyList.add(model);
                doneCount++;
                spentTotal += price;           // Lịch đã xong → tính tiền đã chi
            } else if ("BOOKED".equals(status)) {
                upcomingList.add(model);
                spentTotal += price;           // Đã thanh toán VNPay → cũng tính đã chi
            } else {
                // CANCELED / CANCELLED → lịch sử nhưng không tính tiền
                historyList.add(model);
            }
        }

        // Update Firestore NGOÀI vòng lặp (tránh trigger listener lần nữa ngay lập tức)
        for (Map.Entry<String, String> entry : docsToUpdate.entrySet()) {
            db.collection("consultations").document(entry.getKey())
                    .update("status", entry.getValue());
        }

        // Cập nhật badge count trên tab
        txtTabUpcomingCount.setText(String.valueOf(upcomingList.size()));
        txtTabHistoryCount.setText(String.valueOf(historyList.size()));

        // Cập nhật thống kê
        txtStatDone.setText(String.valueOf(doneCount));
        txtStatSpent.setText(formatMoney(spentTotal));

        // Luôn notify cả 2 adapter vì dữ liệu dùng chung (dù chỉ show 1 tab)
        upcomingAdapter.notifyDataSetChanged();
        historyAdapter.notifyDataSetChanged();

        if (isUpcomingTab) {
            layoutEmpty.setVisibility(upcomingList.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            layoutEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
        }

        // Fetch online status bác sĩ (chỉ cho upcoming)
        fetchDoctorOnlineStatus();
    }

    // ─────────────────────────────────────────────────────────
    //  FETCH ONLINE STATUS (chỉ cho upcoming)
    // ─────────────────────────────────────────────────────────
    private void fetchDoctorOnlineStatus() {
        if (upcomingList.isEmpty()) return;

        final int[] pending = {upcomingList.size()};

        for (ConsultationModel model : upcomingList) {
            String doctorId = model.getDoctorId();
            if (doctorId == null || doctorId.isEmpty()) {
                model.setDoctorOnline(false);
                pending[0]--;
                if (pending[0] <= 0 && isUpcomingTab) upcomingAdapter.notifyDataSetChanged();
                continue;
            }

            db.collection("doctors").document(doctorId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Boolean isOnline = doc.getBoolean("is_online");
                            model.setDoctorOnline(isOnline != null && isOnline);
                        } else {
                            model.setDoctorOnline(false);
                        }
                        pending[0]--;
                        if (pending[0] <= 0 && isUpcomingTab) upcomingAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        model.setDoctorOnline(false);
                        pending[0]--;
                        if (pending[0] <= 0 && isUpcomingTab) upcomingAdapter.notifyDataSetChanged();
                    });
        }
    }

    // ─────────────────────────────────────────────────────────
    //  FORMAT MONEY (gọn gàng, không tràn UI)
    // ─────────────────────────────────────────────────────────
    private String formatMoney(double amount) {
        if (amount >= 1_000_000) {
            double m = amount / 1_000_000.0;
            if (m == (long) m) return String.format(Locale.getDefault(), "%dTr", (long) m);
            return String.format(Locale.getDefault(), "%.1fTr", m);
        } else if (amount >= 1_000) {
            double k = amount / 1_000.0;
            if (k == (long) k) return String.format(Locale.getDefault(), "%dK", (long) k);
            return String.format(Locale.getDefault(), "%.1fK", k);
        }
        return String.format(Locale.getDefault(), "%,.0fđ", amount);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (consultationListener != null) consultationListener.remove();
    }
}
