package com.example.heami.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.AdminAccountListItem;
import com.example.heami.data.repositories.AdminAccountRepository;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminAccountsActivity extends AppCompatActivity {

    private android.widget.EditText edtSearchAccount;
    private TextView chipRoleAll;
    private TextView chipRoleUser;
    private TextView chipRoleDoctor;
    private TextView chipRoleAdmin;
    private TextView chipStatusAll;
    private TextView chipStatusActive;
    private TextView chipStatusPending;
    private TextView chipStatusBanned;

    private final List<AdminAccountListItem> filteredAccounts = new ArrayList<>();

    private String selectedRoleFilter = "ALL";
    private String selectedStatusFilter = "ALL";
    private String currentSearchQuery = "";

    private ImageView btnBackAccounts;
    private RecyclerView rvAccounts;
    private LinearLayout layoutLoadingAccounts;
    private TextView txtEmptyAccounts;

    private TextView txtStatTotal;
    private TextView txtStatUser;
    private TextView txtStatDoctor;
    private TextView txtStatAdmin;

    private AdminAccountAdapter adminAccountAdapter;
    private AdminAccountRepository repository;

    private final List<AdminAccountListItem> allAccounts = new ArrayList<>();

    private final ActivityResultLauncher<Intent> accountDetailLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadAccounts();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_accounts);

        repository = new AdminAccountRepository();

        bindViews();
        setupRecyclerView();
        setupClicks();
        setupSearchAndFilters();
        loadAccounts();
    }

    private void bindViews() {
        btnBackAccounts = findViewById(R.id.btnBackAccounts);
        rvAccounts = findViewById(R.id.rvAccounts);
        layoutLoadingAccounts = findViewById(R.id.layoutLoadingAccounts);
        txtEmptyAccounts = findViewById(R.id.txtEmptyAccounts);

        txtStatTotal = findViewById(R.id.txtStatTotal);
        txtStatUser = findViewById(R.id.txtStatUser);
        txtStatDoctor = findViewById(R.id.txtStatDoctor);
        txtStatAdmin = findViewById(R.id.txtStatAdmin);

        edtSearchAccount = findViewById(R.id.edtSearchAccount);
        chipRoleAll = findViewById(R.id.chipRoleAll);
        chipRoleUser = findViewById(R.id.chipRoleUser);
        chipRoleDoctor = findViewById(R.id.chipRoleDoctor);
        chipRoleAdmin = findViewById(R.id.chipRoleAdmin);
        chipStatusAll = findViewById(R.id.chipStatusAll);
        chipStatusActive = findViewById(R.id.chipStatusActive);
        chipStatusPending = findViewById(R.id.chipStatusPending);
        chipStatusBanned = findViewById(R.id.chipStatusBanned);
    }

    private void setupRecyclerView() {
        adminAccountAdapter = new AdminAccountAdapter(item -> {
            Intent intent = new Intent(
                    AdminAccountsActivity.this,
                    AdminAccountDetailActivity.class
            );
            intent.putExtra(AdminAccountDetailActivity.EXTRA_ACCOUNT_ID, item.getAccountId());
            accountDetailLauncher.launch(intent);
        });

        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(adminAccountAdapter);
    }

    private void setupClicks() {
        btnBackAccounts.setOnClickListener(v -> finish());
    }

    private void loadAccounts() {
        showLoading(true);

        repository.loadAllAccounts(new AdminAccountRepository.LoadAccountsListener() {
            @Override
            public void onSuccess(@NonNull List<AdminAccountListItem> items) {
                showLoading(false);

                allAccounts.clear();
                allAccounts.addAll(items);
                applyFilters();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                showLoading(false);
                updateEmptyState(true);
                Toast.makeText(AdminAccountsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchAndFilters() {
        edtSearchAccount.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        chipRoleAll.setOnClickListener(v -> {
            selectedRoleFilter = "ALL";
            updateRoleChips();
            applyFilters();
        });

        chipRoleUser.setOnClickListener(v -> {
            selectedRoleFilter = "USER";
            updateRoleChips();
            applyFilters();
        });

        chipRoleDoctor.setOnClickListener(v -> {
            selectedRoleFilter = "DOCTOR";
            updateRoleChips();
            applyFilters();
        });

        chipRoleAdmin.setOnClickListener(v -> {
            selectedRoleFilter = "ADMIN";
            updateRoleChips();
            applyFilters();
        });

        chipStatusAll.setOnClickListener(v -> {
            selectedStatusFilter = "ALL";
            updateStatusChips();
            applyFilters();
        });

        chipStatusActive.setOnClickListener(v -> {
            selectedStatusFilter = "ACTIVE";
            updateStatusChips();
            applyFilters();
        });

        chipStatusPending.setOnClickListener(v -> {
            selectedStatusFilter = "PENDING_VERIFY";
            updateStatusChips();
            applyFilters();
        });

        chipStatusBanned.setOnClickListener(v -> {
            selectedStatusFilter = "BANNED";
            updateStatusChips();
            applyFilters();
        });

        updateRoleChips();
        updateStatusChips();
    }

    private void updateRoleChips() {
        applyChipStyle(chipRoleAll, "ALL".equals(selectedRoleFilter), true);
        applyChipStyle(chipRoleUser, "USER".equals(selectedRoleFilter), true);
        applyChipStyle(chipRoleDoctor, "DOCTOR".equals(selectedRoleFilter), true);
        applyChipStyle(chipRoleAdmin, "ADMIN".equals(selectedRoleFilter), true);
    }

    private void updateStatusChips() {
        applyChipStyle(chipStatusAll, "ALL".equals(selectedStatusFilter), false);
        applyChipStyle(chipStatusActive, "ACTIVE".equals(selectedStatusFilter), false);
        applyChipStyle(chipStatusPending, "PENDING_VERIFY".equals(selectedStatusFilter), false);
        applyChipStyle(chipStatusBanned, "BANNED".equals(selectedStatusFilter), false);
    }

    private void applyChipStyle(TextView chip, boolean selected, boolean roleGroup) {
        if (selected) {
            chip.setBackgroundResource(roleGroup
                    ? R.drawable.bg_chip_active_purple
                    : R.drawable.bg_chip_active_teal);
            chip.setTextColor(android.graphics.Color.parseColor(
                    roleGroup ? "#6A42C2" : "#1D9E92"
            ));
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_inactive);
            chip.setTextColor(android.graphics.Color.parseColor("#84788F"));
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.NORMAL);
        }
    }

    private void applyFilters() {
        filteredAccounts.clear();

        String query = safeLower(currentSearchQuery);

        for (AdminAccountListItem item : allAccounts) {
            String role = safeUpper(item.getRole(), "USER");
            String status = safeUpper(item.getStatus(), "ACTIVE");

            boolean roleMatch = "ALL".equals(selectedRoleFilter) || selectedRoleFilter.equals(role);

            boolean statusMatch;
            if ("ALL".equals(selectedStatusFilter)) {
                statusMatch = true;
            } else if ("PENDING_VERIFY".equals(selectedStatusFilter)) {
                statusMatch = "PENDING_VERIFY".equals(status) || "PENDING".equals(status);
            } else {
                statusMatch = selectedStatusFilter.equals(status);
            }

            boolean searchMatch = query.isEmpty()
                    || safeLower(item.getDisplayName()).contains(query)
                    || safeLower(item.getEmail()).contains(query)
                    || safeLower(item.getAccountId()).contains(query)
                    || safeLower(item.getSubtitle()).contains(query);

            if (roleMatch && statusMatch && searchMatch) {
                filteredAccounts.add(item);
            }
        }

        adminAccountAdapter.submitList(new ArrayList<>(filteredAccounts));
        updateStats(filteredAccounts);
        updateEmptyState(filteredAccounts.isEmpty());
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

    private void showLoading(boolean isLoading) {
        layoutLoadingAccounts.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvAccounts.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        txtEmptyAccounts.setVisibility(View.GONE);
    }

    private void updateEmptyState(boolean isEmpty) {
        txtEmptyAccounts.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvAccounts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            txtEmptyAccounts.setText(
                    currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()
                            ? "Không tìm thấy tài khoản phù hợp với bộ lọc hiện tại"
                            : "Chưa có tài khoản nào"
            );
        }
    }

    private void updateStats(@NonNull List<AdminAccountListItem> items) {
        int total = items.size();
        int userCount = 0;
        int doctorCount = 0;
        int adminCount = 0;

        for (AdminAccountListItem item : items) {
            String role = safeText(item.getRole(), "USER").toUpperCase(Locale.ROOT);
            switch (role) {
                case "DOCTOR":
                    doctorCount++;
                    break;
                case "ADMIN":
                    adminCount++;
                    break;
                case "USER":
                default:
                    userCount++;
                    break;
            }
        }

        txtStatTotal.setText(String.valueOf(total));
        txtStatUser.setText(String.valueOf(userCount));
        txtStatDoctor.setText(String.valueOf(doctorCount));
        txtStatAdmin.setText(String.valueOf(adminCount));
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}