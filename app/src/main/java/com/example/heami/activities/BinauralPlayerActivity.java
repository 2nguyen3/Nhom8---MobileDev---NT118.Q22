package com.example.heami.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

public class BinauralPlayerActivity extends AppCompatActivity {

    private ImageButton btnPlayPause, btnLike, btnMinimize, btnList;
    private View viewRotatingDisc, viewPulseGlow, viewDiscRing;
    private ImageView imgTonearm, imgTimerIconMain;
    private SeekBar seekBar;
    private TextView tvStatusPlay, tvTimerTextMain;
    private LinearLayout layoutTimerAction, layoutShareAction;

    private ObjectAnimator discAnimator, discRingAnimator;
    private AnimatorSet pulseSet;
    private ObjectAnimator statusFadeAnimator;
    private boolean isPlaying = false;
    private boolean isLiked = false;
    private long timeLeftInMillis = 0;
    private int selectedTimerMinutes = -1;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Handler seekBarHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private static final String COLOR_PINK = "#F48FB1";
    private static final long SECOND_MS = 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binaural);

        initViews();
        setupAnimations();
        setupListeners();
    }

    private void initViews() {
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnLike = findViewById(R.id.btnLike);
        btnMinimize = findViewById(R.id.btnMinimize);
        btnList = findViewById(R.id.btnList);

        viewRotatingDisc = findViewById(R.id.viewRotatingDisc);
        viewPulseGlow = findViewById(R.id.viewPulseGlow);
        viewDiscRing = findViewById(R.id.viewDiscRing);
        imgTonearm = findViewById(R.id.imgTonearm);

        seekBar = findViewById(R.id.seekBar);
        tvStatusPlay = findViewById(R.id.tvStatusPlay);

        layoutTimerAction = findViewById(R.id.layoutTimerAction);
        layoutShareAction = findViewById(R.id.layoutShareAction);
        tvTimerTextMain = findViewById(R.id.tvTimerTextMain);
        imgTimerIconMain = findViewById(R.id.imgTimerIconMain);

        btnMinimize.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseMusic();
            } else {
                playMusic();
            }
        });

        if (layoutTimerAction != null) {
            layoutTimerAction.setOnClickListener(v -> showTimerBottomSheet());
        }

        if (layoutShareAction != null) {
            layoutShareAction.setOnClickListener(v -> showShareBottomSheet());
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

        setShareClickListener(view, R.id.bg_social_zalo, "com.zing.zalo", shareMsg, dialog);
        setShareClickListener(view, R.id.bg_social_mess, "com.facebook.orca", shareMsg, dialog);
        setShareClickListener(view, R.id.bg_social_insta, "com.instagram.android", shareMsg, dialog);
        setShareClickListener(view, R.id.bg_social_threads, "com.instagram.barcelona", shareMsg, dialog);

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

    private void setShareClickListener(View root, int viewId, String pkg, String msg, BottomSheetDialog dialog) {
        View v = root.findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(view -> {
                shareToApp(pkg, msg);
                dialog.dismiss();
            });
        }
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

        // Ánh xạ các ô chọn
        LinearLayout btn5 = view.findViewById(R.id.btnTimer5);
        LinearLayout btn15 = view.findViewById(R.id.btnTimer15);
        LinearLayout btn30 = view.findViewById(R.id.btnTimer30);
        LinearLayout btn60 = view.findViewById(R.id.btnTimer60);
        LinearLayout btnInf = view.findViewById(R.id.btnTimerInfinite);
        LinearLayout btnCancel = view.findViewById(R.id.btnCancelTimer);

        // Đưa các nút vào mảng để duyệt cho nhanh
        LinearLayout[] timerButtons = {btn5, btn15, btn30, btn60, btnInf};

        // Kiểm tra xem có đang chạy timer không để highlight ô cũ
        if (timeLeftInMillis > 0 || selectedTimerMinutes == 0) {
            highlightSelectedButton(timerButtons, selectedTimerMinutes);
        }

        if (btnCancel != null) {
            btnCancel.setVisibility(timeLeftInMillis > 0 ? View.VISIBLE : View.GONE);
            btnCancel.setOnClickListener(v -> { cancelSleepTimer(); selectedTimerMinutes = -1; dialog.dismiss(); });
        }

        // Gán sự kiện click cho từng nút
        setupTimerButtonClick(btn5, 5, timerButtons, dialog);
        setupTimerButtonClick(btn15, 15, timerButtons, dialog);
        setupTimerButtonClick(btn30, 30, timerButtons, dialog);
        setupTimerButtonClick(btn60, 60, timerButtons, dialog);

        if (btnInf != null) {
            btnInf.setOnClickListener(v -> {
                highlightSelectedButton(timerButtons, 0); // 0 coi như vô cực
                selectedTimerMinutes = 0;
                new Handler().postDelayed(() -> { // Delay xíu cho user thấy hiệu ứng highlight
                    cancelSleepTimer();
                    tvTimerTextMain.setText("Vô cực");
                    dialog.dismiss();
                }, 200);
            });
        }

        view.findViewById(R.id.btnCloseSheet).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Hàm hỗ trợ Click và Highlight
    private void setupTimerButtonClick(LinearLayout btn, int mins, LinearLayout[] allBtns, BottomSheetDialog dialog) {
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            highlightSelectedButton(allBtns, mins);
            selectedTimerMinutes = mins;

            // Delay 200ms để người dùng kịp thấy ô đó sáng lên rồi mới đóng Dialog
            new Handler().postDelayed(() -> {
                startSleepTimer(mins, dialog);
            }, 200);
        });
    }

    // Hàm thực hiện việc đổi màu viền/sáng khung
    private void highlightSelectedButton(LinearLayout[] allBtns, int mins) {
        for (LinearLayout btn : allBtns) {
            if (btn == null) continue;
            // Reset về background mặc định (bg_music_control_sub)
            btn.setBackgroundResource(R.drawable.bg_music_control_sub);
            // Reset màu chữ/icon bên trong về trắng
            updateTimerItemUI(btn, Color.WHITE);
        }

        // Tìm đúng nút để làm sáng
        for (LinearLayout btn : allBtns) {
            if (btn == null) continue;
            // Bạn có thể dùng Tag trong XML hoặc so sánh ID để tìm nút
            // Ở đây mình check theo logic minutes đơn giản
            if ((mins == 5 && btn.getId() == R.id.btnTimer5) ||
                    (mins == 15 && btn.getId() == R.id.btnTimer15) ||
                    (mins == 30 && btn.getId() == R.id.btnTimer30) ||
                    (mins == 60 && btn.getId() == R.id.btnTimer60) ||
                    (mins == 0 && btn.getId() == R.id.btnTimerInfinite)) {

                btn.setBackgroundResource(R.drawable.bg_timer_item_selected);
                updateTimerItemUI(btn, Color.parseColor(COLOR_PINK)); // Chữ & Icon thành hồng
            }
        }
    }

    // Hàm phụ để đổi màu cả TextView và ImageView bên trong ô cho đồng bộ
    private void updateTimerItemUI(LinearLayout layout, int color) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof TextView) ((TextView) v).setTextColor(color);
            if (v instanceof ImageView) ((ImageView) v).setColorFilter(color);
        }
    }

    private void startSleepTimer(int minutes, BottomSheetDialog dialog) {
        stopTimerHandler();
        timeLeftInMillis = minutes * 60 * SECOND_MS;
        tvTimerTextMain.setTextColor(Color.parseColor(COLOR_PINK));
        imgTimerIconMain.setColorFilter(Color.parseColor(COLOR_PINK));

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && timeLeftInMillis > 0) {
                    timeLeftInMillis -= SECOND_MS;
                    int mins = (int) (timeLeftInMillis / 1000) / 60;
                    int secs = (int) (timeLeftInMillis / 1000) % 60;
                    tvTimerTextMain.setText(String.format(Locale.getDefault(), "Tắt sau %02d:%02d", mins, secs));
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
        tvTimerTextMain.setText("Hẹn giờ tắt");
        tvTimerTextMain.setTextColor(Color.WHITE);
        imgTimerIconMain.clearColorFilter();
    }

    private void playMusic() {
        isPlaying = true;
        tvStatusPlay.setText("Đang phát");
        tvStatusPlay.setTextColor(Color.parseColor(COLOR_PINK));

        if (statusFadeAnimator != null) {
            statusFadeAnimator.start();
        }

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
        tvStatusPlay.setText("Đã dừng");
        tvStatusPlay.setTextColor(Color.WHITE);

        if (statusFadeAnimator != null) {
            statusFadeAnimator.cancel();
            tvStatusPlay.setAlpha(1.0f);
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

        imgTonearm.animate().rotation(-45f).setDuration(500).start();
        discAnimator.pause();
        seekBarHandler.removeCallbacks(updateSeekBarTask);
    }

    private void setupAnimations() {
        discAnimator = ObjectAnimator.ofFloat(viewRotatingDisc, "rotation", 0f, 360f);
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

        statusFadeAnimator = ObjectAnimator.ofFloat(tvStatusPlay, "alpha", 1.0f, 0.4f);
        statusFadeAnimator.setDuration(1000);
        statusFadeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        statusFadeAnimator.setRepeatMode(ValueAnimator.REVERSE);
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