package com.example.heami.ui.doctor;

import com.example.heami.HeamiApp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.activity.OnBackPressedCallback;

import com.example.heami.R;
import com.example.heami.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.graphics.Color;
import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import java.util.Calendar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DoctorProfileActivity extends AppCompatActivity {

    private TextView txtDoctorProfileName;
    private TextView txtDoctorProfileTitle;
    private ImageView imgDoctorProfileAvatar;
    private CardView cardDoctorLogout;

    private TextView txtDoctorProfileSub;
    private TextView txtDoctorProfileBio;
    private TextView txtDoctorProfileRating;
    private TextView txtDoctorProfileReviewCount;
    private TextView txtDoctorProfileSessions;
    private TextView txtDoctorProfileExperience;
    private android.widget.ImageButton btnProfileSettings;
    private ImageView btnDoctorEditAvatar;
    private com.google.android.material.button.MaterialButton btnSetupSchedule;

    private String currentDoctorUid = "doc_001";
    private String currentFullName = "";
    private String currentDegree = "";
    private String currentSpecialty = "";
    private String currentBio = "";
    private String currentAvatarUrl = "";
    private String currentLocation = "";
    private long currentExperienceYears = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        initViews();
        // setupActions() will be called in onResume()

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DoctorProfileActivity.this, DoctorHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load lại dữ liệu mỗi khi quay về màn hình này để cập nhật lịch rảnh
        setupActions();
    }

    private void initViews() {
        txtDoctorProfileName = findViewById(R.id.txtDoctorProfileName);
        txtDoctorProfileTitle = findViewById(R.id.txtDoctorProfileTitle);
        imgDoctorProfileAvatar = findViewById(R.id.imgDoctorProfileAvatar);
        cardDoctorLogout = findViewById(R.id.cardDoctorLogout);

        txtDoctorProfileSub = findViewById(R.id.txtDoctorProfileSub);
        txtDoctorProfileBio = findViewById(R.id.txtDoctorProfileBio);
        txtDoctorProfileRating = findViewById(R.id.txtDoctorProfileRating);
        txtDoctorProfileReviewCount = findViewById(R.id.txtDoctorProfileReviewCount);
        txtDoctorProfileSessions = findViewById(R.id.txtDoctorProfileSessions);
        txtDoctorProfileExperience = findViewById(R.id.txtDoctorProfileExperience);
        btnProfileSettings = findViewById(R.id.btnProfileSettings);
        btnDoctorEditAvatar = findViewById(R.id.btnDoctorEditAvatar);
        btnSetupSchedule = findViewById(R.id.btnSetupSchedule);
    }

    private void setupActions() {
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);
        currentDoctorUid = "doc_001";
        if (isDoctor) {
            currentDoctorUid = prefs.getString("doctor_id", "doc_001");
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentDoctorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        FirebaseFirestore.getInstance().collection("doctors").document(currentDoctorUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentFullName = documentSnapshot.getString("full_name");
                        currentDegree = documentSnapshot.getString("degree");
                        java.util.List<String> specs = (java.util.List<String>) documentSnapshot.get("specialization");
                        currentSpecialty = (specs != null && !specs.isEmpty()) ? specs.get(0) : "";
                        currentAvatarUrl = documentSnapshot.getString("avatar_url");
                        currentLocation = documentSnapshot.getString("location");
                        currentBio = documentSnapshot.getString("bio");
                        
                        Double rating = documentSnapshot.getDouble("rating_avg");
                        Long reviewCount = documentSnapshot.getLong("review_count");
                        Long experienceYears = documentSnapshot.getLong("experience_years");
                        Long totalSessions = documentSnapshot.getLong("total_sessions");

                        if (currentLocation == null) currentLocation = "Hà Nội";
                        if (currentBio == null) currentBio = "";
                        if (experienceYears != null) currentExperienceYears = experienceYears;

                        if (currentFullName != null && !currentFullName.isEmpty()) {
                            txtDoctorProfileName.setText(currentFullName);
                        }

                        StringBuilder titleBuilder = new StringBuilder();
                        if (currentDegree != null && !currentDegree.isEmpty()) {
                            titleBuilder.append(currentDegree);
                        }
                        if (currentSpecialty != null && !currentSpecialty.isEmpty()) {
                            if (titleBuilder.length() > 0) {
                                titleBuilder.append(" ");
                            }
                            titleBuilder.append(currentSpecialty);
                        }
                        if (titleBuilder.length() > 0) {
                            txtDoctorProfileTitle.setText(titleBuilder.toString());
                        }

                        txtDoctorProfileSub.setText("📍 " + currentLocation);

                        if (!currentBio.isEmpty()) {
                            txtDoctorProfileBio.setText(currentBio);
                        }

                        if (rating != null) {
                            txtDoctorProfileRating.setText(String.format(java.util.Locale.US, "%.1f", rating));
                        }
                        if (reviewCount != null) {
                            txtDoctorProfileReviewCount.setText("Đánh giá (" + reviewCount + ")");
                        }
                        if (totalSessions != null) {
                            txtDoctorProfileSessions.setText(String.valueOf(totalSessions));
                        }
                        txtDoctorProfileExperience.setText(currentExperienceYears + "+");

                        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty() && imgDoctorProfileAvatar != null) {
                            Glide.with(DoctorProfileActivity.this)
                                    .load(currentAvatarUrl)
                                    .placeholder(R.drawable.img_doctor_1)
                                    .error(R.drawable.img_doctor_1)
                                    .into(imgDoctorProfileAvatar);
                        }

                        // Load động danh sách lịch rảnh tóm tắt
                        loadScheduleSummary();
                    }
                });

        if (cardDoctorLogout != null) {
            cardDoctorLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLogoutDialog();
                }
            });
        }

        View.OnClickListener editListener = v -> showEditDoctorProfileDialog();
        if (btnProfileSettings != null) {
            btnProfileSettings.setOnClickListener(editListener);
        }
        if (btnDoctorEditAvatar != null) {
            btnDoctorEditAvatar.setOnClickListener(editListener);
        }
        if (btnSetupSchedule != null) {
            btnSetupSchedule.setOnClickListener(v -> {
                Intent intent = new Intent(DoctorProfileActivity.this, DoctorScheduleActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void showEditDoctorProfileDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_doctor_edit_profile);

        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = android.view.Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        ImageView btnDismiss = dialog.findViewById(R.id.btnDoctorDismiss);
        EditText etAvatar = dialog.findViewById(R.id.etDoctorEditAvatar);
        EditText etName = dialog.findViewById(R.id.etDoctorEditName);
        EditText etDegree = dialog.findViewById(R.id.etDoctorEditDegree);
        EditText etSpecialty = dialog.findViewById(R.id.etDoctorEditSpecialty);
        EditText etLocation = dialog.findViewById(R.id.etDoctorEditLocation);
        EditText etExperience = dialog.findViewById(R.id.etDoctorEditExperience);
        EditText etBio = dialog.findViewById(R.id.etDoctorEditBio);
        com.google.android.material.button.MaterialButton btnSave = dialog.findViewById(R.id.btnSaveDoctorProfile);

        if (etAvatar != null) etAvatar.setText(currentAvatarUrl);
        if (etName != null) etName.setText(currentFullName);
        if (etDegree != null) etDegree.setText(currentDegree);
        if (etSpecialty != null) etSpecialty.setText(currentSpecialty);
        if (etLocation != null) etLocation.setText(currentLocation);
        if (etExperience != null) etExperience.setText(String.valueOf(currentExperienceYears));
        if (etBio != null) {
            etBio.setText(currentBio);
            etBio.setOnTouchListener((v, event) -> {
                if (v.getId() == R.id.etDoctorEditBio) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    switch (event.getAction() & android.view.MotionEvent.ACTION_MASK) {
                        case android.view.MotionEvent.ACTION_UP:
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            break;
                    }
                }
                return false;
            });
        }

        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String newAvatar = etAvatar != null ? etAvatar.getText().toString().trim() : "";
                String newName = etName != null ? etName.getText().toString().trim() : "";
                String newDegree = etDegree != null ? etDegree.getText().toString().trim() : "";
                String newSpecialty = etSpecialty != null ? etSpecialty.getText().toString().trim() : "";
                String newLocation = etLocation != null ? etLocation.getText().toString().trim() : "";
                String newExpStr = etExperience != null ? etExperience.getText().toString().trim() : "";
                String newBio = etBio != null ? etBio.getText().toString().trim() : "";

                if (newName.isEmpty()) {
                    Toast.makeText(this, "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
                    return;
                }

                long newExp = currentExperienceYears;
                try {
                    if (!newExpStr.isEmpty()) {
                        newExp = Long.parseLong(newExpStr);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Số năm kinh nghiệm không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }

                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("avatar_url", newAvatar);
                updates.put("full_name", newName);
                updates.put("degree", newDegree);
                updates.put("specialization", java.util.Collections.singletonList(newSpecialty));
                updates.put("location", newLocation);
                updates.put("experience_years", newExp);
                updates.put("bio", newBio);

                FirebaseFirestore.getInstance().collection("doctors").document(currentDoctorUid)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            dialog.dismiss();
                            Toast.makeText(DoctorProfileActivity.this, "Đã cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                            setupActions();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(DoctorProfileActivity.this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }

        dialog.show();
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

    private void performLogout() {
        if (getApplication() instanceof HeamiApp) {
            ((HeamiApp) getApplication()).forceClearPresenceBeforeLogout(this::finishLogoutFlow);
        } else {
            finishLogoutFlow();
        }
    }

    private void finishLogoutFlow() {
        FirebaseAuth.getInstance().signOut();

        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadScheduleSummary() {
        LinearLayout container = findViewById(R.id.layoutDoctorScheduleList);
        if (container == null) return;

        container.removeAllViews();

        // Lấy mốc thời gian bắt đầu ngày hôm nay
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar nextWeek = (Calendar) today.clone();
        nextWeek.add(Calendar.DAY_OF_YEAR, 7); // Giới hạn 1 tuần (7 ngày)

        Timestamp tsStart = new Timestamp(today.getTime());
        Timestamp tsEnd = new Timestamp(nextWeek.getTime());

        FirebaseFirestore.getInstance().collection("doctors").document(currentDoctorUid)
                .collection("time_slots")
                .whereGreaterThanOrEqualTo("start_time", tsStart)
                .whereLessThanOrEqualTo("start_time", tsEnd)
                .orderBy("start_time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        LayoutInflater inflater = LayoutInflater.from(this);
                        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        // Sử dụng Map để nhóm các slot giờ theo từng ngày trước khi vẽ UI
                        java.util.Map<String, java.util.List<String>> groupedSlots = new java.util.LinkedHashMap<>();

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Timestamp start = doc.getTimestamp("start_time");
                            Timestamp end = doc.getTimestamp("end_time");
                            if (start != null) {
                                String dateStr = dayFormat.format(start.toDate());
                                String timeStr = timeFormat.format(start.toDate());
                                if (end != null) {
                                    timeStr += " - " + timeFormat.format(end.toDate());
                                }
                                if (!groupedSlots.containsKey(dateStr)) {
                                    groupedSlots.put(dateStr, new java.util.ArrayList<>());
                                }
                                groupedSlots.get(dateStr).add(timeStr);
                            }
                        }

                        // Vẽ UI: duyệt qua từng ngày đã nhóm
                        for (java.util.Map.Entry<String, java.util.List<String>> entry : groupedSlots.entrySet()) {
                            String dateStr = entry.getKey();
                            java.util.List<String> times = entry.getValue();

                            // Tạo một container ngang cho dòng đầu tiên (Ngày + Slot giờ thứ nhất ở bên phải)
                            LinearLayout firstRow = new LinearLayout(this);
                            firstRow.setOrientation(LinearLayout.HORIZONTAL);
                            firstRow.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            firstRow.setPadding(0, 16, 0, 8);

                            // TextView hiển thị ngày ở lề trái
                            TextView tvDate = new TextView(this);
                            tvDate.setText(dateStr);
                            tvDate.setTextColor(Color.parseColor("#09A38C"));
                            tvDate.setTextSize(14);
                            tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
                            LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                            tvDate.setLayoutParams(dateLp);
                            firstRow.addView(tvDate);

                            // TextView hiển thị slot giờ thứ nhất ở lề phải
                            TextView tvFirstTime = new TextView(this);
                            tvFirstTime.setText(times.get(0));
                            tvFirstTime.setTextColor(Color.parseColor("#1A2530"));
                            tvFirstTime.setTextSize(13);
                            tvFirstTime.setGravity(android.view.Gravity.END);
                            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            tvFirstTime.setLayoutParams(timeLp);
                            firstRow.addView(tvFirstTime);

                            container.addView(firstRow);

                            // Nếu ngày này có thêm các slot giờ khác, tạo các dòng tiếp theo dồn về bên phải
                            for (int k = 1; k < times.size(); k++) {
                                LinearLayout extraRow = new LinearLayout(this);
                                extraRow.setOrientation(LinearLayout.HORIZONTAL);
                                extraRow.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT));
                                extraRow.setPadding(0, 4, 0, 4);

                                // View đệm chiếm hết không gian bên trái
                                View spacer = new View(this);
                                LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
                                        0, 1, 1f);
                                spacer.setLayoutParams(spacerLp);
                                extraRow.addView(spacer);

                                // TextView hiển thị slot giờ tiếp theo ở lề phải
                                TextView tvExtraTime = new TextView(this);
                                tvExtraTime.setText(times.get(k));
                                tvExtraTime.setTextColor(Color.parseColor("#1A2530"));
                                tvExtraTime.setTextSize(13);
                                tvExtraTime.setGravity(android.view.Gravity.END);
                                tvExtraTime.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT));
                                extraRow.addView(tvExtraTime);

                                container.addView(extraRow);
                            }

                            // Đường kẻ chia ngày
                            View divider = new View(this);
                            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    (int) (0.5 * getResources().getDisplayMetrics().density));
                            divLp.topMargin = 8;
                            divider.setLayoutParams(divLp);
                            divider.setBackgroundColor(Color.parseColor("#F2F4F7"));
                            container.addView(divider);
                        }
                    } else {
                        TextView emptyTv = new TextView(this);
                        emptyTv.setText("Chưa thiết lập lịch rảnh nào.");
                        emptyTv.setTextColor(Color.parseColor("#9E8AAA"));
                        emptyTv.setTextSize(14);
                        emptyTv.setPadding(0, 16, 0, 16);
                        container.addView(emptyTv);
                    }
                })
                .addOnFailureListener(e -> {
                    TextView errorTv = new TextView(this);
                    errorTv.setText("Lỗi tải tóm tắt lịch.");
                    errorTv.setTextColor(Color.parseColor("#FF5A5F"));
                    errorTv.setTextSize(14);
                    container.addView(errorTv);
                });
    }
}
