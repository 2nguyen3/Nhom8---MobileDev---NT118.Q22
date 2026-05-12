package com.example.heami.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class WeeklyLineChartView extends View {

    private Paint gridPaint;
    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointPaint;
    private Paint pointInnerPaint;
    private Paint textPaint;

    private float[] weeklyMoods = {4f, 3f, 5f, 4f, 5f, 5f, 4f};
    private String[] daysOfWeek = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    private String[] moodEmojis = {"😡", "😢", "😮‍💨", "😌", "😊"};

    public WeeklyLineChartView(Context context) {
        super(context);
        init();
    }

    public WeeklyLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeeklyLineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F0F0F0"));
        gridPaint.setStrokeWidth(1f * density);
        gridPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#00BFA5"));
        linePaint.setStrokeWidth(3f * density);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.parseColor("#00BFA5"));
        pointPaint.setStyle(Paint.Style.FILL);

        pointInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointInnerPaint.setColor(Color.WHITE);
        pointInnerPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#7D8BB7"));
        textPaint.setTextSize(11f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setWeeklyMoods(float[] moods) {
        if (moods != null && moods.length == 7) {
            this.weeklyMoods = moods;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float paddingLeft = 18f * density;
        float paddingRight = 8f * density;
        float paddingTop = 20f * density;
        float paddingBottom = 30f * density;

        float width = getWidth();
        float height = getHeight();

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        for (int i = 0; i < 5; i++) {
            float y = paddingTop + (chartHeight / 4f) * (4 - i);
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);

            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(moodEmojis[i], 0f, y + 4f * density, textPaint);
        }

        float[] pointsX = new float[7];
        float[] pointsY = new float[7];

        for (int i = 0; i < 7; i++) {
            pointsX[i] = paddingLeft + (chartWidth / 6f) * i;
            float moodVal = weeklyMoods[i];
            float normalizedMood = (moodVal - 1f) / 4f;
            pointsY[i] = paddingTop + chartHeight * (1f - normalizedMood);
        }

        Path fillPath = new Path();
        fillPath.moveTo(pointsX[0], paddingTop + chartHeight);
        for (int i = 0; i < 7; i++) {
            fillPath.lineTo(pointsX[i], pointsY[i]);
        }
        fillPath.lineTo(pointsX[6], paddingTop + chartHeight);
        fillPath.close();

        @SuppressLint("DrawAllocation") LinearGradient fillShader = new LinearGradient(
                0, paddingTop, 0, paddingTop + chartHeight,
                Color.parseColor("#3300BFA5"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        );
        fillPaint.setShader(fillShader);
        canvas.drawPath(fillPath, fillPaint);

        Path linePath = new Path();
        linePath.moveTo(pointsX[0], pointsY[0]);
        for (int i = 1; i < 7; i++) {
            linePath.lineTo(pointsX[i], pointsY[i]);
        }
        canvas.drawPath(linePath, linePaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            canvas.drawText(daysOfWeek[i], pointsX[i], height - 10f * density, textPaint);
            canvas.drawCircle(pointsX[i], pointsY[i], 6f * density, pointPaint);
            canvas.drawCircle(pointsX[i], pointsY[i], 3f * density, pointInnerPaint);
        }
    }
}
