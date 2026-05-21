package com.example.heami.ui.admin;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.CommunityReportModel;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(@NonNull CommunityReportModel report);
    }

    private final List<CommunityReportModel> items = new ArrayList<>();
    private final OnReportClickListener listener;

    public AdminReportAdapter(@NonNull OnReportClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<CommunityReportModel> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        CommunityReportModel item = items.get(position);

        String targetType = safeUpper(item.getTarget_type(), "POST");
        String authorName = safeText(item.getSnapshot_author_name(), "Ẩn danh");
        String snapshotText = safeText(item.getSnapshot_text(), "Không có nội dung snapshot");
        String reasonText = safeText(item.getReason_text(), "Không rõ lý do");
        String status = safeUpper(item.getStatus(), "PENDING");

        holder.txtReportTitle.setText(
                ("COMMENT".equals(targetType) ? "Report bình luận" : "Report bài viết")
                        + " • " + authorName
        );
        holder.txtReportReason.setText("Lý do: " + reasonText);
        holder.txtReportSnapshot.setText(snapshotText);
        holder.txtReportMeta.setText(
                "Trạng thái: " + status + " • " + formatTime(item.getCreated_at())
        );

        bindStatus(holder.txtReportStatus, status);
        holder.txtReportStatus.setText(status);

        holder.itemView.setOnClickListener(v -> listener.onReportClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView txtReportTitle;
        TextView txtReportReason;
        TextView txtReportSnapshot;
        TextView txtReportMeta;
        TextView txtReportStatus;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            txtReportTitle = itemView.findViewById(R.id.txtReportTitle);
            txtReportReason = itemView.findViewById(R.id.txtReportReason);
            txtReportSnapshot = itemView.findViewById(R.id.txtReportSnapshot);
            txtReportMeta = itemView.findViewById(R.id.txtReportMeta);
            txtReportStatus = itemView.findViewById(R.id.txtReportStatus);
        }
    }

    private void bindStatus(@NonNull TextView view, @NonNull String status) {
        switch (status) {
            case "RESOLVED":
                view.setBackgroundResource(R.drawable.bg_chip_active_teal);
                view.setTextColor(Color.parseColor("#1D9E92"));
                break;
            case "REJECTED":
                view.setBackgroundResource(R.drawable.bg_chip_inactive);
                view.setTextColor(Color.parseColor("#C2516A"));
                break;
            case "REVIEWED":
                view.setBackgroundResource(R.drawable.bg_chip_active_yellow);
                view.setTextColor(Color.parseColor("#B7791F"));
                break;
            case "PENDING":
            default:
                view.setBackgroundResource(R.drawable.bg_chip_active_purple);
                view.setTextColor(Color.parseColor("#6A42C2"));
                break;
        }
    }

    @NonNull
    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "--";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(timestamp.toDate());
    }

    @NonNull
    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @NonNull
    private String safeUpper(String value, String fallback) {
        return safeText(value, fallback).toUpperCase(Locale.ROOT);
    }
}