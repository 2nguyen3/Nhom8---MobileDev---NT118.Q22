package com.example.heami.ui.doctor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.TimeSlotsModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị lịch tuần (T2-CN).
 * Mỗi item = 1 ngày, bên phải là các chip slot:
 *   - available → chip viền xanh
 *   - booked    → chip viền đỏ
 * Không có filter: hiển thị tất cả slot.
 */
public class WeekScheduleAdapter extends RecyclerView.Adapter<WeekScheduleAdapter.DayViewHolder> {

    // ── Ngày bắt đầu tuần (Thứ 2) ──────────────────────────────────
    private final Calendar weekStart;

    // ── Dữ liệu từ Firestore ───────────────────────────────────────
    private List<TimeSlotsModel> slots = new ArrayList<>();

    // ── Format helpers ─────────────────────────────────────────────
    private final SimpleDateFormat dayKeyFmt   = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private final SimpleDateFormat timeFmt     = new SimpleDateFormat("HH:mm",    Locale.getDefault());
    private final String[]         DAY_LABELS  = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    // ──────────────────────────────────────────────────────────────

    public WeekScheduleAdapter() {
        weekStart = getMonday();
    }

    /** Cập nhật danh sách slot và vẽ lại */
    public void setSlots(List<TimeSlotsModel> newSlots) {
        slots.clear();
        if (newSlots != null) slots.addAll(newSlots);
        notifyDataSetChanged();
    }

    // ──────────────────────────────────────────────────────────────
    //  RecyclerView.Adapter overrides
    // ──────────────────────────────────────────────────────────────

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Context ctx = holder.itemView.getContext();

        // Tính ngày tương ứng với position (0=T2 … 6=CN)
        Calendar dayCal = (Calendar) weekStart.clone();
        dayCal.add(Calendar.DAY_OF_YEAR, position);

        // Kiểm tra hôm nay / quá khứ
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        boolean isToday   = dayKeyFmt.format(dayCal.getTime()).equals(dayKeyFmt.format(today.getTime()));
        boolean isPast    = dayCal.before(today);

        // ── Tên thứ & số ngày ──
        holder.tvDayName.setText(DAY_LABELS[position]);
        holder.tvDayNumber.setText(String.valueOf(dayCal.get(Calendar.DAY_OF_MONTH)));

        // ── Style badge ──
        if (isToday) {
            holder.cardDayBadge.setCardBackgroundColor(Color.parseColor("#09A38C"));
            holder.tvDayName.setTextColor(Color.WHITE);
            holder.tvDayNumber.setTextColor(Color.WHITE);
        } else if (isPast) {
            holder.cardDayBadge.setCardBackgroundColor(Color.parseColor("#F5F7FA"));
            holder.tvDayName.setTextColor(Color.parseColor("#C0CCDD"));
            holder.tvDayNumber.setTextColor(Color.parseColor("#C0CCDD"));
        } else {
            holder.cardDayBadge.setCardBackgroundColor(Color.parseColor("#F0FBF9"));
            holder.tvDayName.setTextColor(Color.parseColor("#09A38C"));
            holder.tvDayNumber.setTextColor(Color.parseColor("#1A2530"));
        }

        // ── Lọc slot của ngày này ──
        String targetKey = dayKeyFmt.format(dayCal.getTime());
        List<TimeSlotsModel> daySlots = new ArrayList<>();
        for (TimeSlotsModel s : slots) {
            if (s == null || s.getStart_time() == null) continue;
            if (targetKey.equals(dayKeyFmt.format(s.getStart_time().toDate()))) {
                daySlots.add(s);
            }
        }

        // ── Sắp xếp slot theo giờ bắt đầu ──
        daySlots.sort((a, b) -> a.getStart_time().compareTo(b.getStart_time()));

        // ── Render slot chips ──
        holder.containerSlots.removeAllViews();

        if (daySlots.isEmpty()) {
            holder.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            holder.layoutEmpty.setVisibility(View.GONE);
            for (TimeSlotsModel slot : daySlots) {
                holder.containerSlots.addView(makeSlotChip(ctx, slot, isPast));
            }
        }

        // Làm mờ toàn bộ ngày quá khứ
        holder.itemView.setAlpha(isPast ? 0.55f : 1f);
    }

    @Override
    public int getItemCount() {
        return 7; // T2 → CN
    }

    // ──────────────────────────────────────────────────────────────
    //  Slot chip builder
    // ──────────────────────────────────────────────────────────────

    private View makeSlotChip(Context ctx, TimeSlotsModel slot, boolean isPast) {
        boolean isBooked = "booked".equalsIgnoreCase(slot.getStatus());

        // Outer container (margin bottom)
        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wLp.bottomMargin = dp(ctx, 6);
        wrapper.setLayoutParams(wLp);

        // Chip card
        LinearLayout chip = new LinearLayout(ctx);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(ctx, 10), dp(ctx, 7), dp(ctx, 10), dp(ctx, 7));

        if (!isPast) {
            chip.setBackgroundResource(isBooked ? R.drawable.bg_slot_booked : R.drawable.bg_slot_available);
        } else {
            // Slot quá khứ: nền xám nhạt
            chip.setBackgroundResource(R.drawable.bg_chip_inactive);
        }

        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        chip.setLayoutParams(chipLp);

        // ── Thời gian ──
        String timeStr = timeFmt.format(slot.getStart_time().toDate());
        if (slot.getEnd_time() != null) {
            timeStr += " – " + timeFmt.format(slot.getEnd_time().toDate());
        }

        TextView tvTime = new TextView(ctx);
        tvTime.setText(timeStr);
        tvTime.setTextSize(12.5f);
        tvTime.setTypeface(null, Typeface.BOLD);
        tvTime.setTextColor(isPast ? Color.parseColor("#C0CCDD")
                : (isBooked ? Color.parseColor("#E53E3E") : Color.parseColor("#09A38C")));
        chip.addView(tvTime);

        // ── Nhãn trạng thái ──
        String label = isBooked ? "  Đã đặt" : "  Rảnh";
        TextView tvLabel = new TextView(ctx);
        tvLabel.setText(label);
        tvLabel.setTextSize(11f);
        tvLabel.setTextColor(isPast ? Color.parseColor("#C0CCDD")
                : (isBooked ? Color.parseColor("#FF8A8D") : Color.parseColor("#5BBFB0")));
        chip.addView(tvLabel);

        wrapper.addView(chip);
        return wrapper;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────

    /** Trả về Calendar đặt vào Thứ 2 của tuần hiện tại (00:00:00) */
    private Calendar getMonday() {
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SUNDAY) {
            c.add(Calendar.DAY_OF_YEAR, -6);
        } else {
            c.add(Calendar.DAY_OF_YEAR, Calendar.MONDAY - dow);
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private int dp(Context ctx, float dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    // ──────────────────────────────────────────────────────────────
    //  ViewHolder
    // ──────────────────────────────────────────────────────────────

    static class DayViewHolder extends RecyclerView.ViewHolder {
        CardView   cardDayBadge;
        TextView   tvDayName;
        TextView   tvDayNumber;
        LinearLayout containerSlots;
        LinearLayout layoutEmpty;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDayBadge   = itemView.findViewById(R.id.cardDayBadge);
            tvDayName      = itemView.findViewById(R.id.tvDayName);
            tvDayNumber    = itemView.findViewById(R.id.tvDayNumber);
            containerSlots = itemView.findViewById(R.id.containerSlots);
            layoutEmpty    = itemView.findViewById(R.id.layoutEmpty);
        }
    }
}
