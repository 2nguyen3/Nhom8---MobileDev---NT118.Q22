package com.example.heami.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.utils.FavoriteManager;

public class DoctorDetailActivity extends AppCompatActivity {

    private ImageButton btnBackDoctorDetail;
    private ImageButton btnDoctorDetailFavorite;

    private ImageView imgDoctorDetailAvatar;

    private TextView txtDoctorDetailName;
    private TextView txtDoctorDetailDegree;
    private TextView txtDoctorDetailSpecialty;
    private TextView txtDoctorDetailLocationText;
    private TextView txtDoctorDetailAvailable;

    private TextView txtDoctorStatRating;
    private TextView txtDoctorStatSessions;
    private TextView txtDoctorStatExperience;

    private TextView txtDoctorIntro;
    private TextView txtDoctorReviewTitle;

    private LinearLayout cardPackage15;
    private LinearLayout cardPackage30;
    private LinearLayout cardPackage7Days;

    private RadioButton radioPackage15;
    private RadioButton radioPackage30;
    private RadioButton radioPackage7Days;

    private LinearLayout cardDoctorFormatCall;
    private LinearLayout cardDoctorFormatChat;

    private FrameLayout layoutFormatCallCheck;
    private FrameLayout layoutFormatChatCheck;

    private TextView txtFormatCallTitle;
    private TextView txtFormatCallSubtitle;
    private TextView txtFormatChatTitle;
    private TextView txtFormatChatSubtitle;

    private TextView txtDoctorBottomSummaryType;
    private TextView txtDoctorBottomSummaryPrice;
    private TextView txtDoctorBottomFlexible;
    private LinearLayout btnDoctorCheckout;

    private int selectedPackageType = 30;
    private String selectedFormatType = "call";
    private String doctorId; // ID định danh bác sĩ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_detail);

        // Lấy ID bác sĩ từ Intent
        doctorId = getIntent().getStringExtra("doctor_id");

        bindViews();
        setupActions();
        bindDoctorDataFromIntent();
        
