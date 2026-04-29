package com.example.heami.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.ChatRoomModel;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommunityChatRoomAdapter extends RecyclerView.Adapter<CommunityChatRoomAdapter.ChatRoomViewHolder> {

    public interface OnChatRoomClickListener {
        void onChatRoomClick(@NonNull ChatRoomModel room);
    }

    private final List<ChatRoomModel> roomList = new ArrayList<>();
    private final String currentUserId;
    private final OnChatRoomClickListener listener;

    public CommunityChatRoomAdapter(
            @NonNull String currentUserId,
            @NonNull OnChatRoomClickListener listener
    ) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void submitList(@NonNull List<ChatRoomModel> newList) {
        roomList.clear();
        roomList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoomModel room = roomList.get(position);
        holder.bind(room);
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtAvatarEmoji;
        private final TextView txtPartnerName;
        private final TextView txtRoomTime;
        private final TextView txtLastMessage;
        private final TextView txtUnreadBadge;

        ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAvatarEmoji = itemView.findViewById(R.id.txtChatRoomAvatarEmoji);
            txtPartnerName = itemView.findViewById(R.id.txtChatRoomPartnerName);
            txtRoomTime = itemView.findViewById(R.id.txtChatRoomTime);
            txtLastMessage = itemView.findViewById(R.id.txtChatRoomLastMessage);
            txtUnreadBadge = itemView.findViewById(R.id.txtChatRoomUnreadBadge);
        }

        void bind(@NonNull ChatRoomModel room) {
            String partnerName = resolvePartnerName(room);
            String moodTag = safeText(room.getMatch_mood_tag(), "stress");
            String lastMessage = safeText(room.getLast_message(), "");

            txtAvatarEmoji.setText(resolveAvatarEmoji(moodTag));
            txtPartnerName.setText(partnerName);
            txtRoomTime.setText(resolveTimeAgo(room.getLast_message_at(), room.getCreated_at()));
            txtLastMessage.setText(
                    lastMessage.isEmpty()
                            ? "Bạn vừa được kết nối qua Mood Match"
                            : lastMessage
            );

            int unreadCount = getUnreadCount(room);
            txtUnreadBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
            txtUnreadBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));

            itemView.setOnClickListener(v -> listener.onChatRoomClick(room));
        }

        @NonNull
        private String resolvePartnerName(@NonNull ChatRoomModel room) {
            List<String> memberIds = room.getMember_ids();
            List<String> memberNames = room.getMember_names();

            if (memberIds != null) {
                for (int i = 0; i < memberIds.size(); i++) {
                    String memberId = safeText(memberIds.get(i), "");
                    if (!memberId.equals(currentUserId)) {
                        if (memberNames != null && i < memberNames.size()) {
                            return safeText(memberNames.get(i), "Người bạn ẩn danh");
                        }
                    }
                }
            }

            return "Người bạn ẩn danh";
        }

        private int getUnreadCount(@NonNull ChatRoomModel room) {
            if (room.getUnread_count_map() == null) {
                return 0;
            }

            Long value = room.getUnread_count_map().get(currentUserId);
            return value != null ? value.intValue() : 0;
        }

        @NonNull
        private String resolveTimeAgo(Timestamp primaryTime, Timestamp fallbackTime) {
            Timestamp finalTime = primaryTime != null ? primaryTime : fallbackTime;
            if (finalTime == null) {
                return "Vừa xong";
            }

            long diffMs = System.currentTimeMillis() - finalTime.toDate().getTime();
            if (diffMs < 0) diffMs = 0;

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);

            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            return days + " ngày trước";
        }

        @NonNull
        private String resolveAvatarEmoji(@NonNull String moodTag) {
            switch (moodTag) {
                case "happy":
                    return "😊";
                case "sad":
                    return "🥺";
                case "stress":
                    return "🫠";
                case "fear":
                    return "😟";
                case "angry":
                    return "😤";
                default:
                    return "🌸";
            }
        }

        @NonNull
        private String safeText(String value, String fallback) {
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }
            return value.trim();
        }
    }
}