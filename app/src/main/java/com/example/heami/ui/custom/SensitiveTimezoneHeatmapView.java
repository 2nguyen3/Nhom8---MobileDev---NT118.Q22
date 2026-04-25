package com.example.heami.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SensitiveTimezoneHeatmapView extends View {

    private Paint cellPaint;
    private Paint textPaint;
    private Paint labelPaint;

    // Trục tung: 4 Múi giờ trong ngày
    private final String[] timeSlots = {"Sáng", "Trưa", "Chiều", "Tối"};
    // Trục hoành: 7 Ngày trong tuần
    private final String[] daysOfWeek = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    // Lưới 4x7 chứa mức độ nhạy cảm (giá trị từ 0 đến 3)
    // 0: Bình yên (Màu xám nhạt)
    // 1: Nhạy cảm Thấp (Teal Nhạt)
    // 2: Nhạy cảm Vừa (Teal Trung bình)
    // 3: Nhạy cảm Cao (Màu Hồng nhạt/Đậm thể hiện "nhạy cảm/áp lực")
    private int[][] heatmapData = {
        {0, 1, 0, 2, 0, 1, 0}, // Sáng
        {1, 2, 1, 0, 3, 0, 1}, // Trưa
        {2, 3, 2, 1, 2, 1, 0}, // Chiều
        {1, 1, 3, 2, 1, 0, 2}  // Tối
    };

    // Màu sắc tương ứng các cấp độ
    private final String[] colors = {
        "#F5F6F8", // Cấp 0 - Bình yên (Xám nhạt sạch sẽ)
        "#B2DFDB", // Cấp 1 - Ít nhạy cảm (Teal nhạt)
        "#4DB6AC", // Cấp 2 - Nhạy cảm vừa (Teal trung bình)
        "#E86FA0"  // Cấp 3 - Nhạy cảm cao (Màu hồng cam nổi bật)
    };

    public SensitiveTimezoneHeatmapView(Context context) {
        super(context);
        init();
    }

    public SensitiveTimezoneHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SensitiveTimezoneHeatmapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#7D8BB7"));
        textPaint.setTextSize(10f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#313866"));
        labelPaint.setTextSize(10f * density);
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        labelPaint.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
    }

    public void setHeatmapData(int[][] data) {
        if (data != null && data.length == 4 && data[0].length == 7) {
            this.heatmapData = data;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float leftMargin = 45f * density; // Khoảng trống vẽ nhãn trục tung
        float topMargin = 25f * density;  // Khoảng trống vẽ nhãn trục hoành
        float bottomMargin = 10f * density;
        float rightMargin = 10f * density;

        float width = getWidth();
        float height = getHeight();

        float gridWidth = width - leftMargin - rightMargin;
        float gridHeight = height - topMargin - bottomMargin;

        float cellWidth = gridWidth / 7f;
        float cellHeight = gridHeight / 4f;
        float cellPadding = 3f * density;

        // 1. Vẽ tiêu đề cột (Trục hoành - Các ngày trong tuần)
        textPaint.setColor(Color.parseColor("#7D8BB7"));
        for (int col = 0; col < 7; col++) {
            float cx = leftMargin + cellWidth * col + cellWidth / 2f;
            canvas.drawText(daysOfWeek[col], cx, topMargin - 8f * density, textPaint);
        }

        // 2. Vẽ lưới ô nhiệt và nhãn hàng (Trục tung - Các buổi)
        RectF cellRect = new RectF();
        for (int row = 0; row < 4; row++) {
            // Vẽ nhãn hàng bên trái trục tung
            float ly = topMargin + cellHeight * row + cellHeight / 2f + 4f * density;
            canvas.drawText(timeSlots[row], leftMargin - 8f * density, ly, labelPaint);

            for (int col = 0; col < 7; col++) {
                float left = leftMargin + cellWidth * col + cellPadding;
                float top = topMargin + cellHeight * row + cellPadding;
                float right = leftMargin + cellWidth * (col + 1) - cellPadding;
                float bottom = topMargin + cellHeight * (row + 1) - cellPadding;

                cellRect.set(left, top, right, bottom);

                int level = heatmapData[row][col];
                if (level < 0) level = 0;
                if (level > 3) level = 3;

                cellPaint.setColor(Color.parseColor(colors[level]));
                // Vẽ ô bo góc 6dp cực đẹp
                canvas.drawRoundRect(cellRect, 6f * density, 6f * density, cellPaint);
            }
        }
    }
}
