package com.example.heami.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.CommunityReportModel;
import com.example.heami.data.repositories.AdminModerationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminModerationActivity extends AppCompatActivity {

    private ImageView btnBackModeration;
    private RecyclerView rvModerationReports;
    private LinearLayout layoutLoadingModeration;
    private TextView txtEmptyModeration;

    private TextView txtStatPendingReports;
    private TextView txtStatPostReports;
    private TextView txtStatCommentReports;

    private EditText edtSearchModeration;
    private TextView chipTypeAll;
    private TextView chipTypePost;
    private TextView chipTypeComment;
    private TextView chipStatusAllModeration;
    private TextView chipStatusPendingModeration;
    private TextView chipStatusResolvedModeration;

    private AdminModerationRepository repository;
    private AdminReportAdapter adapter;

    private final List<CommunityReportModel> allReports = new ArrayList<>();
    private final List<CommunityReportModel> filteredReports = new ArrayList<>();

    private String selectedTypeFilter = "ALL";
    private String selectedStatusFilter = "ALL";
    private String currentSearchQuery = "";

    private final ActivityResultLauncher<Intent> reportDetailLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> loadReports()
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_moderation);

        repository = new AdminModerationRepository();

        bindViews();
        setupRecyclerView();
        setupClicks();
        setupFilters();
        loadReports();
    }

    private void bindViews() {
        btnBackModeration = findViewById(R.id.btnBackModeration);
        rvModerationReports = findViewById(R.id.rvModerationReports);
        layoutLoadingModeration = findViewById(R.id.layoutLoadingModeration);
        txtEmptyModeration = findViewById(R.id.txtEmptyModeration);

        txtStatPendingReports = findViewById(R.id.txtStatPendingReports);
        txtStatPostReports = findViewById(R.id.txtStatPostReports);
        txtStatCommentReports = findViewById(R.id.txtStatCommentReports);

        edtSearchModeration = findViewById(R.id.edtSearchModeration);
        chipTypeAll = findViewById(R.id.chipTypeAll);
        chipTypePost = findViewById(R.id.chipTypePost);
        chipTypeComment = findViewById(R.id.chipTypeComment);
        chipStatusAllModeration = findViewById(R.id.chipStatusAllModeration);
        chipStatusPendingModeration = findViewById(R.id.chipStatusPendingModeration);
        chipStatusResolvedModeration = findViewById(R.id.chipStatusResolvedModeration);
    }

    private void setupRecyclerView() {
        adapter = new AdminReportAdapter(report -> {
            Intent intent = new Intent(
                    AdminModerationActivity.this,
                    AdminReportDetailActivity.class
            );
            intent.putExtra(AdminReportDetailActivity.EXTRA_REPORT_ID, report.getReport_id());
            reportDetailLauncher.launch(intent);
        });

        rvModerationReports.setLayoutManager(new LinearLayoutManager(this));
        rvModerationReports.setAdapter(adapter);
    }

    private void setupClicks() {
        btnBackModeration.setOnClickListener(v -> finish());
    }

    private void setupFilters() {
        edtSearchModeration.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipTypeAll.setOnClickListener(v -> {
            selectedTypeFilter = "ALL";
            updateTypeChips();
            applyFilters();
        });

        chipTypePost.setOnClickListener(v -> {
            selectedTypeFilter = "POST";
            updateTypeChips();
            applyFilters();
        });

        chipTypeComment.setOnClickListener(v -> {
            selectedTypeFilter = "COMMENT";
            updateTypeChips();
            applyFilters();
        });

        chipStatusAllModeration.setOnClickListener(v -> {
            selectedStatusFilter = "ALL";
            updateStatusChips();
            applyFilters();
        });

        chipStatusPendingModeration.setOnClickListener(v -> {
            selectedStatusFilter = "PENDING";
            updateStatusChips();
            applyFilters();
        });

        chipStatusResolvedModeration.setOnClickListener(v -> {
            selectedStatusFilter = "RESOLVED";
            updateStatusChips();
            applyFilters();
        });

        updateTypeChips();
        updateStatusChips();
    }

    private void loadReports() {
        showLoading(true);

        repository.loadReports(new AdminModerationRepository.LoadReportsListener() {
            @Override
            public void onSuccess(@NonNull List<CommunityReportModel> reports) {
                showLoading(false);

                allReports.clear();
                allReports.addAll(reports);

                applyFilters();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                showLoading(false);
                allReports.clear();
                filteredReports.clear();
                adapter.submitList(new ArrayList<>());
                updateStats();
                updateEmptyState(true);
                Toast.makeText(AdminModerationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredReports.clear();

        String query = safeLower(currentSearchQuery);

        for (CommunityReportModel item : allReports) {
            String type = safeUpper(item.getTarget_type(), "POST");
            String status = safeUpper(item.getStatus(), "PENDING");

            boolean matchType = "ALL".equals(selectedTypeFilter) || selectedTypeFilter.equals(type);
            boolean matchStatus = "ALL".equals(selectedStatusFilter) || selectedStatusFilter.equals(status);

            boolean matchSearch = query.isEmpty()
                    || safeLower(item.getSnapshot_author_name()).contains(query)
                    || safeLower(item.getSnapshot_text()).contains(query)
                    || safeLower(item.getReport_id()).contains(query)
                    || safeLower(item.getReason_text()).contains(query);

            if (matchType && matchStatus && matchSearch) {
                filteredReports.add(item);
            }
        }

        adapter.submitList(new ArrayList<>(filteredReports));
        updateStats();
        updateEmptyState(filteredReports.isEmpty());
    }

    private void updateTypeChips() {
        applyChipStyle(chipTypeAll, "ALL".equals(selectedTypeFilter), true);
        applyChipStyle(chipTypePost, "POST".equals(selectedTypeFilter), true);
        applyChipStyle(chipTypeComment, "COMMENT".equals(selectedTypeFilter), true);
    }

    private void updateStatusChips() {
        applyChipStyle(chipStatusAllModeration, "ALL".equals(selectedStatusFilter), false);
        applyChipStyle(chipStatusPendingModeration, "PENDING".equals(selectedStatusFilter), false);
        applyChipStyle(chipStatusResolvedModeration, "RESOLVED".equals(selectedStatusFilter), false);
    }

    private void applyChipStyle(TextView chip, boolean selected, boolean typeGroup) {
        if (selected) {
            chip.setBackgroundResource(typeGroup
                    ? R.drawable.bg_chip_active_purple
                    : R.drawable.bg_chip_active_teal);
            chip.setTextColor(Color.parseColor(typeGroup ? "#6A42C2" : "#1D9E92"));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_inactive);
            chip.setTextColor(Color.parseColor("#84788F"));
        }
    }

    private void showLoading(boolean isLoading) {
        layoutLoadingModeration.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvModerationReports.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        txtEmptyModeration.setVisibility(View.GONE);
    }

    private void updateEmptyState(boolean isEmpty) {
        txtEmptyModeration.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvModerationReports.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            if (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()) {
                txtEmptyModeration.setText("Không tìm thấy bài viết phù hợp");
            } else {
                txtEmptyModeration.setText("Chưa có bài viết nào bị report");
            }
        }
    }

    private void updateStats() {
        txtStatPendingReports.setText(String.valueOf(repository.countPending(filteredReports)));
        txtStatPostReports.setText(String.valueOf(repository.countPostReports(filteredReports)));
        txtStatCommentReports.setText(String.valueOf(repository.countCommentReports(filteredReports)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }

    @NonNull
    private String safeUpper(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    @NonNull
    private String safeLower(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}