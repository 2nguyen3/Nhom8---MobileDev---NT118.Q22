package com.example.heami.ui.therapy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.example.heami.R;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private static final String CHANNEL_ID = "MusicPlaybackChannel";
    private static final int NOTIFICATION_ID = 999;

    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";

    // 🌟 THÊM HÀNH ĐỘNG MỚI ĐỂ ĐIỀU KHIỂN TỪ XA KHI VÀO LẠI APP
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_PLAY_NEW = "ACTION_PLAY_NEW";
    public static final String EXTRA_SONG_ID = "EXTRA_SONG_ID";

    private int currentPlayingRawId = R.raw.cortis_acai;
    private MediaPlayer.OnCompletionListener completionListener;

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaPlayer = MediaPlayer.create(this, currentPlayingRawId);
        mediaPlayer.setLooping(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            // 🌟 SỬA LOGIC NHẬN LỆNH: Tiếp nhận lệnh Intent xuyên suốt kể cả khi app bị đóng băng Binder
            switch (action) {
                case ACTION_PLAY_PAUSE:
                    togglePlayPause();
                    break;
                case ACTION_START:
                    startMusic();
                    break;
                case ACTION_PAUSE:
                    pauseMusic();
                    break;
                case ACTION_PLAY_NEW:
                    int songId = intent.getIntExtra(EXTRA_SONG_ID, R.raw.cortis_acai);
                    playSelectedSong(songId);
                    break;
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_STICKY;
    }

    public void playSelectedSong(int songRawId) {

        currentPlayingRawId = songRawId;

        try {

            if (mediaPlayer != null) {

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = MediaPlayer.create(this, songRawId);

            if (mediaPlayer == null) {
                return;
            }

            mediaPlayer.setLooping(true);

            if (completionListener != null) {
                mediaPlayer.setOnCompletionListener(completionListener);
            }

            mediaPlayer.start();

            updateNotification();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateNotification();
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateNotification();
        }
    }

    public void togglePlayPause() {
        if (isPlaying()) {
            pauseMusic();
        } else {
            startMusic();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void seekTo(int msec) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(msec);
        }
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        completionListener = listener;

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(listener);
        }
    }

    public int getCurrentPlayingRawId() {
        return currentPlayingRawId;
    }
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MusicPlayerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MusicService.class).setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 1,
                playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int iconPlayPause = isPlaying() ? R.drawable.ic_pause : R.drawable.ic_playlist_play;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(isPlaying() ? "Giai điệu thư giãn" : "Đã tạm dừng")
                .setContentText("HEAMI - Ứng dụng chữa lành")
                .setSmallIcon(R.drawable.ic_playlist_play)
                .setContentIntent(pendingIntent)
                .addAction(iconPlayPause, isPlaying() ? "Pause" : "Play", playPausePendingIntent)
                .setStyle(new MediaStyle().setShowActionsInCompactView(0))
                .setOnlyAlertOnce(true)
                .build();
    }

    public void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Heami Music Service", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {

            try {

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

            } catch (Exception ignored) {}

            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}