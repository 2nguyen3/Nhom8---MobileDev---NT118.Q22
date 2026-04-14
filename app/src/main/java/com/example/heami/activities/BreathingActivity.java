package com.example.heami.activities;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.heami.R;

public class BreathingActivity extends AppCompatActivity {

    private TextView tvStatus, tvInstruction, tvCycleCount;
    private Button btnAction;
    private ImageButton btnMinimize;
    private ProgressBar cpbBreath;
    private ImageView imgCloud;
    private View viewPulseGlow;
    private LinearLayout btn1Min, btn3Min, btn5Min, layoutOptions, layoutCycleInfo, layoutDots;

    private boolean isRunning = false;
    private int selectedTime = 1;
    private int cycleCount = 0;
    private CountDownTimer mainTimer, cycleTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing);

        initViews();
        setupTimeSelection();

        if (btnMinimize != null) {
            btnMinimize.setOnClickListener(v -> finish());
        }

        int targetTime = getIntent().getIntExtra("TARGET_TIME", 1);
        handleIncomingIntent(targetTime);
        // --------------------------------------------------

        btnAction.setOnClickListener(v -> {
            if (!isRunning) startBreathingSession();
            else stopBreathingSession();
        });
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvInstruction = findViewById(R.id.tvInstruction);
        btnAction = findViewById(R.id.btnAction);
        btnMinimize = findViewById(R.id.btnMinimize);
        cpbBreath = findViewById(R.id.cpbBreath);
        imgCloud = findViewById(R.id.imgCloud);
        viewPulseGlow = findViewById(R.id.viewPulseGlow);
        layoutOptions = findViewById(R.id.layoutOptions);
        btn1Min = findViewById(R.id.btn1Min);
        btn3Min = findViewById(R.id.btn3Min);
        btn5Min = findViewById(R.id.btn5Min);
        layoutCycleInfo = findViewById(R.id.layoutCycleInfo);
        layoutDots = findViewById(R.id.layoutDots);
        tvCycleCount = findViewById(R.id.tvCycleCount);

        resetAllButtons();
        layoutCycleInfo.setVisibility(View.INVISIBLE);
    }

    private void handleIncomingIntent(int time) {
        if (time == 3) {
            selectedTime = 3;
            selectButton(btn3Min);
        } else if (time == 5) {
            selectedTime = 5;
            selectButton(btn5Min);
        } else {
            selectedTime = 1;
            selectButton(btn1Min);
        }
    }

    private void setupTimeSelection() {
        View.OnClickListener listener = v -> {
            if (isRunning) return;
            selectButton((LinearLayout) v);

            if (v.getId() == R.id.btn1Min) selectedTime = 1;
            else if (v.getId() == R.id.btn3Min) selectedTime = 3;
            else if (v.getId() == R.id.btn5Min) selectedTime = 5;
        };
        btn1Min.setOnClickListener(listener);
        btn3Min.setOnClickListener(listener);
        btn5Min.setOnClickListener(listener);
    }

    private void selectButton(LinearLayout selectedBtn) {
        resetAllButtons();
        selectedBtn.setBackgroundResource(R.drawable.bg_circle_glass_active);
        setButtonTextColor(selectedBtn, "#F48FB1");
        updateButtonScale(selectedBtn);

        cycleCount = 0;
        layoutDots.removeAllViews();
        layoutCycleInfo.setVisibility(View.INVISIBLE);
    }

    private void resetAllButtons() {
        LinearLayout[] buttons = {btn1Min, btn3Min, btn5Min};
        for (LinearLayout btn : buttons) {
            btn.setBackgroundResource(R.drawable.bg_circle_glass_inactive);
            setButtonTextColor(btn, "#80FFFFFF");
        }
    }

    private void setButtonTextColor(LinearLayout layout, String colorHex) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(Color.parseColor(colorHex));
            }
        }
    }

    private void updateButtonScale(View selectedView) {
        View[] buttons = {btn1Min, btn3Min, btn5Min};
        for (View btn : buttons) {
            float scale = (btn == selectedView) ? 1.2f : 1.0f;
            btn.animate().scaleX(scale).scaleY(scale).setDuration(250).start();
        }
    }

    private void startBreathingSession() {
        isRunning = true;
        cycleCount = 0;
        layoutDots.removeAllViews();
        tvCycleCount.setText("");

        btnAction.setText("Dừng lại");
        layoutOptions.setVisibility(View.GONE);

        layoutCycleInfo.setVisibility(View.VISIBLE);
        layoutCycleInfo.setAlpha(1f);

        startCycle("HÍT VÀO", 4);

        mainTimer = new CountDownTimer(selectedTime * 60 * 1000, 1000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override public void onFinish() { stopBreathingSession(); }
        }.start();
    }

    private void startCycle(String type, int seconds) {
        if (!isRunning) return;
        tvStatus.setText(type);

        if (type.equals("HÍT VÀO")) {
            tvInstruction.setText("Hít thật sâu...");
            animateVisuals(1.0f, 1.4f, seconds * 1000);
        } else if (type.equals("THỞ RA")) {
            tvInstruction.setText("Thở ra nhẹ nhàng...");
            animateVisuals(1.4f, 1.0f, seconds * 1000);
        } else {
            tvInstruction.setText("Giữ hơi thở...");
        }

        cycleTimer = new CountDownTimer(seconds * 1000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int timeLeft = (int) (millisUntilFinished / 1000) + 1;
                tvStatus.setText(type + " (" + timeLeft + "s)");
            }

            @Override
            public void onFinish() {
                if (type.equals("HÍT VÀO")) startCycle("GIỮ HƠI THỞ", 2);
                else if (type.equals("GIỮ HƠI THỞ")) startCycle("THỞ RA", 4);
                else {
                    updateCycleUI();
                    startCycle("HÍT VÀO", 4);
                }
            }
        }.start();
    }

    private void updateCycleUI() {
        cycleCount++;
        tvCycleCount.setText("chu kỳ " + cycleCount);

        View dot = new View(this);
        int dotSize = (int) (8 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
        params.setMargins(10, 0, 10, 0);
        dot.setLayoutParams(params);
        dot.setBackgroundResource(R.drawable.dot_indicator);
        layoutDots.addView(dot);
    }

    private void stopBreathingSession() {
        isRunning = false;
        if (mainTimer != null) mainTimer.cancel();
        if (cycleTimer != null) cycleTimer.cancel();

        if (cycleCount > 0) showFinishDialog();

        btnAction.setText("Bắt đầu");
        tvStatus.setText("Sẵn sàng?");
        tvInstruction.setText("Hãy ngồi thoải mái và thư giãn");

        layoutOptions.setVisibility(View.VISIBLE);
        layoutOptions.setAlpha(1f);
        layoutCycleInfo.setVisibility(View.INVISIBLE);

        imgCloud.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
        viewPulseGlow.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
        cpbBreath.setProgress(0);
    }

    private void showFinishDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_finish_breath);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvResult = dialog.findViewById(R.id.tvResultDetail);
        TextView tvFlower = dialog.findViewById(R.id.tvFlowerEmoji);

        tvResult.setText("Bạn đã thực hiện được\n" + cycleCount + " chu kỳ hít thở 💚");

        if (tvFlower != null) {
            ObjectAnimator rotate = ObjectAnimator.ofFloat(tvFlower, "rotation", 0f, 20f, -20f, 0f);
            rotate.setDuration(2000); rotate.setRepeatCount(ObjectAnimator.INFINITE);
            rotate.start();

            PropertyValuesHolder pX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f, 1f);
            PropertyValuesHolder pY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f, 1f);
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(tvFlower, pX, pY);
            pulse.setDuration(2000); pulse.setRepeatCount(ObjectAnimator.INFINITE);
            pulse.start();
        }

        dialog.findViewById(R.id.btnRetry).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void animateVisuals(float from, float to, int duration) {
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, from, to);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, from, to);
        ObjectAnimator.ofPropertyValuesHolder(imgCloud, pvhX, pvhY).setDuration(duration).start();
        ObjectAnimator.ofPropertyValuesHolder(viewPulseGlow, pvhX, pvhY).setDuration(duration).start();
        ObjectAnimator.ofInt(cpbBreath, "progress", from == 1.0f ? 0 : 100, from == 1.0f ? 100 : 0).setDuration(duration).start();
    }
}