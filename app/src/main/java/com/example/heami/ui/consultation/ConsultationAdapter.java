package com.example.heami.ui.consultation;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class ConsultationAdapter extends RecyclerView.Adapter<ConsultationAdapter.ViewHolder> {

    private List<ConsultationModel> consultations;
    private Context context;
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ConsultationAdapter(List<ConsultationModel> consultations) {
        this.consultations = consultations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_consultation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConsultationModel model = consultations.get(position);

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

        // Luôn ẩn dòng Hint theo yêu cầu
        holder.txtHint.setVisibility(View.GONE);

        // Status Badge and Join Button Logic
        setupStatusAndAction(holder, model);

        // Icon for Format
        if (model.getFormatType() != null && model.getFormatType().toLowerCase().contains("video")) {
            holder.imgFormatIcon.setImageResource(R.drawable.ic_video_call);
            holder.txtFormat.setText("Gọi Video");
            holder.txtFormat.setTextColor(Color.parseColor("#E8507A"));
            holder.imgFormatIcon.setColorFilter(Color.parseColor("#E8507A"));
        } else {
            holder.imgFormatIcon.setImageResource(R.drawable.ic_community_chat);
            holder.txtFormat.setText("Chat");
            holder.txtFormat.setTextColor(Color.parseColor("#6EDCD9"));
            holder.imgFormatIcon.setColorFilter(Color.parseColor("#6EDCD9"));
        }

        Glide.with(context)
                .load(model.getDoctorAvatar())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(holder.imgDoctorAvatar);
    }

    private void setupStatusAndAction(ViewHolder holder, ConsultationModel model) {
        String status = model.getStatus();

        // Mặc định ẩn nút Join
        holder.btnJoin.setVisibility(View.GONE);

        if ("BOOKED".equals(status)) {
            holder.txtStatusBadge.setText("● Sắp diễn ra");
            holder.txtStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF0F5")));
            holder.txtStatusBadge.setTextColor(Color.parseColor("#E8507A"));
            
            // Luôn hiển thị nút Vào phòng và cho phép nhấn (không check thời gian)
            holder.btnJoin.setVisibility(View.VISIBLE);
            holder.btnJoin.setEnabled(true);
            
            holder.btnJoin.setOnClickListener(v -> {
                // Luồng xử lý mở ChatRoom sau này
                // Intent intent = new Intent(context, ChatRoomActivity.class);
                // context.startActivity(intent);
            });

        } else if ("COMPLETED".equals(status)) {
            holder.txtStatusBadge.setText("● Đã hoàn thành");
            holder.txtStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0F2F1")));
            holder.txtStatusBadge.setTextColor(Color.parseColor("#00BFA5"));
        } else if ("CANCELED".equals(status)) {
            holder.txtStatusBadge.setText("● Đã hủy");
            holder.txtStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
            holder.txtStatusBadge.setTextColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return consultations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgDoctorAvatar;
        TextView txtDoctorName, txtDoctorTitle, txtStatusBadge, txtDate, txtTime, txtFormat, txtPackage, txtHint;
        ImageView imgFormatIcon;
        AppCompatButton btnJoin;

        public ViewHolder(@NonNull View itemView) {
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
}
