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

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private final String[] musicTitles = {
            "ACAI", "BLUE LIPS", "JOYRIDE",
            "LULLABY", "Nếu như ta chẳng còn"
    };

    private int selectedPosition;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public MusicAdapter(int selectedPosition, OnItemClickListener listener) {
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
        String title = musicTitles[position];
        holder.tvTitle.setText(title);

        holder.tvSubtitle.setText("Âm nhạc thư giãn");

        // Giữ nguyên logic màu xanh lục bảo bạn yêu cầu
        if (position == selectedPosition) {
            holder.imgCheck.setVisibility(View.VISIBLE);
            holder.tvTitle.setTextColor(Color.parseColor("#81C784"));
            holder.itemView.setBackgroundResource(R.drawable.bg_item_selected);
        } else {
            holder.imgCheck.setVisibility(View.GONE);
            holder.tvTitle.setTextColor(Color.WHITE);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return musicTitles.length;
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