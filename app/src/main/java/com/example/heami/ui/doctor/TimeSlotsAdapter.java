package com.example.heami.ui.doctor;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.TimeSlotsModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TimeSlotsAdapter extends RecyclerView.Adapter<TimeSlotsAdapter.ViewHolder> {

    private final Context context;
    private final List<TimeSlotsModel> slotList;

    public TimeSlotsAdapter(Context context, List<TimeSlotsModel> slotList) {
        this.context = context;
        this.slotList = slotList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeSlotsModel model = slotList.get(position);
        if (model == null) return;

        // ========================================================================
        // 1. LẤY THỜI GIAN VÀ TRẠNG THÁI TỪ BẢNG LICH_HEN
        // ========================================================================
        if (model.getStart_time() != null) {
            java.util.Date date = model.getStart_time().toDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.tvScheduledTime.setText(dateFormat.format(date) + " · " + timeFormat.format(date));
        } else {
            holder.tvScheduledTime.setText("--/-- · --:--");
        }

        String status = model.getStatus();
        if ("booked".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("Sắp diễn ra");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#2C7A7B"));
            holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E6FFFA")));
        } else if ("completed".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("Hoàn thành");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#718096"));
            holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EDF2F7")));
        } else {
            holder.tvStatusBadge.setText(status != null ? status : "Khác");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#718096"));
            holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EDF2F7")));
        }

        // Đặt avatar mặc định là 🦋
        if (holder.imgAvatar != null) {
            holder.imgAvatar.setText("🦋");
        }

        // ========================================================================
        // 2. TỰ ĐỘNG ĐỒNG BỘ TRUY VẤN DỮ LIỆU THẬT TỪ "consultations"
        // ========================================================================
        String sessionId = model.getSession_id();

        if (sessionId != null && !sessionId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("consultations")
                    .document(sessionId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("patient_name");
                            String moodText = documentSnapshot.getString("mood_text");
                            String moodEmoji = documentSnapshot.getString("mood_emoji");
                            String duration = documentSnapshot.getString("duration");
                            String method = documentSnapshot.getString("method_text");
                            String textColor = documentSnapshot.getString("mood_color");
                            String bgColor = documentSnapshot.getString("mood_bg_color");

                            holder.tvUsername.setText(name != null ? name : "Khách hàng");
                            holder.tvMoodText.setText(moodText != null ? moodText : "Ổn định");
                            holder.tvMoodEmoji.setText(moodEmoji != null ? moodEmoji : "😊");
                            holder.tvDuration.setText(duration != null ? duration : "30 phút");
                            holder.tvMethodText.setText(method != null ? method : "Chat");

                            try {
                                int textHex = Color.parseColor(textColor != null ? textColor : "#2C7A7B");
                                int bgHex = Color.parseColor(bgColor != null ? bgColor : "#E6FFFA");
                                holder.tvMoodText.setTextColor(textHex);
                                holder.layoutMoodTag.setBackgroundTintList(ColorStateList.valueOf(bgHex));
                            } catch (Exception e) {
                                Log.e("HEAMI_COLOR", "Mã màu Hex sai định dạng: " + e.getMessage());
                            }

                            if ("Chat".equalsIgnoreCase(method)) {
                                holder.imgMethodIcon.setImageResource(android.R.drawable.stat_notify_chat);
                            } else if ("Cuộc gọi".equalsIgnoreCase(method) || "Call".equalsIgnoreCase(method)) {
                                holder.imgMethodIcon.setImageResource(android.R.drawable.ic_menu_call);
                            } else {
                                holder.imgMethodIcon.setImageResource(android.R.drawable.ic_menu_mylocation);
                            }
                        } else {
                            setFallbackUI(holder);
                        }
                    })
                    .addOnFailureListener(e -> setFallbackUI(holder));
        } else {
            setFallbackUI(holder);
        }

        // ========================================================================
        // 3. LOGIC CLICK ITEM CHUYỂN QUA TRANG DETAIL TƯƠNG ỨNG
        // ========================================================================
        holder.itemView.setOnClickListener(view -> {
            // 🌟 Đã chuyển Log debug xuống đúng vị trí an toàn
            Log.d("HEAMI_DEBUG", "Đang nhấn chuyển trang: slot_id = " + model.getSlot_id());
            Log.d("HEAMI_DEBUG", "Đang nhấn chuyển trang: session_id = " + model.getSession_id());

            if (model.getSlot_id() == null || model.getSlot_id().isEmpty()) {
                Toast.makeText(context, "Thiếu thông tin lịch hẹn!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(context, SessionDetailActivity.class);
            // Đồng bộ khóa viết thường đồng nhất 100% toàn bộ hệ thống
            intent.putExtra("slot_id", model.getSlot_id());
            intent.putExtra("session_id", model.getSession_id());
            context.startActivity(intent);
        });
    }

    private void setFallbackUI(ViewHolder holder) {
        holder.tvUsername.setText("Khách hàng");
        holder.tvMoodText.setText("Ổn định");
        holder.tvMoodEmoji.setText("😊");
        holder.tvDuration.setText("30 phút");
        holder.tvMethodText.setText("Chat");
        holder.layoutMoodTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E6FFFA")));
        holder.tvMoodText.setTextColor(Color.parseColor("#2C7A7B"));
    }

    @Override
    public int getItemCount() {
        return slotList != null ? slotList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvMoodEmoji, tvMoodText, tvStatusBadge, tvDuration, tvMethodText, tvScheduledTime;
        TextView imgAvatar;
        LinearLayout layoutMoodTag;
        ImageView imgMethodIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvMoodEmoji = itemView.findViewById(R.id.tv_mood_emoji);
            tvMoodText = itemView.findViewById(R.id.tv_mood_text);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvMethodText = itemView.findViewById(R.id.tv_method_text);
            tvScheduledTime = itemView.findViewById(R.id.tv_scheduled_time);
            layoutMoodTag = itemView.findViewById(R.id.layout_mood_tag);
            imgMethodIcon = itemView.findViewById(R.id.img_method_icon);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
        }
    }
}