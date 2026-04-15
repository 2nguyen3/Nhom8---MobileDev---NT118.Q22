package com.example.heami.ui.therapy;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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

import com.example.heami.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;

public class MusicPlayerActivity extends AppCompatActivity {

    private ImageButton btnPlayPause, btnLike;
    private View imgRotatingDisc, viewPulseGlow, viewDiscRing;
    private ImageView imgTonearm, imgTimerIconMain;
    private SeekBar seekBar;
    private TextView tvStatus, tvTimerStatusMain;
    private LinearLayout layoutTimerOpen, layoutShareOpen;

    private ObjectAnimator discAnimator, discRingAnimator, statusTextAnimator;
    private AnimatorSet pulseSet;

    private boolean isPlaying = false;
    private boolean isLiked = false;
    private long timeLeftInMillis = 0;

    private int selectedTimerMinutes = -1;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Handler seekBarHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private static final String COLOR_TEAL = "#00E5FF";
    private static final String COLOR_PINK = "#F48FB1";
    private static final long SECOND_MS = 1000L;

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
        btnLike = findViewById(R.id.btnLike);
        imgRotatingDisc = findViewById(R.id.viewRotatingDisc);
        viewPulseGlow = findViewById(R.id.viewPulseGlow);
        viewDiscRing = findViewById(R.id.viewDiscRing);
        imgTonearm = findViewById(R.id.imgTonearm);
        seekBar = findViewById(R.id.seekBar);
        tvStatus = findViewById(R.id.tvStatusPlay);
        layoutTimerOpen = findViewById(R.id.layoutTimerAction);
        layoutShareOpen = findViewById(R.id.layoutShareAction);
        tvTimerStatusMain = findViewById(R.id.tvTimerTextMain);
        imgTimerIconMain = findViewById(R.id.imgTimerIconMain);

