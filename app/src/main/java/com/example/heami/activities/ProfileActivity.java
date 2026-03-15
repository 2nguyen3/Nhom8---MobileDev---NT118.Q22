package com.example.heami.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private int colorActive, colorTextOff;
    private ColorStateList thumbStates, trackStates;
    private ViewGroup rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        rootView = findViewById(android.R.id.content);

        initColors();
        setupDarkMode();
        setupPrivacy();
        setupNotifications();
        setupAccountActions();
        setupAllFaqs();

        // 2. GỌI HÀM HIỂN THỊ AVATAR TẠI ĐÂY
        updateUserAvatar();
        // 3. Cập nhật nickname và motto từ Firestore
        updateUserInfo();
    }

    // HÀM LẤY NICKNAME VÀ MOTTO TỪ FIRESTORE
    private void updateUserInfo() {
        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvBio = findViewById(R.id.tvProfileBio);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nickname = documentSnapshot.getString("nickname");
                            String motto = documentSnapshot.getString("motto");
                            if (nickname != null && !nickname.isEmpty() && tvName != null) {
                                tvName.setText(nickname);
                            }
                            if (motto != null && !motto.isEmpty() && tvBio != null) {
                                tvBio.setText(motto);
                            }
                        }
                    });
        }
    }

    // HÀM LẤY EMOJI ĐÃ LƯU VÀ HIỂN THỊ LÊN AVATAR
    private void updateUserAvatar() {
        // Ánh xạ TextView hiển thị Emoji (ID này phải khớp với XML mình hướng dẫn)
        TextView tvAvatarEmoji = findViewById(R.id.tvAvatarEmoji);

        // Mở file lưu trữ "HeamiData"
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);

        // Lấy giá trị emoji ra, nếu chưa chọn thì mặc định là bông hoa "🌸"
        String selectedEmoji = prefs.getString("user_avatar_emoji", "🌸");

        if (tvAvatarEmoji != null) {
            tvAvatarEmoji.setText(selectedEmoji);
        }
    }

    private void initColors() {
        colorActive = Color.parseColor("#00BFA5");
        colorTextOff = Color.parseColor("#7D8BB7");

        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked}
        };

        thumbStates = new ColorStateList(states, new int[]{Color.WHITE, colorActive});
        trackStates = new ColorStateList(states, new int[]{Color.parseColor("#E0E0E0"), Color.parseColor("#B2DFDB")});
    }

    private void setupDarkMode() {
        SwitchMaterial sw = findViewById(R.id.switchDarkMode);
        TextView tv = findViewById(R.id.tvDarkModeStatus);
        applySwitchStyle(sw);
        if (sw != null) {
            sw.setOnCheckedChangeListener((btn, isChecked) -> {
                tv.setText(isChecked ? "Đang bật" : "Đang tắt");
                tv.setTextColor(isChecked ? colorActive : colorTextOff);
            });
        }
    }

    private void setupPrivacy() {
        SwitchMaterial sw = findViewById(R.id.switchPrivacy);
        TextView tv = findViewById(R.id.tvPrivacySub);
        applySwitchStyle(sw);
        if (sw != null) {
            sw.setOnCheckedChangeListener((btn, isChecked) -> {
                TransitionManager.beginDelayedTransition(rootView, new AutoTransition());
                tv.setText(isChecked ? "Hồ sơ ẩn với cộng đồng" : "Hồ sơ hiển thị với cộng đồng");
                tv.setTextColor(isChecked ? colorActive : colorTextOff);
            });
        }
    }

    private void setupNotifications() {
        setupSingleNoti(R.id.switchNotiCheckin, R.id.tvNotiCheckinSub);
        setupSingleNoti(R.id.switchNotiPlan, R.id.tvNotiPlanSub);
        setupSingleNoti(R.id.switchNotiDr, R.id.tvNotiDrSub);
    }

    private void setupSingleNoti(int swId, int tvId) {
        SwitchMaterial sw = findViewById(swId);
        TextView tv = findViewById(tvId);
        applySwitchStyle(sw);
        if (sw != null && tv != null) {
            sw.setOnCheckedChangeListener((btn, isChecked) -> tv.setTextColor(isChecked ? colorActive : colorTextOff));
        }
    }

    private void applySwitchStyle(SwitchMaterial sw) {
        if (sw != null) {
            sw.setThumbTintList(thumbStates);
            sw.setTrackTintList(trackStates);
        }
    }

    private void setupAllFaqs() {
        setupFaqItem(R.id.layoutQuestion1, R.id.layoutAnswer1, R.id.imgChevron1);
        setupFaqItem(R.id.layoutQuestion2, R.id.layoutAnswer2, R.id.imgChevron2);
        setupFaqItem(R.id.layoutQuestion3, R.id.layoutAnswer3, R.id.imgChevron3);
        setupFaqItem(R.id.layoutQuestion4, R.id.layoutAnswer4, R.id.imgChevron4);
    }

    private void setupFaqItem(int questionId, int answerId, int chevronId) {
        RelativeLayout question = findViewById(questionId);
        final LinearLayout answer = findViewById(answerId);
        final ImageView chevron = findViewById(chevronId);

        if (question != null && answer != null && chevron != null) {
            question.setOnClickListener(v -> {
                // Dùng Visibility đơn giản, không thêm TransitionManager ở đây để tránh bị "nhảy"
                if (answer.getVisibility() == View.GONE) {
                    answer.setVisibility(View.VISIBLE);
                    chevron.setRotation(90); // Xoay mũi tên chỉ xuống
                } else {
                    answer.setVisibility(View.GONE);
                    chevron.setRotation(0);  // Xoay mũi tên về vị trí cũ
                }
            });
        }
    }

    private void setupAccountActions() {
        if (findViewById(R.id.btnBackProfile) != null) {
            findViewById(R.id.btnBackProfile).setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (findViewById(R.id.btnViewAnalysis) != null)
            findViewById(R.id.btnViewAnalysis).setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));

        if (findViewById(R.id.cardSOS) != null)
            findViewById(R.id.cardSOS).setOnClickListener(v -> startActivity(new Intent(this, SosActivity.class)));

        if (findViewById(R.id.layoutLogout) != null) {
            findViewById(R.id.layoutLogout).setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void showLogoutDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_logout_confirmation);

        // Chặn tắt popup
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        // Thiết lập kích thước để đảm bảo luôn ở giữa và căn lề
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        com.google.android.material.button.MaterialButton btnStay = dialog.findViewById(R.id.btnStay);
        TextView tvConfirmLogout = dialog.findViewById(R.id.tvConfirmLogout);
        TextView tvDialogAvatarEmoji = dialog.findViewById(R.id.tvDialogAvatarEmoji);

        // Lấy emoji từ SharedPreferences và hiển thị lên dialog
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        String selectedEmoji = prefs.getString("user_avatar_emoji", "🌸");
        if (tvDialogAvatarEmoji != null) {
            tvDialogAvatarEmoji.setText(selectedEmoji);
        }

        if (btnStay != null) {
            btnStay.setOnClickListener(v -> dialog.dismiss());
        }

        if (tvConfirmLogout != null) {
            tvConfirmLogout.setOnClickListener(v -> {
                dialog.dismiss();
                performLogout();
            });
        }

        dialog.show();
    }

    private void performLogout() {
        Log.d("Logout", ">>> Bắt đầu quy trình đăng xuất...");

        // 1. Đăng xuất khỏi Firebase
        FirebaseAuth.getInstance().signOut();

        // KIỂM CHỨNG TOKEN: Nếu currentUser là null, nghĩa là Token đã bị hủy sạch
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d("Logout", "SUCCESS: Firebase Token đã xóa sạch. CurrentUser = null");
        } else {
            Log.e("Logout", "FAILED: Firebase Token vẫn còn tồn tại!");
        }

        // 2. Xóa dữ liệu local trong SharedPreferences
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Log.d("Logout", "SUCCESS: Đã dọn dẹp bộ nhớ tạm (SharedPreferences)");

        // 3. Điều hướng về Login và xóa stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Log.d("Logout", "SUCCESS: Đang điều hướng về LoginActivity và đóng các màn hình cũ.");

        startActivity(intent);
        finish();
    }
}