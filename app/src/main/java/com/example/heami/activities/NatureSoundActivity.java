package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;
import java.util.Random;

public class NatureSoundActivity extends AppCompatActivity {

    private final int[] natureSounds = {
            R.raw.birds_singing, R.raw.dry_leaves, R.raw.mountain_stream,
            R.raw.ocean_waves, R.raw.rain_sound, R.raw.singing_bowl, R.raw.summer_night
    };

    private final String[] natureTitles = {
            "Tiếng chim hót", "Lá khô xào xạc", "Suối nguồn tươi trẻ",
            "Sóng biển rì rào", "Tiếng mưa rơi", "Chuông xoay thiền", "Dế mèn đêm hè"
    };

    private MediaPlayer mediaPlayer;
    private ImageButton btnPlayPause, btnNext, btnPrev, btnList, btnShuffle, btnRepeat, btnLike;
    private TextView tvSongTitle, tvStatus, tvTimeTotal, tvTimerStatusMain;
    private SeekBar sbVolume;

    // Đủ 3 biến View cho đĩa
    private View imgDisc, viewPulseGlow, viewDiscRing;
    private ImageView imgTonearm, imgTimerIconMain;
    private LinearLayout layoutTimerOpen, layoutShareOpen;

    // Đủ 3 loại Animator
    private ObjectAnimator discAnimator, discRingAnimator, statusAnimator;
    private AnimatorSet pulseSet;

