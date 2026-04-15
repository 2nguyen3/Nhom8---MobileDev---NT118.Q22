package com.example.heami.ui.therapy;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;

public class PodcastActivity extends AppCompatActivity {

    private ImageButton btnPlayPause, btnForward15, btnReplay15, btnPlaylist, btnMinimize, btnTimer;
    private CardView cardPlayPause;
    private SeekBar sbPodcast;
    private TextView tvTitle, tvAuthor, tvCurrentTime, tvTotalTime, tvStatusPlay, tvQuote, tvSkipFeedback, btnSpeed;

    private View[] visualizerBars = new View[5];
    private Handler visualizerHandler = new Handler();
    private boolean isVisualizerRunning = false;

    private final String COLOR_ORANGE = "#FFB74D";
    private final int SECOND_MS = 1000;

    private long timeLeftInMillis = 0;
    private int selectedMinutes = 0;
    private Runnable timerRunnable;
    private Handler timerHandler = new Handler();

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private BottomSheetDialog bottomSheetDialog;

    private float currentSpeed = 1.0f;
    private int currentIndex = 4;

    private final int[] songs = {
            R.raw.tu_om_lay_chinh_minh,
            R.raw.cam_on_vi_da_hien_dien,
            R.raw.dung_buon_nua_hay_vui_len,
            R.raw.co_con_nguoi_song_ma_nhu_qua_doi,
            R.raw.neu_ca_doi_khong_ruc_ro
    };

    private final String[] titles = {
            "Tự ôm lấy chính mình",
            "Cảm ơn vì đã hiện diện",
            "Đừng buồn nữa, hãy vui lên",
            "Có con người sống mà như qua đời",
            "Nếu cả đời không rực rỡ thì sao"
    };

    private final String[] quotes = {
            "“Hãy cứ bao dung với chính mình như cách bạn làm với người khác.”",
            "“Mỗi sự hiện diện đều là một món quà vô giá của cuộc đời.”",
            "“Nỗi buồn là một phần của sự trưởng thành, hãy cứ để nó trôi qua.”",
            "“Đừng chỉ tồn tại, hãy học cách sống thật rực rỡ từ bên trong.”",
            "“Đôi khi không rực rỡ, lại là một vẻ đẹp bình yên nhất.”"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_podcast_player);

        initViews();
        setupPlayer(currentIndex);

