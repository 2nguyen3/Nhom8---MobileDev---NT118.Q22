package com.example.heami.ui.therapy;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.heami.R;
import java.util.List;

public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.MonthViewHolder> {

    private List<String> monthList;
    private OnMonthClickListener listener;
    private int selectedPosition;

    public interface OnMonthClickListener {
        void onMonthClick(int monthNumber);
    }

    public MonthAdapter(List<String> monthList, int currentMonth, OnMonthClickListener listener) {
        this.monthList = monthList;
        this.listener = listener;
        this.selectedPosition = currentMonth - 1;
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 🌟 ĐỔI THÀNH LAYOUT CUSTOM CỦA BẠN
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month, parent, false);
        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        String monthText = monthList.get(position);
        holder.tvMonthName.setText(monthText);

        // Tạo bo góc mềm mại bằng Code thay vì file xml drawable cho tiện
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(30f); // Độ bo tròn bong bóng

        // Đổ màu dựa theo trạng thái active
        if (position == selectedPosition) {
            holder.tvMonthName.setTextColor(Color.WHITE);
            shape.setColor(Color.parseColor("#E86FA0")); // Màu hồng rực rỡ chuẩn tone chủ đạo app bạn
            holder.tvMonthName.setBackground(shape);
        } else {
            holder.tvMonthName.setTextColor(Color.parseColor("#7D8BB7")); // Chữ xám xanh dịu
            shape.setColor(Color.parseColor("#FFFFFF")); // Nền trắng tinh khôi
            holder.tvMonthName.setBackground(shape);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition == holder.getAdapterPosition()) return;

            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onMonthClick(selectedPosition + 1);
            }
        });
    }

    @Override
    public int getItemCount() {
        return monthList.size();
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthName;
        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthName = itemView.findViewById(R.id.tv_month_name);
        }
    }
}