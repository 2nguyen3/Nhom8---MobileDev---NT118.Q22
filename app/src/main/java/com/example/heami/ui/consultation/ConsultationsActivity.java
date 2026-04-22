package com.example.heami.ui.consultation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ConsultationModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ConsultationsActivity extends AppCompatActivity {

    private static final String TAG = "HEAMI_STATS";
    private RecyclerView rvContent;
    private ConsultationAdapter adapter;
    private List<ConsultationModel> consultationList = new ArrayList<>();
    private List<ConsultationModel> upcomingList = new ArrayList<>();
    private List<ConsultationModel> historyList = new ArrayList<>();
    
    private View tabUpcoming, tabHistory;
    private TextView txtTabUpcomingCount, txtTabHistoryCount;
    private TextView txtTabUpcomingLabel, txtTabHistoryLabel;
    private TextView txtStatDone, txtStatUpcoming, txtStatSpent;
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
        setupRecyclerView();
        setupListeners();
        
        switchTab(true);
        loadData();
    }

    private void initViews() {
        rvContent = findViewById(R.id.rvContent);
        tabUpcoming = findViewById(R.id.tabUpcoming);
        tabHistory = findViewById(R.id.tabHistory);
        
        txtTabUpcomingCount = findViewById(R.id.txtTabUpcomingCount);
        txtTabHistoryCount = findViewById(R.id.txtTabHistoryCount);
        txtTabUpcomingLabel = findViewById(R.id.txtTabUpcomingLabel);
        txtTabHistoryLabel = findViewById(R.id.txtTabHistoryLabel);

        txtStatDone = findViewById(R.id.txtStatDone);
        txtStatUpcoming = findViewById(R.id.txtStatUpcoming);
        txtStatSpent = findViewById(R.id.txtStatSpent);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnNewBooking = findViewById(R.id.btnNewBooking);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        btnNewBooking.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, Class.forName("com.example.heami.ui.consultation.BookingFlowActivity"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ConsultationAdapter(this, consultationList);
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.setAdapter(adapter);
    }

    private void setupListeners() {
        tabUpcoming.setOnClickListener(v -> switchTab(true));
        tabHistory.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean isUpcomingSelected) {
        this.isUpcomingTab = isUpcomingSelected;
        int pinkBold = Color.parseColor("#E8507A");
        int pinkLight = Color.parseColor("#FFF0F5");
        int grayText = Color.parseColor("#8E8E93");
        int grayBadgeBg = Color.parseColor("#F5F5F5");

        if (isUpcomingSelected) {
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
        }
        updateListUI();
    }

    private void loadData() {
        if (userId == null) return;

        if (consultationListener != null) consultationListener.remove();

        consultationListener = db.collection("consultations")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi Firestore: " + error.getMessage());
                        return;
                    }

                    if (value == null) return;

                    // CHÍNH XÁC: Phải xóa sạch danh sách TOÀN CỤC trước khi xử lý snapshot mới
                    upcomingList.clear();
                    historyList.clear();

                    int doneCount = 0;
                    double spentTotal = 0;
                    Date now = new Date();

                    Set<String> uniqueIds = new HashSet<>();

                    Log.d(TAG, "=== BẮT ĐẦU XỬ LÝ DỮ LIỆU (" + value.size() + " docs) ===");

                    for (QueryDocumentSnapshot doc : value) {
                        Map<String, Object> data = doc.getData();

                        // 1. Kiểm tra UserID
                        String docUid = String.valueOf(data.getOrDefault("user_id", data.getOrDefault("userId", "")));
                        if (!userId.equals(docUid)) continue;

                        String docId = doc.getId();
                        if (uniqueIds.contains(docId)) continue;
                        uniqueIds.add(docId);

                        // 2. Lấy Status
                        String statusRaw = String.valueOf(data.getOrDefault("status", data.getOrDefault("Status", ""))).trim();
                        String status = statusRaw.toUpperCase();

                        // 3. Lấy Price an toàn
                        double price = 0.0;
                        Object priceObj = data.getOrDefault("price", data.get("Price"));
                        if (priceObj instanceof Number) {
                            price = ((Number) priceObj).doubleValue();
                        } else if (priceObj instanceof String) {
                            try {
                                String clean = ((String) priceObj).replaceAll("[^0-9.]", "");
                                price = Double.parseDouble(clean);
                            } catch (Exception e) { price = 0.0; }
                        }

                        Timestamp endTime = (Timestamp) data.getOrDefault("end_time", data.get("endTime"));

                        ConsultationModel model = doc.toObject(ConsultationModel.class);
                        model.setSessionId(docId);
                        model.setPrice(price); // Đảm bảo gán lại price chuẩn đã qua xử lý an toàn

                        // 4. Logic Tự động hoàn thành (Sửa đổi tránh vòng lặp vô hạn)
                        if ("BOOKED".equals(status) && endTime != null && endTime.toDate().before(now)) {
                            Log.d(TAG, "--> Phát hiện hết giờ, tự động chuyển COMPLETED trong UI cho: " + docId);
                            status = "COMPLETED";
                            model.setStatus("COMPLETED");

                            // Cập nhật ngầm lên Firestore (Bản chất lệnh này kích hoạt listener chạy lại,
                            // nhưng do ta đã clear list ở đầu hàm nên không sợ bị nhân bản dữ liệu nữa)
                            db.collection("consultations").document(docId).update("status", "COMPLETED");
                        }

                        // 5. Thống kê và Phân loại chuẩn xác
                        if ("COMPLETED".equals(status)) {
                            historyList.add(model);
                            doneCount++;
                            spentTotal += price;
                        } else if ("BOOKED".equals(status)) {
                            upcomingList.add(model);
                        } else {
                            historyList.add(model); // Bao gồm các trạng thái như CANCELED, CANCELLED
                        }
                    }

                    // CẬP NHẬT GIAO DIỆN GỐC
                    txtStatDone.setText(String.valueOf(doneCount));
                    txtStatUpcoming.setText(String.valueOf(upcomingList.size()));
                    txtStatSpent.setText(String.format(Locale.getDefault(), "%,.0fk", spentTotal / 1000));

                    Log.d(TAG, "KẾT QUẢ CUỐI - Done: " + doneCount + " | Spent: " + spentTotal);

                    updateListUI();
                });
    }

    private void updateListUI() {
        consultationList.clear();
        if (isUpcomingTab) {
            consultationList.addAll(upcomingList);
        } else {
            consultationList.addAll(historyList);
        }
        
        txtTabUpcomingCount.setText(String.valueOf(upcomingList.size()));
        txtTabHistoryCount.setText(String.valueOf(historyList.size()));
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        layoutEmpty.setVisibility(consultationList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (consultationListener != null) consultationListener.remove();
    }
}
