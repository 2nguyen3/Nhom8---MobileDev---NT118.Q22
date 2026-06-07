package com.example.heami.ui.doctor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Custom tháng lịch hiển thị dot đỏ trên ngày có slot "booked",
 * tương tự Google Calendar.
 */
public class MonthCalendarView extends LinearLayout {

    // ──────────── Constants ────────────
    private static final int COLOR_TEAL       = 0xFF09A38C;
    private static final int COLOR_TEXT_DARK  = 0xFF1A2530;
    private static final int COLOR_TEXT_GRAY  = 0xFF9CA3AF;
    private static final int COLOR_DOT_RED    = 0xFFE53E3E;
    private static final int COLOR_TODAY_BG   = 0xFF09A38C;
    private static final int COLOR_HEADER_BG  = 0xFFF8FFFE;
    private static final String[] DAY_LABELS  = {"T2","T3","T4","T5","T6","T7","CN"};
    private static final String[] MONTH_NAMES = {
            "Tháng 1","Tháng 2","Tháng 3","Tháng 4","Tháng 5","Tháng 6",
            "Tháng 7","Tháng 8","Tháng 9","Tháng 10","Tháng 11","Tháng 12"
    };

    // ──────────── State ────────────
    private Calendar displayedMonth = Calendar.getInstance();
    private Calendar selectedDay = Calendar.getInstance(); // Ngày đang được chọn
    /** Set các ngày dạng "yyyyMMdd" có slot booked */
    private final Set<String> bookedDays = new HashSet<>();
    private final SimpleDateFormat keyFmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    // Callback chọn ngày
    public interface OnDateClickListener {
        void onDateClick(int year, int month, int day);
    }
    private OnDateClickListener onDateClickListener;

    // ──────────── UI components ────────────
    private TextView tvMonth;
    private GridLayout gridDays;

    public MonthCalendarView(Context context) {
        super(context);
        init(context);
    }

    public MonthCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MonthCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ──────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────

    /**
     * Cập nhật tập hợp ngày có slot booked và vẽ lại lịch.
     * @param days Set<String> các key dạng "yyyyMMdd"
     */
    public void setBookedDays(Set<String> days) {
        bookedDays.clear();
        if (days != null) bookedDays.addAll(days);
        renderGrid();
    }

    public void setOnDateClickListener(OnDateClickListener listener) {
        this.onDateClickListener = listener;
    }

    public void setSelectedDay(Calendar cal) {
        if (cal != null) {
            this.selectedDay = (Calendar) cal.clone();
            this.displayedMonth = (Calendar) cal.clone();
            renderGrid();
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Init
    // ──────────────────────────────────────────────────────────────

    private void init(Context context) {
        setOrientation(VERTICAL);
        setBackgroundColor(Color.WHITE);

        // ── Header: prev / month-year / next ──
        LinearLayout header = buildHeader(context);
        addView(header);

        // ── Day-of-week labels row ──
        LinearLayout labelRow = buildDayLabels(context);
        addView(labelRow);

        // ── Separator ──
        View sep = new View(context);
        sep.setBackgroundColor(0xFFEDF2F7);
        LinearLayout.LayoutParams sepLp =
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(context, 1));
        sepLp.topMargin = dpToPx(context, 6);
        sepLp.bottomMargin = dpToPx(context, 4);
        addView(sep, sepLp);

        // ── Day grid ──
        gridDays = new GridLayout(context);
        gridDays.setColumnCount(7);
        addView(gridDays, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        renderGrid();
    }

    // ──────────────────────────────────────────────────────────────
    //  Header
    // ──────────────────────────────────────────────────────────────

    private LinearLayout buildHeader(Context context) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(COLOR_HEADER_BG);
        int padH = dpToPx(context, 12);
        int padV = dpToPx(context, 10);
        header.setPadding(padH, padV, padH, padV);

        // Prev button
        ImageButton btnPrev = new ImageButton(context);
        btnPrev.setImageDrawable(makeArrow(context, false));
        btnPrev.setBackground(null);
        btnPrev.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            renderGrid();
        });

        // Month/Year label
        tvMonth = new TextView(context);
        tvMonth.setTextSize(15f);
        tvMonth.setTypeface(null, Typeface.BOLD);
        tvMonth.setTextColor(COLOR_TEXT_DARK);
        tvMonth.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams monthLp =
                new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        tvMonth.setLayoutParams(monthLp);

