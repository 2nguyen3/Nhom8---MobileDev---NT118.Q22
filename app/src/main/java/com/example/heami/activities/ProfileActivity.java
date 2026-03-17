package com.example.heami.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private int colorActive, colorTextOff;
    private ColorStateList thumbStates, trackStates;
    private ViewGroup rootView;
    private GestureDetector gestureDetector;

    private final String[] allGoals = {
            "😮‍💨 Giảm căng thẳng", "😴 Cải thiện giấc ngủ", "🧠 Tập trung làm việc",
            "☀️ Ổn định cảm xúc", "✨ Sống tích cực", "📅 Xây dựng thói quen",
            "🌿 Phát triển bản thân", "🤝 Kết nối mọi người"
    };
    private List<String> userSelectedGoals = new ArrayList<>();
    private String currentAvatarEmoji = "🌸";
    private String currentUserEmail = "";

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
        setupSwipeToBack();
        setupOnBackPressed();

        // Load ban đầu từ local để tránh màn hình trống
        updateUserAvatarLocal();
        // Cập nhật dữ liệu từ Database (bao gồm cả Avatar và Email)
        updateUserInfo();
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSwipeToBack() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2.getX() - e1.getX() > 150 && Math.abs(velocityX) > 200) {
                    getOnBackPressedDispatcher().onBackPressed();
                    return true;
                }
                return false;
            }
        });

        View scrollView = findViewById(R.id.profileScrollView);
        if (scrollView != null) {
            scrollView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return false;
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void updateUserInfo() {
        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvBio = findViewById(R.id.tvProfileBio);
        TextView tvAvatarEmoji = findViewById(R.id.tvAvatarEmoji);
        LinearLayout layoutChips = findViewById(R.id.layoutGoalsContainer);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Lấy email trực tiếp từ Auth để sử dụng cho popup chỉnh sửa
            currentUserEmail = currentUser.getEmail();

            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nickname = documentSnapshot.getString("nickname");
                            String motto = documentSnapshot.getString("motto");
                            String avatarEmoji = documentSnapshot.getString("avatar_url");
                            List<String> goals = (List<String>) documentSnapshot.get("goals");

                            if (nickname != null && !nickname.isEmpty() && tvName != null) tvName.setText(nickname);
                            if (motto != null && !motto.isEmpty() && tvBio != null) tvBio.setText(motto);
                            
                            if (avatarEmoji != null && !avatarEmoji.isEmpty() && tvAvatarEmoji != null) {
                                tvAvatarEmoji.setText(avatarEmoji);
                                currentAvatarEmoji = avatarEmoji;
                                SharedPreferences.Editor editor = getSharedPreferences("HeamiData", MODE_PRIVATE).edit();
                                editor.putString("user_avatar_emoji", avatarEmoji);
                                editor.apply();
                            }

                            if (goals != null) {
                                userSelectedGoals = new ArrayList<>(goals);
                                if (layoutChips != null) {
                                    updateGoalsUI(layoutChips, goals);
                                }
                            }
                        }
                    });
        }
    }

    private void updateGoalsUI(LinearLayout container, List<String> goals) {
        container.removeAllViews();
        for (String goal : goals) {
            Chip chip = new Chip(this);
            chip.setText(goal);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FEE4F0")));
            chip.setTextColor(Color.parseColor("#E86FA0"));
            chip.setTextSize(10);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setChipStrokeWidth(0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 80);
            params.setMargins(0, 0, 16, 0);
            container.addView(chip, params);
        }
    }

    private void updateUserAvatarLocal() {
        TextView tvAvatarEmoji = findViewById(R.id.tvAvatarEmoji);
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        String selectedEmoji = prefs.getString("user_avatar_emoji", "🌸");
        if (tvAvatarEmoji != null) tvAvatarEmoji.setText(selectedEmoji);
    }

    private void initColors() {
        colorActive = Color.parseColor("#00BFA5");
        colorTextOff = Color.parseColor("#7D8BB7");
        int[][] states = new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}};
        thumbStates = new ColorStateList(states, new int[]{Color.WHITE, colorActive});
        trackStates = new ColorStateList(states, new int[]{Color.parseColor("#E0E0E0"), Color.parseColor("#B2DFDB")});
    }

    private void setupDarkMode() {
        SwitchMaterial sw = findViewById(R.id.switchDarkMode);
        TextView tv = findViewById(R.id.tvDarkModeStatus);
        applySwitchStyle(sw);
        if (sw != null) {
            sw.setOnCheckedChangeListener((btn, isChecked) -> {
                if (tv != null) {
                    tv.setText(isChecked ? "Đang bật" : "Đang tắt");
                    tv.setTextColor(isChecked ? colorActive : colorTextOff);
                }
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
                if (tv != null) {
                    tv.setText(isChecked ? "Hồ sơ ẩn với cộng đồng" : "Hồ sơ hiển thị với cộng đồng");
                    tv.setTextColor(isChecked ? colorActive : colorTextOff);
                }
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
                if (answer.getVisibility() == View.GONE) {
                    answer.setVisibility(View.VISIBLE);
                    chevron.setRotation(90);
                } else {
                    answer.setVisibility(View.GONE);
                    chevron.setRotation(0);
                }
            });
        }
    }

    private void setupAccountActions() {
        if (findViewById(R.id.imgSettings) != null)
            findViewById(R.id.imgSettings).setOnClickListener(v -> showEditProfileDialog());

        if (findViewById(R.id.btnViewAnalysis) != null)
            findViewById(R.id.btnViewAnalysis).setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));

        if (findViewById(R.id.cardSOS) != null)
            findViewById(R.id.cardSOS).setOnClickListener(v -> startActivity(new Intent(this, SosActivity.class)));

        if (findViewById(R.id.layoutLogout) != null)
            findViewById(R.id.layoutLogout).setOnClickListener(v -> showLogoutDialog());
    }

    private void showEditProfileDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_edit_profile);

        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        EditText etEmail = dialog.findViewById(R.id.etEditEmail);
        EditText etNickname = dialog.findViewById(R.id.etEditNickname);
        EditText etBio = dialog.findViewById(R.id.etEditBio);
        TextView tvEditEmoji = dialog.findViewById(R.id.tvEditAvatarEmoji);
        View layoutEditAvatar = dialog.findViewById(R.id.layoutEditAvatar);
        FlexboxLayout flexGoals = dialog.findViewById(R.id.flexGoals);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveProfile);
        View btnChangePass = dialog.findViewById(R.id.btnChangePassword);
        View btnDeleteAcc = dialog.findViewById(R.id.layoutDeleteAccount);
        ImageView btnDismiss = dialog.findViewById(R.id.btnDismiss);

        // Pre-fill data
        if (etEmail != null) etEmail.setText(currentUserEmail);
        etNickname.setText(((TextView) findViewById(R.id.tvProfileName)).getText());
        etBio.setText(((TextView) findViewById(R.id.tvProfileBio)).getText());
        tvEditEmoji.setText(currentAvatarEmoji);

        final String[] selectedEmoji = {currentAvatarEmoji};
        layoutEditAvatar.setOnClickListener(v -> showEmojiSelector(emoji -> {
            tvEditEmoji.setText(emoji);
            selectedEmoji[0] = emoji;
        }));

        List<String> tempSelectedGoals = new ArrayList<>(userSelectedGoals);
        populateGoalsInDialog(flexGoals, tempSelectedGoals);

        btnDismiss.setOnClickListener(v -> dialog.dismiss());
        btnChangePass.setOnClickListener(v -> {
            dialog.dismiss();
            showChangePasswordDialog();
        });

        if (btnDeleteAcc != null) {
            btnDeleteAcc.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteAccountDialog();
            });
        }

        btnSave.setOnClickListener(v -> {
            String newName = etNickname.getText().toString().trim();
            String newBio = etBio.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }
            saveProfileChanges(newName, newBio, selectedEmoji[0], tempSelectedGoals, dialog);
        });

        dialog.show();
    }

    private void showEmojiSelector(OnEmojiSelectedListener listener) {
        android.app.Dialog emojiDialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        emojiDialog.setContentView(R.layout.dialog_avatar_selector);

        GridLayout grid = emojiDialog.findViewById(R.id.gridEmojiSelector);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (child instanceof TextView) {
                child.setOnClickListener(v -> {
                    listener.onEmojiSelected(((TextView) v).getText().toString());
                    emojiDialog.dismiss();
                });
            }
        }
        emojiDialog.show();
    }

    interface OnEmojiSelectedListener {
        void onEmojiSelected(String emoji);
    }

    private void populateGoalsInDialog(FlexboxLayout flex, List<String> selected) {
        flex.removeAllViews();
        for (String goal : allGoals) {
            Chip chip = new Chip(this);
            chip.setText(goal);
            chip.setCheckable(true);
            boolean isChecked = selected.contains(goal);
            chip.setChecked(isChecked);
            updateChipStyle(chip, isChecked);

            chip.setOnCheckedChangeListener((buttonView, isCheckedNow) -> {
                if (isCheckedNow) {
                    if (selected.size() >= 3) {
                        chip.setChecked(false);
                        Toast.makeText(this, "Chỉ chọn tối đa 3 mục tiêu", Toast.LENGTH_SHORT).show();
                    } else {
                        selected.add(goal);
                        updateChipStyle(chip, true);
                    }
                } else {
                    selected.remove(goal);
                    updateChipStyle(chip, false);
                }
            });

            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 12, 12);
            flex.addView(chip, params);
        }
    }

    private void updateChipStyle(Chip chip, boolean isChecked) {
        if (isChecked) {
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E86FA0")));
            chip.setTextColor(Color.WHITE);
            chip.setChipStrokeWidth(0);
        } else {
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FEE4F0")));
            chip.setTextColor(Color.parseColor("#E86FA0"));
            chip.setChipStrokeWidth(0);
        }
    }

    private void saveProfileChanges(String name, String bio, String avatar, List<String> goals, android.app.Dialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("nickname", name);
            updates.put("motto", bio);
            updates.put("avatar_url", avatar);
            updates.put("goals", goals);

            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật hồ sơ", Toast.LENGTH_SHORT).show();
                        updateUserInfo();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show());
        }
    }

    private void showChangePasswordDialog() {
        Toast.makeText(this, "Vui lòng kiểm tra email để đặt lại mật khẩu", Toast.LENGTH_LONG).show();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Email đổi mật khẩu đã được gửi!", Toast.LENGTH_SHORT).show());
        }
    }

    private void showLogoutDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_logout_confirmation);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

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

        if (btnStay != null) btnStay.setOnClickListener(v -> dialog.dismiss());
        if (tvConfirmLogout != null) {
            tvConfirmLogout.setOnClickListener(v -> {
                dialog.dismiss();
                performLogout();
            });
        }
        dialog.show();
    }

    private void showDeleteAccountDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_delete_account_confirmation);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelDelete);
        TextView tvConfirmDelete = dialog.findViewById(R.id.tvConfirmDeleteAccount);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (tvConfirmDelete != null) {
            tvConfirmDelete.setOnClickListener(v -> {
                dialog.dismiss();
                deleteAccountPermanently();
            });
        }
        dialog.show();
    }

    private void deleteAccountPermanently() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            WriteBatch batch = db.batch();
            batch.delete(db.collection("users").document(uid));
            batch.delete(db.collection("accounts").document(uid));
            batch.delete(db.collection("settings").document(uid));

            batch.commit().addOnSuccessListener(aVoid -> user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    getSharedPreferences("HeamiData", MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this, "Tài khoản đã xóa vĩnh viễn", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập lại để xóa tài khoản", Toast.LENGTH_LONG).show();
                }
            }));
        }
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}