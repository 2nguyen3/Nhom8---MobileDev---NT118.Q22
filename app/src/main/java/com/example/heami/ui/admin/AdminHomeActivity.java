package com.example.heami.ui.admin;

import com.example.heami.HeamiApp;
import android.widget.Toast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.data.repositories.AdminModerationRepository;
import com.example.heami.data.repositories.PresenceCountRepository;
import com.example.heami.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminHomeActivity extends AppCompatActivity {

    private static final String PREFS_ADMIN = "HeamiAdminPrefs";
    private static final String KEY_LAST_SEEN_PENDING = "last_seen_pending_reports";

    private TextView txtAdminName;
    private TextView txtAdminRole;
    private TextView txtStatAccounts;
    private TextView txtStatReports;
    private TextView txtStatOnline;
    private TextView txtPendingModerationBadge;
    private TextView txtAdminModerationAlertTitle;
    private TextView txtAdminModerationAlertDesc;

    private View cardAdminAccounts;
    private View cardAdminModeration;
    private View cardAdminAnalytics;
    private View layoutAdminModerationAlert;
    private ImageView btnAdminLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private AdminModerationRepository moderationRepository;
    private PresenceCountRepository presenceCountRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        moderationRepository = new AdminModerationRepository();
        presenceCountRepository = new PresenceCountRepository();

        bindViews();
        setupClicks();
        loadAdminProfile();
        loadAccountsCount();
        loadPendingReportsCount();
        startOnlineCountListener();
        loadPendingModerationBadge();
    }

    private void bindViews() {
        txtAdminName = findViewById(R.id.txtAdminName);
        txtAdminRole = findViewById(R.id.txtAdminRole);
        txtStatAccounts = findViewById(R.id.txtStatAccounts);
        txtStatReports = findViewById(R.id.txtStatReports);
        txtStatOnline = findViewById(R.id.txtStatOnline);
        txtPendingModerationBadge = findViewById(R.id.txtPendingModerationBadge);
        txtAdminModerationAlertTitle = findViewById(R.id.txtAdminModerationAlertTitle);
        txtAdminModerationAlertDesc = findViewById(R.id.txtAdminModerationAlertDesc);

        cardAdminAccounts = findViewById(R.id.cardAdminAccounts);
        cardAdminModeration = findViewById(R.id.cardAdminModeration);
        cardAdminAnalytics = findViewById(R.id.cardAdminAnalytics);
        layoutAdminModerationAlert = findViewById(R.id.layoutAdminModerationAlert);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);
    }

    private void setupClicks() {
        cardAdminAccounts.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminHomeActivity.this,
                        AdminAccountsActivity.class
                ))
        );

        cardAdminModeration.setOnClickListener(v -> openModerationCenter());

        cardAdminAnalytics.setOnClickListener(v ->
                startActivity(new Intent(
                        AdminHomeActivity.this,
                        AdminAnalyticsActivity.class
                ))
        );

        layoutAdminModerationAlert.setOnClickListener(v -> openModerationCenter());
        btnAdminLogout.setOnClickListener(v -> performLogout());
    }

    private void openModerationCenter() {
        markPendingReportsAsSeen();
        startActivity(new Intent(
                AdminHomeActivity.this,
                AdminModerationActivity.class
        ));
    }

    private void markPendingReportsAsSeen() {
        String currentText = txtStatReports.getText() != null
                ? txtStatReports.getText().toString().trim()
                : "0";

        int currentPending = safeParseInt(currentText);

        SharedPreferences prefs = getSharedPreferences(PREFS_ADMIN, MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_LAST_SEEN_PENDING, Math.max(currentPending, 0))
                .apply();
    }

    private void loadAdminProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            txtAdminName.setText("Heami Admin");
            txtAdminRole.setText("Administrator");
            return;
        }

        firestore.collection("admins")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String fullName = "Heami Admin";

                    if (snapshot.exists()) {
                        String dbName = snapshot.getString("full_name");
                        if (dbName != null && !dbName.trim().isEmpty()) {
                            fullName = dbName.trim();
                        }
                    }

                    txtAdminName.setText(fullName);
                    txtAdminRole.setText("Administrator");
                })
                .addOnFailureListener(e -> {
                    txtAdminName.setText("Heami Admin");
                    txtAdminRole.setText("Administrator");
                });
    }

    private void loadAccountsCount() {
        firestore.collection("accounts")
                .get()
                .addOnSuccessListener(querySnapshot ->
                        txtStatAccounts.setText(String.valueOf(querySnapshot.size()))
                )
                .addOnFailureListener(e -> txtStatAccounts.setText("--"));
    }

    private void loadPendingReportsCount() {
        moderationRepository.loadPendingReportsCount(new AdminModerationRepository.LoadPendingCountListener() {
            @Override
            public void onSuccess(int pendingCount) {
                txtStatReports.setText(String.valueOf(pendingCount));
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                txtStatReports.setText("--");
            }
        });
    }

    private void loadPendingModerationBadge() {
        moderationRepository.loadPendingReportsCount(new AdminModerationRepository.LoadPendingCountListener() {
            @Override
            public void onSuccess(int pendingCount) {
                bindPendingBadge(pendingCount);
                bindModerationAlert(pendingCount);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                txtPendingModerationBadge.setVisibility(View.GONE);
                layoutAdminModerationAlert.setVisibility(View.GONE);
            }
        });
    }

    private void bindPendingBadge(int pendingCount) {
        if (pendingCount <= 0) {
            txtPendingModerationBadge.setVisibility(View.GONE);
            return;
        }

        txtPendingModerationBadge.setVisibility(View.VISIBLE);
        txtPendingModerationBadge.setText(
                pendingCount > 99 ? "99+" : String.valueOf(pendingCount)
        );
    }

    private void bindModerationAlert(int pendingCount) {
        if (pendingCount <= 0) {
            layoutAdminModerationAlert.setVisibility(View.GONE);
            return;
        }

        layoutAdminModerationAlert.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREFS_ADMIN, MODE_PRIVATE);
        int lastSeenPending = prefs.getInt(KEY_LAST_SEEN_PENDING, 0);

        if (pendingCount > lastSeenPending) {
            int newCount = pendingCount - lastSeenPending;
            txtAdminModerationAlertTitle.setText("Có report mới cần xử lý");
            txtAdminModerationAlertDesc.setText(
                    "Hiện có " + pendingCount + " report pending, tăng thêm " + newCount + " kể từ lần xem gần nhất."
            );
        } else {
            txtAdminModerationAlertTitle.setText("Vẫn còn report chờ xử lý");
            txtAdminModerationAlertDesc.setText(
                    "Hiện có " + pendingCount + " report pending trong khu vực kiểm duyệt cộng đồng."
            );
        }
    }

    private void startOnlineCountListener() {
        stopOnlineCountListener();

        if (presenceCountRepository == null) {
            presenceCountRepository = new PresenceCountRepository();
        }

        presenceCountRepository.observePresenceCounts(new PresenceCountRepository.PresenceCountCallback() {
            @Override
            public void onChanged(@NonNull com.example.heami.data.models.PresenceCountModel countModel) {
                if (txtStatOnline != null) {
                    /*
                     * Admin Home hiển thị tổng online của cả 3 role:
                     * USER + DOCTOR + ADMIN.
                     */
                    txtStatOnline.setText(String.valueOf(countModel.getOnlineTotal()));
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (txtStatOnline != null) {
                    txtStatOnline.setText("--");
                }
            }
        });
    }

    private void stopOnlineCountListener() {
        if (presenceCountRepository != null) {
            presenceCountRepository.stopObservingPresenceCounts();
        }
    }

    private void performLogout() {
        if (getApplication() instanceof HeamiApp) {
            ((HeamiApp) getApplication()).forceClearPresenceBeforeLogout(this::finishLogoutFlow);
        } else {
            finishLogoutFlow();
        }
    }

    private void finishLogoutFlow() {
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_doctor", false)
                .putBoolean("is_admin", false)
                .apply();

        if (auth != null) {
            auth.signOut();
        } else {
            FirebaseAuth.getInstance().signOut();
        }

        Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int safeParseInt(@NonNull String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccountsCount();
        loadPendingReportsCount();
        loadPendingModerationBadge();
    }

    @Override
    protected void onDestroy() {
        stopOnlineCountListener();
        super.onDestroy();
    }
}