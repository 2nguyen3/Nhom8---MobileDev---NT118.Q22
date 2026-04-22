package com.example.heami.ui.consultation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ConsultationModel;
import com.example.heami.ui.main.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConsultationsActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultations);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupRecyclerView();
        setupListeners();
        
        // Khởi tạo trạng thái mặc định là Tab Sắp tới
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
            Intent intent = new Intent(this, DoctorActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        adapter = new ConsultationAdapter(consultationList);
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.setAdapter(adapter);
    }

    private void setupListeners() {
        // Gán sự kiện click cho 2 Tab
        tabUpcoming.setOnClickListener(v -> switchTab(true));
        tabHistory.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean isUpcomingSelected) {
        this.isUpcomingTab = isUpcomingSelected;

        // Định nghĩa các mã màu theo yêu cầu
        int pinkBold = Color.parseColor("#E8507A");
        int pinkLight = Color.parseColor("#FFF0F5");
        int grayText = Color.parseColor("#8E8E93");
        int grayBadgeBg = Color.parseColor("#F5F5F5");

        if (isUpcomingSelected) {
            // --- TAB SẮP TỚI: ACTIVE ---
            tabUpcoming.setBackgroundResource(R.drawable.bg_payment_tab_selected);
            tabUpcoming.setBackgroundTintList(ColorStateList.valueOf(pinkLight));
            txtTabUpcomingLabel.setTextColor(pinkBold);
            txtTabUpcomingCount.setBackgroundTintList(ColorStateList.valueOf(pinkBold));
            txtTabUpcomingCount.setTextColor(Color.WHITE);

            // --- TAB HÓA ĐƠN: INACTIVE ---
            tabHistory.setBackgroundColor(Color.TRANSPARENT);
            txtTabHistoryLabel.setTextColor(grayText);
            txtTabHistoryCount.setBackgroundTintList(ColorStateList.valueOf(grayBadgeBg));
            txtTabHistoryCount.setTextColor(grayText);

            // Hiển thị nút đặt lịch mới ở tab Sắp tới
            btnNewBooking.setVisibility(View.VISIBLE);
        } else {
            // --- TAB HÓA ĐƠN: ACTIVE ---
            tabHistory.setBackgroundResource(R.drawable.bg_payment_tab_selected);
            tabHistory.setBackgroundTintList(ColorStateList.valueOf(pinkLight));
            txtTabHistoryLabel.setTextColor(pinkBold);
            txtTabHistoryCount.setBackgroundTintList(ColorStateList.valueOf(pinkBold));
            txtTabHistoryCount.setTextColor(Color.WHITE);

            // --- TAB SẮP TỚI: INACTIVE ---
            tabUpcoming.setBackgroundColor(Color.TRANSPARENT);
            txtTabUpcomingLabel.setTextColor(grayText);
            txtTabUpcomingCount.setBackgroundTintList(ColorStateList.valueOf(grayBadgeBg));
            txtTabUpcomingCount.setTextColor(grayText);

            // Ẩn nút đặt lịch mới ở tab Lịch sử
            btnNewBooking.setVisibility(View.GONE);
        }
        
        updateListUI();
    }

    private void loadData() {
        if (userId == null) return;

        db.collection("consultations")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    upcomingList.clear();
                    historyList.clear();
                    int doneCount = 0;
                    double totalSpent = 0;

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            ConsultationModel model = doc.toObject(ConsultationModel.class);
                            String status = model.getStatus();

                            if ("BOOKED".equals(status)) {
                                upcomingList.add(model);
                            } else if ("COMPLETED".equals(status)) {
                                historyList.add(model);
                                doneCount++;
                                totalSpent += model.getPrice();
                            } else {
                                historyList.add(model);
                            }
                        }
                    }

                    // Cập nhật stats
                    txtStatDone.setText(String.valueOf(doneCount));
                    txtStatUpcoming.setText(String.valueOf(upcomingList.size()));
                    txtStatSpent.setText(String.format(Locale.getDefault(), "%,.0fk", totalSpent / 1000));
                    
                    txtTabUpcomingCount.setText(String.valueOf(upcomingList.size()));
                    txtTabHistoryCount.setText(String.valueOf(historyList.size()));

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
        
        adapter.notifyDataSetChanged();
        layoutEmpty.setVisibility(consultationList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