        // Cập nhật trạng thái nút yêu thích ban đầu từ máy
        updateFavoriteUI();
    }

    private void bindViews() {
        btnBackDoctorDetail = findViewById(R.id.btnBackDoctorDetail);
        btnDoctorDetailFavorite = findViewById(R.id.btnDoctorDetailFavorite);
        imgDoctorDetailAvatar = findViewById(R.id.imgDoctorDetailAvatar);
        txtDoctorDetailName = findViewById(R.id.txtDoctorDetailName);
        txtDoctorDetailDegree = findViewById(R.id.txtDoctorDetailDegree);
        txtDoctorDetailSpecialty = findViewById(R.id.txtDoctorDetailSpecialty);
        txtDoctorDetailLocationText = findViewById(R.id.txtDoctorDetailLocationText);
        txtDoctorDetailAvailable = findViewById(R.id.txtDoctorDetailAvailable);
        txtDoctorStatRating = findViewById(R.id.txtDoctorStatRating);
        txtDoctorStatSessions = findViewById(R.id.txtDoctorStatSessions);
        txtDoctorStatExperience = findViewById(R.id.txtDoctorStatExperience);
        txtDoctorIntro = findViewById(R.id.txtDoctorIntro);
        txtDoctorReviewTitle = findViewById(R.id.txtDoctorReviewTitle);
        cardPackage15 = findViewById(R.id.cardPackage15);
        cardPackage30 = findViewById(R.id.cardPackage30);
        cardPackage7Days = findViewById(R.id.cardPackage7Days);
        radioPackage15 = findViewById(R.id.radioPackage15);
        radioPackage30 = findViewById(R.id.radioPackage30);
        radioPackage7Days = findViewById(R.id.radioPackage7Days);
        cardDoctorFormatCall = findViewById(R.id.cardDoctorFormatCall);
        cardDoctorFormatChat = findViewById(R.id.cardDoctorFormatChat);
        layoutFormatCallCheck = findViewById(R.id.layoutFormatCallCheck);
        layoutFormatChatCheck = findViewById(R.id.layoutFormatChatCheck);
        txtFormatCallTitle = findViewById(R.id.txtFormatCallTitle);
        txtFormatCallSubtitle = findViewById(R.id.txtFormatCallSubtitle);
        txtFormatChatTitle = findViewById(R.id.txtFormatChatTitle);
        txtFormatChatSubtitle = findViewById(R.id.txtFormatChatSubtitle);
        txtDoctorBottomSummaryType = findViewById(R.id.txtDoctorBottomSummaryType);
        txtDoctorBottomSummaryPrice = findViewById(R.id.txtDoctorBottomSummaryPrice);
        txtDoctorBottomFlexible = findViewById(R.id.txtDoctorBottomFlexible);
        btnDoctorCheckout = findViewById(R.id.btnDoctorCheckout);
    }

    private void setupActions() {
        if (btnBackDoctorDetail != null) {
            btnBackDoctorDetail.setOnClickListener(v -> finish());
        }

        // Logic Yêu thích: Lưu vào máy qua FavoriteManager
        if (btnDoctorDetailFavorite != null) {
            btnDoctorDetailFavorite.setOnClickListener(v -> {
                if (doctorId == null) return;
                
                // Đảo ngược trạng thái và lưu vĩnh viễn
                FavoriteManager.getInstance(this).toggleFavorite(doctorId);
                
                // Cập nhật giao diện nút dựa trên dữ liệu vừa lưu
                updateFavoriteUI();
                
                boolean isFav = FavoriteManager.getInstance(this).isFavorite(doctorId);
                Toast.makeText(this, isFav ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                
                // Hiệu ứng scale
                v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() -> 
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                ).start();
            });
        }

        if (cardPackage15 != null) cardPackage15.setOnClickListener(v -> selectPackage(15));
        if (cardPackage30 != null) cardPackage30.setOnClickListener(v -> selectPackage(30));
        if (cardPackage7Days != null) cardPackage7Days.setOnClickListener(v -> selectPackage(7));
        if (cardDoctorFormatCall != null) cardDoctorFormatCall.setOnClickListener(v -> selectFormat("call"));
        if (cardDoctorFormatChat != null) cardDoctorFormatChat.setOnClickListener(v -> selectFormat("chat"));
    }

    private void updateFavoriteUI() {
        if (btnDoctorDetailFavorite != null && doctorId != null) {
            // Luôn lấy trạng thái thật sự từ FavoriteManager (SharedPreferences)
            boolean isFav = FavoriteManager.getInstance(this).isFavorite(doctorId);
            btnDoctorDetailFavorite.setImageResource(isFav ? R.drawable.ic_doctor_favorite_filled : R.drawable.ic_doctor_favorite);
        }
    }

    private void bindDoctorDataFromIntent() {
        String name = getIntent().getStringExtra("doctor_name");
        String degree = getIntent().getStringExtra("doctor_degree");
        String specialty = getIntent().getStringExtra("doctor_specialty");
        String location = getIntent().getStringExtra("doctor_location");
        String rating = getIntent().getStringExtra("doctor_rating");
        String sessions = getIntent().getStringExtra("doctor_sessions");
        String experience = getIntent().getStringExtra("doctor_experience");
        String intro = getIntent().getStringExtra("doctor_intro");
        String avatarUrl = getIntent().getStringExtra("doctor_avatar");
        int imageResFallback = getIntent().getIntExtra("doctor_image", R.drawable.img_doctor_1);

        if (txtDoctorDetailName != null) txtDoctorDetailName.setText(name);
        if (txtDoctorDetailDegree != null) txtDoctorDetailDegree.setText(degree);
        if (txtDoctorDetailSpecialty != null) txtDoctorDetailSpecialty.setText(specialty);
        if (txtDoctorDetailLocationText != null) txtDoctorDetailLocationText.setText(location);
        if (txtDoctorStatRating != null) txtDoctorStatRating.setText(rating);
        if (txtDoctorStatSessions != null) txtDoctorStatSessions.setText(sessions);
        if (txtDoctorStatExperience != null) txtDoctorStatExperience.setText(experience);
        if (txtDoctorIntro != null) txtDoctorIntro.setText(intro);
        
        if (imgDoctorDetailAvatar != null) {
            Glide.with(this).load(avatarUrl).placeholder(imageResFallback).error(imageResFallback).into(imgDoctorDetailAvatar);
        }
        updatePackageRadios();
        updatePackageCardStates();
        updateFormatCardStates();
        updateBottomSummary();
    }

    private void selectPackage(int packageType) {
        selectedPackageType = packageType;
        updatePackageRadios();
        updatePackageCardStates();
        updateBottomSummary();
    }

    private void selectFormat(String formatType) {
        selectedFormatType = formatType;
        updateFormatCardStates();
        updateBottomSummary();
    }

    private void updatePackageRadios() {
        if (radioPackage15 != null) radioPackage15.setChecked(selectedPackageType == 15);
        if (radioPackage30 != null) radioPackage30.setChecked(selectedPackageType == 30);
        if (radioPackage7Days != null) radioPackage7Days.setChecked(selectedPackageType == 7);
    }

    private void updatePackageCardStates() {
        if (cardPackage15 != null) cardPackage15.setBackgroundResource(selectedPackageType == 15 ? R.drawable.bg_doctor_package_card_selected : R.drawable.bg_doctor_package_card);
        if (cardPackage30 != null) cardPackage30.setBackgroundResource(selectedPackageType == 30 ? R.drawable.bg_doctor_package_card_selected : R.drawable.bg_doctor_package_card);
        if (cardPackage7Days != null) cardPackage7Days.setBackgroundResource(selectedPackageType == 7 ? R.drawable.bg_doctor_package_card_selected : R.drawable.bg_doctor_package_card);
    }

    private void updateFormatCardStates() {
        boolean isCallSelected = "call".equals(selectedFormatType);
        if (cardDoctorFormatCall != null) cardDoctorFormatCall.setBackgroundResource(isCallSelected ? R.drawable.bg_doctor_format_selected : R.drawable.bg_doctor_format_card);
        if (cardDoctorFormatChat != null) cardDoctorFormatChat.setBackgroundResource(!isCallSelected ? R.drawable.bg_doctor_format_selected : R.drawable.bg_doctor_format_card);
        if (layoutFormatCallCheck != null) layoutFormatCallCheck.setVisibility(isCallSelected ? View.VISIBLE : View.INVISIBLE);
        if (layoutFormatChatCheck != null) layoutFormatChatCheck.setVisibility(!isCallSelected ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateBottomSummary() {
        String packageLabel = selectedPackageType == 7 ? "Gói 7 ngày" : selectedPackageType + " phút";
        String formatLabel = "chat".equals(selectedFormatType) ? "Chat trong 24h" : "Đặt lịch gọi";
        if (txtDoctorBottomSummaryType != null) txtDoctorBottomSummaryType.setText(packageLabel + " · " + formatLabel);
    }
}
