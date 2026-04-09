package com.example.heami.ai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class EmotionClassifier implements Closeable {

    private static final String MODEL_FILE = "emotion_model.tflite";
    private static final String LABEL_FILE = "labels.txt";

    private static final int INPUT_SIZE = 160;
    private static final int CHANNEL_SIZE = 3;

    private final Interpreter interpreter;
    private final List<String> labels;

    public EmotionClassifier(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        interpreter = new Interpreter(loadModelFile(context), options);
        labels = loadLabels(context);
    }

    public EmotionResult classify(Bitmap bitmap) {
        if (bitmap == null) {
            return getFallbackResult();
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                INPUT_SIZE,
                INPUT_SIZE,
                true
        );

        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        float[][] output = new float[1][labels.size()];

        interpreter.run(inputBuffer, output);

        int maxIndex = 0;
        float maxConfidence = output[0][0];

        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > maxConfidence) {
                maxConfidence = output[0][i];
                maxIndex = i;
            }
        }

        String englishLabel = labels.get(maxIndex);
        return mapToHeamiMood(englishLabel, maxConfidence);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                4 * INPUT_SIZE * INPUT_SIZE * CHANNEL_SIZE
        );

        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(
                pixels,
                0,
                INPUT_SIZE,
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
        );

        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            // Model đã có preprocess_input của MobileNetV2 bên trong,
            // nên Android đưa pixel float dạng 0..255.
            byteBuffer.putFloat((float) r);
            byteBuffer.putFloat((float) g);
            byteBuffer.putFloat((float) b);
        }

        byteBuffer.rewind();
        return byteBuffer;
    }

    private EmotionResult mapToHeamiMood(String label, float confidence) {
        int percent = confidenceToPercent(confidence);

        switch (label) {
            case "happy":
                return new EmotionResult(
                        label,
                        "Vui vẻ",
                        "😊",
                        "Heami thấy bạn đang rất ổn!",
                        percent,
                        confidence
                );

            case "sad":
                return new EmotionResult(
                        label,
                        "Buồn",
                        "🥲",
                        "Hôm nay có gì nặng lòng không?",
                        percent,
                        confidence
                );

            case "angry":
                return new EmotionResult(
                        label,
                        "Tức giận",
                        "🤬",
                        "Có điều gì đó đang bất ổn trong bạn...",
                        percent,
                        confidence
                );

            case "fear":
                return new EmotionResult(
                        label,
                        "Sợ hãi",
                        "😭",
                        "Năng lượng của bạn có vẻ hơi yếu và dễ tổn thương...",
                        percent,
                        confidence
                );

            case "disgust":
                return new EmotionResult(
                        label,
                        "Ghê tởm",
                        "🤢",
                        "Cơ thể bạn có vẻ đang cần nghỉ ngơi...",
                        percent,
                        confidence
                );

            case "surprise":
            case "neutral":
            default:
                return new EmotionResult(
                        label,
                        "Căng thẳng",
                        "😤",
                        "Heami cảm nhận hôm nay bạn cần nghỉ nhẹ một chút...",
                        percent,
                        confidence
                );
        }
    }

    private int confidenceToPercent(float confidence) {
        int percent = Math.round(confidence * 100f);

        if (percent < 45) {
            percent = 45;
        }

        if (percent > 99) {
            percent = 99;
        }

        return percent;
    }

    private EmotionResult getFallbackResult() {
        return new EmotionResult(
                "unknown",
                "Căng thẳng",
                "😤",
                "Heami cảm nhận hôm nay bạn cần nghỉ nhẹ một chút...",
                68,
                0f
        );
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);

        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
        );
    }

    private List<String> loadLabels(Context context) throws IOException {
        List<String> result = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(LABEL_FILE))
        );

        String line;

        while ((line = reader.readLine()) != null) {
            String label = line.trim();

            if (!label.isEmpty()) {
                result.add(label);
            }
        }

        reader.close();

        return result;
    }

    @Override
    public void close() {
        interpreter.close();
    }

    public static class EmotionResult {
        private final String rawLabel;
        private final String moodName;
        private final String moodEmoji;
        private final String moodDesc;
        private final int moodPercent;
        private final float confidence;

        public EmotionResult(
                String rawLabel,
                String moodName,
                String moodEmoji,
                String moodDesc,
                int moodPercent,
                float confidence
        ) {
            this.rawLabel = rawLabel;
            this.moodName = moodName;
            this.moodEmoji = moodEmoji;
            this.moodDesc = moodDesc;
            this.moodPercent = moodPercent;
            this.confidence = confidence;
        }

        public String getRawLabel() {
            return rawLabel;
        }

        public String getMoodName() {
            return moodName;
        }

        public String getMoodEmoji() {
            return moodEmoji;
        }

        public String getMoodDesc() {
            return moodDesc;
        }

        public int getMoodPercent() {
            return moodPercent;
        }

        public float getConfidence() {
            return confidence;
        }
    }
}