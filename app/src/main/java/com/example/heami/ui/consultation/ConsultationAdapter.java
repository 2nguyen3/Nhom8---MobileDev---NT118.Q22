package com.example.heami.ui.consultation;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.data.models.ConsultationModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConsultationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_UPCOMING = 0;
    private static final int TYPE_HISTORY = 1;

    private List<ConsultationModel> consultations;
    private Context context;
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ConsultationAdapter(Context context, List<ConsultationModel> consultations) {
        this.context = context;
        this.consultations = consultations;
    }

    @Override
    public int getItemViewType(int position) {
        String status = consultations.get(position).getStatus();
        if ("BOOKED".equals(status)) {
            return TYPE_UPCOMING;
        }
        return TYPE_HISTORY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HISTORY) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_consultation_history, parent, false);
            return new HistoryViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_consultation, parent, false);
        return new UpcomingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ConsultationModel model = consultations.get(position);

        if (holder instanceof HistoryViewHolder) {
            bindHistoryItem((HistoryViewHolder) holder, model, position);
        } else if (holder instanceof UpcomingViewHolder) {
            bindUpcomingItem((UpcomingViewHolder) holder, model);
        }
    }

    private void bindHistoryItem(HistoryViewHolder holder, ConsultationModel model, int position) {
        holder.txtDoctorName.setText(model.getDoctorName() != null ? model.getDoctorName() : "N/A");
        holder.txtPriceHeader.setText(String.format("%,.0fđ", model.getPrice()));
        
        if (model.getStartTime() != null) {
            holder.txtBookingDate.setText(dayFormat.format(model.getStartTime().toDate()));
        }

        // Status Badge Logic
        if ("COMPLETED".equals(model.getStatus())) {
            holder.txtStatusBadge.setText("● Hoàn thành");
            holder.txtStatusBadge.setTextColor(Color.parseColor("#00BFA5"));
            holder.txtStatusBadge.setBackgroundResource(R.drawable.bg_tag_teal);
            
            holder.layoutFeedbackContent.setVisibility(View.VISIBLE);
            holder.txtCancelMessage.setVisibility(View.GONE);
            
            if (model.getUserFeedback() != null) {
                Object comment = model.getUserFeedback().get("comment");
                Object rating = model.getUserFeedback().get("rating");
                holder.txtUserComment.setText(comment != null ? "\"" + comment.toString() + "\"" : "");
                
                if (rating instanceof Number) {
                    holder.ratingBar.setRating(((Number) rating).floatValue());
                } else if (rating instanceof String) {
                    try {
                        holder.ratingBar.setRating(Float.parseFloat((String) rating));
                    } catch (Exception e) {
                        holder.ratingBar.setRating(0f);
                    }
                }
            }
        } else {
            holder.txtStatusBadge.setText("✕ Đã hủy");
            holder.txtStatusBadge.setTextColor(Color.parseColor("#FF5252"));
            holder.txtStatusBadge.setBackgroundResource(R.drawable.bg_tag_red);
            
            holder.layoutFeedbackContent.setVisibility(View.GONE);
            holder.txtCancelMessage.setVisibility(View.VISIBLE);
        }

        // Details
        holder.txtTransactionId.setText(model.getTransactionId());
        holder.txtDuration.setText(model.getPackageType());
        holder.txtFormat.setText(model.getFormatType());
        holder.txtTotalPrice.setText(String.format("%,.0fđ", model.getPrice()));

        // Expand/Collapse Logic
        boolean isExpanded = model.isExpanded();
        holder.layoutDetail.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.imgChevron.setRotation(isExpanded ? 180f : 0f);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            model.setExpanded(!model.isExpanded());
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView.getParent(), new AutoTransition());
            notifyItemChanged(pos);
        });

        Glide.with(context)
                .load(model.getDoctorAvatar())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(holder.imgDoctorAvatar);
    }

    private void bindUpcomingItem(UpcomingViewHolder holder, ConsultationModel model) {
        holder.txtDoctorName.setText(model.getDoctorName());
        holder.txtDoctorTitle.setText("Chuyên gia tâm lý"); 
        holder.txtFormat.setText(model.getFormatType());
        holder.txtPackage.setText(model.getPackageType());

        if (model.getStartTime() != null) {
            Date startDate = model.getStartTime().toDate();
            holder.txtDate.setText(dayFormat.format(startDate));
            String timeRange = timeFormat.format(startDate);
            if (model.getEndTime() != null) {
                long diffInMs = model.getEndTime().toDate().getTime() - startDate.getTime();
                long diffInMinutes = diffInMs / (60 * 1000);
                timeRange += " - " + diffInMinutes + " phút";
            }
            holder.txtTime.setText(timeRange);
        }

        holder.txtHint.setVisibility(View.GONE);
        setupUpcomingStatus(holder, model);

        if (model.getFormatType() != null && model.getFormatType().toLowerCase().contains("video")) {
            holder.imgFormatIcon.setImageResource(R.drawable.ic_video_call);
            holder.imgFormatIcon.setColorFilter(Color.parseColor("#E8507A"));
        } else {
            holder.imgFormatIcon.setImageResource(R.drawable.ic_community_chat);
            holder.imgFormatIcon.setColorFilter(Color.parseColor("#6EDCD9"));
        }

        Glide.with(context)
                .load(model.getDoctorAvatar())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(holder.imgDoctorAvatar);
    }

    private void setupUpcomingStatus(UpcomingViewHolder holder, ConsultationModel model) {
        holder.txtStatusBadge.setText("● Sắp diễn ra");
        holder.txtStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF0F5")));
        holder.txtStatusBadge.setTextColor(Color.parseColor("#E8507A"));
        holder.btnJoin.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return consultations.size();
    }

    public static class UpcomingViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgDoctorAvatar;
        TextView txtDoctorName, txtDoctorTitle, txtStatusBadge, txtDate, txtTime, txtFormat, txtPackage, txtHint;
        ImageView imgFormatIcon;
        AppCompatButton btnJoin;

        public UpcomingViewHolder(@NonNull View itemView) {
            super(itemView);
            imgDoctorAvatar = itemView.findViewById(R.id.imgDoctorAvatar);
            txtDoctorName = itemView.findViewById(R.id.txtDoctorName);
            txtDoctorTitle = itemView.findViewById(R.id.txtDoctorTitle);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtFormat = itemView.findViewById(R.id.txtFormat);
            txtPackage = itemView.findViewById(R.id.txtPackage);
            txtHint = itemView.findViewById(R.id.txtHint);
            imgFormatIcon = itemView.findViewById(R.id.imgFormatIcon);
            btnJoin = itemView.findViewById(R.id.btnJoin);
        }
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgDoctorAvatar;
        ImageView imgChevron;
        TextView txtDoctorName, txtPriceHeader, txtBookingDate, txtStatusBadge;
        TextView txtTransactionId, txtDuration, txtFormat, txtTotalPrice, txtUserComment, txtCancelMessage;
        LinearLayout layoutDetail, layoutFeedbackContent;
        RatingBar ratingBar;

        public HistoryViewHolder(@NonNull View v) {
            super(v);
            imgDoctorAvatar = v.findViewById(R.id.imgDoctorAvatar);
            imgChevron = v.findViewById(R.id.imgChevron);
            txtDoctorName = v.findViewById(R.id.txtDoctorName);
            txtPriceHeader = v.findViewById(R.id.txtPriceHeader);
            txtBookingDate = v.findViewById(R.id.txtBookingDate);
            txtStatusBadge = v.findViewById(R.id.txtStatusBadge);
            txtTransactionId = v.findViewById(R.id.txtTransactionId);
            txtDuration = v.findViewById(R.id.txtDuration);
            txtFormat = v.findViewById(R.id.txtFormat);
            txtTotalPrice = v.findViewById(R.id.txtTotalPrice);
            txtUserComment = v.findViewById(R.id.txtUserComment);
            txtCancelMessage = v.findViewById(R.id.txtCancelMessage);
            layoutDetail = v.findViewById(R.id.layoutDetail);
            layoutFeedbackContent = v.findViewById(R.id.layoutFeedbackContent);
            ratingBar = v.findViewById(R.id.ratingBar);
        }
    }
}
