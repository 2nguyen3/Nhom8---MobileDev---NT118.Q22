package com.example.moodmusic;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private static final float ARC_DP = 18f;

    private TextView emoSad, emoHappy, emoStress, tvMoodLabel;
    private MaterialButton btnStop;

    private boolean isAnimating = false;
    private final FastOutSlowInInterpolator ease = new FastOutSlowInInterpolator();

    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emoSad = findViewById(R.id.emoSad);
        emoHappy = findViewById(R.id.emoHappy);
        emoStress = findViewById(R.id.emoStress);
        tvMoodLabel = findViewById(R.id.tvMoodLabel);
        btnStop = findViewById(R.id.btnStop);

        // Default label (emoji đã nằm trong XML qua @string/emoji_*)
        updateLabelFromCenter();

        emoSad.setOnClickListener(v -> {
            if (isAnimating) return;
            swapEmojiSmooth(emoSad);
        });

        emoStress.setOnClickListener(v -> {
            if (isAnimating) return;
            swapEmojiSmooth(emoStress);
        });

        emoHappy.setOnClickListener(v -> {
            if (isAnimating) return;
            emoHappy.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            pop(emoHappy);
            playMusic(getSongFromCenter());
        });

        btnStop.setOnClickListener(v -> stopMusic());
    }

    private void swapEmojiSmooth(TextView side) {
        if (isAnimating) return;
        isAnimating = true;

        side.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

        side.post(() -> {
            resetMotion(emoHappy);
            resetMotion(side);

            float dx = centerXOnScreen(emoHappy) - centerXOnScreen(side);
            float arc = dp(ARC_DP);

            side.animate()
                    .translationX(dx)
                    .translationY(-arc)
                    .scaleX(1.08f).scaleY(1.08f)
                    .alpha(1f)
                    .setDuration(260)
                    .setInterpolator(ease)
                    .start();

            emoHappy.animate()
                    .translationX(-dx)
                    .translationY(arc)
                    .scaleX(0.92f).scaleY(0.92f)
                    .alpha(0.85f)
                    .setDuration(260)
                    .setInterpolator(ease)
                    .withEndAction(() -> {
                        swapText(emoHappy, side);

                        updateLabelFromCenter();
                        playMusic(getSongFromCenter());

                        side.animate()
                                .translationX(0f).translationY(0f)
                                .scaleX(1f).scaleY(1f)
                                .alpha(0.7f)
                                .setDuration(220)
                                .setInterpolator(ease)
                                .start();

                        emoHappy.animate()
                                .translationX(0f).translationY(0f)
                                .scaleX(1f).scaleY(1f)
                                .alpha(1f)
                                .setDuration(220)
                                .setInterpolator(ease)
                                .withEndAction(() -> {
                                    pop(emoHappy);
                                    isAnimating = false;
                                })
                                .start();
                    })
                    .start();
        });
    }

    private int getSongFromCenter() {
        String center = String.valueOf(emoHappy.getText());
        if (center.contains("😊")) return R.raw.happy;
        if (center.contains("😔")) return R.raw.sad;
        if (center.contains("😵")) return R.raw.stress;
        return R.raw.happy;
    }

    private void playMusic(int songId) {
        stopMusic();

        player = MediaPlayer.create(this, songId);
        if (player == null) return;

        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
        } catch (Exception ignored) {}

        player.setLooping(true);
        player.setVolume(1f, 1f);
        player.start();
    }

    private void stopMusic() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
            } catch (Exception ignored) {}
            try {
                player.release();
            } catch (Exception ignored) {}
            player = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopMusic();
    }

    // ===== UI helpers =====
    private void resetMotion(TextView v) {
        v.animate().cancel();
        v.setTranslationX(0f);
        v.setTranslationY(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setAlpha(v == emoHappy ? 1f : 0.7f);
    }

    private float centerXOnScreen(TextView v) {
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return loc[0] + v.getWidth() / 2f;
    }

    private void swapText(TextView a, TextView b) {
        CharSequence t = a.getText();
        a.setText(b.getText());
        b.setText(t);
    }

    private void updateLabelFromCenter() {
        String center = String.valueOf(emoHappy.getText());

        if (center.contains("😊")) tvMoodLabel.setText(R.string.mood_happy);
        else if (center.contains("😔")) tvMoodLabel.setText(R.string.mood_sad);
        else if (center.contains("😵")) tvMoodLabel.setText(R.string.mood_stress);
        else tvMoodLabel.setText(R.string.mood_unknown);

        tvMoodLabel.setAlpha(0.2f);
        tvMoodLabel.animate()
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(ease)
                .start();
    }

    private void pop(TextView v) {
        v.animate().cancel();
        v.animate()
                .scaleX(1.10f).scaleY(1.10f)
                .setDuration(150)
                .setInterpolator(ease)
                .withEndAction(() -> v.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(160)
                        .setInterpolator(ease)
                        .start())
                .start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}