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
    private TextView tvSongTitle, tvStatus, tvTimeTotal, tvTimeCurrent, tvTimerStatusMain;
    private SeekBar sbProgress;

    private View imgDisc, viewPulseGlow, viewDiscRing;
    private ImageView imgTonearm, imgTimerIconMain;
    private LinearLayout layoutTimerOpen, layoutShareOpen;

    private ObjectAnimator discAnimator, discRingAnimator, statusAnimator;
    private AnimatorSet pulseSet;

    private int currentIndex = 4;
    private boolean isPlaying = false, isShuffle = false, isRepeat = false, isLiked = false;
    private long timeLeftInMillis = 0;
    private int selectedTimerMinutes = -1;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private final Handler seekBarHandler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarTask;

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
        tvTimeCurrent = findViewById(R.id.tvTimeCurrent);
        sbProgress = findViewById(R.id.seekBar);

        imgDisc = findViewById(R.id.viewRotatingDisc);
        viewPulseGlow = findViewById(R.id.viewPulseGlowNature);
        viewDiscRing = findViewById(R.id.viewDiscRing);

        imgTonearm = findViewById(R.id.imgTonearm);
        layoutTimerOpen = findViewById(R.id.layoutTimerAction);
        layoutShareOpen = findViewById(R.id.layoutShareAction);
        tvTimerStatusMain = findViewById(R.id.tvTimerTextMain);
        imgTimerIconMain = findViewById(R.id.imgTimerIconMain);

        // FIX: Không set progress(100) ở đây nữa để tránh bị đầy lúc mới vào
        sbProgress.getProgressDrawable().setColorFilter(Color.parseColor("#81C784"), PorterDuff.Mode.SRC_IN);
        sbProgress.getThumb().setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN);
    }

    private void setupAnimations() {
        discAnimator = ObjectAnimator.ofFloat(imgDisc, "rotation", 0f, 360f);
        discAnimator.setDuration(15000);
        discAnimator.setRepeatCount(ValueAnimator.INFINITE);
        discAnimator.setInterpolator(new LinearInterpolator());

        statusAnimator = ObjectAnimator.ofPropertyValuesHolder(tvStatus,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f));
        statusAnimator.setDuration(800);
        statusAnimator.setRepeatCount(ValueAnimator.INFINITE);
        statusAnimator.setRepeatMode(ValueAnimator.REVERSE);

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

        if (viewDiscRing != null) {
            discRingAnimator = ObjectAnimator.ofFloat(viewDiscRing, "alpha", 1f, 0.2f);
            discRingAnimator.setDuration(1500);
            discRingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            discRingAnimator.setRepeatMode(ValueAnimator.REVERSE);
        }
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, natureSounds[currentIndex]);
        mediaPlayer.setLooping(isRepeat);

        tvSongTitle.setText(natureTitles[currentIndex]);

        // FIX: Cập nhật Max và Progress về 0 khi khởi tạo bài mới
        int duration = mediaPlayer.getDuration();
        sbProgress.setMax(duration);
        sbProgress.setProgress(0);

        if (tvTimeCurrent != null) tvTimeCurrent.setText("00:00");
        if (tvTimeTotal != null) tvTimeTotal.setText(formatTime(duration));

        mediaPlayer.setOnCompletionListener(mp -> {
            if (!isRepeat) changeSound(true);
        });
    }

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

        // FIX: Thay đổi logic từ Volume sang SeekTo (Tua nhạc)
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    if (tvTimeCurrent != null) tvTimeCurrent.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void startSeekBarUpdate() {
        seekBarHandler.removeCallbacks(updateSeekBarTask);
        updateSeekBarTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    int currentPos = mediaPlayer.getCurrentPosition();
                    sbProgress.setProgress(currentPos); // Chạy SeekBar theo nhạc
                    if (tvTimeCurrent != null) tvTimeCurrent.setText(formatTime(currentPos));
                    seekBarHandler.postDelayed(this, 1000);
                }
            }
        };
        seekBarHandler.post(updateSeekBarTask);
    }

    private void playMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_playing);
            tvStatus.setText("Đang nghe");
            tvStatus.setTextColor(Color.parseColor("#81C784"));

            statusAnimator.start();
            if (discAnimator.isPaused()) discAnimator.resume(); else discAnimator.start();

            if (viewPulseGlow != null) {
                viewPulseGlow.setVisibility(View.VISIBLE);
                pulseSet.start();
            }
            if (viewDiscRing != null) {
                viewDiscRing.setVisibility(View.VISIBLE);
                discRingAnimator.start();
            }

            imgTonearm.animate().rotation(5f).setDuration(500).start();
            startSeekBarUpdate();
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            tvStatus.setText("Đã dừng");
            tvStatus.setTextColor(Color.WHITE);

            statusAnimator.cancel();
            tvStatus.setScaleX(1f); tvStatus.setScaleY(1f);
            discAnimator.pause();

            if (viewPulseGlow != null) { pulseSet.end(); viewPulseGlow.setVisibility(View.INVISIBLE); }
            if (viewDiscRing != null) { discRingAnimator.end(); viewDiscRing.setVisibility(View.INVISIBLE); }

            imgTonearm.animate().rotation(-45).setDuration(500).start();
            seekBarHandler.removeCallbacks(updateSeekBarTask);
        }
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

        LinearLayout btn5 = view.findViewById(R.id.btnTimer5);
        LinearLayout btn15 = view.findViewById(R.id.btnTimer15);
        LinearLayout btn30 = view.findViewById(R.id.btnTimer30);
        LinearLayout btn60 = view.findViewById(R.id.btnTimer60);
        LinearLayout btnInf = view.findViewById(R.id.btnTimerInfinite);
        LinearLayout btnCancel = view.findViewById(R.id.btnCancelTimer);

        LinearLayout[] allBtns = {btn5, btn15, btn30, btn60, btnInf};

        if (timeLeftInMillis > 0 || selectedTimerMinutes == 0) applyHighlight(allBtns, selectedTimerMinutes);

        if (btnCancel != null) {
            btnCancel.setVisibility(timeLeftInMillis > 0 ? View.VISIBLE : View.GONE);
            btnCancel.setOnClickListener(v -> {
                cancelSleepTimer();
                selectedTimerMinutes = -1;
                dialog.dismiss();
            });
        }

        setupTimerItemClick(btn5, 5, allBtns, dialog);
        setupTimerItemClick(btn15, 15, allBtns, dialog);
        setupTimerItemClick(btn30, 30, allBtns, dialog);
        setupTimerItemClick(btn60, 60, allBtns, dialog);

        if (btnInf != null) {
            btnInf.setOnClickListener(v -> {
                applyHighlight(allBtns, 0);
                selectedTimerMinutes = 0;
                new Handler().postDelayed(() -> {
                    cancelSleepTimer();
                    tvTimerStatusMain.setText("Vô cực");
                    dialog.dismiss();
                }, 200);
            });
        }
        view.findViewById(R.id.btnCloseSheet).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupTimerItemClick(LinearLayout btn, int mins, LinearLayout[] all, BottomSheetDialog dialog) {
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            applyHighlight(all, mins);
            selectedTimerMinutes = mins;
            new Handler().postDelayed(() -> startSleepTimer(mins, dialog), 200);
        });
    }

    private void applyHighlight(LinearLayout[] all, int minutes) {
        for (LinearLayout btn : all) {
            if (btn == null) continue;
            btn.setBackgroundResource(R.drawable.bg_music_control_sub);
            updateItemUI(btn, Color.parseColor("#B3FFFFFF"), false);
        }
        for (LinearLayout btn : all) {
            if (btn == null) continue;
            boolean match = false;
            int id = btn.getId();
            if (minutes == 5 && id == R.id.btnTimer5) match = true;
            else if (minutes == 15 && id == R.id.btnTimer15) match = true;
            else if (minutes == 30 && id == R.id.btnTimer30) match = true;
            else if (minutes == 60 && id == R.id.btnTimer60) match = true;
            else if (minutes == 0 && id == R.id.btnTimerInfinite) match = true;

            if (match) {
                btn.setBackgroundResource(R.drawable.bg_timer_item_selected_green);
                updateItemUI(btn, Color.parseColor("#81C784"), true);
            }
        }
    }

    private void updateItemUI(LinearLayout layout, int color, boolean isBold) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(color);
                ((TextView) v).setTypeface(null, isBold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            } else if (v instanceof ImageView) {
                ((ImageView) v).setColorFilter(color);
            }
        }
    }

    private void showShareBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_share_bottom_sheet, null);
        dialog.setContentView(view);

        String shareMsg = "Đang nghe nhạc cực chill trên HEAMI! Nghe cùng mình nhé.";

        if (view.findViewById(R.id.bg_social_zalo) != null) {
            view.findViewById(R.id.bg_social_zalo).setOnClickListener(v -> {
                shareToApp("com.zing.zalo", shareMsg);
                dialog.dismiss();
            });
        }
        if (view.findViewById(R.id.bg_social_mess) != null) {
            view.findViewById(R.id.bg_social_mess).setOnClickListener(v -> {
                shareToApp("com.facebook.orca", shareMsg);
                dialog.dismiss();
            });
        }
        if (view.findViewById(R.id.bg_social_insta) != null) {
            view.findViewById(R.id.bg_social_insta).setOnClickListener(v -> {
                shareToApp("com.instagram.android", shareMsg);
                dialog.dismiss();
            });
        }
        if (view.findViewById(R.id.bg_social_threads) != null) {
            view.findViewById(R.id.bg_social_threads).setOnClickListener(v -> {
                shareToApp("com.instagram.barcelona", shareMsg);
                dialog.dismiss();
            });
        }

        View copy = view.findViewById(R.id.btnCopyContent);
        if (copy != null) {
            copy.setOnClickListener(v -> {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("HEAMI", shareMsg));
                Toast.makeText(this, "Đã sao chép nội dung!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        View close = view.findViewById(R.id.btnCloseShare);
        if (close != null) {
            close.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void shareToApp(String packageName, String msg) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, msg);
        intent.setPackage(packageName);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Ứng dụng chưa được cài đặt!", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTime(int ms) {
        int m = (ms / 1000) / 60;
        int s = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
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
        timerHandler.removeCallbacks(timerRunnable);
        seekBarHandler.removeCallbacks(updateSeekBarTask);
    }
}