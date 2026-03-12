package com.example.heami.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class DoctorDetailActivity extends AppCompatActivity {

    private ImageButton btnBackDoctorDetail;
    private ImageButton btnDoctorDetailFavorite;

    private ImageView imgDoctorDetailAvatar;
    private ImageView imgDoctorDetailVerified;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_detail);

        bindViews();
        setupActions();
        bindDoctorDataFromIntent();
    }

    private void bindViews() {
        btnBackDoctorDetail = findViewById(R.id.btnBackDoctorDetail);
        btnDoctorDetailFavorite = findViewById(R.id.btnDoctorDetailFavorite);

        imgDoctorDetailAvatar = findViewById(R.id.imgDoctorDetailAvatar);
        imgDoctorDetailVerified = findViewById(R.id.imgDoctorDetailVerified);

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

        if (btnDoctorDetailFavorite != null) {
            btnDoctorDetailFavorite.setOnClickListener(v -> {
                // tạm để trống
            });
        }

        if (cardPackage15 != null) {
            cardPackage15.setOnClickListener(v -> selectPackage(15));
        }

        if (cardPackage30 != null) {
            cardPackage30.setOnClickListener(v -> selectPackage(30));
        }

        if (cardPackage7Days != null) {
            cardPackage7Days.setOnClickListener(v -> selectPackage(7));
        }

        if (cardDoctorFormatCall != null) {
            cardDoctorFormatCall.setOnClickListener(v -> selectFormat("call"));
        }

        if (cardDoctorFormatChat != null) {
            cardDoctorFormatChat.setOnClickListener(v -> selectFormat("chat"));
        }

        if (btnDoctorCheckout != null) {
            btnDoctorCheckout.setOnClickListener(v -> {
                // bước sau nối flow thanh toán
            });
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
        int imageRes = getIntent().getIntExtra("doctor_image", R.drawable.img_doctor_1);

        if (txtDoctorDetailName != null) {
            txtDoctorDetailName.setText(name != null ? name : "TS. Nguyễn Minh Anh");
        }
        if (txtDoctorDetailDegree != null) {
            txtDoctorDetailDegree.setText(degree != null ? degree : "Tiến sĩ Tâm lý học");
        }
        if (txtDoctorDetailSpecialty != null) {
            txtDoctorDetailSpecialty.setText(specialty != null ? specialty : "Tâm lý lâm sàng");
        }
        if (txtDoctorDetailLocationText != null) {
            txtDoctorDetailLocationText.setText(location != null ? location : "Hà Nội");
        }
        if (txtDoctorDetailAvailable != null) {
            txtDoctorDetailAvailable.setText("Đang nhận lịch hôm nay");
        }
        if (txtDoctorStatRating != null) {
            txtDoctorStatRating.setText(rating != null ? rating : "4.9");
        }
        if (txtDoctorStatSessions != null) {
            txtDoctorStatSessions.setText(sessions != null ? sessions : "312+");
        }
        if (txtDoctorStatExperience != null) {
            txtDoctorStatExperience.setText(experience != null ? experience : "10 năm");
        }
        if (txtDoctorIntro != null) {
            txtDoctorIntro.setText(
                    intro != null
                            ? intro
                            : "Với hơn 10 năm kinh nghiệm trong lĩnh vực tâm lý lâm sàng..."
            );
        }
        if (imgDoctorDetailAvatar != null) {
            imgDoctorDetailAvatar.setImageResource(imageRes);
        }
        if (txtDoctorReviewTitle != null) {
            txtDoctorReviewTitle.setText("Đánh giá (312)");
        }

        selectedPackageType = 30;
        selectedFormatType = "call";

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
        if (radioPackage15 != null) {
            boolean isChecked = selectedPackageType == 15;
            radioPackage15.setChecked(isChecked);
            radioPackage15.setBackgroundResource(
                    isChecked
                            ? R.drawable.bg_doctor_radio_checked
                            : R.drawable.bg_doctor_radio_unchecked
            );
        }

        if (radioPackage30 != null) {
            boolean isChecked = selectedPackageType == 30;
            radioPackage30.setChecked(isChecked);
            radioPackage30.setBackgroundResource(
                    isChecked
                            ? R.drawable.bg_doctor_radio_checked
                            : R.drawable.bg_doctor_radio_unchecked
            );
        }

        if (radioPackage7Days != null) {
            boolean isChecked = selectedPackageType == 7;
            radioPackage7Days.setChecked(isChecked);
            radioPackage7Days.setBackgroundResource(
                    isChecked
                            ? R.drawable.bg_doctor_radio_checked
                            : R.drawable.bg_doctor_radio_unchecked
            );
        }
    }

    private void updatePackageCardStates() {
        if (cardPackage15 != null) {
            cardPackage15.setBackgroundResource(
                    selectedPackageType == 15
                            ? R.drawable.bg_doctor_package_card_selected
                            : R.drawable.bg_doctor_package_card
            );
        }

        if (cardPackage30 != null) {
            cardPackage30.setBackgroundResource(
                    selectedPackageType == 30
                            ? R.drawable.bg_doctor_package_card_selected
                            : R.drawable.bg_doctor_package_card
            );
        }

        if (cardPackage7Days != null) {
            cardPackage7Days.setBackgroundResource(
                    selectedPackageType == 7
                            ? R.drawable.bg_doctor_package_card_selected
                            : R.drawable.bg_doctor_package_card
            );
        }
    }

    private void updateFormatCardStates() {
        boolean isCallSelected = "call".equals(selectedFormatType);
        boolean isChatSelected = "chat".equals(selectedFormatType);

        if (cardDoctorFormatCall != null) {
            cardDoctorFormatCall.setBackgroundResource(
                    isCallSelected
                            ? R.drawable.bg_doctor_format_selected
                            : R.drawable.bg_doctor_format_card
            );
        }

        if (cardDoctorFormatChat != null) {
            cardDoctorFormatChat.setBackgroundResource(
                    isChatSelected
                            ? R.drawable.bg_doctor_format_selected
                            : R.drawable.bg_doctor_format_card
            );
        }

        if (layoutFormatCallCheck != null) {
            layoutFormatCallCheck.setVisibility(isCallSelected ? View.VISIBLE : View.INVISIBLE);
        }

        if (layoutFormatChatCheck != null) {
            layoutFormatChatCheck.setVisibility(isChatSelected ? View.VISIBLE : View.INVISIBLE);
        }

        if (txtFormatCallTitle != null) {
            txtFormatCallTitle.setTextColor(isCallSelected ? 0xFF2D1B47 : 0xFF9E8AAA);
            txtFormatCallTitle.setAlpha(isCallSelected ? 1f : 0.9f);
        }

        if (txtFormatCallSubtitle != null) {
            txtFormatCallSubtitle.setTextColor(isCallSelected ? 0xFF9EDDD0 : 0xFFC0AED0);
            txtFormatCallSubtitle.setAlpha(isCallSelected ? 1f : 0.9f);
        }

        if (txtFormatChatTitle != null) {
            txtFormatChatTitle.setTextColor(isChatSelected ? 0xFF2D1B47 : 0xFF9E8AAA);
            txtFormatChatTitle.setAlpha(isChatSelected ? 1f : 0.9f);
        }

        if (txtFormatChatSubtitle != null) {
            txtFormatChatSubtitle.setTextColor(isChatSelected ? 0xFF9EDDD0 : 0xFFC0AED0);
            txtFormatChatSubtitle.setAlpha(isChatSelected ? 1f : 0.9f);
        }
    }

    private void updateBottomSummary() {
        if (txtDoctorBottomSummaryType == null || txtDoctorBottomSummaryPrice == null) {
            return;
        }

        String packageLabel;
        String priceLabel;

        if (selectedPackageType == 15) {
            packageLabel = "15 phút";
            priceLabel = "125.000đ";
        } else if (selectedPackageType == 30) {
            packageLabel = "30 phút";
            priceLabel = "250.000đ";
        } else {
            packageLabel = "Gói 7 ngày";
            priceLabel = "1.125.000đ";
        }

        String formatLabel;
        if ("chat".equals(selectedFormatType)) {
            formatLabel = "Chat trong 24h";
        } else {
            formatLabel = "Đặt lịch gọi";
        }

        txtDoctorBottomSummaryType.setText(packageLabel + " · " + formatLabel);
        txtDoctorBottomSummaryPrice.setText(priceLabel);

        if (txtDoctorBottomFlexible != null) {
            if ("chat".equals(selectedFormatType)) {
                txtDoctorBottomFlexible.setText("Nhắn tin linh hoạt");
            } else {
                txtDoctorBottomFlexible.setText("Đặt lịch linh hoạt");
            }
        }
    }
}