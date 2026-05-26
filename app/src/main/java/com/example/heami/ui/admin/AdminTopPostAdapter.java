package com.example.heami.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.AdminTopPostItem;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;

public class AdminTopPostAdapter extends RecyclerView.Adapter<AdminTopPostAdapter.TopPostViewHolder> {

    private final List<AdminTopPostItem> items = new ArrayList<>();

    public void submitList(@NonNull List<AdminTopPostItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_top_post, parent, false);
        return new TopPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopPostViewHolder holder, int position) {
        AdminTopPostItem item = items.get(position);

        holder.txtTopPostRank.setText("#" + (position + 1));
        holder.txtTopPostAuthor.setText(item.getAuthorName());
        holder.txtTopPostPreview.setText(item.getContentPreview());
        holder.txtTopPostMeta.setText(
                "Like " + item.getLikeCount()
                        + " • Comment " + item.getCommentCount()
                        + " • Report " + item.getReportCount()
        );

        bindStatusBadge(holder.txtTopPostStatus, item);
    }

    private void bindStatusBadge(@NonNull TextView view, @NonNull AdminTopPostItem item) {
        String status = item.getStatus() != null ? item.getStatus().trim().toUpperCase() : "ACTIVE";
        String moderationStatus = item.getModerationStatus() != null
                ? item.getModerationStatus().trim().toUpperCase()
                : "VISIBLE";

        if ("DELETED".equals(status)) {
            view.setText("DELETED");
            view.setBackgroundResource(R.drawable.bg_chip_inactive);
            view.setTextColor(Color.parseColor("#C2516A"));
            return;
        }

        if ("HIDDEN".equals(moderationStatus)) {
            view.setText("HIDDEN");
            view.setBackgroundResource(R.drawable.bg_chip_active_yellow);
            view.setTextColor(Color.parseColor("#B7791F"));
            return;
        }

        if (item.getReportCount() > 0) {
            view.setText("REPORTED");
            view.setBackgroundResource(R.drawable.bg_chip_active_purple);
            view.setTextColor(Color.parseColor("#6A42C2"));
            return;
        }

        view.setText("VISIBLE");
        view.setBackgroundResource(R.drawable.bg_chip_active_teal);
        view.setTextColor(Color.parseColor("#1D9E92"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TopPostViewHolder extends RecyclerView.ViewHolder {
        TextView txtTopPostRank;
        TextView txtTopPostAuthor;
        TextView txtTopPostStatus;
        TextView txtTopPostPreview;
        TextView txtTopPostMeta;

        public TopPostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTopPostRank = itemView.findViewById(R.id.txtTopPostRank);
            txtTopPostAuthor = itemView.findViewById(R.id.txtTopPostAuthor);
            txtTopPostStatus = itemView.findViewById(R.id.txtTopPostStatus);
            txtTopPostPreview = itemView.findViewById(R.id.txtTopPostPreview);
            txtTopPostMeta = itemView.findViewById(R.id.txtTopPostMeta);
        }
    }
}