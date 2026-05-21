package com.example.heami.ui.admin;

import android.content.Intent;
import com.example.heami.data.repositories.AdminAccountRepository;
import com.google.firebase.auth.FirebaseAuth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.data.models.AccountModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminAccountDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "extra_account_id";

    private ImageView btnBackDetail;
    private ImageView imgDetailAvatar;

    private TextView txtDetailName;
    private TextView txtDetailSubtitle;
    private TextView txtDetailRoleBadge;
    private TextView txtDetailStatusBadge;

    private TextView txtDetailEmail;
    private TextView txtDetailUid;
    private TextView txtDetailCreatedAt;
    private TextView txtDetailLastSignIn;
    private TextView txtDetailSessionId;

    private LinearLayout cardRoleInfo;
    private TextView txtRoleInfoTitle;
    private TextView txtRoleInfoBody;

    private LinearLayout layoutLoadingDetail;
    private android.widget.ScrollView layoutContentDetail;
    private TextView txtDetailError;

    private TextView btnToggleStatus;

    private FirebaseFirestore firestore;
    private String accountId = "";

    private AdminAccountRepository repository;
    private String currentRole = "USER";
    private String currentStatus = "ACTIVE";
    private boolean isUpdatingStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_account_detail);

        firestore = FirebaseFirestore.getInstance();
        repository = new AdminAccountRepository();
        accountId = safeText(getIntent().getStringExtra(EXTRA_ACCOUNT_ID), "");

        bindViews();
        setupClicks();

        if (accountId.isEmpty()) {
            showError("Thiếu account_id để mở chi tiết tài khoản");
            return;
        }

        loadAccountDetail();
    }

    private void bindViews() {
        btnBackDetail = findViewById(R.id.btnBackDetail);
        imgDetailAvatar = findViewById(R.id.imgDetailAvatar);

        txtDetailName = findViewById(R.id.txtDetailName);
        txtDetailSubtitle = findViewById(R.id.txtDetailSubtitle);
        txtDetailRoleBadge = findViewById(R.id.txtDetailRoleBadge);
        txtDetailStatusBadge = findViewById(R.id.txtDetailStatusBadge);

        txtDetailEmail = findViewById(R.id.txtDetailEmail);
        txtDetailUid = findViewById(R.id.txtDetailUid);
        txtDetailCreatedAt = findViewById(R.id.txtDetailCreatedAt);
        txtDetailLastSignIn = findViewById(R.id.txtDetailLastSignIn);
        txtDetailSessionId = findViewById(R.id.txtDetailSessionId);

        cardRoleInfo = findViewById(R.id.cardRoleInfo);
        txtRoleInfoTitle = findViewById(R.id.txtRoleInfoTitle);
        txtRoleInfoBody = findViewById(R.id.txtRoleInfoBody);

        layoutLoadingDetail = findViewById(R.id.layoutLoadingDetail);
        layoutContentDetail = findViewById(R.id.layoutContentDetail);
        txtDetailError = findViewById(R.id.txtDetailError);

        btnToggleStatus = findViewById(R.id.btnToggleStatus);
    }

    private void setupClicks() {
        btnBackDetail.setOnClickListener(v -> finish());

        btnToggleStatus.setOnClickListener(v -> handleToggleStatusClick());
    }

    private void loadAccountDetail() {
        showLoading(true);

        firestore.collection("accounts")
                .document(accountId)
                .get()
                .addOnSuccessListener(this::handleAccountSnapshot)
                .addOnFailureListener(e -> showError(
                        e.getMessage() != null ? e.getMessage() : "Không thể tải chi tiết tài khoản"
                ));
    }

    private void handleAccountSnapshot(@NonNull DocumentSnapshot snapshot) {
        if (!snapshot.exists()) {
            showError("Không tìm thấy tài khoản");
            return;
        }

        AccountModel account = snapshot.toObject(AccountModel.class);
        if (account == null) {
            account = new AccountModel();
        }

        String uid = safeText(account.getAccount_id(), snapshot.getId());
        String email = safeText(account.getEmail(), "");
        String role = safeUpper(account.getRole(), "USER");
        String status = safeUpper(account.getStatus(), "ACTIVE");

        currentRole = role;
        currentStatus = status;

        txtDetailEmail.setText(email.isEmpty() ? "--" : email);
        txtDetailUid.setText(uid);
        txtDetailCreatedAt.setText(formatTimestamp(account.getCreated_at()));
        txtDetailLastSignIn.setText(formatTimestamp(account.getLast_sign_in_at()));
        txtDetailSessionId.setText(safeText(account.getActive_session_id(), "--"));

        bindRoleBadge(txtDetailRoleBadge, role);
        bindStatusBadge(txtDetailStatusBadge, status);
        txtDetailRoleBadge.setText(role);
        txtDetailStatusBadge.setText(status);
        updateToggleStatusButton();

        switch (role) {
            case "DOCTOR":
                loadDoctorProfile(uid);
                break;
            case "ADMIN":
                loadAdminProfile(uid);
                break;
            case "USER":
            default:
                loadUserProfile(uid);
                break;
        }
    }

    private void handleToggleStatusClick() {
        if (isUpdatingStatus || accountId.isEmpty()) {
            return;
        }

        String myUid = FirebaseAuth.getInstance().getUid();

        if ("ACTIVE".equals(currentStatus) && accountId.equals(myUid)) {
            Toast.makeText(
                    this,
                    "Không thể tự khóa tài khoản admin đang đăng nhập",
                    Toast.LENGTH_SHORT
            ).show();
            loadAccountDetail();
            return;
        }

        final String targetStatus;
        if ("BANNED".equals(currentStatus)) {
            targetStatus = "ACTIVE";
        } else if ("PENDING_VERIFY".equals(currentStatus)) {
            targetStatus = "ACTIVE";
        } else {
            targetStatus = "BANNED";
        }

        isUpdatingStatus = true;
        updateToggleStatusButton();

        repository.updateAccountStatus(accountId, targetStatus, new AdminAccountRepository.UpdateStatusListener() {
            @Override
            public void onSuccess() {
                isUpdatingStatus = false;
                currentStatus = targetStatus;

                txtDetailStatusBadge.setText(currentStatus);
                bindStatusBadge(txtDetailStatusBadge, currentStatus);
                updateToggleStatusButton();

                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_ACCOUNT_ID, accountId);
                resultIntent.putExtra("extra_new_status", currentStatus);
                setResult(RESULT_OK, resultIntent);

                Toast.makeText(
                        AdminAccountDetailActivity.this,
                        "Đã cập nhật trạng thái thành " + currentStatus,
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                isUpdatingStatus = false;
                updateToggleStatusButton();

                Toast.makeText(
                        AdminAccountDetailActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void updateToggleStatusButton() {
        btnToggleStatus.setEnabled(!isUpdatingStatus);

        if (isUpdatingStatus) {
            btnToggleStatus.setText("Đang cập nhật...");
            btnToggleStatus.setAlpha(0.7f);
            btnToggleStatus.setBackgroundResource(R.drawable.bg_button_teal);
            return;
        }

        btnToggleStatus.setAlpha(1f);

        if ("BANNED".equals(currentStatus)) {
            btnToggleStatus.setText("Mở khóa tài khoản");
            btnToggleStatus.setBackgroundResource(R.drawable.bg_button_teal);
        } else if ("PENDING_VERIFY".equals(currentStatus)) {
            btnToggleStatus.setText("Kích hoạt tài khoản");
            btnToggleStatus.setBackgroundResource(R.drawable.bg_button_teal);
        } else {
            btnToggleStatus.setText("Khóa tài khoản");
            btnToggleStatus.setBackgroundResource(R.drawable.bg_button_soft_red);
        }
    }

    private void loadUserProfile(@NonNull String uid) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String nickname = safeText(doc.getString("nickname"), "Người dùng Heami");
                    String avatarUrl = safeText(doc.getString("avatar_url"), "");
                    String motto = safeText(doc.getString("motto"), "Người dùng Heami");

                    txtDetailName.setText(nickname);
                    txtDetailSubtitle.setText(motto);

                    bindAvatar(avatarUrl);

                    txtRoleInfoTitle.setText("Hồ sơ người dùng");
                    txtRoleInfoBody.setText(
                            "Nickname: " + nickname + "\n"
                                    + "Motto: " + motto
                    );

                    showContent();
                })
                .addOnFailureListener(e -> showError(
                        e.getMessage() != null ? e.getMessage() : "Không thể tải hồ sơ người dùng"
                ));
    }

    private void loadDoctorProfile(@NonNull String uid) {
        firestore.collection("doctors")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String fullName = safeText(doc.getString("full_name"), "Bác sĩ Heami");
                    String avatarUrl = safeText(doc.getString("avatar_url"), "");
                    boolean isOnline = Boolean.TRUE.equals(doc.getBoolean("is_online"));

                    List<String> specialization = (List<String>) doc.get("specialization");
                    String primarySpec = (specialization != null && !specialization.isEmpty())
                            ? specialization.get(0)
                            : "Chưa cập nhật chuyên môn";

                    txtDetailName.setText(fullName);
                    txtDetailSubtitle.setText("Bác sĩ • " + primarySpec);

                    bindAvatar(avatarUrl);

                    txtRoleInfoTitle.setText("Hồ sơ bác sĩ");
                    txtRoleInfoBody.setText(
                            "Họ tên: " + fullName + "\n"
                                    + "Chuyên môn: " + primarySpec + "\n"
                                    + "Online: " + (isOnline ? "Có" : "Không")
                    );

                    showContent();
                })
                .addOnFailureListener(e -> showError(
                        e.getMessage() != null ? e.getMessage() : "Không thể tải hồ sơ bác sĩ"
                ));
    }

    private void loadAdminProfile(@NonNull String uid) {
        firestore.collection("admins")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String fullName = safeText(doc.getString("full_name"), "Heami Admin");
                    String avatarUrl = safeText(doc.getString("avatar_url"), "");

                    txtDetailName.setText(fullName);
                    txtDetailSubtitle.setText("Quản trị viên hệ thống");

                    bindAvatar(avatarUrl);

                    Object permissions = doc.get("permissions");

                    txtRoleInfoTitle.setText("Hồ sơ quản trị viên");
                    txtRoleInfoBody.setText(
                            "Họ tên: " + fullName + "\n"
                                    + "Quyền: " + (permissions != null ? permissions.toString() : "Chưa cấu hình")
                    );

                    showContent();
                })
                .addOnFailureListener(e -> showError(
                        e.getMessage() != null ? e.getMessage() : "Không thể tải hồ sơ quản trị viên"
                ));
    }

    private void bindAvatar(@Nullable String avatarUrl) {
        if (TextUtils.isEmpty(avatarUrl)) {
            imgDetailAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }

        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(imgDetailAvatar);
    }

    private void bindRoleBadge(@NonNull TextView view, @NonNull String role) {
        switch (role) {
            case "DOCTOR":
                view.setBackgroundResource(R.drawable.bg_chip_active_purple);
                view.setTextColor(android.graphics.Color.parseColor("#6A42C2"));
                break;
            case "ADMIN":
                view.setBackgroundResource(R.drawable.bg_chip_active_yellow);
                view.setTextColor(android.graphics.Color.parseColor("#B7791F"));
                break;
            case "USER":
            default:
                view.setBackgroundResource(R.drawable.bg_chip_active_teal);
                view.setTextColor(android.graphics.Color.parseColor("#1D9E92"));
                break;
        }
    }

    private void bindStatusBadge(@NonNull TextView view, @NonNull String status) {
        switch (status) {
            case "BANNED":
                view.setBackgroundResource(R.drawable.bg_chip_inactive);
                view.setTextColor(android.graphics.Color.parseColor("#C2516A"));
                break;
            case "PENDING_VERIFY":
                view.setBackgroundResource(R.drawable.bg_chip_active_yellow);
                view.setTextColor(android.graphics.Color.parseColor("#B7791F"));
                break;
            case "ACTIVE":
            default:
                view.setBackgroundResource(R.drawable.bg_chip_active_teal);
                view.setTextColor(android.graphics.Color.parseColor("#1D9E92"));
                break;
        }
    }

    private void showLoading(boolean isLoading) {
        layoutLoadingDetail.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        layoutContentDetail.setVisibility(View.GONE);
        txtDetailError.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoadingDetail.setVisibility(View.GONE);
        layoutContentDetail.setVisibility(View.VISIBLE);
        txtDetailError.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        layoutLoadingDetail.setVisibility(View.GONE);
        layoutContentDetail.setVisibility(View.GONE);
        txtDetailError.setVisibility(View.VISIBLE);
        txtDetailError.setText(message);
    }

    @NonNull
    private String formatTimestamp(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "--";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(timestamp.toDate());
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    @NonNull
    private String safeUpper(@Nullable String value, @NonNull String fallback) {
        return safeText(value, fallback).toUpperCase(Locale.ROOT);
    }
}