        // Next button
        ImageButton btnNext = new ImageButton(context);
        btnNext.setImageDrawable(makeArrow(context, true));
        btnNext.setBackground(null);
        btnNext.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            renderGrid();
        });

        header.addView(btnPrev);
        header.addView(tvMonth);
        header.addView(btnNext);
        return header;
    }

    /** Tạo drawable mũi tên trái/phải bằng canvas */
    private android.graphics.drawable.Drawable makeArrow(Context ctx, boolean right) {
        int size = dpToPx(ctx, 28);
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(size, size,
                android.graphics.Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(COLOR_TEAL);
        p.setStrokeWidth(dpToPx(ctx, 2));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        float cx = size / 2f;
        float cy = size / 2f;
        float arm = size * 0.22f;
        if (right) {
            c.drawLine(cx - arm * 0.7f, cy - arm, cx + arm * 0.7f, cy, p);
            c.drawLine(cx + arm * 0.7f, cy, cx - arm * 0.7f, cy + arm, p);
        } else {
            c.drawLine(cx + arm * 0.7f, cy - arm, cx - arm * 0.7f, cy, p);
            c.drawLine(cx - arm * 0.7f, cy, cx + arm * 0.7f, cy + arm, p);
        }
        return new android.graphics.drawable.BitmapDrawable(getResources(), bmp);
    }

    // ──────────────────────────────────────────────────────────────
    //  Day-of-week label row
    // ──────────────────────────────────────────────────────────────

    private LinearLayout buildDayLabels(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setPadding(dpToPx(context, 4), dpToPx(context, 8), dpToPx(context, 4), 0);
        for (String label : DAY_LABELS) {
            TextView tv = new TextView(context);
            tv.setText(label);
            tv.setTextSize(11f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            // CN (Sunday) -> teal, rest -> gray
            tv.setTextColor("CN".equals(label) ? COLOR_TEAL : COLOR_TEXT_GRAY);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            row.addView(tv);
        }
        return row;
    }

    // ──────────────────────────────────────────────────────────────
    //  Grid rendering
    // ──────────────────────────────────────────────────────────────

    private void renderGrid() {
        // Update month/year label
        int year  = displayedMonth.get(Calendar.YEAR);
        int month = displayedMonth.get(Calendar.MONTH);
        if (tvMonth != null) {
            tvMonth.setText(MONTH_NAMES[month] + " " + year);
        }

        if (gridDays == null) return;
        gridDays.removeAllViews();

        // Build calendar for first day of displayed month
        Calendar cal = (Calendar) displayedMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // Map: Calendar.DAY_OF_WEEK (SUN=1..SAT=7) → column (Mon=0..Sun=6)
        int firstDow = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun..7=Sat
        int startCol = (firstDow == Calendar.SUNDAY) ? 6 : firstDow - 2; // Mon-based

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Today for highlighting
        Calendar today = Calendar.getInstance();
        int todayYear  = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH);
        int todayDay   = today.get(Calendar.DAY_OF_MONTH);

        // Selected day for highlighting
        int selYear = selectedDay.get(Calendar.YEAR);
        int selMonth = selectedDay.get(Calendar.MONTH);
        int selDay = selectedDay.get(Calendar.DAY_OF_MONTH);

        Context ctx = getContext();
        int cellSize = dpToPx(ctx, 38);

        // Fill leading empty cells
        for (int i = 0; i < startCol; i++) {
            gridDays.addView(makeEmptyCell(ctx, cellSize));
        }

        // Fill day cells
        for (int day = 1; day <= daysInMonth; day++) {
            String key = String.format(Locale.getDefault(), "%04d%02d%02d", year, month + 1, day);
            boolean isToday = (year == todayYear && month == todayMonth && day == todayDay);
            boolean isSelected = (year == selYear && month == selMonth && day == selDay);
            boolean hasBooked = bookedDays.contains(key);
            boolean isPast = isPast(year, month, day, todayYear, todayMonth, todayDay);

            gridDays.addView(makeDayCell(ctx, day, isToday, isSelected, hasBooked, isPast, cellSize, year, month));
        }

        // Fill trailing empty cells to complete the last row
        int totalCells = startCol + daysInMonth;
        int remainder  = totalCells % 7;
        if (remainder != 0) {
            for (int i = 0; i < (7 - remainder); i++) {
                gridDays.addView(makeEmptyCell(ctx, cellSize));
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Cell factories
    // ──────────────────────────────────────────────────────────────

    private View makeEmptyCell(Context ctx, int size) {
        View v = new View(ctx);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width  = 0;
        lp.height = size;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        v.setLayoutParams(lp);
        return v;
    }

    private View makeDayCell(Context ctx, int day, boolean isToday, boolean isSelected,
                              boolean hasBooked, boolean isPast, int cellSize, int year, int month) {
        // Outer frame: vertical, center-aligned
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(0, dpToPx(ctx, 4), 0, dpToPx(ctx, 4));
        cell.setClickable(true);
        cell.setFocusable(true);

        // Day number circle or plain text
        TextView tvDay = new TextView(ctx);
        tvDay.setText(String.valueOf(day));
        tvDay.setTextSize(13f);
        tvDay.setGravity(Gravity.CENTER);

        int circleSize = dpToPx(ctx, 30);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(circleSize, circleSize);
        tvDay.setLayoutParams(tvLp);

        if (isToday) {
            tvDay.setBackground(makeTodayBackground(ctx));
            tvDay.setTextColor(Color.WHITE);
            tvDay.setTypeface(null, Typeface.BOLD);
        } else if (isSelected) {
            // Highlight ngày được chọn
            tvDay.setBackground(makeSelectedBackground(ctx));
            tvDay.setTextColor(COLOR_TEAL);
            tvDay.setTypeface(null, Typeface.BOLD);
        } else if (isPast) {
            tvDay.setTextColor(0xFFCBD5E0);
        } else {
            tvDay.setTextColor(COLOR_TEXT_DARK);
        }

        cell.addView(tvDay);

        // Dot below the number
        DotView dot = new DotView(ctx, hasBooked ? COLOR_DOT_RED : Color.TRANSPARENT,
                dpToPx(ctx, 5));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                dpToPx(ctx, 5), dpToPx(ctx, 5));
        dotLp.topMargin = dpToPx(ctx, 2);
        dotLp.gravity = Gravity.CENTER_HORIZONTAL;
        dot.setLayoutParams(dotLp);
        cell.addView(dot);

        // Gán sự kiện click cho ô ngày
        cell.setOnClickListener(v -> {
            selectedDay.set(Calendar.YEAR, year);
            selectedDay.set(Calendar.MONTH, month);
            selectedDay.set(Calendar.DAY_OF_MONTH, day);
            renderGrid();
            if (onDateClickListener != null) {
                onDateClickListener.onDateClick(year, month, day);
            }
        });

        // Grid layout params
        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width  = 0;
        glp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cell.setLayoutParams(glp);

        return cell;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────

    private android.graphics.drawable.Drawable makeTodayBackground(Context ctx) {
        android.graphics.drawable.ShapeDrawable sd =
                new android.graphics.drawable.ShapeDrawable(
                        new android.graphics.drawable.shapes.OvalShape());
        sd.getPaint().setColor(COLOR_TODAY_BG);
        return sd;
    }

    private android.graphics.drawable.Drawable makeSelectedBackground(Context ctx) {
        android.graphics.drawable.ShapeDrawable sd =
                new android.graphics.drawable.ShapeDrawable(
                        new android.graphics.drawable.shapes.OvalShape());
        sd.getPaint().setColor(0xFFE0F2F1); // Màu nền xanh nhạt cho ngày được chọn
        return sd;
    }

    private boolean isPast(int year, int month, int day,
                            int todayYear, int todayMonth, int todayDay) {
        if (year < todayYear) return true;
        if (year > todayYear) return false;
        if (month < todayMonth) return true;
        if (month > todayMonth) return false;
        return day < todayDay;
    }

    private int dpToPx(Context ctx, float dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    // ──────────────────────────────────────────────────────────────
    //  Inner: tiny colored dot
    // ──────────────────────────────────────────────────────────────

    private static class DotView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        DotView(Context ctx, int color, int sizePx) {
            super(ctx);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth()  / 2f;
            float cy = getHeight() / 2f;
            float r  = Math.min(cx, cy);
            canvas.drawCircle(cx, cy, r, paint);
        }
    }
}
