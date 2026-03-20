package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.heami.R;

public class MusicPlayerActivity extends AppCompatActivity {

    private ImageButton btnPlayPause, btnMinimize, btnLike;
    private View imgRotatingDisc, viewPulseGlow, viewDiscRing, viewStatusDot;
    private ImageView imgTonearm;
    private SeekBar seekBar;
    private TextView tvStatus;

    private ObjectAnimator discAnimator, discRingAnimator, statusTextAnimator;
    private AnimatorSet pulseSet, dotPulseSet;

    private boolean isPlaying = false;
    private boolean isLiked = false;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        initViews();
        setupAnimations();
        setupListeners();
    }

    private void initViews() {
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnMinimize = findViewById(R.id.btnMinimize);
        btnLike = findViewById(R.id.btnLike);
        imgRotatingDisc = findViewById(R.id.viewRotatingDisc);
        viewPulseGlow = findViewById(R.id.viewPulseGlow);
        viewDiscRing = findViewById(R.id.viewDiscRing);
        viewStatusDot = findViewById(R.id.viewStatusDot);
        imgTonearm = findViewById(R.id.imgTonearm);
        seekBar = findViewById(R.id.seekBar);
        tvStatus = findViewById(R.id.tvStatusPlay);
    }

    private void setupAnimations() {
        discAnimator = ObjectAnimator.ofFloat(imgRotatingDisc, "rotation", 0f, 360f);
        discAnimator.setDuration(10000);
        discAnimator.setRepeatCount(ValueAnimator.INFINITE);
        discAnimator.setInterpolator(new LinearInterpolator());

        if (viewPulseGlow != null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(viewPulseGlow, "scaleX", 1f, 1.5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(viewPulseGlow, "scaleY", 1f, 1.5f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(viewPulseGlow, "alpha", 0.8f, 0f);
            setInfiniteReverse(scaleX, scaleY, alpha);
            pulseSet = new AnimatorSet();
            pulseSet.playTogether(scaleX, scaleY, alpha);
            pulseSet.setDuration(1200);
            pulseSet.setInterpolator(new AccelerateDecelerateInterpolator());
        }

        if (viewDiscRing != null) {
            discRingAnimator = ObjectAnimator.ofFloat(viewDiscRing, "alpha", 1f, 0.2f);
            discRingAnimator.setDuration(1500);
            discRingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            discRingAnimator.setRepeatMode(ValueAnimator.REVERSE);
        }

        if (viewStatusDot != null) {
            ObjectAnimator dotX = ObjectAnimator.ofFloat(viewStatusDot, "scaleX", 1f, 1.6f);
            ObjectAnimator dotY = ObjectAnimator.ofFloat(viewStatusDot, "scaleY", 1f, 1.6f);
            setInfiniteReverse(dotX, dotY);
            dotPulseSet = new AnimatorSet();
            dotPulseSet.playTogether(dotX, dotY);
            dotPulseSet.setDuration(700);
        }
    }

    private void setupListeners() {
        btnMinimize.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) pauseMusic(); else playMusic();
        });

        btnLike.setOnClickListener(v -> {
            isLiked = !isLiked;
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled);
                btnLike.setColorFilter(Color.parseColor("#F48FB1"));
                btnLike.setBackgroundResource(R.drawable.bg_music_control_sub_pink);
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline);
                btnLike.clearColorFilter();
                btnLike.setBackgroundResource(R.drawable.bg_music_control_sub);
            }
        });
    }

    private void playMusic() {
        isPlaying = true;

        tvStatus.setText("Đang phát");
        tvStatus.setTextColor(Color.parseColor("#4DA7FFEB"));
        statusTextAnimator = ObjectAnimator.ofPropertyValuesHolder(tvStatus,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f));
        statusTextAnimator.setDuration(800);
        statusTextAnimator.setRepeatCount(ValueAnimator.INFINITE);
        statusTextAnimator.setRepeatMode(ValueAnimator.REVERSE);
        statusTextAnimator.start();

        btnPlayPause.setImageResource(R.drawable.ic_playing);

        if (viewPulseGlow != null) {
            viewPulseGlow.setVisibility(View.VISIBLE);
            viewPulseGlow.setAlpha(0.8f);
            pulseSet.start();
        }
        if (viewDiscRing != null) {
            viewDiscRing.setVisibility(View.VISIBLE);
            discRingAnimator.start();
        }
        if (viewStatusDot != null) {
            viewStatusDot.setVisibility(View.VISIBLE);
            dotPulseSet.start();
        }

        imgTonearm.animate().rotation(5f).setDuration(500).start();

        if (discAnimator.isPaused()) {
            discAnimator.resume();
        } else {
            discAnimator.start();
        }

        startSeekBarUpdate();
    }

    private void pauseMusic() {
        isPlaying = false;

        tvStatus.setText("Đã dừng");
        tvStatus.setTextColor(Color.WHITE);
        if (statusTextAnimator != null) {
            statusTextAnimator.cancel();
            tvStatus.setScaleX(1f);
            tvStatus.setScaleY(1f);
        }

        btnPlayPause.setImageResource(R.drawable.ic_pause);

        if (viewPulseGlow != null) {
            pulseSet.end();
            viewPulseGlow.setVisibility(View.INVISIBLE);
        }
        if (viewDiscRing != null) {
            discRingAnimator.end();
            viewDiscRing.setVisibility(View.INVISIBLE);
        }
        if (viewStatusDot != null) {
            dotPulseSet.end();
            viewStatusDot.setVisibility(View.INVISIBLE);
        }

        imgTonearm.animate().rotation(-45f).setDuration(500).start();
        discAnimator.pause();
        handler.removeCallbacks(updateSeekBarTask);
    }

    private void setInfiniteReverse(ObjectAnimator... anims) {
        for (ObjectAnimator anim : anims) {
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setRepeatMode(ValueAnimator.REVERSE);
        }
    }

    private void startSeekBarUpdate() {
        handler.removeCallbacks(updateSeekBarTask);
        handler.postDelayed(updateSeekBarTask, 1000);
    }

    private final Runnable updateSeekBarTask = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                int progress = seekBar.getProgress();
                if (progress < seekBar.getMax()) {
                    seekBar.setProgress(progress + 1);
                    handler.postDelayed(this, 1000);
                }
            }
        }
    };
}