package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class DoctorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        BottomNavManager.setup(this, BottomNavManager.TAB_DOCTOR);

        setupActions();
        setupDoctorFilters();
        setupDoctorCardClicks();
        startDoctorAnimations();
    }

    private void setupActions() {

    }

    private void setupDoctorFilters() {
        TextView filterAll = findViewById(R.id.filterDoctorAll);
        TextView filterClinical = findViewById(R.id.filterDoctorClinical);
        TextView filterPsychiatry = findViewById(R.id.filterDoctorPsychiatry);
        TextView filterTherapy = findViewById(R.id.filterDoctorTherapy);
        TextView filterPositive = findViewById(R.id.filterDoctorPositive);
        TextView filterCare = findViewById(R.id.filterDoctorCare);

        if (filterAll != null) {
            filterAll.setOnClickListener(v -> applyDoctorFilter("all"));
        }
        if (filterClinical != null) {
            filterClinical.setOnClickListener(v -> applyDoctorFilter("clinical"));
        }
        if (filterPsychiatry != null) {
            filterPsychiatry.setOnClickListener(v -> applyDoctorFilter("psychiatry"));
        }
        if (filterTherapy != null) {
            filterTherapy.setOnClickListener(v -> applyDoctorFilter("therapy"));
        }
        if (filterPositive != null) {
            filterPositive.setOnClickListener(v -> applyDoctorFilter("positive"));
        }
        if (filterCare != null) {
            filterCare.setOnClickListener(v -> applyDoctorFilter("care"));
        }

        applyDoctorFilter("all");
    }

    private void applyDoctorFilter(String filter) {
        updateDoctorFilterTabs(filter);

        View cardDoctor1 = findViewById(R.id.cardDoctor1); // clinical
        View cardDoctor2 = findViewById(R.id.cardDoctor2); // therapy
        View cardDoctor3 = findViewById(R.id.cardDoctor3); // psychiatry
        View cardDoctor4 = findViewById(R.id.cardDoctor4); // care
        View cardDoctor5 = findViewById(R.id.cardDoctor5); // positive

        setDoctorCardVisible(cardDoctor1, filter.equals("all") || filter.equals("clinical"));
        setDoctorCardVisible(cardDoctor2, filter.equals("all") || filter.equals("therapy"));
        setDoctorCardVisible(cardDoctor3, filter.equals("all") || filter.equals("psychiatry"));
        setDoctorCardVisible(cardDoctor4, filter.equals("all") || filter.equals("care"));
        setDoctorCardVisible(cardDoctor5, filter.equals("all") || filter.equals("positive"));
    }

    private void setDoctorCardVisible(View view, boolean visible) {
        if (view == null) return;
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateDoctorFilterTabs(String activeFilter) {
        updateSingleDoctorFilterTab(R.id.filterDoctorAll, activeFilter.equals("all"));
        updateSingleDoctorFilterTab(R.id.filterDoctorClinical, activeFilter.equals("clinical"));
        updateSingleDoctorFilterTab(R.id.filterDoctorPsychiatry, activeFilter.equals("psychiatry"));
        updateSingleDoctorFilterTab(R.id.filterDoctorTherapy, activeFilter.equals("therapy"));
        updateSingleDoctorFilterTab(R.id.filterDoctorPositive, activeFilter.equals("positive"));
        updateSingleDoctorFilterTab(R.id.filterDoctorCare, activeFilter.equals("care"));
    }

    private void updateSingleDoctorFilterTab(int viewId, boolean isActive) {
        TextView tab = findViewById(viewId);
        if (tab == null) return;

        if (isActive) {
            tab.setBackgroundResource(R.drawable.bg_doctor_filter_active);
            tab.setTextColor(0xFFE86FA0);
        } else {
            tab.setBackgroundResource(R.drawable.bg_doctor_filter);
            tab.setTextColor(0xFF8A9AAA);
        }
    }

    private void startDoctorAnimations() {
        // Chỉ giữ animation cho available dot, bỏ toàn bộ animation khác
        startPulseDot(findViewById(R.id.dotDoctorAvailable1), 0.85f, 1.28f, 1500);
        startPulseDot(findViewById(R.id.dotDoctorAvailable3), 0.85f, 1.28f, 1500);
        startPulseDot(findViewById(R.id.dotDoctorAvailable4), 0.85f, 1.28f, 1500);
        startPulseDot(findViewById(R.id.dotDoctorAvailable5), 0.85f, 1.28f, 1500);
    }

    private void startPulseDot(View view, float fromScale, float toScale, long duration) {
        if (view == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, fromScale, toScale, fromScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, fromScale, toScale, fromScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.55f, 1f, 0.55f);

        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.start();
    }

    private void setupDoctorCardClicks() {
        View cardDoctor1 = findViewById(R.id.cardDoctor1);
        View cardDoctor2 = findViewById(R.id.cardDoctor2);
        View cardDoctor3 = findViewById(R.id.cardDoctor3);
        View cardDoctor4 = findViewById(R.id.cardDoctor4);
        View cardDoctor5 = findViewById(R.id.cardDoctor5);

        if (cardDoctor1 != null) {
            cardDoctor1.setOnClickListener(v -> openDoctorDetail(
                    "TS. Nguyễn Minh Anh",
                    "Tiến sĩ Tâm lý học",
                    "Tâm lý lâm sàng",
                    "Hà Nội",
                    "4.9",
                    "312+",
                    "10 năm",
                    "Với hơn 10 năm kinh nghiệm trong lĩnh vực tâm lý lâm sàng, tôi chuyên hỗ trợ các vấn đề về lo âu, trầm cảm, stress. Phương pháp tiếp cận của tôi tập trung vào sự lắng nghe, thấu hiểu và đồng hành cùng bạn trên hành trình chữa lành.",
                    R.drawable.img_doctor_1
            ));
        }

        if (cardDoctor2 != null) {
            cardDoctor2.setOnClickListener(v -> openDoctorDetail(
                    "CN. Lê Thị Hương",
                    "Chuyên gia Tâm lý trị liệu",
                    "Tâm lý trị liệu",
                    "Đà Nẵng",
                    "4.9",
                    "445+",
                    "8 năm",
                    "Tôi tập trung hỗ trợ chữa lành cảm xúc, các vấn đề trong mối quan hệ, stress kéo dài và sự mất cân bằng trong cuộc sống. Phong cách đồng hành của tôi nhẹ nhàng, gần gũi và thực tế.",
                    R.drawable.img_doctor_2
            ));
        }

        if (cardDoctor3 != null) {
            cardDoctor3.setOnClickListener(v -> openDoctorDetail(
                    "ThS. Trần Văn Hùng",
                    "Thạc sĩ Tâm thần học",
                    "Tâm thần học",
                    "TP. HCM",
                    "4.8",
                    "198+",
                    "9 năm",
                    "Tôi có kinh nghiệm trong việc đánh giá và hỗ trợ các vấn đề tâm thần thường gặp như mất ngủ, lo âu, rối loạn cảm xúc và áp lực kéo dài. Mục tiêu là giúp bạn hiểu rõ tình trạng của mình và tìm hướng đi phù hợp.",
                    R.drawable.img_doctor_3
            ));
        }

        if (cardDoctor4 != null) {
            cardDoctor4.setOnClickListener(v -> openDoctorDetail(
                    "ThS. Vũ Thị Lan",
                    "Thạc sĩ Chăm sóc tâm lý",
                    "Chăm sóc tâm lý",
                    "TP. HCM",
                    "4.8",
                    "189+",
                    "7 năm",
                    "Tôi đồng hành cùng người trẻ trong các giai đoạn nhiều áp lực, hỗ trợ xây dựng lại nhịp sống ổn định, cân bằng cảm xúc và cải thiện sức khỏe tinh thần hằng ngày.",
                    R.drawable.img_doctor_4
            ));
        }

        if (cardDoctor5 != null) {
            cardDoctor5.setOnClickListener(v -> openDoctorDetail(
                    "TS. Phạm Quốc Bảo",
                    "Tiến sĩ Tâm lý tích cực",
                    "Tâm lý tích cực",
                    "Hà Nội",
                    "4.7",
                    "267+",
                    "10 năm",
                    "Tôi hướng đến việc giúp bạn phục hồi nội lực, phát triển tư duy tích cực, xây dựng thói quen tốt và tìm lại động lực sống thông qua các phương pháp tâm lý hiện đại.",
                    R.drawable.img_doctor_5
            ));
        }
    }

    private void openDoctorDetail(
            String name,
            String degree,
            String specialty,
            String location,
            String rating,
            String sessions,
            String experience,
            String intro,
            int imageRes
    ) {
        Intent intent = new Intent(DoctorActivity.this, DoctorDetailActivity.class);
        intent.putExtra("doctor_name", name);
        intent.putExtra("doctor_degree", degree);
        intent.putExtra("doctor_specialty", specialty);
        intent.putExtra("doctor_location", location);
        intent.putExtra("doctor_rating", rating);
        intent.putExtra("doctor_sessions", sessions);
        intent.putExtra("doctor_experience", experience);
        intent.putExtra("doctor_intro", intro);
        intent.putExtra("doctor_image", imageRes);
        startActivity(intent);
    }
}