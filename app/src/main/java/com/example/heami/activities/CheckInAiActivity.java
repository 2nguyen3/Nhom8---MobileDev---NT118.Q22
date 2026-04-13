package com.example.heami.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.core.CameraInfo;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Size;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import android.graphics.Rect;
import android.graphics.Bitmap;

import com.example.heami.ai.EmotionClassifier;
import com.example.heami.ai.ImageProxyUtils;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class CheckInAiActivity extends AppCompatActivity {

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

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    private PreviewView previewCheckInCamera;
    private ExecutorService cameraExecutor;

    private FaceDetector faceDetector;
    private EmotionClassifier emotionClassifier;
    private Bitmap latestCameraBitmap;

    private boolean isProcessingFrame = false;
    private long lastAnalyzeTime = 0L;
    private int stableFaceCount = 0;

    private Rect lastFaceBounds = null;
    private float lastHeadY = 0f;
    private float lastHeadZ = 0f;

    private boolean isNavigatingResult = false;
    private static final int REQUIRED_EMOTION_SAMPLES = 3;
    private final List<float[]> emotionScoreSamples = new ArrayList<>();
    private long lastEmotionSampleTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin_ai);

        bindViews();
        setupActions();
        startCheckInAnimations();

        cameraExecutor = Executors.newSingleThreadExecutor();
        setupFaceDetector();
        setupEmotionClassifier();
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
                stableFaceCount = 0;
                clearEmotionSamples();
                isNavigatingResult = false;

                Toast.makeText(
                        CheckInAiActivity.this,
                        "Heami đang quét lại khuôn mặt của bạn...",
                        Toast.LENGTH_SHORT
                ).show();
            });
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

    private void setupFaceDetector() {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();

        faceDetector = FaceDetection.getClient(options);
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
                        "Bạn cần cấp quyền camera để Heami nhận diện cảm xúc nha",
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

                CameraSelector anyAvailableCamera =
                        new CameraSelector.Builder()
                                .addCameraFilter(cameraInfos -> {
                                    List<CameraInfo> result = new ArrayList<>();

                                    if (!cameraInfos.isEmpty()) {
                                        result.add(cameraInfos.get(0));
                                    }

                                    return result;
                                })
                                .build();

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        anyAvailableCamera,
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
        long now = System.currentTimeMillis();

        // Giảm tải, chỉ phân tích khoảng mỗi 900ms/lần
        if (now - lastAnalyzeTime < 900 || isProcessingFrame) {
            imageProxy.close();
            return;
        }

        lastAnalyzeTime = now;
        isProcessingFrame = true;

        try {
            latestCameraBitmap = ImageProxyUtils.imageProxyToBitmap(imageProxy);
        } catch (Exception e) {
            latestCameraBitmap = null;
        }

        if (latestCameraBitmap == null) {
            isProcessingFrame = false;
            imageProxy.close();
            return;
        }

        // Quan trọng: ML Kit detect trên chính Bitmap dùng để crop
        InputImage image = InputImage.fromBitmap(latestCameraBitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(this::handleDetectedFaces)
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    stableFaceCount = 0;
                    clearEmotionSamples();

                    if (txtCheckInInstruction != null) {
                        txtCheckInInstruction.setText(
                                "Heami chưa quét được khuôn mặt, thử giữ máy ổn định hơn nha"
                        );
                    }
                }))
                .addOnCompleteListener(task -> {
                    isProcessingFrame = false;
                    imageProxy.close();
                });
    }

    private void handleDetectedFaces(List<Face> faces) {
        if (faces == null || faces.isEmpty()) {
            stableFaceCount = 0;
            lastFaceBounds = null;
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

        Face face = faces.get(0);

        Rect currentBounds = face.getBoundingBox();

        float headY = Math.abs(face.getHeadEulerAngleY());
        float headZ = Math.abs(face.getHeadEulerAngleZ());

        boolean isLookingAway = headY > 18f || headZ > 15f;
        boolean isMoving = isFaceMoving(currentBounds, headY, headZ);

        String message;

        if (isLookingAway) {
            stableFaceCount = 0;
            clearEmotionSamples();
            message = "Heami thấy mặt bạn rồi, thử nhìn thẳng vào camera hơn một chút nha";
        } else if (isMoving) {
            stableFaceCount = 0;
            clearEmotionSamples();
            message = "Bạn đang di chuyển hơi nhiều, giữ yên khuôn mặt một chút nha";
        } else {
            stableFaceCount++;

            if (stableFaceCount < 3) {
                message = "Heami đã thấy bạn rồi, giữ yên thêm một chút nhé...";
            } else {
                message = "Khuôn mặt đã ổn định. Heami đang phân tích cảm xúc của bạn...";

                if (!isNavigatingResult) {
                    collectEmotionSampleAndMaybeOpen(face);
                }
            }
        }

        lastFaceBounds = new Rect(currentBounds);
        lastHeadY = headY;
        lastHeadZ = headZ;

        runOnUiThread(() -> {
            if (txtCheckInInstruction != null) {
                txtCheckInInstruction.setText(message);
            }
        });
    }

    private boolean isFaceMoving(Rect currentBounds, float currentHeadY, float currentHeadZ) {
        if (lastFaceBounds == null || currentBounds == null) {
            return false;
        }

        int currentCenterX = currentBounds.centerX();
        int currentCenterY = currentBounds.centerY();

        int lastCenterX = lastFaceBounds.centerX();
        int lastCenterY = lastFaceBounds.centerY();

        int deltaX = Math.abs(currentCenterX - lastCenterX);
        int deltaY = Math.abs(currentCenterY - lastCenterY);

        int currentWidth = currentBounds.width();
        int currentHeight = currentBounds.height();

        int lastWidth = lastFaceBounds.width();
        int lastHeight = lastFaceBounds.height();

        int deltaWidth = Math.abs(currentWidth - lastWidth);
        int deltaHeight = Math.abs(currentHeight - lastHeight);

        float deltaHeadY = Math.abs(currentHeadY - lastHeadY);
        float deltaHeadZ = Math.abs(currentHeadZ - lastHeadZ);

        return deltaX > 18
                || deltaY > 18
                || deltaWidth > 22
                || deltaHeight > 22
                || deltaHeadY > 8f
                || deltaHeadZ > 8f;
    }

    private void openFallbackResult() {
        Intent intent = new Intent(CheckInAiActivity.this, CheckInResultActivity.class);

        intent.putExtra("mood_name", "Căng thẳng");
        intent.putExtra("mood_emoji", "😤");
        intent.putExtra("mood_desc", "Heami cảm nhận hôm nay bạn cần nghỉ nhẹ một chút...");
        intent.putExtra("mood_percent", 68);
        intent.putExtra("source", "ai_camera_tflite_fallback");
        intent.putExtra("raw_emotion_label", "unknown");
        intent.putExtra("ai_confidence", 0f);
        intent.putExtra("model_name", "RAF-DB MobileNetV2");
        intent.putExtra("model_version", "v1");

        startActivity(intent);
    }

    private void setupEmotionClassifier() {
        try {
            emotionClassifier = new EmotionClassifier(this);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Không thể tải model AI cảm xúc: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void clearEmotionSamples() {
        emotionScoreSamples.clear();
        lastEmotionSampleTime = 0L;
    }

    private Bitmap getFaceBitmap(Face face) {
        try {
            if (latestCameraBitmap != null && face != null) {
                Rect faceBounds = face.getBoundingBox();
                return ImageProxyUtils.cropFace(latestCameraBitmap, faceBounds);
            }
        } catch (Exception e) {
            return latestCameraBitmap;
        }

        return latestCameraBitmap;
    }

    private void collectEmotionSampleAndMaybeOpen(Face face) {
        if (emotionClassifier == null) {
            openFallbackResult();
            return;
        }

        long now = System.currentTimeMillis();

        // Tránh lấy mẫu quá sát nhau
        if (now - lastEmotionSampleTime < 450) {
            return;
        }

        Bitmap faceBitmap = getFaceBitmap(face);
        float[] scores = emotionClassifier.predictScores(faceBitmap);

        if (scores == null) {
            return;
        }

        emotionScoreSamples.add(scores);
        lastEmotionSampleTime = now;

        int currentSample = emotionScoreSamples.size();

        runOnUiThread(() -> {
            if (txtCheckInInstruction != null) {
                txtCheckInInstruction.setText(
                        "Heami đang phân tích cảm xúc của bạn... (" +
                                currentSample + "/" + REQUIRED_EMOTION_SAMPLES + ")"
                );
            }
        });

        if (currentSample >= REQUIRED_EMOTION_SAMPLES) {
            isNavigatingResult = true;

            float[] averagedScores = averageEmotionScores();
            cardCameraPreview.postDelayed(() -> openResultFromScores(averagedScores), 500);
        }
    }

    private float[] averageEmotionScores() {
        if (emotionScoreSamples.isEmpty()) {
            return null;
        }

        int length = emotionScoreSamples.get(0).length;
        float[] averagedScores = new float[length];

        for (float[] scores : emotionScoreSamples) {
            for (int i = 0; i < length; i++) {
                averagedScores[i] += scores[i];
            }
        }

        for (int i = 0; i < length; i++) {
            averagedScores[i] /= emotionScoreSamples.size();
        }

        return averagedScores;
    }

    private void openResultFromScores(float[] averagedScores) {
        if (emotionClassifier == null) {
            openFallbackResult();
            return;
        }

        Log.d(
                "HeamiEmotion",
                "AVG scores: " + emotionClassifier.formatScores(averagedScores)
        );

        EmotionClassifier.EmotionResult result =
                emotionClassifier.classifyFromScores(averagedScores);

        Intent intent = new Intent(CheckInAiActivity.this, CheckInResultActivity.class);
        intent.putExtra("mood_name", result.getMoodName());
        intent.putExtra("mood_emoji", result.getMoodEmoji());
        intent.putExtra("mood_desc", result.getMoodDesc());
        intent.putExtra("mood_percent", result.getMoodPercent());

        intent.putExtra("source", "ai_camera_tflite_avg3_" + result.getRawLabel());
        intent.putExtra("raw_emotion_label", result.getRawLabel());
        intent.putExtra("ai_confidence", result.getConfidence());
        intent.putExtra("model_name", "RAF-DB MobileNetV2");
        intent.putExtra("model_version", "v1_avg3");

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        isNavigatingResult = false;
        stableFaceCount = 0;
        lastFaceBounds = null;
        clearEmotionSamples();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (faceDetector != null) {
            faceDetector.close();
        }

        if (emotionClassifier != null) {
            emotionClassifier.close();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}