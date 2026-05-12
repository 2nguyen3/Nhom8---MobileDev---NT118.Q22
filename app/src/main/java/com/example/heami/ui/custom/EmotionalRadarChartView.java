package com.example.heami.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class EmotionalRadarChartView extends View {

    private Paint webPaint;
    private Paint linePaint;
    private Paint fillPaint;
    private Paint textPaint;

    // Các nhãn cảm xúc và thứ tự góc vẽ (5 góc đều nhau 72 độ)
    private final String[] labels = {"😊 Vui vẻ", "😌 Bình yên", "😮‍💨 Áp lực", "😢 Buồn", "😡 Giận"};
    // Dữ liệu mặc định dạng tỉ lệ (phần trăm từ 0.0 đến 1.0)
    private float[] data = {0.45f, 0.25f, 0.15f, 0.10f, 0.05f};

    public EmotionalRadarChartView(Context context) {
        super(context);
        init();
    }

    public EmotionalRadarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EmotionalRadarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        webPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        webPaint.setColor(Color.parseColor("#E0E4F0"));
        webPaint.setStrokeWidth(1f * density);
        webPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#00BFA5")); // Màu chủ đạo Teal
        linePaint.setStrokeWidth(2f * density);
        linePaint.setStyle(Paint.Style.STROKE);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#2600BFA5")); // Teal trong suốt
        fillPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#313866"));
        textPaint.setTextSize(11f * density);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    }

    public void setData(float happy, float calm, float stressed, float sad, float angry) {
        data[0] = happy;
        data[1] = calm;
        data[2] = stressed;
        data[3] = sad;
        data[4] = angry;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int count = labels.length;
        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        
        // Bán kính biểu đồ radar (Chừa lề 32dp để nhãn top 'Vui vẻ' có đủ không gian chiều cao không bị cắt)
        float density = getResources().getDisplayMetrics().density;
        float maxRadius = Math.min(centerX, centerY) - 32f * density;
        if (maxRadius < 10) return;

        double angleStep = 2 * Math.PI / count;

        // 1. Vẽ các đường đa giác lưới nhện (4 mức: 25%, 50%, 75%, 100%)
        for (int r = 1; r <= 4; r++) {
            float radius = maxRadius / 4f * r;
            Path webPath = new Path();
            for (int i = 0; i < count; i++) {
                double angle = i * angleStep - Math.PI / 2; // Bắt đầu hướng 12h
                float x = (float) (centerX + radius * Math.cos(angle));
                float y = (float) (centerY + radius * Math.sin(angle));
                if (i == 0) {
                    webPath.moveTo(x, y);
                } else {
                    webPath.lineTo(x, y);
                }
            }
            webPath.close();
            canvas.drawPath(webPath, webPaint);
        }

        // 2. Vẽ các trục tia từ tâm ra 5 đỉnh
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep - Math.PI / 2;
            float x = (float) (centerX + maxRadius * Math.cos(angle));
            float y = (float) (centerY + maxRadius * Math.sin(angle));
            canvas.drawLine(centerX, centerY, x, y, webPaint);
        }

        // 3. Vẽ vùng biểu diễn giá trị dữ liệu cảm xúc thực tế
        Path dataPath = new Path();
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep - Math.PI / 2;
            float valRadius = data[i] * maxRadius;
            float x = (float) (centerX + valRadius * Math.cos(angle));
            float y = (float) (centerY + valRadius * Math.sin(angle));
            if (i == 0) {
                dataPath.moveTo(x, y);
            } else {
                dataPath.lineTo(x, y);
            }
        }
        dataPath.close();
        canvas.drawPath(dataPath, fillPaint);
        canvas.drawPath(dataPath, linePaint);

        // 4. Viết nhãn cảm xúc và số % quanh 5 đỉnh (Vẽ 2 dòng để tránh tràn viền)
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep - Math.PI / 2;
            float labelRadius = maxRadius + 6f * density;
            float x = (float) (centerX + labelRadius * Math.cos(angle));
            float y = (float) (centerY + labelRadius * Math.sin(angle));

            String line1 = labels[i];
            String line2 = "(" + Math.round(data[i] * 100) + "%)";

            float w1 = textPaint.measureText(line1);
            float w2 = textPaint.measureText(line2);

            float drawX1 = x;
            float drawX2 = x;
            float drawY1 = y;
            float drawY2 = y + 12f * density;

            if (Math.abs(Math.cos(angle)) < 0.1) { // Đỉnh trên hoặc dưới
                drawX1 = x - w1 / 2f;
                drawX2 = x - w2 / 2f;
                if (Math.sin(angle) < 0) { // Đỉnh trên
                    drawY1 = y - 14f * density;
                    drawY2 = y;
                } else { // Đỉnh dưới
                    drawY1 = y + 8f * density;
                    drawY2 = y + 20f * density;
                }
            } else if (Math.cos(angle) > 0) { // Bên phải (Đặc biệt là Bình yên)
                // Căn chỉnh thụt lề thông minh cho bên phải
                drawX1 = x;
                drawX2 = x + 4f * density; // Thụt lề nhẹ cho dòng 2 tỉ lệ %
                drawY1 = y - 4f * density;
                drawY2 = y + 8f * density;
            } else { // Bên trái (Áp lực, Buồn...)
                drawX1 = x - w1;
                drawX2 = x - w2 - 4f * density;
                drawY1 = y - 4f * density;
                drawY2 = y + 8f * density;
            }

            canvas.drawText(line1, drawX1, drawY1, textPaint);
            canvas.drawText(line2, drawX2, drawY2, textPaint);
        }
    }
}
