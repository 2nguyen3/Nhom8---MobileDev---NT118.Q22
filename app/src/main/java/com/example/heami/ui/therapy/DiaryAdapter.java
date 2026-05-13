package com.example.heami.ui.therapy;


import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.DiaryModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    private Context context;
    private List<DiaryModel> diaryList;

    public DiaryAdapter(Context context, List<DiaryModel> diaryList) {
        this.context = context;
        this.diaryList = diaryList;
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_diary_timeline, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        DiaryModel diary = diaryList.get(position);

        // 1. Gắn nội dung chữ nhật ký
        holder.tvContent.setText(diary.getContent());

        // 2. Xử lý Emoji và Điểm số theo Mood từ Model dữ liệu của bạn
        String mood = diary.getMood();
        if (mood.contains("Vui vẻ")) {
            holder.tvMoodIcon.setText("😊");
            holder.tvScore.setText("9/10");
            holder.tvScore.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C2EFE3"))); // Ngọc pastel đậm nhạt vừa chọn
            holder.tvScore.setTextColor(Color.parseColor("#115948"));
        } else if (mood.contains("Buồn")) {
            holder.tvMoodIcon.setText("😢");
            holder.tvScore.setText("4/10");
            holder.tvScore.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FADBD8"))); // Nền hồng nhạt khi buồn
            holder.tvScore.setTextColor(Color.parseColor("#78281F"));
        } else if (mood.contains("Căng thẳng")) {
            holder.tvMoodIcon.setText("😤");
            holder.tvScore.setText("5/10");
            holder.tvScore.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FCF3CF")));
            holder.tvScore.setTextColor(Color.parseColor("#7E5109"));
        } else {
            holder.tvMoodIcon.setText("😌"); // Mặc định Bình yên
            holder.tvScore.setText("7.5/10");
            holder.tvScore.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C2EFE3")));
            holder.tvScore.setTextColor(Color.parseColor("#115948"));
        }

        // 3. Định dạng ngày tháng hiển thị từ Timestamp (created_at)
        Date date = new Date(diary.getCreated_at());
        SimpleDateFormat titleFormat = new SimpleDateFormat("EEEE, d 'Tháng' M", new Locale("vi", "VN"));
        holder.tvTitle.setText(titleFormat.format(date)); // Ví dụ: Thứ 2, 10 Tháng 3

        // Tự động kiểm tra thời gian để hiện nhãn HÔM NAY / HÔM QUA
        long diff = System.currentTimeMillis() - diary.getCreated_at();
        if (diff < 24 * 60 * 60 * 1000) {
            holder.tvDayLabel.setText("HÔM NAY");
        } else if (diff < 48 * 60 * 60 * 1000) {
            holder.tvDayLabel.setText("HÔM QUA");
        } else {
            SimpleDateFormat labelFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            holder.tvDayLabel.setText(labelFormat.format(date));
        }

        // 4. Sinh các tag (#CôngViên, #ThờiGian) động từ danh sách List<String> trong model
        holder.chipGroupTags.removeAllViews(); // Xóa chip cũ tránh lỗi trùng lặp khi cuộn
        if (diary.getTags() != null) {
            for (String tag : diary.getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setTextSize(12);
                chip.setTextColor(Color.parseColor("#D13E8D")); // Đổi chữ màu hồng đậm cho tiệp tone app
                chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF0F3"))); // Nền hồng siêu nhạt
                chip.setChipStrokeWidth(0f);
                holder.chipGroupTags.addView(chip);
            }
        }
    }

    @Override
    public int getItemCount() {
        return diaryList.size();
    }

    static class DiaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvMoodIcon, tvDayLabel, tvScore, tvTitle, tvContent;
        ChipGroup chipGroupTags;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMoodIcon = itemView.findViewById(R.id.tv_item_mood_icon);
            tvDayLabel = itemView.findViewById(R.id.tv_item_day_label);
            tvScore = itemView.findViewById(R.id.tv_item_score);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvContent = itemView.findViewById(R.id.tv_item_content);
            chipGroupTags = itemView.findViewById(R.id.item_chip_group_tags);
        }
    }
}
