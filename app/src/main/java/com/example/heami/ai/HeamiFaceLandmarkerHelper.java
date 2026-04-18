package com.example.heami.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

public class HeamiFaceLandmarkerHelper {

    public interface LandmarkerListener {
        void onResult(
                @NonNull FaceLandmarkerResult result,
                int imageWidth,
                int imageHeight
        );

        void onError(@NonNull String errorMessage);
    }

    private static final String MODEL_NAME = "face_landmarker.task";

    private static final float MIN_FACE_DETECTION_CONFIDENCE = 0.5f;
    private static final float MIN_FACE_PRESENCE_CONFIDENCE = 0.5f;
    private static final float MIN_TRACKING_CONFIDENCE = 0.5f;

    private final Context appContext;
    private final LandmarkerListener listener;

    @Nullable
    private FaceLandmarker faceLandmarker;

    public HeamiFaceLandmarkerHelper(
            @NonNull Context context,
            @Nullable LandmarkerListener listener
    ) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
        setupFaceLandmarker();
    }

    private void setupFaceLandmarker() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath(MODEL_NAME)
                    .build();

            FaceLandmarker.FaceLandmarkerOptions options =
                    FaceLandmarker.FaceLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setRunningMode(RunningMode.LIVE_STREAM)
                            .setNumFaces(1)
                            .setMinFaceDetectionConfidence(MIN_FACE_DETECTION_CONFIDENCE)
                            .setMinFacePresenceConfidence(MIN_FACE_PRESENCE_CONFIDENCE)
                            .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
                            .setOutputFaceBlendshapes(true)
                            .setOutputFacialTransformationMatrixes(true)
                            .setResultListener((result, inputImage) -> {
                                if (this.listener != null) {
                                    this.listener.onResult(
                                            result,
                                            inputImage.getWidth(),
                                            inputImage.getHeight()
                                    );
                                }
                            })
                            .setErrorListener(error -> {
                                if (this.listener != null) {
                                    String message = error != null && error.getMessage() != null
                                            ? error.getMessage()
                                            : "Unknown MediaPipe error";
                                    this.listener.onError(message);
                                }
                            })
                            .build();

            faceLandmarker = FaceLandmarker.createFromOptions(appContext, options);

        } catch (Exception e) {
            faceLandmarker = null;

            if (listener != null) {
                String message = e.getMessage() != null
                        ? e.getMessage()
                        : "Không thể khởi tạo MediaPipe Face Landmarker";
                listener.onError(message);
            }
        }
    }

    public boolean isReady() {
        return faceLandmarker != null;
    }

    public void detectAsync(@Nullable Bitmap bitmap, long frameTimestampMs) {
        if (bitmap == null || faceLandmarker == null) {
            return;
        }

        Bitmap inputBitmap;

        if (bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
            inputBitmap = bitmap;
        } else {
            inputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }

        MPImage mpImage = new BitmapImageBuilder(inputBitmap).build();

        long timestamp = frameTimestampMs > 0
                ? frameTimestampMs
                : SystemClock.uptimeMillis();

        faceLandmarker.detectAsync(mpImage, timestamp);
    }

    public void clear() {
        if (faceLandmarker != null) {
            faceLandmarker.close();
            faceLandmarker = null;
        }
    }
}