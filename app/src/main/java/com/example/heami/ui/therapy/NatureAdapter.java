package com.example.heami.ui.therapy;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.heami.R;

public class NatureAdapter extends RecyclerView.Adapter<NatureAdapter.ViewHolder> {
    private String[] titles;
    private int selectedPosition;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public NatureAdapter(String[] titles, int selectedPosition, OnItemClickListener listener) {
        this.titles = titles;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nature_sound, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String title = titles[position];
        holder.tvTitle.setText(title);

        // Subtitle logic
        String titleLower = title.toLowerCase();
        if (titleLower.contains("mưa")) holder.tvSubtitle.setText("Âm thanh thiên nhiên • Ngủ sâu");
        else if (titleLower.contains("chim")) holder.tvSubtitle.setText("Âm thanh thiên nhiên • Tỉnh táo");
        else holder.tvSubtitle.setText("Âm thanh trị liệu • Thư giãn");

        // Xử lý Highlight dòng được chọn
        if (position == selectedPosition) {
            holder.imgCheck.setVisibility(View.VISIBLE);
            holder.tvTitle.setTextColor(Color.parseColor("#81C784")); // Màu xanh Nature
            holder.itemView.setBackgroundResource(R.drawable.bg_item_selected); // Nền mờ bo góc
        } else {
            holder.imgCheck.setVisibility(View.GONE);
            holder.tvTitle.setTextColor(Color.WHITE);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() { return titles != null ? titles.length : 0; }

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