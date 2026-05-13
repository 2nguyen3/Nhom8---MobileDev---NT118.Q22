package com.example.heami.ui.therapy;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    private final View[] visualizerBars = new View[5];
    private final Handler visualizerHandler = new Handler(Looper.getMainLooper());
    private boolean isVisualizerRunning = false;

    private final String COLOR_ORANGE = "#FFB74D";
    private final int SECOND_MS = 1000;

    private long timeLeftInMillis = 0;
    private int selectedMinutes = 0;
    private Runnable timerRunnable;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private PodcastService podcastService;
    private boolean isBound = false;
    private final Handler seekBarHandler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarTask;

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
        setupListeners();

        Intent intent = new Intent(this, PodcastService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, PodcastService.class);
        if (!isBound) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PodcastService.PodcastBinder binder = (PodcastService.PodcastBinder) service;
            podcastService = binder.getService();
            isBound = true;

            syncWithServiceState();

            podcastService.setOnCompletionListener(mp -> {
                currentIndex = (currentIndex + 1) % songs.length;
                sendPlayNewCommand(songs[currentIndex]);
                updateUIByTrack(currentIndex);
            });

            startSeekBarUpdate();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void syncWithServiceState() {
        if (podcastService != null) {
            int realRawId = podcastService.getCurrentPlayingRawId();
            boolean found = false;

            // 1. Tìm xem bài hát Service đang phát ngầm là bài nào trong danh sách
            for (int i = 0; i < songs.length; i++) {
                if (songs[i] == realRawId) {
                    currentIndex = i;
                    found = true;
                    break;
                }
            }

            // 2. Cập nhật thông tin bài hát (Tiêu đề, Quote) dựa theo currentIndex chuẩn vừa tìm được
            updateUIByTrack(currentIndex);

            // 3. Kiểm tra trạng thái thực tế của Service để ép giao diện hiển thị đúng
            if (podcastService.isPlaying()) {
                // Nếu thực sự đang phát ngầm -> Ép giao diện hiển thị "Đang phát" và hiện nút PAUSE
                updateUIPlaying();

                // Lấy thời lượng thực tế từ bài đang phát ngầm gán cho SeekBar
                int totalDuration = podcastService.getDuration();
                if (totalDuration > 0) {
                    sbPodcast.setMax(totalDuration);
                    tvTotalTime.setText(formatTime(totalDuration));
                }
            } else {
                // Nếu thực sự đang dừng -> Hiện chữ "Đã dừng" và hiện nút PLAY
                updateUIPaused();

                // Nếu chưa chạy bài nào bao giờ, khởi tạo thông số của bài mặc định ban đầu
                if (!found && podcastService.getDuration() == 0) {
                    android.media.MediaPlayer tempMp = android.media.MediaPlayer.create(this, songs[currentIndex]);
                    if (tempMp != null) {
                        tvTotalTime.setText(formatTime(tempMp.getDuration()));
                        sbPodcast.setMax(tempMp.getDuration());
                        tempMp.release();
                    }
                }
            }
        }
    }

    private void updateUIByTrack(int index) {
        tvTitle.setText(titles[index]);
        tvQuote.setText(quotes[index]);
        if (podcastService != null) {
            tvTotalTime.setText(formatTime(podcastService.getDuration()));
            sbPodcast.setMax(podcastService.getDuration());
        }
    }

    private void sendCommandToService(String action) {
        Intent intent = new Intent(this, PodcastService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void sendPlayNewCommand(int songRawId) {
        Intent intent = new Intent(this, PodcastService.class);
        intent.setAction(PodcastService.ACTION_PLAY_NEW);
        intent.putExtra(PodcastService.EXTRA_SONG_ID, songRawId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
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

    private void setupListeners() {
        btnMinimize.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            applyClickAnimation(cardPlayPause);

            if (!isBound || podcastService == null) return;

            // Lần đầu chưa có MediaPlayer -> phát bài hiện tại
            if (podcastService.getCurrentPlayingRawId() == -1) {
                sendPlayNewCommand(songs[currentIndex]);
                updateUIByTrack(currentIndex);
                updateUIPlaying();
                return;
            }

            // Đã có MediaPlayer
            if (podcastService.isPlaying()) {
                sendCommandToService(PodcastService.ACTION_PAUSE);
                updateUIPaused();
            } else {
                sendCommandToService(PodcastService.ACTION_START);
                updateUIPlaying();
            }
        });

        btnForward15.setOnClickListener(v -> {
            applyClickAnimation(v);
            showSkipFeedback("+15s");
            if (isBound && podcastService != null) {
                int target = podcastService.getCurrentPosition() + 15000;
                podcastService.seekTo(Math.min(target, podcastService.getDuration()));
            }
        });

        btnReplay15.setOnClickListener(v -> {
            applyClickAnimation(v);
            showSkipFeedback("-15s");
            if (isBound && podcastService != null) {
                int target = podcastService.getCurrentPosition() - 15000;
                podcastService.seekTo(Math.max(target, 0));
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
                if (fromUser && isBound && podcastService != null) {
                    podcastService.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateUIPlaying() {
        btnPlayPause.setImageResource(R.drawable.ic_playing);
        tvStatusPlay.setText("Đang phát...");
        tvStatusPlay.setTextColor(Color.parseColor(COLOR_ORANGE));
        startBlinkAnimation(tvStatusPlay);
        startVisualizer();
    }

    private void updateUIPaused() {
        btnPlayPause.setImageResource(R.drawable.ic_pause);
        tvStatusPlay.setText("Đã dừng");
        tvStatusPlay.setTextColor(Color.WHITE);
        tvStatusPlay.clearAnimation();
        stopVisualizer();
    }

    private void startSeekBarUpdate() {
        seekBarHandler.removeCallbacks(updateSeekBarTask);
        updateSeekBarTask = new Runnable() {
            @Override
            public void run() {
                if (isBound && podcastService != null) {
                    int currentPos = podcastService.getCurrentPosition();
                    int totalDuration = podcastService.getDuration();
                    sbPodcast.setMax(totalDuration);
                    sbPodcast.setProgress(currentPos);
                    tvCurrentTime.setText(formatTime(currentPos));
                    tvTotalTime.setText(formatTime(totalDuration));
                }
                seekBarHandler.postDelayed(this, 1000);
            }
        };
        seekBarHandler.post(updateSeekBarTask);
    }

    private void changePlayerSpeed() {
        if (currentSpeed == 1.0f) currentSpeed = 1.5f;
        else if (currentSpeed == 1.5f) currentSpeed = 2.0f;
        else currentSpeed = 1.0f;

        btnSpeed.setText(currentSpeed + "x");

        if (isBound && podcastService != null) {
            podcastService.setSpeed(currentSpeed);
        }
    }

    private void startSleepTimer(int minutes) {
        stopTimerHandler();
        timeLeftInMillis = (long) minutes * 60 * SECOND_MS;

        btnTimer.setColorFilter(Color.parseColor(COLOR_ORANGE));
        btnTimer.setAlpha(1.0f);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBound && podcastService != null && podcastService.isPlaying() && timeLeftInMillis > 0) {
                    timeLeftInMillis -= SECOND_MS;
                    timerHandler.postDelayed(this, SECOND_MS);
                }

                if (timeLeftInMillis <= 0) {
                    sendCommandToService(PodcastService.ACTION_PAUSE);
                    updateUIPaused();
                    cancelSleepTimer();
                    selectedMinutes = 0;
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, SECOND_MS);
        Toast.makeText(this, "Hẹn giờ tắt sau " + minutes + " phút", Toast.LENGTH_SHORT).show();
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

            final Handler dialogHandler = new Handler(Looper.getMainLooper());
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

    private void highlightTimerItem(View view) {
        view.setBackgroundResource(R.drawable.bg_timer_item_selected);
        if (view instanceof LinearLayout) {
            LinearLayout l = (LinearLayout) view;
            for (int i = 0; i < l.getChildCount(); i++) {
                View child = l.getChildAt(i);
                if (child instanceof TextView) ((TextView) child).setTextColor(Color.WHITE);
                if (child instanceof ImageView) ((ImageView) child).setColorFilter(Color.WHITE);
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
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    private void applyClickAnimation(View view) {
        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
    }

    private void showSkipFeedback(String text) {
        tvSkipFeedback.setText(text);
        tvSkipFeedback.setVisibility(View.VISIBLE);
        tvSkipFeedback.setAlpha(1f);
        tvSkipFeedback.setTranslationY(0f);
        tvSkipFeedback.animate().translationY(-120f).alpha(0f).setDuration(600)
                .withEndAction(() -> tvSkipFeedback.setVisibility(View.INVISIBLE)).start();
    }

    private void startVisualizer() {
        if (isVisualizerRunning) return;
        isVisualizerRunning = true;
        visualizerRunnable.run();
    }

    private final Runnable visualizerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && podcastService != null && podcastService.isPlaying()) {
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

    private void showPlaylistDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_nature_list, null);
        bottomSheetDialog.setContentView(view);

        RecyclerView rv = view.findViewById(R.id.rvNatureSounds);
        view.findViewById(R.id.btnCloseList).setOnClickListener(v -> bottomSheetDialog.dismiss());

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new PodcastAdapter(titles, currentIndex, position -> {
            currentIndex = position;
            sendPlayNewCommand(songs[currentIndex]);
            updateUIByTrack(currentIndex);
            updateUIPlaying();
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
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        seekBarHandler.removeCallbacks(updateSeekBarTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimerHandler();
        seekBarHandler.removeCallbacksAndMessages(null);
        visualizerHandler.removeCallbacksAndMessages(null);
    }
}