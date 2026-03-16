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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class ProfileActivity extends AppCompatActivity {

    private int colorActive, colorTextOff;
    private ColorStateList thumbStates, trackStates;
    private ViewGroup rootView;
    private GestureDetector gestureDetector;

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

        // 2. GỌI HÀM HIỂN THỊ AVATAR TẠI ĐÂY
        updateUserAvatar();
        // 3. Cập nhật nickname và motto từ Firestore
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
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Lướt từ trái sang phải (velocityX > 0)
                if (e1 != null && e2 != null && e2.getX() - e1.getX() > 150 && Math.abs(velocityX) > 200) {
                    getOnBackPressedDispatcher().onBackPressed();
                    return true;
                }
                return false;
            }
        });

        // Gán listener cho rootView để nhận diện cử chỉ lướt
        View scrollView = findViewById(R.id.profileScrollView);
        if (scrollView != null) {
            scrollView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return false; // Trả về false để ScrollView vẫn cuộn dọc được bình thường
            });
        }
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
        if (findViewById(R.id.btnViewAnalysis) != null)
            findViewById(R.id.btnViewAnalysis).setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));

        if (findViewById(R.id.cardSOS) != null)
            findViewById(R.id.cardSOS).setOnClickListener(v -> startActivity(new Intent(this, SosActivity.class)));

        if (findViewById(R.id.layoutLogout) != null) {
            findViewById(R.id.layoutLogout).setOnClickListener(v -> showLogoutDialog());
        }

        if (findViewById(R.id.layoutDeleteAccount) != null) {
            findViewById(R.id.layoutDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());
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

            // Sử dụng WriteBatch để xóa nhiều document cùng lúc cho sạch sẽ
            WriteBatch batch = db.batch();
            batch.delete(db.collection("users").document(uid));
            batch.delete(db.collection("accounts").document(uid));
            batch.delete(db.collection("settings").document(uid));

            // 1. Thực thi xóa tất cả các bảng dữ liệu
            batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // 2. Sau khi sạch dữ liệu DB, tiến hành xóa Authentication
                    user.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("DeleteAcc", "SUCCESS: Đã xóa sạch dữ liệu và Auth.");
                                getSharedPreferences("HeamiData", MODE_PRIVATE).edit().clear().apply();
                                
                                Toast.makeText(this, "Tài khoản của bạn đã được xóa vĩnh viễn", Toast.LENGTH_LONG).show();
                                
                                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.e("DeleteAcc", "FAILED: Lỗi xóa Auth.");
                                Toast.makeText(this, "Vui lòng đăng nhập lại trước khi xóa tài khoản.", Toast.LENGTH_LONG).show();
                            }
                        });
                })
                .addOnFailureListener(e -> {
                    Log.e("DeleteAcc", "FAILED: Lỗi khi dọn dẹp Database.");
                    Toast.makeText(this, "Lỗi khi xóa dữ liệu. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void performLogout() {
        Log.d("Heami_Logout", ">>> Bắt đầu quy trình đăng xuất...");

        // 1. Đăng xuất khỏi Firebase
        FirebaseAuth.getInstance().signOut();

        // KIỂM CHỨNG TOKEN
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d("Heami_Logout", "SUCCESS: Firebase Token đã xóa sạch.");
        }

        // 2. Xóa dữ liệu local
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();

        // 3. Điều hướng
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}