        btnMinimize.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            applyClickAnimation(cardPlayPause);
            if (mediaPlayer != null && mediaPlayer.isPlaying()) pausePodcast();
            else playPodcast();
        });

        btnForward15.setOnClickListener(v -> {
            applyClickAnimation(v);
            showSkipFeedback("+15s");
            if (mediaPlayer != null) {
                int target = mediaPlayer.getCurrentPosition() + 15000;
                mediaPlayer.seekTo(Math.min(target, mediaPlayer.getDuration()));
            }
        });

        btnReplay15.setOnClickListener(v -> {
            applyClickAnimation(v);
            showSkipFeedback("-15s");
            if (mediaPlayer != null) {
                int target = mediaPlayer.getCurrentPosition() - 15000;
                mediaPlayer.seekTo(Math.max(target, 0));
            }
        });

        btnSpeed.setOnClickListener(v -> {
            applyClickAnimation(v);
            changePlayerSpeed();
        });

        btnTimer.setOnClickListener(v -> {
            applyClickAnimation(v);
            showTimerBottomSheet();
        });

        btnPlaylist.setOnClickListener(v -> {
            applyClickAnimation(v);
            showPlaylistDialog();
        });

        sbPodcast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            currentIndex = (currentIndex + 1) % songs.length;
            setupPlayer(currentIndex);
        });
    }

    private void initViews() {
        btnMinimize = findViewById(R.id.btnMinimize);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        cardPlayPause = findViewById(R.id.cardPlayPause);
        btnForward15 = findViewById(R.id.btnForward15);
        btnReplay15 = findViewById(R.id.btnReplay15);
        btnPlaylist = findViewById(R.id.btnPlaylist);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnTimer = findViewById(R.id.btnTimer);

        sbPodcast = findViewById(R.id.sbPodcast);
        tvTitle = findViewById(R.id.tvPodcastTitle);
        tvAuthor = findViewById(R.id.tvPodcastAuthor);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvStatusPlay = findViewById(R.id.tvStatusPlay);
        tvQuote = findViewById(R.id.tvQuote);
        tvSkipFeedback = findViewById(R.id.tvSkipFeedback);

        visualizerBars[0] = findViewById(R.id.bar1);
        visualizerBars[1] = findViewById(R.id.bar2);
        visualizerBars[2] = findViewById(R.id.bar3);
        visualizerBars[3] = findViewById(R.id.bar4);
        visualizerBars[4] = findViewById(R.id.bar5);
    }

    private void showTimerBottomSheet() {
        BottomSheetDialog timerDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_timer_bottom_sheet, null);
        timerDialog.setContentView(view);

        LinearLayout btn5 = view.findViewById(R.id.btnTimer5);
        LinearLayout btn15 = view.findViewById(R.id.btnTimer15);
        LinearLayout btn30 = view.findViewById(R.id.btnTimer30);
        LinearLayout btn60 = view.findViewById(R.id.btnTimer60);
        LinearLayout btnCancel = view.findViewById(R.id.btnCancelTimer);
        TextView tvCancelText = view.findViewById(R.id.tvCancelTimerText);

        if (timeLeftInMillis > 0) {
            btnCancel.setVisibility(View.VISIBLE);

            if (selectedMinutes == 5) highlightTimerItem(btn5);
            else if (selectedMinutes == 15) highlightTimerItem(btn15);
            else if (selectedMinutes == 30) highlightTimerItem(btn30);
            else if (selectedMinutes == 60) highlightTimerItem(btn60);

            final Handler dialogHandler = new Handler();
            dialogHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (timeLeftInMillis > 0 && timerDialog.isShowing()) {
                        int m = (int) (timeLeftInMillis / 1000) / 60;
                        int s = (int) (timeLeftInMillis / 1000) % 60;
                        tvCancelText.setText(String.format(Locale.getDefault(), "Hủy hẹn giờ (%02d:%02d)", m, s));
                        dialogHandler.postDelayed(this, 1000);
                    }
                }
            });
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        btn5.setOnClickListener(v -> { selectedMinutes = 5; startSleepTimer(5); timerDialog.dismiss(); });
        btn15.setOnClickListener(v -> { selectedMinutes = 15; startSleepTimer(15); timerDialog.dismiss(); });
        btn30.setOnClickListener(v -> { selectedMinutes = 30; startSleepTimer(30); timerDialog.dismiss(); });
        btn60.setOnClickListener(v -> { selectedMinutes = 60; startSleepTimer(60); timerDialog.dismiss(); });

        btnCancel.setOnClickListener(v -> {
            cancelSleepTimer();
            selectedMinutes = 0;
            timerDialog.dismiss();
        });

        timerDialog.show();
    }

    private void startSleepTimer(int minutes) {
        stopTimerHandler();
        timeLeftInMillis = (long) minutes * 60 * SECOND_MS;

        btnTimer.setColorFilter(Color.parseColor(COLOR_ORANGE));
        btnTimer.setAlpha(1.0f);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && timeLeftInMillis > 0) {
                    timeLeftInMillis -= SECOND_MS;
                    timerHandler.postDelayed(this, SECOND_MS);
                }

                if (timeLeftInMillis <= 0) {
                    pausePodcast();
                    cancelSleepTimer();
                    selectedMinutes = 0;
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, SECOND_MS);
        Toast.makeText(this, "Hẹn giờ tắt sau " + minutes + " phút", Toast.LENGTH_SHORT).show();
    }

    private void highlightTimerItem(View view) {
        view.setBackgroundResource(R.drawable.bg_timer_item_selected);

        if (view instanceof LinearLayout) {
            LinearLayout l = (LinearLayout) view;

            for (int i = 0; i < l.getChildCount(); i++) {
                View child = l.getChildAt(i);

                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(Color.WHITE);
                }

                if (child instanceof ImageView) {
                    ((ImageView) child).setColorFilter(Color.WHITE);
                }
            }
        }
    }

    private void cancelSleepTimer() {
        stopTimerHandler();
        timeLeftInMillis = 0;
        btnTimer.clearColorFilter();
        btnTimer.setAlpha(0.6f);
    }

    private void stopTimerHandler() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void setupPlayer(int index) {
        if (mediaPlayer != null) mediaPlayer.release();

        mediaPlayer = MediaPlayer.create(this, songs[index]);

        tvTitle.setText(titles[index]);
        tvQuote.setText(quotes[index]);
        tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));

        sbPodcast.setMax(mediaPlayer.getDuration());
        sbPodcast.setProgress(0);
        tvCurrentTime.setText("00:00");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer.setPlaybackParams(
                    mediaPlayer.getPlaybackParams().setSpeed(currentSpeed)
            );
        }

        playPodcast();
    }

    private void playPodcast() {
        if (mediaPlayer != null) {
            mediaPlayer.start();

            btnPlayPause.setImageResource(R.drawable.ic_playing);
            tvStatusPlay.setText("Đang phát...");
            tvStatusPlay.setTextColor(Color.parseColor(COLOR_ORANGE));

            startBlinkAnimation(tvStatusPlay);
            startVisualizer();
            updateSeekBar();
        }
    }

    private void pausePodcast() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();

            btnPlayPause.setImageResource(R.drawable.ic_pause);
            tvStatusPlay.setText("Đã dừng");
            tvStatusPlay.setTextColor(Color.WHITE);
            tvStatusPlay.clearAnimation();

            stopVisualizer();
        }
    }

    private void applyClickAnimation(View view) {
        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(100)
                .withEndAction(() ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                ).start();
    }

    private void showSkipFeedback(String text) {
        tvSkipFeedback.setText(text);
        tvSkipFeedback.setVisibility(View.VISIBLE);

        tvSkipFeedback.setAlpha(1f);
        tvSkipFeedback.setTranslationY(0f);

        tvSkipFeedback.animate()
                .translationY(-120f)
                .alpha(0f)
                .setDuration(600)
                .withEndAction(() -> tvSkipFeedback.setVisibility(View.INVISIBLE))
                .start();
    }

    private void startVisualizer() {
        if (isVisualizerRunning) return;
        isVisualizerRunning = true;
        visualizerRunnable.run();
    }

    private final Runnable visualizerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                for (View bar : visualizerBars) {
                    float scale = 0.4f + (float) Math.random() * 0.7f;
                    bar.animate().scaleY(scale).setDuration(150).start();
                }
                visualizerHandler.postDelayed(this, 150);
            } else {
                stopVisualizer();
            }
        }
    };

    private void stopVisualizer() {
        isVisualizerRunning = false;
        visualizerHandler.removeCallbacks(visualizerRunnable);

        for (View bar : visualizerBars) {
            bar.animate().scaleY(1f).setDuration(300).start();
        }
    }

    private void changePlayerSpeed() {
        if (currentSpeed == 1.0f) currentSpeed = 1.5f;
        else if (currentSpeed == 1.5f) currentSpeed = 2.0f;
        else currentSpeed = 1.0f;

        btnSpeed.setText(currentSpeed + "x");

        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer.setPlaybackParams(
                    mediaPlayer.getPlaybackParams().setSpeed(currentSpeed)
            );
        }
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    sbPodcast.setProgress(mediaPlayer.getCurrentPosition());
                    tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void showPlaylistDialog() {
        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_nature_list, null);

        bottomSheetDialog.setContentView(view);

        RecyclerView rv = view.findViewById(R.id.rvNatureSounds);
        view.findViewById(R.id.btnCloseList)
                .setOnClickListener(v -> bottomSheetDialog.dismiss());

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new PodcastAdapter(titles, currentIndex, position -> {
            currentIndex = position;
            setupPlayer(currentIndex);
            bottomSheetDialog.dismiss();
        }));

        bottomSheetDialog.show();
    }

    private void startBlinkAnimation(View view) {
        Animation anim = new AlphaAnimation(0.4f, 1.0f);
        anim.setDuration(1000);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        view.startAnimation(anim);
    }

    private String formatTime(int ms) {
        int m = (ms / 1000) / 60;
        int s = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        stopTimerHandler();
        handler.removeCallbacksAndMessages(null);
        visualizerHandler.removeCallbacksAndMessages(null);
    }
}