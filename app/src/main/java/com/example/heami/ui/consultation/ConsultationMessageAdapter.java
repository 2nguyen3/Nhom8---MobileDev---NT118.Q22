package com.example.heami.ui.consultation;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ChatMessageModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConsultationMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_RECEIVED = 0;
    private static final int VIEW_TYPE_SENT = 1;

    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final List<ChatMessageModel> messages = new ArrayList<>();
    private String doctorAvatarBadge = "🩺";

    public ConsultationMessageAdapter(@NonNull String currentUserId) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public void setDoctorAvatarBadge(@NonNull String doctorAvatarBadge) {
        if (!TextUtils.isEmpty(doctorAvatarBadge.trim())) {
            this.doctorAvatarBadge = doctorAvatarBadge.trim();
        }
        notifyDataSetChanged();
    }

    public void submitList(@NonNull List<ChatMessageModel> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageModel model = messages.get(position);
        String senderId = model.getSender_id() != null ? model.getSender_id().trim() : "";
        return senderId.equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageModel model = messages.get(position);

        if (holder instanceof SentMessageViewHolder) {
            bindSentMessage((SentMessageViewHolder) holder, model);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            bindReceivedMessage((ReceivedMessageViewHolder) holder, model);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private void bindSentMessage(@NonNull SentMessageViewHolder holder, @NonNull ChatMessageModel model) {
        holder.txtSentMessage.setText(safeText(model.getText(), ""));
        holder.txtSentTime.setText(resolveTime(model));
        holder.txtSentStatus.setText(resolveStatusLabel(model));
    }

    private void bindReceivedMessage(@NonNull ReceivedMessageViewHolder holder, @NonNull ChatMessageModel model) {
        holder.txtReceivedMessage.setText(safeText(model.getText(), ""));
        holder.txtReceivedTime.setText(resolveTime(model));
        holder.txtReceivedAvatarEmoji.setText(doctorAvatarBadge);
    }

    @NonNull
    private String resolveTime(@NonNull ChatMessageModel model) {
        if (model.getCreated_at() != null && model.getCreated_at().toDate() != null) {
            return timeFormat.format(model.getCreated_at().toDate());
        }

        Long clientCreatedAt = model.getClient_created_at_ms();
        if (clientCreatedAt != null && clientCreatedAt > 0L) {
            return timeFormat.format(clientCreatedAt);
        }

        return "--:--";
    }

    @NonNull
    private String resolveStatusLabel(@NonNull ChatMessageModel model) {
        List<String> seenIds = model.getSeen_user_ids();
        if (seenIds != null && !seenIds.isEmpty()) {
            return "Đã xem";
        }

        List<String> deliveredIds = model.getDelivered_user_ids();
        if (deliveredIds != null && !deliveredIds.isEmpty()) {
            return "Đã nhận";
        }

        return "Đã gửi";
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtSentMessage;
        TextView txtSentTime;
        TextView txtSentStatus;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSentMessage = itemView.findViewById(R.id.txtSentMessage);
            txtSentTime = itemView.findViewById(R.id.txtSentTime);
            txtSentStatus = itemView.findViewById(R.id.txtSentStatus);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtReceivedAvatarEmoji;
        TextView txtReceivedMessage;
        TextView txtReceivedTime;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtReceivedAvatarEmoji = itemView.findViewById(R.id.txtReceivedAvatarEmoji);
            txtReceivedMessage = itemView.findViewById(R.id.txtReceivedMessage);
            txtReceivedTime = itemView.findViewById(R.id.txtReceivedTime);
        }
    }
}