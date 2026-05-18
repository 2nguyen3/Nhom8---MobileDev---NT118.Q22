package com.example.heami.ui.admin;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.data.models.AdminAccountListItem;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminAccountAdapter extends RecyclerView.Adapter<AdminAccountAdapter.AccountViewHolder> {

    public interface OnAccountClickListener {
        void onAccountClick(@NonNull AdminAccountListItem item);
    }

    private final List<AdminAccountListItem> items = new ArrayList<>();
    private final OnAccountClickListener listener;

    public AdminAccountAdapter(@NonNull OnAccountClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<AdminAccountListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        AdminAccountListItem item = items.get(position);

        holder.txtAccountName.setText(safeText(item.getDisplayName(), "Heami Account"));
        holder.txtAccountSubtitle.setText(safeText(item.getSubtitle(), "Tài khoản Heami"));

        String meta = safeText(item.getEmail(), "");
        if (!item.getAccountId().isEmpty()) {
            meta = meta.isEmpty()
                    ? "ID: " + item.getAccountId()
                    : meta + " • ID: " + item.getAccountId();
        }

        String lastSignIn = formatLastSignIn(item.getLastSignInAt());
        if (!lastSignIn.isEmpty()) {
            meta = meta.isEmpty()
                    ? lastSignIn
                    : meta + "\n" + lastSignIn;
        }

        holder.txtAccountMeta.setText(meta);

        holder.txtRoleBadge.setText(safeText(item.getRole(), "USER"));
        bindRoleBadge(holder.txtRoleBadge, item.getRole());

        holder.txtStatusBadge.setText(safeText(item.getStatus(), "ACTIVE"));
        bindStatusBadge(holder.txtStatusBadge, item.getStatus());

        holder.viewOnlineDot.setVisibility(item.isOnline() ? View.VISIBLE : View.GONE);

        String avatarUrl = safeText(item.getAvatarUrl(), "");
        if (avatarUrl.isEmpty()) {
            holder.imgAccountAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(holder.imgAccountAvatar);
        }

        holder.itemView.setOnClickListener(v -> listener.onAccountClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAccountAvatar;
        View viewOnlineDot;
        TextView txtAccountName;
        TextView txtAccountSubtitle;
        TextView txtAccountMeta;
        TextView txtRoleBadge;
        TextView txtStatusBadge;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAccountAvatar = itemView.findViewById(R.id.imgAccountAvatar);
            viewOnlineDot = itemView.findViewById(R.id.viewOnlineDot);
            txtAccountName = itemView.findViewById(R.id.txtAccountName);
            txtAccountSubtitle = itemView.findViewById(R.id.txtAccountSubtitle);
            txtAccountMeta = itemView.findViewById(R.id.txtAccountMeta);
            txtRoleBadge = itemView.findViewById(R.id.txtRoleBadge);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
        }
    }

    private void bindRoleBadge(@NonNull TextView view, String role) {
        String normalized = safeText(role, "USER").toUpperCase(Locale.ROOT);

        switch (normalized) {
            case "DOCTOR":
                view.setBackgroundResource(R.drawable.bg_chip_active_purple);
                view.setTextColor(Color.parseColor("#6A42C2"));
                break;

            case "ADMIN":
                view.setBackgroundResource(R.drawable.bg_chip_active_yellow);
                view.setTextColor(Color.parseColor("#B7791F"));
                break;

            case "USER":
            default:
                view.setBackgroundResource(R.drawable.bg_chip_active_teal);
                view.setTextColor(Color.parseColor("#1D9E92"));
                break;
        }
    }

    private void bindStatusBadge(@NonNull TextView view, String status) {
        String normalized = safeText(status, "ACTIVE").toUpperCase(Locale.ROOT);

        switch (normalized) {
            case "BANNED":
                view.setBackgroundResource(R.drawable.bg_chip_inactive);
                view.setTextColor(Color.parseColor("#C2516A"));
                break;

            case "PENDING_VERIFY":
                view.setBackgroundResource(R.drawable.bg_chip_active_yellow);
                view.setTextColor(Color.parseColor("#B7791F"));
                break;

            case "ACTIVE":
            default:
                view.setBackgroundResource(R.drawable.bg_chip_active_teal);
                view.setTextColor(Color.parseColor("#1D9E92"));
                break;
        }
    }

    @NonNull
    private String formatLastSignIn(Timestamp timestamp) {
        if (timestamp == null) return "";
        return "Đăng nhập gần nhất: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(timestamp.toDate());
    }

    @NonNull
    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value.trim();
    }
}