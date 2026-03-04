package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
        startDoctorAnimations();
    }

    private void setupActions() {
        View btnBackDoctor = findViewById(R.id.btnBackDoctor);
        if (btnBackDoctor != null) {
            btnBackDoctor.setOnClickListener(v -> finish());
        }
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
}