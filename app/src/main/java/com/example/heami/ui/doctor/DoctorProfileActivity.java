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
import com.bumptech.glide.Glide;

public class DoctorProfileActivity extends AppCompatActivity {

    private TextView txtDoctorProfileName;
    private TextView txtDoctorProfileTitle;
    private ImageView imgDoctorProfileAvatar;
    private CardView cardDoctorLogout;

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
    }

    private void setupActions() {
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);
        String uid = "doc_001";
        if (isDoctor) {
            uid = prefs.getString("doctor_id", "doc_001");
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        FirebaseFirestore.getInstance().collection("doctors").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("full_name");
                        String degree = documentSnapshot.getString("degree");
                        java.util.List<String> specs = (java.util.List<String>) documentSnapshot.get("specialization");
                        String avatarUrl = documentSnapshot.getString("avatar_url");

                        if (fullName != null && !fullName.isEmpty()) {
                            txtDoctorProfileName.setText(fullName);
                        }

                        StringBuilder titleBuilder = new StringBuilder();
                        if (degree != null && !degree.isEmpty()) {
                            titleBuilder.append(degree);
                        }
                        if (specs != null && !specs.isEmpty()) {
                            if (titleBuilder.length() > 0) {
                                titleBuilder.append(" ");
                            }
                            titleBuilder.append(specs.get(0));
                        }
                        if (titleBuilder.length() > 0) {
                            txtDoctorProfileTitle.setText(titleBuilder.toString());
                        }

                        if (avatarUrl != null && !avatarUrl.isEmpty() && imgDoctorProfileAvatar != null) {
                            Glide.with(DoctorProfileActivity.this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.img_doctor_1)
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
}
