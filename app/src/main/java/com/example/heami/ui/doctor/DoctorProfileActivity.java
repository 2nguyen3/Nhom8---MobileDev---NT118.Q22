package com.example.heami.ui.doctor;

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
import com.bumptech.glide.Glide;

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
        setupActions();

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
        FirebaseAuth.getInstance().signOut();
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