        findViewById(R.id.btnMinimize).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseMusic();
            } else {
                playMusic();
            }
        });

        if (layoutTimerOpen != null) {
            layoutTimerOpen.setOnClickListener(v -> showTimerBottomSheet());
        }

        if (layoutShareOpen != null) {
            layoutShareOpen.setOnClickListener(v -> showShareBottomSheet());
        }

        btnLike.setOnClickListener(v -> {
            isLiked = !isLiked;
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled);
                btnLike.setColorFilter(Color.parseColor(COLOR_PINK));
                btnLike.setBackgroundResource(R.drawable.bg_music_control_sub_pink);
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline);
                btnLike.clearColorFilter();
                btnLike.setBackgroundResource(R.drawable.bg_music_control_sub);
            }
        });
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

        // Đưa vào mảng để dễ quản lý trạng thái sáng/tối
        LinearLayout[] timerButtons = {btn5, btn15, btn30, btn60, btnInf};

        // Nếu đang có timer chạy, làm sáng ô đó ngay khi mở lên
        if (timeLeftInMillis > 0 || selectedTimerMinutes == 0) {
            applyHighlightToSelected(timerButtons, selectedTimerMinutes);
        }

        if (btnCancel != null) {
            btnCancel.setVisibility(timeLeftInMillis > 0 ? View.VISIBLE : View.GONE);
            btnCancel.setOnClickListener(v -> {
                cancelSleepTimer();
                selectedTimerMinutes = -1;
                dialog.dismiss();
            });
        }

        // Thiết lập sự kiện click cho các ô thời gian
        setupTimerItemClick(btn5, 5, timerButtons, dialog);
        setupTimerItemClick(btn15, 15, timerButtons, dialog);
        setupTimerItemClick(btn30, 30, timerButtons, dialog);
        setupTimerItemClick(btn60, 60, timerButtons, dialog);

        if (btnInf != null) {
            btnInf.setOnClickListener(v -> {
                applyHighlightToSelected(timerButtons, 0); // Vô cực là 0
                selectedTimerMinutes = 0;
                new Handler().postDelayed(() -> {
                    cancelSleepTimer();
                    tvTimerStatusMain.setText("Vô cực");
                    dialog.dismiss();
                }, 200);
            });
        }

        View close = view.findViewById(R.id.btnCloseSheet);
        if (close != null) close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupTimerItemClick(LinearLayout btn, int mins, LinearLayout[] all, BottomSheetDialog dialog) {
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            applyHighlightToSelected(all, mins); // Làm sáng ô vừa chọn
            selectedTimerMinutes = mins;         // Lưu lại lựa chọn

            new Handler().postDelayed(() -> {
                startSleepTimer(mins, dialog);
            }, 200);
        });
    }

    private void applyHighlightToSelected(LinearLayout[] all, int minutes) {
        for (LinearLayout btn : all) {
            if (btn == null) continue;
            // Reset về nền mặc định
            btn.setBackgroundResource(R.drawable.bg_music_control_sub);
            updateItemContentUI(btn, Color.WHITE, false);
        }

        // Tìm ô khớp với số phút để highlight
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
                btn.setBackgroundResource(R.drawable.bg_timer_item_selected_teal);
                updateItemContentUI(btn, Color.parseColor(COLOR_TEAL), true);
            }
        }
    }

    private void updateItemContentUI(LinearLayout layout, int color, boolean isBold) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
                ((TextView) child).setTypeface(null, isBold ? Typeface.BOLD : Typeface.NORMAL);
            } else if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(color);
            }
        }
    }

    private void startSleepTimer(int minutes, BottomSheetDialog dialog) {
        stopTimerHandler();
        timeLeftInMillis = minutes * 60 * SECOND_MS;
        tvTimerStatusMain.setTextColor(Color.parseColor(COLOR_TEAL));
        imgTimerIconMain.setColorFilter(Color.parseColor(COLOR_TEAL));

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && timeLeftInMillis > 0) {
                    timeLeftInMillis -= SECOND_MS;
                    int mins = (int) (timeLeftInMillis / 1000) / 60;
                    int secs = (int) (timeLeftInMillis / 1000) % 60;
                    tvTimerStatusMain.setText(String.format(Locale.getDefault(), "Tắt sau %02d:%02d", mins, secs));
                }
                if (timeLeftInMillis <= 0) {
                    pauseMusic();
                    cancelSleepTimer();
                } else {
                    timerHandler.postDelayed(this, SECOND_MS);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, SECOND_MS);
        dialog.dismiss();
    }

    private void stopTimerHandler() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void cancelSleepTimer() {
        stopTimerHandler();
        timeLeftInMillis = 0;
        tvTimerStatusMain.setText("Hẹn giờ tắt");
        tvTimerStatusMain.setTextColor(Color.WHITE);
        imgTimerIconMain.clearColorFilter();
    }

    private void playMusic() {
        isPlaying = true;
        tvStatus.setText("Đang phát");
        tvStatus.setTextColor(Color.parseColor(COLOR_TEAL));
        btnPlayPause.setImageResource(R.drawable.ic_playing);
        if (viewPulseGlow != null) {
            viewPulseGlow.setVisibility(View.VISIBLE);
            pulseSet.start();
        }
        if (viewDiscRing != null) {
            viewDiscRing.setVisibility(View.VISIBLE);
            discRingAnimator.start();
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
        btnPlayPause.setImageResource(R.drawable.ic_pause);
        if (viewPulseGlow != null) {
            pulseSet.end();
            viewPulseGlow.setVisibility(View.INVISIBLE);
        }
        if (viewDiscRing != null) {
            discRingAnimator.end();
            viewDiscRing.setVisibility(View.INVISIBLE);
        }
        imgTonearm.animate().rotation(-45f).setDuration(500).start();
        discAnimator.pause();
        seekBarHandler.removeCallbacks(updateSeekBarTask);
    }

    private void setupAnimations() {
        discAnimator = ObjectAnimator.ofFloat(imgRotatingDisc, "rotation", 0f, 360f);
        discAnimator.setDuration(10000);
        discAnimator.setRepeatCount(ValueAnimator.INFINITE);
        discAnimator.setInterpolator(new LinearInterpolator());

        if (viewPulseGlow != null) {
            ObjectAnimator sx = ObjectAnimator.ofFloat(viewPulseGlow, "scaleX", 1f, 1.5f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(viewPulseGlow, "scaleY", 1f, 1.5f);
            ObjectAnimator al = ObjectAnimator.ofFloat(viewPulseGlow, "alpha", 0.6f, 0f);
            sx.setRepeatCount(ValueAnimator.INFINITE);
            sx.setRepeatMode(ValueAnimator.REVERSE);
            sy.setRepeatCount(ValueAnimator.INFINITE);
            sy.setRepeatMode(ValueAnimator.REVERSE);
            al.setRepeatCount(ValueAnimator.INFINITE);
            al.setRepeatMode(ValueAnimator.REVERSE);
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

    private void startSeekBarUpdate() {
        seekBarHandler.removeCallbacks(updateSeekBarTask);
        seekBarHandler.postDelayed(updateSeekBarTask, SECOND_MS);
    }

    private final Runnable updateSeekBarTask = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                int p = seekBar.getProgress();
                if (p < seekBar.getMax()) {
                    seekBar.setProgress(p + 1);
                    seekBarHandler.postDelayed(this, SECOND_MS);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimerHandler();
        seekBarHandler.removeCallbacks(updateSeekBarTask);
    }
}