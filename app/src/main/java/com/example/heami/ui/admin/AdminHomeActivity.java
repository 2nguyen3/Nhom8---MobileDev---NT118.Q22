package com.example.heami.ui.admin;

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
import com.example.heami.ui.auth.LoginActivity;
import com.example.heami.utils.PresenceUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminHomeActivity extends AppCompatActivity {

    private static final String PREFS_ADMIN = "HeamiAdminPrefs";
    private static final String KEY_LAST_SEEN_PENDING = "last_seen_pending_reports";

    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

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
    private FirebaseDatabase realtimeDb;
    private AdminModerationRepository moderationRepository;

    private DatabaseReference statusRootRef;
    private ValueEventListener onlineCountListener;

    private DatabaseReference serverTimeOffsetRef;
    private ValueEventListener serverTimeOffsetListener;

    private final android.os.Handler onlineRefreshHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private DataSnapshot lastStatusSnapshot;
    private long serverTimeOffsetMs = 0L;

    private final Runnable onlineRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            updateAdminOnlineCount();
            onlineRefreshHandler.postDelayed(this, 5_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance(RTDB_URL);
        moderationRepository = new AdminModerationRepository();

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

        statusRootRef = realtimeDb.getReference("status");
        statusRootRef.keepSynced(true);

        serverTimeOffsetRef = realtimeDb.getReference(".info/serverTimeOffset");
        startServerTimeOffsetListener();

        onlineCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastStatusSnapshot = snapshot;
                updateAdminOnlineCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtStatOnline.setText("--");
            }
        };

        statusRootRef.addValueEventListener(onlineCountListener);

        onlineRefreshHandler.removeCallbacks(onlineRefreshRunnable);
        onlineRefreshHandler.post(onlineRefreshRunnable);
    }

    private void startServerTimeOffsetListener() {
        stopServerTimeOffsetListener();

        if (serverTimeOffsetRef == null) {
            return;
        }

        serverTimeOffsetListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long offset = snapshot.getValue(Long.class);
                serverTimeOffsetMs = offset != null ? offset : 0L;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                serverTimeOffsetMs = 0L;
            }
        };

        serverTimeOffsetRef.addValueEventListener(serverTimeOffsetListener);
    }

    private void stopServerTimeOffsetListener() {
        if (serverTimeOffsetRef != null && serverTimeOffsetListener != null) {
            serverTimeOffsetRef.removeEventListener(serverTimeOffsetListener);
        }

        serverTimeOffsetListener = null;
    }

    private void updateAdminOnlineCount() {
        if (lastStatusSnapshot == null || txtStatOnline == null) {
            return;
        }

        long estimatedServerNow = System.currentTimeMillis() + serverTimeOffsetMs;
        int onlineCount = PresenceUtils.countOnlineAllRoles(lastStatusSnapshot, estimatedServerNow);

        txtStatOnline.setText(String.valueOf(onlineCount));
    }

    private void stopOnlineCountListener() {
        onlineRefreshHandler.removeCallbacks(onlineRefreshRunnable);

        if (statusRootRef != null && onlineCountListener != null) {
            statusRootRef.removeEventListener(onlineCountListener);
        }

        onlineCountListener = null;
        lastStatusSnapshot = null;

        stopServerTimeOffsetListener();
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_doctor", false)
                .putBoolean("is_admin", false)
                .apply();

        auth.signOut();

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