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

public class PodcastService extends Service {

    public static final String ACTION_START = "com.example.heami.PODCAST_START";
    public static final String ACTION_PAUSE = "com.example.heami.PODCAST_PAUSE";
    public static final String ACTION_PLAY_NEW = "com.example.heami.PODCAST_PLAY_NEW";
    public static final String EXTRA_SONG_ID = "extra_podcast_id";

    private static final String CHANNEL_ID = "PodcastChannel";
    private static final int NOTIFICATION_ID = 3;

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new PodcastBinder();
    private int currentRawId = -1;
    private float currentSpeed = 1.0f;
    private MediaPlayer.OnCompletionListener completionListener;

    public class PodcastBinder extends Binder {
        public PodcastService getService() {
            return PodcastService.this;
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
                    startPodcast();
                    break;
                case ACTION_PAUSE:
                    pausePodcast();
                    break;
                case ACTION_PLAY_NEW:
                    int rawId = intent.getIntExtra(EXTRA_SONG_ID, -1);
                    if (rawId != -1) {
                        playNewPodcast(rawId);
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    private void startPodcast() {

        if (mediaPlayer == null) {
            playNewPodcast(R.raw.neu_ca_doi_khong_ruc_ro); // bài mặc định
            return;
        }

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setSpeed(currentSpeed);
            }

            startForeground(
                    NOTIFICATION_ID,
                    buildNotification("Đang phát bài viết chữa lành")
            );
        }
    }

    private void pausePodcast() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
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

    private void playNewPodcast(int rawId) {
        currentRawId = rawId;
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, rawId);
        if (mediaPlayer != null) {
            setSpeed(currentSpeed);
            mediaPlayer.start();
            if (completionListener != null) {
                mediaPlayer.setOnCompletionListener(completionListener);
            }
            startForeground(NOTIFICATION_ID, buildNotification("Đang phát bài viết chữa lành"));
        }
    }

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

    public void setSpeed(float speed) {
        this.currentSpeed = speed;
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private Notification buildNotification(String statusText) {
        Intent notificationIntent = new Intent(this, PodcastActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("HEAMI Podcast")
                .setContentText(statusText)
                .setSmallIcon(R.drawable.ic_playing)
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
                    "Podcast Service Channel",
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