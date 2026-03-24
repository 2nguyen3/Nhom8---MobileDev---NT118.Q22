package com.example.heami.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
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
import com.example.heami.models.UserSettingsModel;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
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

    private final String[] allMoodGoals = {
            "😮‍💨 Giảm căng thẳng", "😴 Cải thiện giấc ngủ", "🧠 Tập trung làm việc",
            "☀️ Ổn định cảm xúc", "✨ Sống tích cực", "📅 Xây dựng thói quen",
            "🌿 Phát triển bản thân", "🤝 Kết nối mọi người"
    };
    private List<String> userSelectedMoodGoals = new ArrayList<>();
    private String currentAvatarEmoji = "🌸";
    private String currentUserEmail = "";
    private UserSettingsModel userSettings;
    private boolean isUpdatingUI = false; // Cờ ngăn chặn trigger ngược khi đang đồng bộ dữ liệu từ DB

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

        updateUserAvatarLocal();
        updateUserInfo();
        loadUserSettings();
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
        TextView tvStatStreak = findViewById(R.id.tvStatStreak);
        TextView tvStatCheckin = findViewById(R.id.tvStatCheckin);
        TextView tvStatTask = findViewById(R.id.tvStatTask);
        LinearLayout layoutChips = findViewById(R.id.layoutGoalsContainer);
        SwitchMaterial switchPrivacy = findViewById(R.id.switchPrivacy);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserEmail = currentUser.getEmail();

            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            isUpdatingUI = true;
                            
                            String nickname = documentSnapshot.getString("nickname");
                            String motto = documentSnapshot.getString("motto");
                            String avatarEmoji = documentSnapshot.getString("avatar_url");
                            List<String> mood_goals = (List<String>) documentSnapshot.get("mood_goals");
                            Long streak = documentSnapshot.getLong("current_streak");
                            Boolean isProtected = documentSnapshot.getBoolean("is_protected_mode");

                            if (nickname != null) tvName.setText(nickname);
                            if (motto != null) tvBio.setText(motto);
                            if (streak != null) tvStatStreak.setText(String.valueOf(streak));
                            
                            tvStatCheckin.setText(documentSnapshot.contains("total_checkins") ? String.valueOf(documentSnapshot.getLong("total_checkins")) : "0");
                            tvStatTask.setText(documentSnapshot.contains("tasks_done") ? String.valueOf(documentSnapshot.getLong("tasks_done")) : "0");

                            if (isProtected != null && switchPrivacy != null) {
                                switchPrivacy.setChecked(isProtected);
                                updatePrivacyStatusUI(isProtected);
                            }

                            if (avatarEmoji != null && !avatarEmoji.isEmpty()) {
                                tvAvatarEmoji.setText(avatarEmoji);
                                currentAvatarEmoji = avatarEmoji;
                                SharedPreferences.Editor editor = getSharedPreferences("HeamiData", MODE_PRIVATE).edit();
                                editor.putString("user_avatar_emoji", avatarEmoji);
                                editor.apply();
                            }

                            if (mood_goals != null) {
                                userSelectedMoodGoals = new ArrayList<>(mood_goals);
                                updateMoodGoalsUI(layoutChips, mood_goals);
                            }
                            
                            isUpdatingUI = false;
                        }
                    });
        }
    }

    private void loadUserSettings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DocumentReference settingsRef = FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("settings").document("default");

        settingsRef.get().addOnSuccessListener(doc -> {
            isUpdatingUI = true;
            if (doc.exists()) {
                userSettings = doc.toObject(UserSettingsModel.class);
                syncSettingsToUI();
            } else {
                userSettings = new UserSettingsModel("LIGHT", true);
                settingsRef.set(userSettings);
                syncSettingsToUI();
            }
            isUpdatingUI = false;
        });
    }

    private void syncSettingsToUI() {
        if (userSettings == null) return;

        SwitchMaterial swDarkMode = findViewById(R.id.switchDarkMode);
        if (swDarkMode != null) {
            swDarkMode.setChecked("DARK".equals(userSettings.getTheme_mode()));
            updateDarkModeStatusUI(swDarkMode.isChecked());
        }

        Map<String, Boolean> config = userSettings.getNotif_config();
        if (config != null) {
            setSwitchChecked(R.id.switchNotiCheckin, config.getOrDefault("checkin", true));
            setSwitchChecked(R.id.switchNotiPlan, config.getOrDefault("plan", true));
            setSwitchChecked(R.id.switchNotiDr, config.getOrDefault("appointment", true));
            setSwitchChecked(R.id.switchNotiChat, config.getOrDefault("chat", true));
            
            updateNotiTextColors();
        }

        // Đồng bộ thời gian nhắc nhở từ database
        Map<String, String> reminders = userSettings.getReminders();
        if (reminders != null && reminders.containsKey("checkin")) {
            TextView tvCheckinTime = findViewById(R.id.tvNotiCheckinSub);
            if (tvCheckinTime != null) {
                tvCheckinTime.setText(reminders.get("checkin") + " mỗi sáng");
            }
        }
    }

    private void setSwitchChecked(int id, boolean checked) {
        SwitchMaterial sw = findViewById(id);
        if (sw != null) sw.setChecked(checked);
    }

    private void updateNotiTextColors() {
        updateSingleNotiColor(R.id.switchNotiCheckin, R.id.tvNotiCheckinSub);
        updateSingleNotiColor(R.id.switchNotiPlan, R.id.tvNotiPlanSub);
        updateSingleNotiColor(R.id.switchNotiDr, R.id.tvNotiDrSub);
        updateSingleNotiColor(R.id.switchNotiChat, R.id.tvNotiChatSub);
    }

    private void updateSingleNotiColor(int swId, int tvId) {
        SwitchMaterial sw = findViewById(swId);
        TextView tv = findViewById(tvId);
        if (sw != null && tv != null) {
            tv.setTextColor(sw.isChecked() ? colorActive : colorTextOff);
        }
    }

    private void saveSettingUpdate(String key, Object value) {
        if (isUpdatingUI) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DocumentReference ref = FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("settings").document("default");
        
        ref.update(key, value).addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi lưu cài đặt", Toast.LENGTH_SHORT).show());
        
        // Cập nhật model cục bộ để đồng bộ
        if (userSettings != null) {
            if ("theme_mode".equals(key)) {
                userSettings.setTheme_mode((String) value);
            } else if (key.startsWith("notif_config.")) {
                String configKey = key.substring("notif_config.".length());
                if (userSettings.getNotif_config() != null) {
                    userSettings.getNotif_config().put(configKey, (Boolean) value);
                }
            }
        }
    }

    private void updateMoodGoalsUI(LinearLayout container, List<String> mood_goals) {
        if (container == null) return;
        container.removeAllViews();
        if (mood_goals == null) return;
        
        float density = getResources().getDisplayMetrics().density;
        int heightPx = (int) (27 * density); // Tăng lên 27dp để tránh bị cắt chữ mà vẫn mảnh mai
        int paddingPx = (int) (10 * density);  
        
        for (String goal : mood_goals) {
            Chip chip = new Chip(this);
            chip.setText(goal);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FEE4F0")));
            chip.setTextColor(Color.parseColor("#E86FA0"));
            chip.setTextSize(10); 
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setChipStrokeWidth(0);
            
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipStartPadding(paddingPx);
            chip.setChipEndPadding(paddingPx);
            chip.setChipMinHeight(heightPx);
            chip.setTextStartPadding(0);
            chip.setTextEndPadding(0);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, heightPx);
            params.setMargins(0, 0, (int) (6 * density), 0);
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
        applySwitchStyle(sw);
        if (sw != null) {
            sw.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isUpdatingUI) return;
                updateDarkModeStatusUI(isChecked);
                saveSettingUpdate("theme_mode", isChecked ? "DARK" : "LIGHT");
            });
        }
    }

    private void updateDarkModeStatusUI(boolean isChecked) {
        TextView tv = findViewById(R.id.tvDarkModeStatus);
        if (tv != null) {
            tv.setText(isChecked ? "Đang bật" : "Đang tắt");
            tv.setTextColor(isChecked ? colorActive : colorTextOff);
        }
    }

    private void setupPrivacy() {
        SwitchMaterial sw = findViewById(R.id.switchPrivacy);
        applySwitchStyle(sw);
        if (sw != null) {
            sw.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isUpdatingUI) return;
                TransitionManager.beginDelayedTransition(rootView, new AutoTransition());
                updatePrivacyStatusUI(isChecked);
                
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                            .update("is_protected_mode", isChecked);
                }
            });
        }
    }

    private void updatePrivacyStatusUI(boolean isChecked) {
        TextView tv = findViewById(R.id.tvPrivacySub);
        if (tv != null) {
            tv.setText(isChecked ? "Hồ sơ ẩn với cộng đồng" : "Hồ sơ hiển thị với cộng đồng");
            tv.setTextColor(isChecked ? colorActive : colorTextOff);
        }
    }

    private void setupNotifications() {
        setupSingleNotiLogic(R.id.switchNotiCheckin, R.id.tvNotiCheckinSub, "checkin");
        setupSingleNotiLogic(R.id.switchNotiPlan, R.id.tvNotiPlanSub, "plan");
        setupSingleNotiLogic(R.id.switchNotiDr, R.id.tvNotiDrSub, "appointment");
        setupSingleNotiLogic(R.id.switchNotiChat, R.id.tvNotiChatSub, "chat");
    }

    private void setupSingleNotiLogic(int swId, int tvId, String configKey) {
        SwitchMaterial sw = findViewById(swId);
        TextView tv = findViewById(tvId);
        applySwitchStyle(sw);
        if (sw != null && tv != null) {
            sw.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isUpdatingUI) return;
                tv.setTextColor(isChecked ? colorActive : colorTextOff);
                saveSettingUpdate("notif_config." + configKey, isChecked);
            });
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

        if (findViewById(R.id.btnViewHistory) != null)
            findViewById(R.id.btnViewHistory).setOnClickListener(v -> Toast.makeText(this, "Lịch sử đang được xử lý", Toast.LENGTH_SHORT).show());

        if (findViewById(R.id.layoutPrivacy2) != null)
            findViewById(R.id.layoutPrivacy2).setOnClickListener(v -> Toast.makeText(this, "Xem chính sách bảo mật", Toast.LENGTH_SHORT).show());

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

        if (etEmail != null) etEmail.setText(currentUserEmail);
        etNickname.setText(((TextView) findViewById(R.id.tvProfileName)).getText());
        etBio.setText(((TextView) findViewById(R.id.tvProfileBio)).getText());
        tvEditEmoji.setText(currentAvatarEmoji);

        final String[] selectedEmoji = {currentAvatarEmoji};
        layoutEditAvatar.setOnClickListener(v -> showEmojiSelector(emoji -> {
            tvEditEmoji.setText(emoji);
            selectedEmoji[0] = emoji;
        }));

        List<String> tempSelectedMoodGoals = new ArrayList<>(userSelectedMoodGoals);
        populateMoodGoalsInDialog(flexGoals, tempSelectedMoodGoals);

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
            saveProfileChanges(newName, newBio, selectedEmoji[0], tempSelectedMoodGoals, dialog);
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

    private void populateMoodGoalsInDialog(FlexboxLayout flex, List<String> selected) {
        flex.removeAllViews();
        for (String goal : allMoodGoals) {
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

    private void saveProfileChanges(String name, String bio, String avatar, List<String> mood_goals, android.app.Dialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("nickname", name);
            updates.put("motto", bio);
            updates.put("avatar_url", avatar);
            updates.put("mood_goals", mood_goals);

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
            batch.delete(db.collection("users").document(uid).collection("settings").document("default"));

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
