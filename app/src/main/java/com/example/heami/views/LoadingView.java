package com.example.heami.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.heami.R;

public class LoadingView extends View {

    private Paint paint;
    private float dotRadius;
    private float dotGap;
    private float jumpHeight;

    private float[] dotOffsets = new float[4];
    private int[] dotColors;
    private ValueAnimator[] animators = new ValueAnimator[4];

    public LoadingView(Context context) {
        super(context);
        init(context);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Chuyển đổi thông số từ dp sang px để hiển thị đồng nhất
        float density = getResources().getDisplayMetrics().density;
        dotRadius = 9 * density;   // Đường kính hạt khoảng 18dp
        dotGap = 12 * density;     // Khoảng cách giữa các hạt
        jumpHeight = 20 * density; // Chiều cao nhảy lên

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        // Lấy 4 màu chủ đạo từ colors.xml của bạn
        dotColors = new int[]{
                ContextCompat.getColor(context, R.color.teal),
                ContextCompat.getColor(context, R.color.orange),
                ContextCompat.getColor(context, R.color.pink),
                ContextCompat.getColor(context, R.color.link_blue)
        };

        setupAnimations();
    }

    private void setupAnimations() {
        for (int i = 0; i < 4; i++) {
            final int index = i;
            // Tạo chuyển động từ vị trí gốc (0) lên trên (-jumpHeight) rồi quay lại
            animators[i] = ValueAnimator.ofFloat(0, -jumpHeight, 0);
            animators[i].setDuration(1000); // 1 giây cho một vòng nhảy
            animators[i].setStartDelay(i * 150); // Tạo hiệu ứng sóng (wave delay)
            animators[i].setRepeatCount(ValueAnimator.INFINITE);
            animators[i].setInterpolator(new AccelerateDecelerateInterpolator());

            animators[i].addUpdateListener(animation -> {
                dotOffsets[index] = (float) animation.getAnimatedValue();
                invalidate(); // Yêu cầu vẽ lại View
            });
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Tính toán để cụm 4 hạt luôn nằm chính giữa View
        float totalWidth = (4 * dotRadius * 2) + (3 * dotGap);
        float startX = (getWidth() - totalWidth) / 2 + dotRadius;
        float centerY = getHeight() / 2 + (jumpHeight / 2);

        for (int i = 0; i < 4; i++) {
            paint.setColor(dotColors[i]);
            float x = startX + i * (dotRadius * 2 + dotGap);
            float y = centerY + dotOffsets[i];
            canvas.drawCircle(x, y, dotRadius, paint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Bắt đầu nhảy khi View xuất hiện, trừ khi đang ở chế độ xem trước (Edit Mode)
        if (isInEditMode()) return;

        for (ValueAnimator animator : animators) animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Dừng nhảy khi View bị hủy để tránh tốn pin/tài nguyên
        for (ValueAnimator animator : animators) animator.cancel();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Đặt chiều cao đủ lớn để hạt không bị cắt khi nhảy lên
        int desiredHeight = (int) ((dotRadius * 2 + jumpHeight) * 1.5f);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), desiredHeight);
    }
}