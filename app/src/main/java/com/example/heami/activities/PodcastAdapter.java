package com.example.heami.activities;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.heami.R;

public class PodcastAdapter extends RecyclerView.Adapter<PodcastAdapter.ViewHolder> {
    private String[] titles;
    private int selectedPosition;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public PodcastAdapter(String[] titles, int selectedPosition, OnItemClickListener listener) {
        this.titles = titles;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item chung để giữ tính nhất quán
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nature_sound, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String title = titles[position];
        holder.tvTitle.setText(title);

        // Text phụ tinh gọn: Chỉ để Podcast Chữa Lành
        holder.tvSubtitle.setText("Podcast Chữa Lành • Radio");

        // Set icon podcast mặc định cho mọi dòng (nếu bạn có icon podcast/mic)
        // holder.imgIcon.setImageResource(R.drawable.ic_mic);

        // Xử lý trạng thái đang được chọn (Màu CAM đặc trưng)
        if (position == selectedPosition) {
            holder.imgCheck.setVisibility(View.VISIBLE);
            holder.imgCheck.setColorFilter(Color.parseColor("#FFB74D")); // Màu cam
            holder.tvTitle.setTextColor(Color.parseColor("#FFB74D"));    // Màu cam
            holder.tvSubtitle.setTextColor(Color.parseColor("#FFB74D")); // Subtitle cũng cam mờ
            holder.tvSubtitle.setAlpha(0.8f);
            holder.itemView.setBackgroundResource(R.drawable.bg_item_selected);
        } else {
            holder.imgCheck.setVisibility(View.GONE);
            holder.tvTitle.setTextColor(Color.WHITE);
            holder.tvSubtitle.setTextColor(Color.parseColor("#B0BEC5")); // Màu xám xanh nhẹ
            holder.tvSubtitle.setAlpha(1.0f);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return titles != null ? titles.length : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        ImageView imgIcon, imgCheck;

        public ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvSoundTitle);
            tvSubtitle = v.findViewById(R.id.tvSoundSubtitle);
            imgIcon = v.findViewById(R.id.imgSoundIcon);
            imgCheck = v.findViewById(R.id.imgCheck);
        }
    }
}