package com.example.heami.ai;

import androidx.annotation.NonNull;

import com.google.mediapipe.tasks.components.containers.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HeamiEmotionRuleEngine {

    public static class EmotionDecision {
        private final String emotionKey;
        private final String moodName;
        private final String moodEmoji;
        private final String moodDesc;
        private final int moodPercent;
        private final float confidence;
        private final String debugSummary;

        public EmotionDecision(
                @NonNull String emotionKey,
                @NonNull String moodName,
                @NonNull String moodEmoji,
                @NonNull String moodDesc,
                int moodPercent,
                float confidence,
                @NonNull String debugSummary
        ) {
            this.emotionKey = emotionKey;
            this.moodName = moodName;
            this.moodEmoji = moodEmoji;
            this.moodDesc = moodDesc;
            this.moodPercent = moodPercent;
            this.confidence = confidence;
            this.debugSummary = debugSummary;
        }

        public String getEmotionKey() {
            return emotionKey;
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

        public String getDebugSummary() {
            return debugSummary;
        }
    }

    @NonNull
    public EmotionDecision inferFromBlendshapes(@NonNull List<Category> blendshapes) {
        Map<String, Float> scoreMap = toScoreMap(blendshapes);

        float mouthSmile = avg(
                get(scoreMap, "mouthSmileLeft"),
                get(scoreMap, "mouthSmileRight")
        );

        float mouthFrown = avg(
                get(scoreMap, "mouthFrownLeft"),
                get(scoreMap, "mouthFrownRight")
        );

        float browDownRaw = avg(
                get(scoreMap, "browDownLeft"),
                get(scoreMap, "browDownRight")
        );

        float browOuterUp = avg(
                get(scoreMap, "browOuterUpLeft"),
                get(scoreMap, "browOuterUpRight")
        );

        float eyeWide = avg(
                get(scoreMap, "eyeWideLeft"),
                get(scoreMap, "eyeWideRight")
        );

        float eyeSquintRaw = avg(
                get(scoreMap, "eyeSquintLeft"),
                get(scoreMap, "eyeSquintRight")
        );

        float cheekSquint = avg(
                get(scoreMap, "cheekSquintLeft"),
                get(scoreMap, "cheekSquintRight")
        );

        float noseSneer = avg(
                get(scoreMap, "noseSneerLeft"),
                get(scoreMap, "noseSneerRight")
        );

        float mouthStretch = avg(
                get(scoreMap, "mouthStretchLeft"),
                get(scoreMap, "mouthStretchRight")
        );

        float jawOpen = get(scoreMap, "jawOpen");
        float browInnerUp = get(scoreMap, "browInnerUp");
        float mouthPucker = get(scoreMap, "mouthPucker");
        float mouthShrugUpper = get(scoreMap, "mouthShrugUpper");
        float mouthShrugLower = get(scoreMap, "mouthShrugLower");

        float mouthPressRaw = mouthPressProxy(mouthPucker, mouthShrugUpper);

        float browDown = clamp01(browDownRaw);
        float eyeSquint = clamp01(eyeSquintRaw);
        float mouthPress = clamp01(mouthPressRaw);

        float happyScore =
                0.74f * mouthSmile +
                        0.07f * cheekSquint +
                        0.03f * eyeSquint +
                        0.16f * (1f - mouthFrown);

        float sadScore =
                0.76f * mouthFrown +
                        0.10f * browInnerUp +
                        0.08f * mouthShrugLower +
                        0.10f * (1f - mouthSmile);

        float angryScore =
                0.46f * browDown +
                        0.10f * eyeSquint +
                        0.16f * noseSneer +
                        0.06f * mouthPress +
                        0.08f * mouthFrown +
                        0.04f * mouthStretch;

        float fearScore =
                0.16f * eyeWide +
                        0.12f * jawOpen +
                        0.16f * browInnerUp +
                        0.08f * browOuterUp +
                        0.06f * mouthStretch;

        float disgustScore =
                0.46f * noseSneer +
                        0.16f * mouthShrugUpper +
                        0.12f * mouthShrugLower +
                        0.10f * browDown +
                        0.08f * (1f - mouthSmile);

        float stressScore =
                0.40f * browDown +
                        0.32f * eyeSquint +
                        0.08f * mouthPress +
                        0.06f * mouthFrown +
                        0.04f * mouthStretch +
                        0.06f * (1f - mouthSmile);

        happyScore = clamp01(happyScore);
        sadScore = clamp01(sadScore);
        angryScore = clamp01(angryScore);
        fearScore = clamp01(fearScore);
        disgustScore = clamp01(disgustScore);
        stressScore = clamp01(stressScore);

        if (mouthSmile > 0.26f && mouthFrown < 0.03f) {
            happyScore = Math.max(
                    happyScore,
                    0.48f + (0.42f * mouthSmile)
            );
            happyScore = clamp01(happyScore);

            stressScore *= 0.72f;
            angryScore *= 0.74f;
            sadScore *= 0.86f;
        }

        if (mouthFrown > 0.04f && mouthSmile < 0.18f) {
            sadScore = Math.max(
                    sadScore,
                    0.34f + (1.22f * mouthFrown) + (0.04f * browInnerUp)
            );
            sadScore = clamp01(sadScore);

            if (browDown < 0.04f && noseSneer < 0.03f) {
                stressScore *= 0.80f;
                angryScore *= 0.42f;
            } else if (browDown < 0.08f && noseSneer < 0.03f) {
                stressScore *= 0.84f;
                angryScore *= 0.62f;
            }
        }

        if (eyeSquint > 0.27f
                && browDown > 0.010f
                && mouthSmile < 0.22f
                && mouthFrown < 0.05f) {
            stressScore = Math.max(
                    stressScore,
                    0.34f + (0.12f * eyeSquint) + (0.22f * browDown)
            );
            stressScore = clamp01(stressScore);
        }

        if (eyeSquint > 0.30f
                && browDown > 0.012f
                && mouthSmile < 0.22f) {
            stressScore = Math.max(
                    stressScore,
                    0.36f + (0.18f * browDown) + (0.15f * eyeSquint)
            );
            stressScore = clamp01(stressScore);
        }

        if (browDown > 0.08f
                && eyeSquint > 0.20f
                && mouthSmile < 0.20f) {
            angryScore = Math.max(
                    angryScore,
                    0.34f + (0.26f * browDown) + (0.06f * mouthFrown) + (0.06f * eyeSquint)
            );
            angryScore = clamp01(angryScore);
        }

        if (browDown > 0.08f
                && mouthPress > 0.14f
                && eyeSquint > 0.20f
                && mouthSmile < 0.18f) {
            angryScore = Math.max(
                    angryScore,
                    0.38f + (0.20f * browDown) + (0.10f * mouthPress)
            );
            angryScore = clamp01(angryScore);
        }

        if (mouthPress > 0.10f && browDown < 0.04f && mouthFrown < 0.04f) {
            angryScore *= 0.52f;
        }

        if (eyeWide < 0.20f && jawOpen < 0.16f) {
            fearScore *= 0.45f;
        }
        if (eyeWide < 0.18f && jawOpen < 0.14f && browInnerUp < 0.10f) {
            fearScore *= 0.65f;
        }
        if (mouthFrown > 0.04f && eyeWide < 0.24f && jawOpen < 0.18f) {
            fearScore *= 0.72f;
        }

        if ((eyeWide > 0.25f || browInnerUp > 0.18f || jawOpen > 0.20f)
                && mouthFrown < 0.04f
                && eyeSquint < 0.20f) {
            fearScore = Math.max(
                    fearScore,
                    0.18f + (0.12f * eyeWide) + (0.08f * browInnerUp) + (0.06f * jawOpen)
            );
            fearScore = clamp01(fearScore);
        }

        if (noseSneer < 0.03f) {
            disgustScore *= 0.50f;
        }

        if (eyeSquint > 0.35f && browDown < 0.03f && mouthPress < 0.05f && mouthFrown < 0.03f) {
            stressScore *= 0.76f;
        }

        happyScore = clamp01(happyScore);
        sadScore = clamp01(sadScore);
        angryScore = clamp01(angryScore);
        fearScore = clamp01(fearScore);
        disgustScore = clamp01(disgustScore);
        stressScore = clamp01(stressScore);

        float maxExpressiveScore = Math.max(
                Math.max(happyScore, sadScore),
                Math.max(
                        Math.max(Math.max(angryScore, fearScore), disgustScore),
                        stressScore
                )
        );

        float neutralScore = clamp01(0.36f - (maxExpressiveScore * 0.16f));

        if (mouthSmile < 0.18f
                && mouthFrown < 0.03f
                && browDown < 0.04f
                && mouthPress < 0.08f
                && eyeSquint < 0.28f) {
            neutralScore = Math.max(neutralScore, 0.30f);
        } else {
            neutralScore = Math.min(neutralScore, 0.24f);
        }

        Map<String, Float> emotionScores = new HashMap<>();
        emotionScores.put("happy", happyScore);
        emotionScores.put("sad", sadScore);
        emotionScores.put("angry", angryScore);
        emotionScores.put("fear", fearScore);
        emotionScores.put("disgust", disgustScore);
        emotionScores.put("stress", stressScore);
        emotionScores.put("neutral", neutralScore);

        String bestEmotion = "neutral";
        float bestScore = -1f;
        float secondBestScore = -1f;

        for (Map.Entry<String, Float> entry : emotionScores.entrySet()) {
            float value = entry.getValue();

            if (value > bestScore) {
                secondBestScore = bestScore;
                bestScore = value;
                bestEmotion = entry.getKey();
            } else if (value > secondBestScore) {
                secondBestScore = value;
            }
        }

        float scoreGap = bestScore - Math.max(secondBestScore, 0f);

        if (bestEmotion.equals("neutral")) {
            if (happyScore > 0.30f && mouthSmile > 0.24f) {
                bestEmotion = "happy";
                bestScore = happyScore;
            } else if (sadScore > 0.26f && mouthFrown > 0.04f) {
                bestEmotion = "sad";
                bestScore = sadScore;
            } else if (stressScore > 0.24f && browDown > 0.01f) {
                bestEmotion = "stress";
                bestScore = stressScore;
            } else if (fearScore > 0.30f && eyeWide > 0.24f) {
                bestEmotion = "fear";
                bestScore = fearScore;
            }
        } else if (scoreGap < 0.05f) {
            if (happyScore >= sadScore && happyScore >= stressScore && mouthSmile > 0.24f) {
                bestEmotion = "happy";
                bestScore = happyScore;
            } else if (sadScore >= stressScore && mouthFrown > 0.04f) {
                bestEmotion = "sad";
                bestScore = sadScore;
            } else if (angryScore >= stressScore && browDown > 0.08f) {
                bestEmotion = "angry";
                bestScore = angryScore;
            } else if (stressScore > 0.24f && browDown > 0.01f) {
                bestEmotion = "stress";
                bestScore = stressScore;
            } else if (fearScore > 0.30f && eyeWide > 0.24f) {
                bestEmotion = "fear";
                bestScore = fearScore;
            }
        }

        if (bestScore < 0.19f && neutralScore >= 0.22f) {
            bestEmotion = "neutral";
            bestScore = neutralScore;
        }

        int moodPercent = Math.round(clamp01(bestScore) * 100f);

        String debugSummary = String.format(
                Locale.US,
                "happy=%.3f | sad=%.3f | angry=%.3f | fear=%.3f | disgust=%.3f | stress=%.3f | neutral=%.3f | browDown=%.3f(raw=%.3f) | eyeSquint=%.3f(raw=%.3f) | mouthFrown=%.3f | noseSneer=%.3f | mouthPress=%.3f(raw=%.3f) | gap=%.3f",
                happyScore,
                sadScore,
                angryScore,
                fearScore,
                disgustScore,
                stressScore,
                neutralScore,
                browDown,
                browDownRaw,
                eyeSquint,
                eyeSquintRaw,
                mouthFrown,
                noseSneer,
                mouthPress,
                mouthPressRaw,
                scoreGap
        );

        switch (bestEmotion) {
            case "happy":
                return new EmotionDecision(
                        "happy",
                        "Vui vẻ",
                        "😊",
                        "Heami cảm nhận bạn đang có năng lượng khá tích cực.",
                        Math.max(moodPercent, 60),
                        bestScore,
                        debugSummary
                );

            case "sad":
                return new EmotionDecision(
                        "sad",
                        "Buồn",
                        "🥲",
                        "Heami thấy hôm nay bạn có vẻ hơi nặng lòng một chút.",
                        Math.max(moodPercent, 58),
                        bestScore,
                        debugSummary
                );

            case "angry":
                return new EmotionDecision(
                        "angry",
                        "Tức giận",
                        "😤",
                        "Heami cảm nhận có điều gì đó đang khiến bạn khá khó chịu.",
                        Math.max(moodPercent, 60),
                        bestScore,
                        debugSummary
                );

            case "fear":
                return new EmotionDecision(
                        "fear",
                        "Lo lắng",
                        "😟",
                        "Heami thấy bạn đang hơi lo lắng hoặc thiếu cảm giác an toàn.",
                        Math.max(moodPercent, 58),
                        bestScore,
                        debugSummary
                );

            case "disgust":
                return new EmotionDecision(
                        "disgust",
                        "Khó chịu",
                        "😣",
                        "Heami cảm nhận bạn đang có phản ứng khá khó chịu với điều gì đó.",
                        Math.max(moodPercent, 58),
                        bestScore,
                        debugSummary
                );

            case "stress":
                return new EmotionDecision(
                        "stress",
                        "Căng thẳng",
                        "😮‍💨",
                        "Heami cảm nhận bạn đang hơi căng thẳng và cần thả lỏng một chút.",
                        Math.max(moodPercent, 60),
                        bestScore,
                        debugSummary
                );

            default:
                return new EmotionDecision(
                        "neutral",
                        "Bình thường",
                        "😌",
                        "Heami thấy trạng thái của bạn hiện tại khá ổn định.",
                        Math.max(moodPercent, 55),
                        Math.max(bestScore, 0.55f),
                        debugSummary
                );
        }
    }

    @NonNull
    private Map<String, Float> toScoreMap(@NonNull List<Category> blendshapes) {
        Map<String, Float> map = new HashMap<>();

        for (Category category : blendshapes) {
            if (category == null) continue;

            String name = category.categoryName();
            float score = category.score();

            if (name == null) continue;

            map.put(name, score);
        }

        return map;
    }

    private float get(@NonNull Map<String, Float> map, @NonNull String key) {
        Float value = map.get(key);
        return value != null ? value : 0f;
    }

    private float avg(float a, float b) {
        return (a + b) / 2f;
    }

    private float mouthPressProxy(float mouthPucker, float mouthShrugUpper) {
        return clamp01((mouthPucker * 0.6f) + (mouthShrugUpper * 0.4f));
    }

    private float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }
}