package com.example.heami.ui.therapy;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.heami.R;

public class NatureService extends Service {

    public static final String ACTION_START = "com.example.heami.START";
    public static final String ACTION_PAUSE = "com.example.heami.PAUSE";
    public static final String ACTION_PLAY_NEW = "com.example.heami.PLAY_NEW";
    public static final String EXTRA_SONG_ID = "extra_song_id";

    private static final String CHANNEL_ID = "NatureSoundChannel";
    private static final int NOTIFICATION_ID = 2; // Khác ID của MusicService để tránh đè nhau

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new NatureBinder();
    private int currentRawId = -1;
    private MediaPlayer.OnCompletionListener completionListener;

    public class NatureBinder extends Binder {
        public NatureService getService() {
            return NatureService.this;
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START:
                    startMusic();
                    break;
                case ACTION_PAUSE:
                    pauseMusic();
                    break;
                case ACTION_PLAY_NEW:
                    int rawId = intent.getIntExtra(EXTRA_SONG_ID, -1);
                    if (rawId != -1) {
                        playNewSound(rawId);
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    private void startMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            startForeground(NOTIFICATION_ID, buildNotification("Đang phát âm thanh tự nhiên"));
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            // Chuyển sang trạng thái có thể tắt bớt notification hoặc giữ tùy cấu hình ngầm
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, buildNotification("Đã tạm dừng"));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH);
            } else {
                stopForeground(false);
            }
        }
    }

    private void playNewSound(int rawId) {
        currentRawId = rawId;
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, rawId);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            if (completionListener != null) {
                mediaPlayer.setOnCompletionListener(completionListener);
            }
            startForeground(NOTIFICATION_ID, buildNotification("Đang phát âm thanh tự nhiên"));
        }
    }

    // Các hàm Helper để Activity tương tác (qua Binder)
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int progress) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(progress);
        }
    }

    public void setLooping(boolean isRepeat) {
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(isRepeat);
        }
    }

    public int getCurrentPlayingRawId() {
        return currentRawId;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        this.completionListener = listener;
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(listener);
        }
    }

    // Khởi tạo Notification cho Foreground Service
    private Notification buildNotification(String statusText) {
        Intent notificationIntent = new Intent(this, NatureSoundActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HEAMI Therapy")
                .setContentText(statusText)
                .setSmallIcon(R.drawable.ic_playing) // Thay bằng icon thích hợp trong mục drawable của bạn
                .setContentIntent(pendingIntent)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nature Sound Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null, null);
            serviceChannel.enableLights(false);
            serviceChannel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
