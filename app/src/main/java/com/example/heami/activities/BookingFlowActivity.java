package com.example.heami.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.heami.R;

public class BookingFlowActivity extends AppCompatActivity {

    private ImageButton btnBackFlow;
    private View btnMainAction;
    private View layoutBtnPayVnpay;
    private View btnComplete;
    private int currentStep = 1;

    // Stepper Views
    private TextView step1Number, step2Number, step3Number;
    private View step1Divider, step2Divider;
    private TextView step1Label, step2Label, step3Label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_flow);

        initViews();
        setupListeners();
        setupBackNavigation();
        
        updateStepUI(1);
    }

    private void initViews() {
        btnMainAction = findViewById(R.id.btnMainAction);
        layoutBtnPayVnpay = findViewById(R.id.layoutBtnPayVnpay);
        btnComplete = findViewById(R.id.btnComplete);

        // Stepper numbers
        step1Number = findViewById(R.id.step1_number);
        step2Number = findViewById(R.id.step2_number);
        step3Number = findViewById(R.id.step3_number);
        
        // Dividers
        step1Divider = findViewById(R.id.step1_divider);
        btnBackFlow = findViewById(R.id.btnBackFlow);
        step2Divider = findViewById(R.id.step2_divider);
        
        // Labels
        step1Label = findViewById(R.id.step1_label);
        step2Label = findViewById(R.id.step2_label);
        step3Label = findViewById(R.id.step3_label);
    }

    private void setupListeners() {
        btnBackFlow.setOnClickListener(v -> handleBackAction());

        if (btnMainAction != null) {
            btnMainAction.setOnClickListener(v -> {
                if (currentStep == 1) updateStepUI(2);
            });
        }

        if (layoutBtnPayVnpay != null) {
            layoutBtnPayVnpay.setOnClickListener(v -> updateStepUI(3));
        }

        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> finish());
        }
    }

    private void setupBackNavigation() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackAction();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void handleBackAction() {
        if (currentStep == 2) updateStepUI(1);
        else finish();
    }

    private void updateStepUI(int step) {
        currentStep = step;
        Fragment fragment;

        switch (step) {
            case 2:
                fragment = new BookingStep2Fragment();
                if (btnMainAction != null) btnMainAction.setVisibility(View.GONE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.VISIBLE);
                if (btnComplete != null) btnComplete.setVisibility(View.GONE);
                break;
            case 3:
                fragment = new BookingStep3Fragment();
                if (btnMainAction != null) btnMainAction.setVisibility(View.GONE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.GONE);
                if (btnComplete != null) btnComplete.setVisibility(View.VISIBLE);
                
                if (findViewById(R.id.layoutBookingTimerRow) != null) 
                    findViewById(R.id.layoutBookingTimerRow).setVisibility(View.GONE);
                break;
            case 1:
            default:
                fragment = new BookingStep1Fragment();
                if (btnMainAction != null) btnMainAction.setVisibility(View.VISIBLE);
                if (layoutBtnPayVnpay != null) layoutBtnPayVnpay.setVisibility(View.GONE);
                if (btnComplete != null) btnComplete.setVisibility(View.GONE);

                if (findViewById(R.id.layoutBookingTimerRow) != null) 
                    findViewById(R.id.layoutBookingTimerRow).setVisibility(View.VISIBLE);
                break;
        }

        replaceFragment(fragment);
        updateStepperGraphics(step);
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.booking_nav_host, fragment)
            .commit();
    }

    private void updateStepperGraphics(int step) {
        int colorActive = Color.parseColor("#00BFA5"); // Teal đậm
        int colorInactive = Color.parseColor("#BDBDBD"); // Xám đậm
        
        int bgActive = R.drawable.bg_light_teal_circle;
        int bgInactive = R.drawable.bg_light_gray_circle;

        // Reset Steps
        updateStepView(step1Number, step1Label, true, bgActive, Color.parseColor("#4A9292"), Color.parseColor("#2D1B47"));
        updateStepView(step2Number, step2Label, step >= 2, step >= 2 ? bgActive : bgInactive, step >= 2 ? Color.parseColor("#4A9292") : colorInactive, step >= 2 ? Color.parseColor("#2D1B47") : colorInactive);
        updateStepView(step3Number, step3Label, step >= 3, step >= 3 ? bgActive : bgInactive, step >= 3 ? Color.parseColor("#4A9292") : colorInactive, step >= 3 ? Color.parseColor("#2D1B47") : colorInactive);

        // Dividers
        if (step1Divider != null) step1Divider.setBackgroundColor(step >= 2 ? colorActive : Color.parseColor("#E0E0E0"));
        if (step2Divider != null) step2Divider.setBackgroundColor(step >= 3 ? colorActive : Color.parseColor("#E0E0E0"));
    }

    private void updateStepView(TextView number, TextView label, boolean active, int bgRes, int textColor, int labelColor) {
        if (number != null) {
            number.setBackgroundResource(bgRes);
            number.setTextColor(textColor);
        }
        if (label != null) {
            label.setTextColor(labelColor);
        }
    }
}