    private int currentIndex = 4;
    private boolean isPlaying = false, isShuffle = false, isRepeat = false, isLiked = false;
    private long timeLeftInMillis = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nature_sound);

        initViews();
        setupAnimations();
        initMediaPlayer();
        setupListeners();
    }

    private void initViews() {
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnList = findViewById(R.id.btnList);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnLike = findViewById(R.id.btnLike);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvStatus = findViewById(R.id.tvStatusPlay);
        tvTimeTotal = findViewById(R.id.tvTimeTotal);
        sbVolume = findViewById(R.id.seekBar);

        // Ánh xạ đủ 3 View đĩa
        imgDisc = findViewById(R.id.viewRotatingDisc);
        viewPulseGlow = findViewById(R.id.viewPulseGlowNature);
        viewDiscRing = findViewById(R.id.viewDiscRing);

        imgTonearm = findViewById(R.id.imgTonearm);
        layoutTimerOpen = findViewById(R.id.layoutTimerAction);
        layoutShareOpen = findViewById(R.id.layoutShareAction);
        tvTimerStatusMain = findViewById(R.id.tvTimerTextMain);
        imgTimerIconMain = findViewById(R.id.imgTimerIconMain);

        if (tvTimeTotal != null) tvTimeTotal.setText("∞");

        sbVolume.getProgressDrawable().setColorFilter(Color.parseColor("#81C784"), PorterDuff.Mode.SRC_IN);
        sbVolume.getThumb().setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN);
    }

    private void setupAnimations() {
        // 1. Xoay đĩa chính
        discAnimator = ObjectAnimator.ofFloat(imgDisc, "rotation", 0f, 360f);
        discAnimator.setDuration(15000); // Chỉnh 15s cho đằm thắm
        discAnimator.setRepeatCount(ValueAnimator.INFINITE);
        discAnimator.setInterpolator(new LinearInterpolator());

        // 2. Chữ đập nhịp nhàng
        statusAnimator = ObjectAnimator.ofPropertyValuesHolder(tvStatus,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f));
        statusAnimator.setDuration(800);
        statusAnimator.setRepeatCount(ValueAnimator.INFINITE);
        statusAnimator.setRepeatMode(ValueAnimator.REVERSE);

        // 3. Vòng hào quang Pulse
        if (viewPulseGlow != null) {
            ObjectAnimator sx = ObjectAnimator.ofFloat(viewPulseGlow, "scaleX", 1f, 1.5f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(viewPulseGlow, "scaleY", 1f, 1.5f);
            ObjectAnimator al = ObjectAnimator.ofFloat(viewPulseGlow, "alpha", 0.6f, 0f);
            sx.setRepeatCount(ValueAnimator.INFINITE); sx.setRepeatMode(ValueAnimator.REVERSE);
            sy.setRepeatCount(ValueAnimator.INFINITE); sy.setRepeatMode(ValueAnimator.REVERSE);
            al.setRepeatCount(ValueAnimator.INFINITE); al.setRepeatMode(ValueAnimator.REVERSE);
            pulseSet = new AnimatorSet();
            pulseSet.playTogether(sx, sy, al);
            pulseSet.setDuration(1200);
        }

        // 4. Vòng sáng quanh đĩa (Disc Ring)
        if (viewDiscRing != null) {
            discRingAnimator = ObjectAnimator.ofFloat(viewDiscRing, "alpha", 1f, 0.2f);
            discRingAnimator.setDuration(1500);
            discRingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            discRingAnimator.setRepeatMode(ValueAnimator.REVERSE);
        }
    }

    private void playMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(1.0f, 1.0f);
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_playing);
            tvStatus.setText("Đang nghe");
            tvStatus.setTextColor(Color.parseColor("#81C784"));

            // Chạy Status đập nhịp
            statusAnimator.start();

            // Hiệu ứng đĩa xoay
            if (discAnimator.isPaused()) discAnimator.resume(); else discAnimator.start();

            // Hiệu ứng hào quang
            if (viewPulseGlow != null) {
                viewPulseGlow.setVisibility(View.VISIBLE);
                pulseSet.start();
            }

            // Hiệu ứng vòng sáng Ring
            if (viewDiscRing != null) {
                viewDiscRing.setVisibility(View.VISIBLE);
                discRingAnimator.start();
            }

            // Cần kim nhích vào
            imgTonearm.animate().rotation(5f).setDuration(500).start();
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            tvStatus.setText("Đã dừng");
            tvStatus.setTextColor(Color.WHITE);

            // Dừng mọi Animation
            statusAnimator.cancel();
            tvStatus.setScaleX(1f); tvStatus.setScaleY(1f);
            discAnimator.pause();

            if (viewPulseGlow != null) { pulseSet.end(); viewPulseGlow.setVisibility(View.INVISIBLE); }
            if (viewDiscRing != null) { discRingAnimator.end(); viewDiscRing.setVisibility(View.INVISIBLE); }

            // Cần kim nhích ra
            imgTonearm.animate().rotation(-45).setDuration(500).start();
        }
    }

    // --- Giữ nguyên các hàm Listeners, Timer, Share, List ở dưới ---
    private void setupListeners() {
        findViewById(R.id.btnMinimize).setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> { applyClickAnimation(v); toggleSound(); });
        btnNext.setOnClickListener(v -> { applyClickAnimation(v); changeSound(true); });
        btnPrev.setOnClickListener(v -> { applyClickAnimation(v); changeSound(false); });
        btnList.setOnClickListener(v -> { applyClickAnimation(v); showNatureList(); });

        btnShuffle.setOnClickListener(v -> {
            applyClickAnimation(v);
            isShuffle = !isShuffle;
            updateToggleButtonStyle(btnShuffle, isShuffle);
        });

        btnRepeat.setOnClickListener(v -> {
            applyClickAnimation(v);
            isRepeat = !isRepeat;
            if (mediaPlayer != null) mediaPlayer.setLooping(isRepeat);
            updateToggleButtonStyle(btnRepeat, isRepeat);
        });

        btnLike.setOnClickListener(v -> {
            applyClickAnimation(v);
            isLiked = !isLiked;
            btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            btnLike.setColorFilter(isLiked ? Color.parseColor("#F48FB1") : Color.WHITE);
        });

        if (layoutTimerOpen != null) layoutTimerOpen.setOnClickListener(v -> showTimerBottomSheet());
        if (layoutShareOpen != null) layoutShareOpen.setOnClickListener(v -> showShareBottomSheet());

        sbVolume.setMax(100);
        sbVolume.setProgress(100);
        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = (float) progress / 100f;
                if (mediaPlayer != null) mediaPlayer.setVolume(volume, volume);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateToggleButtonStyle(ImageButton btn, boolean isActive) {
        if (isActive) {
            btn.setBackgroundResource(R.drawable.bg_music_control_sub_active);
            btn.setColorFilter(Color.WHITE);
            btn.setAlpha(1.0f);
        } else {
            btn.setBackgroundResource(R.drawable.bg_music_control_sub);
            btn.setColorFilter(Color.parseColor("#B3FFFFFF"));
            btn.setAlpha(0.7f);
        }
    }

    private void applyClickAnimation(View view) {
        view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() ->
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        ).start();
    }

    private void startSleepTimer(int minutes, BottomSheetDialog dialog) {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
        timeLeftInMillis = minutes * 60 * 1000L;
        tvTimerStatusMain.setTextColor(Color.parseColor("#81C784"));
        imgTimerIconMain.setColorFilter(Color.parseColor("#81C784"));

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && timeLeftInMillis > 0) {
                    timeLeftInMillis -= 1000;
                    int mins = (int) (timeLeftInMillis / 1000) / 60;
                    int secs = (int) (timeLeftInMillis / 1000) % 60;
                    tvTimerStatusMain.setText(String.format(Locale.getDefault(), "Tắt sau %02d:%02d", mins, secs));
                    timerHandler.postDelayed(this, 1000);
                } else if (timeLeftInMillis <= 0) {
                    pauseMusic();
                    cancelSleepTimer();
                }
            }
        };
        timerHandler.post(timerRunnable);
        dialog.dismiss();
    }

    private void cancelSleepTimer() {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
        timeLeftInMillis = 0;
        tvTimerStatusMain.setText("Hẹn giờ tắt");
        tvTimerStatusMain.setTextColor(Color.WHITE);
        imgTimerIconMain.clearColorFilter();
    }

    private void showTimerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_timer_bottom_sheet, null);
        dialog.setContentView(view);
        view.findViewById(R.id.btnTimer5).setOnClickListener(v -> startSleepTimer(5, dialog));
        view.findViewById(R.id.btnTimer15).setOnClickListener(v -> startSleepTimer(15, dialog));
        view.findViewById(R.id.btnTimer30).setOnClickListener(v -> startSleepTimer(30, dialog));
        view.findViewById(R.id.btnTimer60).setOnClickListener(v -> startSleepTimer(60, dialog));
        view.findViewById(R.id.btnCancelTimer).setOnClickListener(v -> { cancelSleepTimer(); dialog.dismiss(); });
        view.findViewById(R.id.btnCloseSheet).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showShareBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_share_bottom_sheet, null);
        dialog.setContentView(view);
        view.findViewById(R.id.btnCopyContent).setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("HEAMI", "Nghe nhạc cùng mình nhé!"));
            Toast.makeText(this, "Đã sao chép!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        view.findViewById(R.id.btnCloseShare).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, natureSounds[currentIndex]);
        mediaPlayer.setLooping(isRepeat);
        mediaPlayer.setVolume(1.0f, 1.0f);
        tvSongTitle.setText(natureTitles[currentIndex]);
    }

    private void toggleSound() { if (isPlaying) pauseMusic(); else playMusic(); }

    private void changeSound(boolean next) {
        if (isShuffle && next) currentIndex = new Random().nextInt(natureSounds.length);
        else {
            if (next) currentIndex = (currentIndex + 1) % natureSounds.length;
            else currentIndex = (currentIndex - 1 + natureSounds.length) % natureSounds.length;
        }
        initMediaPlayer();
        if (isPlaying) playMusic();
    }

    private void showNatureList() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_nature_list, null);
        RecyclerView rv = view.findViewById(R.id.rvNatureSounds);
        rv.setLayoutManager(new LinearLayoutManager(this));
        NatureAdapter adapter = new NatureAdapter(natureTitles, currentIndex, position -> {
            currentIndex = position;
            initMediaPlayer();
            playMusic();
            dialog.dismiss();
        });
        rv.setAdapter(adapter);
        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }
}