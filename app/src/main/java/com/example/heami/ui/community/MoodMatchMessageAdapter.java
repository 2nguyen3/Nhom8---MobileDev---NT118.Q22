package com.example.heami.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ChatMessageModel;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MoodMatchMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessageModel> messageList = new ArrayList<>();
    private final String currentUserId;
    private final String partnerUserId;
    private final String partnerEmoji;

    public MoodMatchMessageAdapter(
            @NonNull String currentUserId,
            @NonNull String partnerUserId,
            @NonNull String partnerEmoji
    ) {
        this.currentUserId = currentUserId;
        this.partnerUserId = partnerUserId;
        this.partnerEmoji = partnerEmoji;
    }

    public void submitList(@NonNull List<ChatMessageModel> newList) {
        messageList.clear();
        messageList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageModel message = messageList.get(position);
        String senderId = safeText(message.getSender_id(), "");
        return senderId.equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        }

        View view = inflater.inflate(R.layout.item_message_received, parent, false);
        return new ReceivedMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageModel message = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtSentMessage;
        private final TextView txtSentTime;
        private final TextView txtSentStatus;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSentMessage = itemView.findViewById(R.id.txtSentMessage);
            txtSentTime = itemView.findViewById(R.id.txtSentTime);
            txtSentStatus = itemView.findViewById(R.id.txtSentStatus);
        }

        void bind(@NonNull ChatMessageModel message) {
            txtSentMessage.setText(safeText(message.getText(), ""));
            txtSentTime.setText(formatTime(message.getCreated_at()));
            txtSentStatus.setText(resolveSentStatus(message));
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtReceivedAvatarEmoji;
        private final TextView txtReceivedMessage;
        private final TextView txtReceivedTime;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtReceivedAvatarEmoji = itemView.findViewById(R.id.txtReceivedAvatarEmoji);
            txtReceivedMessage = itemView.findViewById(R.id.txtReceivedMessage);
            txtReceivedTime = itemView.findViewById(R.id.txtReceivedTime);
        }

        void bind(@NonNull ChatMessageModel message) {
            txtReceivedAvatarEmoji.setText(partnerEmoji);
            txtReceivedMessage.setText(safeText(message.getText(), ""));
            txtReceivedTime.setText(formatTime(message.getCreated_at()));
        }
    }

    @NonNull
    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "Vừa xong";
        }
        return new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(timestamp.toDate());
    }

    @NonNull
    private String resolveSentStatus(@NonNull ChatMessageModel message) {
        List<String> seenIds = message.getSeen_user_ids();
        if (seenIds != null && seenIds.contains(partnerUserId)) {
            return "Đã xem";
        }

        List<String> deliveredIds = message.getDelivered_user_ids();
        if (deliveredIds != null && deliveredIds.contains(partnerUserId)) {
            return "Đã nhận";
        }

        if (message.getCreated_at() == null) {
            return "Đang gửi";
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
}