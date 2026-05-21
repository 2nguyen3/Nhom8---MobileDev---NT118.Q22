package com.example.heami.ui.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
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

    private static final String RTDB_URL =
            "https://heami-8nt118-default-rtdb.asia-southeast1.firebasedatabase.app";

    private TextView txtAdminName;
    private TextView txtAdminRole;
    private TextView txtStatAccounts;
    private TextView txtStatReports;
    private TextView txtStatOnline;

    private LinearLayout cardAdminAccounts;
    private LinearLayout cardAdminModeration;
    private LinearLayout cardAdminAnalytics;
    private ImageView btnAdminLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseDatabase realtimeDb;

    private DatabaseReference statusRootRef;
    private ValueEventListener onlineCountListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance(RTDB_URL);

        bindViews();
        setupClicks();
        loadAdminProfile();
        loadAccountsCount();
        loadPendingReportsCount();
        startOnlineCountListener();
    }

    private void bindViews() {
        txtAdminName = findViewById(R.id.txtAdminName);
        txtAdminRole = findViewById(R.id.txtAdminRole);
        txtStatAccounts = findViewById(R.id.txtStatAccounts);
        txtStatReports = findViewById(R.id.txtStatReports);
        txtStatOnline = findViewById(R.id.txtStatOnline);

        cardAdminAccounts = findViewById(R.id.cardAdminAccounts);
        cardAdminModeration = findViewById(R.id.cardAdminModeration);
        cardAdminAnalytics = findViewById(R.id.cardAdminAnalytics);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);
    }

    private void setupClicks() {
        cardAdminAccounts.setOnClickListener(v ->
                startActivity(new android.content.Intent(
                        AdminHomeActivity.this,
                        com.example.heami.ui.admin.AdminAccountsActivity.class
                ))
        );
        cardAdminModeration.setOnClickListener(v ->
                startActivity(new android.content.Intent(
                        AdminHomeActivity.this,
                        AdminModerationActivity.class
                ))
        );

        cardAdminAnalytics.setOnClickListener(v ->
                Toast.makeText(this, "Batch D — Phân tích thống kê", Toast.LENGTH_SHORT).show()
        );

        btnAdminLogout.setOnClickListener(v -> performLogout());
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
        firestore.collection("chat_reports")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot ->
                        txtStatReports.setText(String.valueOf(querySnapshot.size()))
                )
                .addOnFailureListener(e -> txtStatReports.setText("--"));
    }

    private void startOnlineCountListener() {
        stopOnlineCountListener();

        statusRootRef = realtimeDb.getReference("status");
        onlineCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int onlineCount = 0;
                long now = System.currentTimeMillis();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (PresenceUtils.isUserOnlineFromConnections(userSnapshot, now)) {
                        onlineCount++;
                    }
                }

                txtStatOnline.setText(String.valueOf(onlineCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txtStatOnline.setText("--");
            }
        };

        statusRootRef.addValueEventListener(onlineCountListener);
    }

    private void stopOnlineCountListener() {
        if (statusRootRef != null && onlineCountListener != null) {
            statusRootRef.removeEventListener(onlineCountListener);
        }
        onlineCountListener = null;
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

    @Override
    protected void onDestroy() {
        stopOnlineCountListener();
        super.onDestroy();
    }
}