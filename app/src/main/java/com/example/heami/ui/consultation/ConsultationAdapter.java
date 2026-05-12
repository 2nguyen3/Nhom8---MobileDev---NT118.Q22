package com.example.heami.ui.consultation;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.transition.Fade;
import android.transition.ChangeBounds;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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

/**
 * Adapter dùng chung cho cả 2 tab.
 * Dùng biến isHistoryMode để quyết định inflate layout nào.
 * Tránh hoàn toàn getItemViewType() đa dạng để không bị ClassCastException.
 */
public class ConsultationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "HEAMI_DEBUG";

    private List<ConsultationModel> consultations;
    private Context context;
    private boolean isHistoryMode;

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, dd/MM/yyyy", new Locale("vi", "VN"));
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ConsultationAdapter(Context context, List<ConsultationModel> consultations, boolean isHistoryMode) {
        this.context = context;
        this.consultations = consultations;
        this.isHistoryMode = isHistoryMode;
    }

    /** Gọi khi switch tab để thay đổi mode + dataset cùng lúc */
    public void setData(List<ConsultationModel> newData, boolean historyMode) {
        this.isHistoryMode = historyMode;
        this.consultations = newData;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        // Luôn trả về 1 viewtype duy nhất dựa trên mode hiện tại
        // Không bao giờ dùng status của model để quyết định → tránh ClassCastException
        return isHistoryMode ? 1 : 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 1) {
            try {
                View v = inflater.inflate(R.layout.item_consultation_history, parent, false);
                return new HistoryViewHolder(v);
            } catch (Throwable t) {
                Log.e(TAG, "Lỗi inflate item_consultation_history: " + t.getMessage(), t);
                View emptyView = new View(parent.getContext());
                emptyView.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, 100));
                return new HistoryViewHolder(emptyView);
            }
        }
        return new UpcomingViewHolder(inflater.inflate(R.layout.item_consultation, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (consultations == null || position < 0 || position >= consultations.size()) return;
        ConsultationModel model = consultations.get(position);
        if (model == null) return;

        try {
            if (holder instanceof HistoryViewHolder) {
                bindHistoryItem((HistoryViewHolder) holder, model);
            } else if (holder instanceof UpcomingViewHolder) {
                bindUpcomingItem((UpcomingViewHolder) holder, model);
            }
        } catch (Throwable t) {
            Log.e(TAG, "onBindViewHolder error at position " + position + ": " + t.getMessage(), t);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HISTORY BIND
    // ─────────────────────────────────────────────────────────
    private void bindHistoryItem(HistoryViewHolder holder, ConsultationModel model) {
        // ── Tên bác sĩ + ảnh ──
        holder.txtDoctorName.setText(model.getDoctorName() != null ? model.getDoctorName() : "N/A");
        Glide.with(context)
                .load(model.getDoctorAvatar())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(holder.imgDoctorAvatar);


        // ── Ngày đặt lịch ──
        if (model.getStartTime() != null) {
            holder.txtBookingDate.setText(dayFormat.format(model.getStartTime().toDate()));
        } else if (model.getBookedAt() != null) {
            holder.txtBookingDate.setText(dayFormat.format(model.getBookedAt().toDate()));
        } else {
            holder.txtBookingDate.setText("—");
        }

        String status = model.getStatus() != null ? model.getStatus().toUpperCase(Locale.ROOT) : "";

        // ── Mã hóa đơn ──
        String txId = model.getTransactionId();
        if (holder.txtTransactionId != null)
            holder.txtTransactionId.setText(txId != null && !txId.isEmpty() ? txId : "—");

        // ── Thời lượng (package type) ──
        String pkg = model.getPackageType();
        if (holder.txtDuration != null)
            holder.txtDuration.setText(pkg != null && !pkg.isEmpty() ? pkg : "—");

        // ── Hình thức ──
        String fmt = model.getFormatType();
        if (holder.txtFormat != null)
            holder.txtFormat.setText(fmt != null && !fmt.isEmpty() ? fmt : "—");

        // ── Phương thức thanh toán ──
        if (holder.txtPaymentMethod != null)
            holder.txtPaymentMethod.setText("VNPAY");

        // ── Tổng tiền (dạng đầy đủ) ──
        if (holder.txtTotalPrice != null)
            holder.txtTotalPrice.setText(formatMoneyFull(model.getPrice()));

        // ── Package Badge: ✦ <packageType> · <formatType> ──
        if (holder.txtPackageBadge != null) {
            boolean hasPkg = pkg != null && !pkg.isEmpty();
            boolean hasFmt = fmt != null && !fmt.isEmpty();
            if (hasPkg || hasFmt) {
                holder.txtPackageBadge.setVisibility(View.VISIBLE);
                StringBuilder badge = new StringBuilder();
                if ("COMPLETED".equals(status)) {
                    badge.append("✦ ");
                    holder.txtPackageBadge.setTextColor(Color.parseColor("#4A9292"));
                    holder.txtPackageBadge.setBackgroundResource(R.drawable.bg_light_teal_circle);
                } else {
                    badge.append("✕ ");
                    holder.txtPackageBadge.setTextColor(Color.parseColor("#FF5252"));
                    holder.txtPackageBadge.setBackgroundResource(R.drawable.bg_tag_red);
                }
                if (hasPkg) badge.append(pkg);
                if (hasPkg && hasFmt) badge.append(" · ");
                if (hasFmt) badge.append(fmt);
                holder.txtPackageBadge.setText(badge.toString());
            } else {
                holder.txtPackageBadge.setVisibility(View.GONE);
            }
        }

        // ── Badge trạng thái + feedback / cancel message ──
        if ("COMPLETED".equals(status)) {
            holder.txtStatusBadge.setText("● Hoàn thành");
            holder.txtStatusBadge.setTextColor(Color.parseColor("#00BFA5"));
            holder.txtStatusBadge.setBackgroundResource(R.drawable.bg_tag_teal);

            if (holder.layoutFeedbackContent != null) holder.layoutFeedbackContent.setVisibility(View.VISIBLE);
            if (holder.txtCancelMessage != null) holder.txtCancelMessage.setVisibility(View.GONE);

            // Feedback: comment + rating
            if (model.getUserFeedback() != null && !model.getUserFeedback().isEmpty()) {
                Object comment = model.getUserFeedback().get("comment");
                Object rating  = model.getUserFeedback().get("rating");
                if (holder.txtUserComment != null)
                    holder.txtUserComment.setText(
                            comment != null && !comment.toString().isEmpty()
                            ? "\"" + comment + "\""
                            : "");
                if (holder.ratingBar != null && rating instanceof Number)
                    holder.ratingBar.setRating(((Number) rating).floatValue());
                else if (holder.ratingBar != null)
                    holder.ratingBar.setRating(0f);
            } else {
                // Chưa có feedback — ẩn comment, rating = 0
                if (holder.txtUserComment != null) holder.txtUserComment.setText("");
                if (holder.ratingBar != null) holder.ratingBar.setRating(0f);
            }
        } else {
            // CANCELED / CANCELLED
            holder.txtStatusBadge.setText("✕ Đã hủy");
            holder.txtStatusBadge.setTextColor(Color.parseColor("#FF5252"));
            holder.txtStatusBadge.setBackgroundResource(R.drawable.bg_tag_red);
            if (holder.layoutFeedbackContent != null) holder.layoutFeedbackContent.setVisibility(View.GONE);
            if (holder.txtCancelMessage != null) {
                holder.txtCancelMessage.setVisibility(View.VISIBLE);
                String note = model.getNote();
                holder.txtCancelMessage.setText(
                        note != null && !note.isEmpty()
                        ? "\"" + note + "\""
                        : "Bạn đã hủy phiên tư vấn này.");
            }
        }

        // Collapsed by default
        boolean expanded = model.isIs_expanded();
        holder.layoutDetail.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.imgChevron.setRotation(expanded ? 180f : 0f);

        holder.itemView.setOnClickListener(v -> {
            boolean nextExpanded = !model.isIs_expanded();
            model.setIs_expanded(nextExpanded);
            if (holder.itemView.getParent() instanceof ViewGroup) {
                TransitionSet set = new TransitionSet()
                        .setOrdering(TransitionSet.ORDERING_TOGETHER)
                        .addTransition(new Fade(Fade.OUT))
                        .addTransition(new ChangeBounds())
                        .addTransition(new Fade(Fade.IN))
                        .setDuration(250);
                TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView.getParent(), set);
            }
            holder.layoutDetail.setVisibility(nextExpanded ? View.VISIBLE : View.GONE);
            holder.imgChevron.animate().rotation(nextExpanded ? 180f : 0f).setDuration(250).start();
        });

        Glide.with(context)
                .load(model.getDoctorAvatar())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(holder.imgDoctorAvatar);
    }

    // ─────────────────────────────────────────────────────────
    //  UPCOMING BIND
    // ─────────────────────────────────────────────────────────
    private void bindUpcomingItem(UpcomingViewHolder holder, ConsultationModel model) {
        holder.txtDoctorName.setText(model.getDoctorName() != null ? model.getDoctorName() : "Bác sĩ chuyên khoa");
        holder.txtDoctorTitle.setText("Chuyên gia tâm lý");

        Glide.with(context)
                .load(model.getDoctorAvatar())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(holder.imgDoctorAvatar);

        if (model.getStartTime() != null) {
            Date start = model.getStartTime().toDate();
            holder.txtDate.setText(dayFormat.format(start));
            String timeRange = timeFormat.format(start);
            if (model.getEndTime() != null) {
                long durationMin = (model.getEndTime().getSeconds() - model.getStartTime().getSeconds()) / 60;
                timeRange += " - " + durationMin + " phút";
            }
            holder.txtTime.setText(timeRange);
        }

        holder.txtFormat.setText(model.getFormatType() != null ? model.getFormatType() : "Trực tuyến");
        holder.txtPackage.setText(model.getPackageType() != null ? model.getPackageType() : "Tiêu chuẩn");

        if (model.getFormatType() != null && model.getFormatType().toLowerCase().contains("video")) {
            holder.imgFormatIcon.setImageResource(R.drawable.ic_video_call);
            holder.imgFormatIcon.setColorFilter(Color.parseColor("#E8507A"));
        } else {
            holder.imgFormatIcon.setImageResource(R.drawable.ic_doctor_chat);
            holder.imgFormatIcon.setColorFilter(Color.parseColor("#6EDCD9"));
        }

        if (holder.txtStatusBadge != null) {
            holder.txtStatusBadge.setText("● Sắp diễn ra");
            holder.txtStatusBadge.setTextColor(Color.parseColor("#E8507A"));
            holder.txtStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF0F5")));
        }

        String hint = (model.getNote() != null && !model.getNote().isEmpty())
                ? model.getNote() : "Phiên tư vấn sẽ bắt đầu đúng giờ.";
        holder.txtHint.setText("\"" + hint + "\"");

        if (holder.badgeDoctorOnline != null) {
            if (model.isDoctor_online()) {
                holder.badgeDoctorOnline.setVisibility(View.VISIBLE);
                startPulseAnimation(holder.dotDoctorOnline);
            } else {
                holder.badgeDoctorOnline.setVisibility(View.GONE);
                stopPulseAnimation(holder.dotDoctorOnline);
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  FORMAT MONEY
    // ─────────────────────────────────────────────────────────
    /** Dạng gọn hiển thị trên header card: 1.5Tr, 350K, 500đ */
    private String formatMoney(double amount) {
        if (amount >= 1_000_000) {
            double m = amount / 1_000_000.0;
            if (m == (long) m) return String.format(Locale.getDefault(), "%dTr", (long) m);
            return String.format(Locale.getDefault(), "%.1fTr", m);
        } else if (amount >= 1_000) {
            double k = amount / 1_000.0;
            if (k == (long) k) return String.format(Locale.getDefault(), "%dK", (long) k);
            return String.format(Locale.getDefault(), "%.1fK", k);
        }
        return String.format(Locale.getDefault(), "%,.0fđ", amount);
    }

    /** Dạng đầy đủ hiển thị trong chi tiết: 1.575.000đ */
    private String formatMoneyFull(double amount) {
        return String.format(Locale.getDefault(), "%,.0fđ", amount);
    }

    // ─────────────────────────────────────────────────────────
    //  PULSE ANIMATION
    // ─────────────────────────────────────────────────────────
    private void startPulseAnimation(View view) {
        if (view == null) return;
        if (view.getTag() instanceof AnimatorSet) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.4f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.4f, 1.0f);
        ObjectAnimator alpha  = ObjectAnimator.ofFloat(view, View.ALPHA,   1.0f, 0.6f, 1.0f);
        scaleX.setDuration(1000); scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setDuration(1000); scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setDuration(1000);  alpha.setRepeatCount(ValueAnimator.INFINITE);

        AccelerateDecelerateInterpolator interp = new AccelerateDecelerateInterpolator();
        scaleX.setInterpolator(interp);
        scaleY.setInterpolator(interp);
        alpha.setInterpolator(interp);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.start();
        view.setTag(set);
    }

    private void stopPulseAnimation(View view) {
        if (view == null) return;
        Object tag = view.getTag();
        if (tag instanceof AnimatorSet) {
            ((AnimatorSet) tag).cancel();
            view.setTag(null);
        }
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(1.0f);
    }

    @Override
    public int getItemCount() {
        return consultations != null ? consultations.size() : 0;
    }

    // ─────────────────────────────────────────────────────────
    //  VIEW HOLDERS
    // ─────────────────────────────────────────────────────────
    public static class UpcomingViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgDoctorAvatar;
        TextView txtDoctorName, txtDoctorTitle, txtStatusBadge, txtDate, txtTime, txtFormat, txtPackage, txtHint;
        ImageView imgFormatIcon;
        AppCompatButton btnJoin;
        LinearLayout badgeDoctorOnline;
        View dotDoctorOnline;

        public UpcomingViewHolder(@NonNull View v) {
            super(v);
            imgDoctorAvatar = v.findViewById(R.id.imgDoctorAvatar);
            txtDoctorName   = v.findViewById(R.id.txtDoctorName);
            txtDoctorTitle  = v.findViewById(R.id.txtDoctorTitle);
            txtStatusBadge  = v.findViewById(R.id.txtStatusBadge);
            txtDate         = v.findViewById(R.id.txtDate);
            txtTime         = v.findViewById(R.id.txtTime);
            txtFormat       = v.findViewById(R.id.txtFormat);
            txtPackage      = v.findViewById(R.id.txtPackage);
            txtHint         = v.findViewById(R.id.txtHint);
            imgFormatIcon   = v.findViewById(R.id.imgFormatIcon);
            btnJoin         = v.findViewById(R.id.btnJoin);
            badgeDoctorOnline = v.findViewById(R.id.badgeDoctorOnline);
            if (badgeDoctorOnline != null && badgeDoctorOnline.getChildCount() > 0) {
                dotDoctorOnline = badgeDoctorOnline.getChildAt(0);
            }
        }
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgDoctorAvatar;
        ImageView imgChevron;
        TextView txtDoctorName, txtBookingDate, txtStatusBadge;
        TextView txtTransactionId, txtDuration, txtFormat, txtPaymentMethod;
        TextView txtTotalPrice, txtUserComment, txtCancelMessage, txtPackageBadge;
        LinearLayout layoutDetail, layoutFeedbackContent;
        RatingBar ratingBar;

        public HistoryViewHolder(@NonNull View v) {
            super(v);
            imgDoctorAvatar       = v.findViewById(R.id.imgDoctorAvatar);
            imgChevron            = v.findViewById(R.id.imgChevron);
            txtDoctorName         = v.findViewById(R.id.txtDoctorName);
            txtBookingDate        = v.findViewById(R.id.txtBookingDate);
            txtStatusBadge        = v.findViewById(R.id.txtStatusBadge);
            txtTransactionId      = v.findViewById(R.id.txtTransactionId);
            txtDuration           = v.findViewById(R.id.txtDuration);
            txtFormat             = v.findViewById(R.id.txtFormat);
            txtPaymentMethod      = v.findViewById(R.id.txtPaymentMethod);   // ID mới
            txtTotalPrice         = v.findViewById(R.id.txtTotalPrice);
            txtUserComment        = v.findViewById(R.id.txtUserComment);
            txtCancelMessage      = v.findViewById(R.id.txtCancelMessage);
            txtPackageBadge       = v.findViewById(R.id.txtPackageBadge);    // ID mới
            layoutDetail          = v.findViewById(R.id.layoutDetail);
            layoutFeedbackContent = v.findViewById(R.id.layoutFeedbackContent);
            ratingBar             = v.findViewById(R.id.ratingBar);
        }
    }
}
