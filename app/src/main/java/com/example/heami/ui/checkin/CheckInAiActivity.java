package com.example.heami.ui.checkin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.heami.R;
import com.example.heami.ai.HeamiEmotionRuleEngine;
import com.example.heami.ai.HeamiFaceLandmarkerHelper;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class CheckInAiActivity extends AppCompatActivity
        implements HeamiFaceLandmarkerHelper.LandmarkerListener {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    private static final int REQUIRED_QUALITY_FRAMES = 3;
    private static final int REQUIRED_EMOTION_SAMPLES = 3;

    private ImageButton btnBackCheckInAi;

    private View imgCheckInHeami;
    private View cardCheckInBubble;
    private View cardCameraPreview;

    private View viewCheckInScanLine;
    private View viewCheckInScanGlow;

    private View glowCheckInTopLeft;
    private View glowCheckInBottomRight;

    private View decorCheckInFlowerTopLeft;
    private View decorCheckInFlowerTopRight;
    private View decorCheckInFlowerMiddleLeft;
    private View decorCheckInFlowerMiddleRight;
    private View decorCheckInFlowerBottomRight;

    private LinearLayout btnManualMood;
    private TextView txtCheckInAiLabel;
    private TextView txtCheckInAiTitle;
    private TextView txtCheckInInstruction;
    private TextView txtManualMoodHint;

    private PreviewView previewCheckInCamera;

    private ExecutorService cameraExecutor;
    private HeamiFaceLandmarkerHelper faceLandmarkerHelper;
    private HeamiEmotionRuleEngine emotionRuleEngine;

    private boolean isProcessingFrame = false;
    private long lastAnalyzeTime = 0L;

    private int stableQualityFrameCount = 0;

    private final List<HeamiEmotionRuleEngine.EmotionDecision> emotionSamples = new ArrayList<>();
    private long lastEmotionSampleTime = 0L;
    private boolean isNavigatingResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin_ai);

        bindViews();
        setupActions();
        startCheckInAnimations();
        setInitialUiState();

        cameraExecutor = Executors.newSingleThreadExecutor();
        faceLandmarkerHelper = new HeamiFaceLandmarkerHelper(this, this);
        emotionRuleEngine = new HeamiEmotionRuleEngine();

        checkCameraPermissionAndStart();
    }

    private void bindViews() {
        btnBackCheckInAi = findViewById(R.id.btnBackCheckInAi);

        imgCheckInHeami = findViewById(R.id.imgCheckInHeami);
        cardCheckInBubble = findViewById(R.id.cardCheckInBubble);
        cardCameraPreview = findViewById(R.id.cardCameraPreview);

        viewCheckInScanLine = findViewById(R.id.viewCheckInScanLine);
        viewCheckInScanGlow = findViewById(R.id.viewCheckInScanGlow);

        glowCheckInTopLeft = findViewById(R.id.glowCheckInTopLeft);
        glowCheckInBottomRight = findViewById(R.id.glowCheckInBottomRight);

        decorCheckInFlowerTopLeft = findViewById(R.id.decorCheckInFlowerTopLeft);
        decorCheckInFlowerTopRight = findViewById(R.id.decorCheckInFlowerTopRight);
        decorCheckInFlowerMiddleLeft = findViewById(R.id.decorCheckInFlowerMiddleLeft);
        decorCheckInFlowerMiddleRight = findViewById(R.id.decorCheckInFlowerMiddleRight);
        decorCheckInFlowerBottomRight = findViewById(R.id.decorCheckInFlowerBottomRight);

        btnManualMood = findViewById(R.id.btnManualMood);
        txtCheckInAiLabel = findViewById(R.id.txtCheckInAiLabel);
        txtCheckInAiTitle = findViewById(R.id.txtCheckInAiTitle);
        txtCheckInInstruction = findViewById(R.id.txtCheckInInstruction);
        txtManualMoodHint = findViewById(R.id.txtManualMoodHint);

        previewCheckInCamera = findViewById(R.id.previewCheckInCamera);
    }

    private void setupActions() {
        if (btnBackCheckInAi != null) {
            btnBackCheckInAi.setOnClickListener(v -> finish());
        }

        if (btnManualMood != null) {
            btnManualMood.setOnClickListener(v -> {
                Intent intent = new Intent(CheckInAiActivity.this, ManualMoodActivity.class);
                startActivity(intent);
            });
        }

        if (cardCameraPreview != null) {
            cardCameraPreview.setOnClickListener(v -> {
                Toast.makeText(
                        CheckInAiActivity.this,
                        "Heami đang dùng MediaPipe để đọc khuôn mặt realtime.",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }
    }

    private void setInitialUiState() {
        if (txtCheckInAiTitle != null) {
            txtCheckInAiTitle.setText("AI cảm xúc");
        }

        if (txtCheckInInstruction != null) {
            txtCheckInInstruction.setText(
                    "Giữ khuôn mặt trong khung hình nhé, khi khung hình ổn Heami sẽ đọc cảm xúc ngay"
            );
        }

        if (txtManualMoodHint != null) {
            txtManualMoodHint.setText(
                    "Nếu camera không thuận tiện, bạn vẫn có thể chọn cảm xúc thủ công"
            );
        }
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(
                        this,
                        "Bạn cần cấp quyền camera để tiếp tục dùng tính năng check-in nha",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void startCamera() {
        if (previewCheckInCamera == null) {
            Toast.makeText(this, "Không tìm thấy khung camera preview", Toast.LENGTH_SHORT).show();
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewCheckInCamera.getSurfaceProvider());

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(640, 480))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "Không thể mở camera: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        long now = SystemClock.uptimeMillis();

        if (isProcessingFrame || now - lastAnalyzeTime < 180) {
            imageProxy.close();
            return;
        }

        lastAnalyzeTime = now;
        isProcessingFrame = true;

        try {
            if (faceLandmarkerHelper == null || !faceLandmarkerHelper.isReady()) {
                imageProxy.close();
                isProcessingFrame = false;
                return;
            }

            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            imageProxy.close();

            if (bitmap == null) {
                isProcessingFrame = false;
                return;
            }

            faceLandmarkerHelper.detectAsync(bitmap, now);

        } catch (Exception e) {
            try {
                imageProxy.close();
            } catch (Exception ignored) {
            }

            isProcessingFrame = false;

            runOnUiThread(() -> {
                if (txtCheckInInstruction != null) {
                    txtCheckInInstruction.setText(
                            "Heami gặp lỗi khi đọc frame camera, thử lại nhé"
                    );
                }
            });
        }
    }

    @Override
    public void onResult(
            @NonNull FaceLandmarkerResult result,
            int imageWidth,
            int imageHeight
    ) {
        isProcessingFrame = false;

        int faceCount = result.faceLandmarks() != null ? result.faceLandmarks().size() : 0;

        if (faceCount <= 0) {
            stableQualityFrameCount = 0;
            clearEmotionSamples();

            runOnUiThread(() -> {
                if (txtCheckInInstruction != null) {
                    txtCheckInInstruction.setText(
                            "Đưa khuôn mặt vào khung hình và nhìn thẳng vào camera nhé"
                    );
                }
            });
            return;
        }

        List<NormalizedLandmark> landmarks = result.faceLandmarks().get(0);
        FaceQualityResult qualityResult = evaluateFaceQuality(landmarks);

        if (!qualityResult.isValid()) {
            stableQualityFrameCount = 0;
            clearEmotionSamples();

            runOnUiThread(() -> {
                if (txtCheckInInstruction != null) {
                    txtCheckInInstruction.setText(qualityResult.getMessage());
                }
            });
            return;
        }

        stableQualityFrameCount++;

        if (stableQualityFrameCount < REQUIRED_QUALITY_FRAMES) {
            runOnUiThread(() -> {
                if (txtCheckInInstruction != null) {
                    txtCheckInInstruction.setText(
                            "Khuôn mặt đã khá ổn rồi, giữ yên thêm chút nhé... ("
                                    + stableQualityFrameCount + "/" + REQUIRED_QUALITY_FRAMES + ")"
                    );
                }
            });
            return;
        }

        collectEmotionSampleAndMaybeOpen(result);
    }

    @Override
    public void onError(@NonNull String errorMessage) {
        isProcessingFrame = false;

        runOnUiThread(() -> {
            if (txtCheckInInstruction != null) {
                txtCheckInInstruction.setText(
                        "MediaPipe đang gặp lỗi, thử lại nhé"
                );
            }

            Toast.makeText(
                    CheckInAiActivity.this,
                    "MediaPipe error: " + errorMessage,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private FaceQualityResult evaluateFaceQuality(@NonNull List<NormalizedLandmark> landmarks) {
        if (landmarks.size() <= 291) {
            return new FaceQualityResult(false, "Heami chưa đọc đủ điểm khuôn mặt, thử giữ yên hơn nhé");
        }

        float minX = 1f;
        float maxX = 0f;
        float minY = 1f;
        float maxY = 0f;

        for (NormalizedLandmark landmark : landmarks) {
            float x = landmark.x();
            float y = landmark.y();

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        float faceWidth = maxX - minX;
        float faceHeight = maxY - minY;
        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;

        if (faceWidth < 0.26f || faceHeight < 0.32f) {
            return new FaceQualityResult(false, "Đưa khuôn mặt lại gần camera hơn một chút nhé");
        }

        if (faceWidth > 0.72f || faceHeight > 0.82f) {
            return new FaceQualityResult(false, "Bạn đang để mặt quá sát camera, lùi ra một chút nhé");
        }

        if (Math.abs(centerX - 0.5f) > 0.13f) {
            return new FaceQualityResult(false, "Đưa khuôn mặt vào giữa khung hình nhé");
        }

        if (centerY < 0.30f || centerY > 0.68f) {
            return new FaceQualityResult(false, "Canh khuôn mặt vào giữa khung hình theo chiều dọc nhé");
        }

        NormalizedLandmark leftEyeOuter = landmarks.get(33);
        NormalizedLandmark rightEyeOuter = landmarks.get(263);
        NormalizedLandmark noseTip = landmarks.get(1);
        NormalizedLandmark upperLip = landmarks.get(13);
        NormalizedLandmark lowerLip = landmarks.get(14);
        NormalizedLandmark chin = landmarks.get(152);

        float eyeMidY = (leftEyeOuter.y() + rightEyeOuter.y()) / 2f;
        float mouthMidY = (upperLip.y() + lowerLip.y()) / 2f;

        float eyeToMouthRatio = (mouthMidY - eyeMidY) / faceHeight;
        float mouthToChinRatio = (chin.y() - mouthMidY) / faceHeight;

        if (centerY < 0.38f || (eyeToMouthRatio < 0.24f && mouthToChinRatio < 0.20f)) {
            return new FaceQualityResult(false, "Bạn đang ngước đầu hơi cao, hạ mặt xuống một chút nhé");
        }

        float eyeTilt = Math.abs(leftEyeOuter.y() - rightEyeOuter.y());
        if (eyeTilt > 0.035f) {
            return new FaceQualityResult(false, "Giữ đầu thẳng hơn một chút nhé");
        }

        float noseToLeftEye = Math.abs(noseTip.x() - leftEyeOuter.x());
        float noseToRightEye = Math.abs(rightEyeOuter.x() - noseTip.x());

        float minEyeDistance = Math.min(noseToLeftEye, noseToRightEye);
        float maxEyeDistance = Math.max(noseToLeftEye, noseToRightEye);

        if (minEyeDistance < 0.0001f) {
            return new FaceQualityResult(false, "Heami chưa đọc rõ khuôn mặt, thử lại nhé");
        }

        float yawRatio = maxEyeDistance / minEyeDistance;
        if (yawRatio > 1.45f) {
            return new FaceQualityResult(false, "Nhìn thẳng vào camera hơn một chút nhé");
        }

        return new FaceQualityResult(true, "Khuôn mặt đạt chuẩn");
    }

    private void collectEmotionSampleAndMaybeOpen(@NonNull FaceLandmarkerResult result) {
        if (emotionRuleEngine == null || isNavigatingResult) {
            return;
        }

        if (result.faceBlendshapes() == null || !result.faceBlendshapes().isPresent()) {
            runOnUiThread(() -> {
                if (txtCheckInInstruction != null) {
                    txtCheckInInstruction.setText(
                            "Heami đã thấy mặt nhưng chưa đọc rõ biểu cảm, giữ yên thêm chút nhé"
                    );
                }
            });
            return;
        }

        List<List<Category>> faceBlendshapeList = result.faceBlendshapes().get();

        if (faceBlendshapeList == null || faceBlendshapeList.isEmpty()) {
            runOnUiThread(() -> {
                if (txtCheckInInstruction != null) {
                    txtCheckInInstruction.setText(
                            "Heami đã thấy mặt nhưng chưa đọc rõ biểu cảm, giữ yên thêm chút nhé"
                    );
                }
            });
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (now - lastEmotionSampleTime < 350) {
            return;
        }

        List<Category> blendshapes = faceBlendshapeList.get(0);

        HeamiEmotionRuleEngine.EmotionDecision decision =
                emotionRuleEngine.inferFromBlendshapes(blendshapes);

        emotionSamples.add(decision);
        lastEmotionSampleTime = now;

        Log.d("HeamiEmotion", "Sample " + emotionSamples.size() + ": " + decision.getDebugSummary());

        runOnUiThread(() -> {
            if (txtCheckInInstruction == null) return;

            if (emotionSamples.size() < REQUIRED_EMOTION_SAMPLES) {
                txtCheckInInstruction.setText(
                        "Heami đang đọc tín hiệu cảm xúc của bạn... ("
                                + emotionSamples.size() + "/" + REQUIRED_EMOTION_SAMPLES + ")"
                );
            } else {
                txtCheckInInstruction.setText(
                        "Heami đã đọc xong tín hiệu cảm xúc, đang chuẩn bị kết quả..."
                );
            }
        });

        if (emotionSamples.size() >= REQUIRED_EMOTION_SAMPLES) {
            isNavigatingResult = true;
            HeamiEmotionRuleEngine.EmotionDecision finalDecision = aggregateEmotionSamples();
            openResultFromDecision(finalDecision);
        }
    }

    @NonNull
    private HeamiEmotionRuleEngine.EmotionDecision aggregateEmotionSamples() {
        if (emotionSamples.isEmpty()) {
            return new HeamiEmotionRuleEngine.EmotionDecision(
                    "neutral",
                    "Bình thường",
                    "😌",
                    "Heami thấy trạng thái của bạn hiện tại khá ổn định.",
                    60,
                    0.60f,
                    "fallback_neutral"
            );
        }

        java.util.Map<String, Float> emotionScoreMap = new java.util.HashMap<>();
        java.util.Map<String, Integer> emotionCountMap = new java.util.HashMap<>();

        HeamiEmotionRuleEngine.EmotionDecision bestTemplate = emotionSamples.get(0);
        float bestConfidence = -1f;

        for (HeamiEmotionRuleEngine.EmotionDecision sample : emotionSamples) {
            String key = sample.getEmotionKey();
            float confidence = sample.getConfidence();

            float oldScore = emotionScoreMap.containsKey(key) ? emotionScoreMap.get(key) : 0f;
            int oldCount = emotionCountMap.containsKey(key) ? emotionCountMap.get(key) : 0;

            emotionScoreMap.put(key, oldScore + confidence);
            emotionCountMap.put(key, oldCount + 1);

            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                bestTemplate = sample;
            }
        }

        String finalEmotionKey = bestTemplate.getEmotionKey();
        float finalTotalScore = -1f;

        for (java.util.Map.Entry<String, Float> entry : emotionScoreMap.entrySet()) {
            String key = entry.getKey();
            float totalScore = entry.getValue();
            int count = emotionCountMap.get(key);

            float weightedScore = totalScore + (count * 0.08f);

            if (weightedScore > finalTotalScore) {
                finalTotalScore = weightedScore;
                finalEmotionKey = key;
            }
        }

        HeamiEmotionRuleEngine.EmotionDecision template = bestTemplate;
        for (HeamiEmotionRuleEngine.EmotionDecision sample : emotionSamples) {
            if (sample.getEmotionKey().equals(finalEmotionKey)) {
                template = sample;
                break;
            }
        }

        float avgConfidence = emotionScoreMap.get(finalEmotionKey) / emotionCountMap.get(finalEmotionKey);
        int moodPercent = Math.max(55, Math.round(avgConfidence * 100f));

        return new HeamiEmotionRuleEngine.EmotionDecision(
                template.getEmotionKey(),
                template.getMoodName(),
                template.getMoodEmoji(),
                template.getMoodDesc(),
                moodPercent,
                avgConfidence,
                "avg3_" + template.getEmotionKey()
        );
    }

    private void openResultFromDecision(@NonNull HeamiEmotionRuleEngine.EmotionDecision decision) {
        Intent intent = new Intent(CheckInAiActivity.this, CheckInResultActivity.class);

        intent.putExtra("mood_name", decision.getMoodName());
        intent.putExtra("mood_emoji", decision.getMoodEmoji());
        intent.putExtra("mood_desc", decision.getMoodDesc());
        intent.putExtra("mood_percent", decision.getMoodPercent());

        intent.putExtra("source", "mediapipe_blendshape_avg3_" + decision.getEmotionKey());
        intent.putExtra("raw_emotion_label", decision.getEmotionKey());
        intent.putExtra("ai_confidence", decision.getConfidence());
        intent.putExtra("model_name", "MediaPipe Face Landmarker");
        intent.putExtra("model_version", "v5_rule_avg3_no_baseline_no_prompt");
        intent.putExtra("confidence_level", decision.getConfidence() >= 0.70f ? "high"
                : decision.getConfidence() >= 0.50f ? "medium" : "low");

        startActivity(intent);
    }

    private void clearEmotionSamples() {
        emotionSamples.clear();
        lastEmotionSampleTime = 0L;
        isNavigatingResult = false;
    }

    private static class FaceQualityResult {
        private final boolean valid;
        private final String message;

        FaceQualityResult(boolean valid, @NonNull String message) {
            this.valid = valid;
            this.message = message;
        }

        boolean isValid() {
            return valid;
        }

        String getMessage() {
            return message;
        }
    }

    private void startCheckInAnimations() {
        startFloatY(imgCheckInHeami, 5f, 4200, 0);

        startScan(viewCheckInScanLine, viewCheckInScanGlow);

        startGlowBreath(glowCheckInTopLeft, 0.55f, 0.78f, 5200, 0);
        startGlowBreath(glowCheckInBottomRight, 0.48f, 0.72f, 5600, 800);

        startFlowerFloat(decorCheckInFlowerTopLeft, 7f, 10f, 5200, 0);
        startFlowerFloat(decorCheckInFlowerTopRight, 6f, -8f, 5000, 500);
        startFlowerFloat(decorCheckInFlowerMiddleLeft, 8f, 12f, 5600, 900);
        startFlowerFloat(decorCheckInFlowerMiddleRight, 6f, -10f, 5300, 1300);
        startFlowerFloat(decorCheckInFlowerBottomRight, 7f, 8f, 5400, 1800);

        startSubtleButtonBreath(btnManualMood);
        startAlphaBreath(txtManualMoodHint, 0.72f, 1.0f, 2400);

        startEntranceFadeUp(txtCheckInAiLabel, 0);
        startEntranceFadeUp(txtCheckInAiTitle, 80);
        startEntranceFadeUp(cardCheckInBubble, 160);
        startEntranceFadeUp(cardCameraPreview, 240);
        startEntranceFadeUp(txtCheckInInstruction, 320);
        startEntranceFadeUp(btnManualMood, 420);
    }

    private void startFloatY(View view, float dpDistance, long duration, long delay) {
        if (view == null) return;

        float distancePx = dpDistance * getResources().getDisplayMetrics().density;

        ObjectAnimator moveY = ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_Y,
                0f,
                -distancePx,
                0f
        );
        moveY.setDuration(duration);
        moveY.setStartDelay(delay);
        moveY.setRepeatCount(ValueAnimator.INFINITE);
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.start();
    }

    private void startScan(View scanLine, View scanGlow) {
        if (scanLine == null || scanGlow == null) return;

        float distancePx = 95f * getResources().getDisplayMetrics().density;

        ObjectAnimator lineMove = ObjectAnimator.ofFloat(
                scanLine,
                View.TRANSLATION_Y,
                -distancePx,
                0f,
                distancePx
        );
        ObjectAnimator lineAlpha = ObjectAnimator.ofFloat(
                scanLine,
                View.ALPHA,
                0f,
                0.95f,
                0.95f,
                0f
        );

        ObjectAnimator glowMove = ObjectAnimator.ofFloat(
                scanGlow,
                View.TRANSLATION_Y,
                -distancePx,
                0f,
                distancePx
        );
        ObjectAnimator glowAlpha = ObjectAnimator.ofFloat(
                scanGlow,
                View.ALPHA,
                0f,
                0.65f,
                0.65f,
                0f
        );

        lineMove.setDuration(3600);
        lineAlpha.setDuration(3600);
        glowMove.setDuration(3600);
        glowAlpha.setDuration(3600);

        lineMove.setRepeatCount(ValueAnimator.INFINITE);
        lineAlpha.setRepeatCount(ValueAnimator.INFINITE);
        glowMove.setRepeatCount(ValueAnimator.INFINITE);
        glowAlpha.setRepeatCount(ValueAnimator.INFINITE);

        lineMove.setInterpolator(new AccelerateDecelerateInterpolator());
        lineAlpha.setInterpolator(new AccelerateDecelerateInterpolator());
        glowMove.setInterpolator(new AccelerateDecelerateInterpolator());
        glowAlpha.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(lineMove, lineAlpha, glowMove, glowAlpha);
        set.start();
    }

    private void startGlowBreath(View view, float fromAlpha, float toAlpha, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, fromAlpha, toAlpha, fromAlpha);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.98f, 1.05f, 0.98f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.98f, 1.05f, 0.98f);

        alpha.setDuration(duration);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);

        alpha.setStartDelay(delay);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);

        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY);
        set.start();
    }

    private void startFlowerFloat(View view, float dpDistance, float rotationDeg, long duration, long delay) {
        if (view == null) return;

        float distancePx = dpDistance * getResources().getDisplayMetrics().density;

        ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -distancePx, 0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, rotationDeg, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.65f, 1.0f, 0.65f);

        moveY.setDuration(duration);
        rotate.setDuration(duration);
        alpha.setDuration(duration);

        moveY.setStartDelay(delay);
        rotate.setStartDelay(delay);
        alpha.setStartDelay(delay);

        moveY.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveY, rotate, alpha);
        set.start();
    }

    private void startSubtleButtonBreath(View view) {
        if (view == null) return;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.94f, 1.0f, 0.94f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.012f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.012f, 1.0f);

        alpha.setDuration(2200);
        scaleX.setDuration(2200);
        scaleY.setDuration(2200);

        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scaleX, scaleY);
        set.start();
    }

    private void startAlphaBreath(View view, float fromAlpha, float toAlpha, long duration) {
        if (view == null) return;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, fromAlpha, toAlpha, fromAlpha);
        alpha.setDuration(duration);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.start();
    }

    private void startEntranceFadeUp(View view, long delay) {
        if (view == null) return;

        float distancePx = 10f * getResources().getDisplayMetrics().density;

        view.setAlpha(0f);
        view.setTranslationY(distancePx);

        ObjectAnimator fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
        ObjectAnimator move = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, distancePx, 0f);

        fade.setDuration(420);
        move.setDuration(420);

        fade.setStartDelay(delay);
        move.setStartDelay(delay);

        fade.setInterpolator(new AccelerateDecelerateInterpolator());
        move.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fade, move);
        set.start();
    }

    private Bitmap imageProxyToBitmap(@NonNull ImageProxy imageProxy) {
        try {
            byte[] nv21 = yuv420ToNv21(imageProxy);

            YuvImage yuvImage = new YuvImage(
                    nv21,
                    ImageFormat.NV21,
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    null
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            yuvImage.compressToJpeg(
                    new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
                    90,
                    outputStream
            );

            byte[] jpegBytes = outputStream.toByteArray();

            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    jpegBytes,
                    0,
                    jpegBytes.length
            );

            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            return rotateBitmap(bitmap, rotationDegrees);

        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (bitmap == null || rotationDegrees == 0) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );
    }

    private byte[] yuv420ToNv21(@NonNull ImageProxy imageProxy) {
        ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = imageProxy.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = imageProxy.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);

        byte[] vBytes = new byte[vSize];
        byte[] uBytes = new byte[uSize];

        vBuffer.get(vBytes);
        uBuffer.get(uBytes);

        int position = ySize;
        int chromaSize = Math.min(vBytes.length, uBytes.length);

        for (int i = 0; i < chromaSize; i++) {
            if (position + 1 >= nv21.length) {
                break;
            }
            nv21[position++] = vBytes[i];
            nv21[position++] = uBytes[i];
        }

        return nv21;
    }

    @Override
    protected void onResume() {
        super.onResume();
        stableQualityFrameCount = 0;
        clearEmotionSamples();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (faceLandmarkerHelper != null) {
            faceLandmarkerHelper.clear();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}

