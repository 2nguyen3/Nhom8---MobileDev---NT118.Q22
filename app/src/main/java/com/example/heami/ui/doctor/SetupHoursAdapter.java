package com.example.heami.ui.doctor;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SetupHoursAdapter extends RecyclerView.Adapter<SetupHoursAdapter.ViewHolder> {

    private final Context context;
    private final List<String> fixedHours;
    private final List<String> selectedHours;

    public SetupHoursAdapter(Context context, List<String> fixedHours, List<String> selectedHours) {
        this.context = context;
        this.fixedHours = fixedHours;
        this.selectedHours = selectedHours;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = new CardView(context);
        cardView.setRadius(16);
        cardView.setElevation(0);

        TextView textView = new TextView(context);
        textView.setTextSize(14);
        textView.setPadding(0, 24, 0, 24);
        textView.setGravity(Gravity.CENTER);
        cardView.addView(textView);

        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(8, 8, 8, 8);
        cardView.setLayoutParams(lp);

        return new ViewHolder(cardView, textView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String hour = fixedHours.get(position);
        holder.tvHour.setText(hour);

        // Đổi màu sắc dựa trên việc giờ này đã được chọn hay chưa
        if (selectedHours.contains(hour)) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#319795")); // Xanh lá đậm khi được chọn
            holder.tvHour.setTextColor(Color.WHITE);
        } else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#EDF2F7")); // Xám nhạt khi trống
            holder.tvHour.setTextColor(Color.parseColor("#4A5568"));
        }

        // Bắt sự kiện click chọn giờ
        holder.itemView.setOnClickListener(v -> {
            if (selectedHours.contains(hour)) {
                selectedHours.remove(hour);
            } else {
                selectedHours.add(hour);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() { return fixedHours.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvHour;
        public ViewHolder(@NonNull CardView cardView, TextView tvHour) {
            super(cardView);
            this.cardView = cardView;
            this.tvHour = tvHour;
        }
    }
}