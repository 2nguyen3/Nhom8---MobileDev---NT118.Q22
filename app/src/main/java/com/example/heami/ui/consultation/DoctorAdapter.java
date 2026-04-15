package com.example.heami.ui.consultation;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.data.models.DoctorModel;
import com.example.heami.utils.FavoriteManager;
import java.text.DecimalFormat;
import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private List<DoctorModel> doctorList;
    private Context context;
    private DecimalFormat formatter = new DecimalFormat("#,###");

    public DoctorAdapter(List<DoctorModel> doctorList, Context context) {
        this.doctorList = doctorList;
        this.context = context;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        DoctorModel doctor = doctorList.get(position);

        holder.txtName.setText(doctor.getFull_name());
        holder.txtSpecialty.setText(doctor.getSpecialization().isEmpty() ? "" : doctor.getSpecialization().get(0));
        holder.txtLocation.setText(doctor.getLocation());
        holder.txtRating.setText(String.valueOf(doctor.getRating_avg()));
        holder.txtReviewCount.setText("(" + doctor.getReview_count() + " reviews)");
        holder.txtPrice.setText(formatter.format(doctor.getMin_price()) + "đ");

        Glide.with(context)
                .load(doctor.getAvatar_url())
                .placeholder(R.drawable.img_doctor_1)
                .error(R.drawable.img_doctor_1)
                .into(holder.imgAvatar);
        
        // Cập nhật trạng thái trực tuyến
        if (doctor.isIs_online()) {
            holder.badgeAvailable.setVisibility(View.VISIBLE);
            startPulseAnimation(holder.dotOnline);
        } else {
            holder.badgeAvailable.setVisibility(View.GONE);
            stopPulseAnimation(holder.dotOnline);
        }

        // Hiển thị trạng thái yêu thích từ FavoriteManager
        boolean isFav = FavoriteManager.getInstance(context).isFavorite(doctor.getDoctor_id());
        holder.btnBookmark.setImageResource(isFav ? R.drawable.ic_doctor_favorite_filled : R.drawable.ic_doctor_bookmark);

        // Khóa không cho người dùng kích hoạt ở trang này
        holder.btnBookmark.setEnabled(false);
        holder.btnBookmark.setClickable(false);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DoctorDetailActivity.class);
            intent.putExtra("doctor_id", doctor.getDoctor_id());
            intent.putExtra("doctor_name", doctor.getFull_name());
            intent.putExtra("doctor_degree", doctor.getDegree());
            intent.putExtra("doctor_specialty", holder.txtSpecialty.getText().toString());
            intent.putExtra("doctor_location", doctor.getLocation());
            intent.putExtra("doctor_rating", String.valueOf(doctor.getRating_avg()));
            intent.putExtra("doctor_sessions", String.valueOf(doctor.getTotal_sessions()) + "+");
            intent.putExtra("doctor_experience", doctor.getExperience_years() + " năm");
            intent.putExtra("doctor_intro", doctor.getBio());
            intent.putExtra("doctor_avatar", doctor.getAvatar_url());
            context.startActivity(intent);
        });
    }

    private void startPulseAnimation(View view) {
        if (view == null) return;

        if (view.getTag() instanceof AnimatorSet) {
            return;
        }

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.4f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.4f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.6f, 1.0f);

        long duration = 1000;
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());

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
        return doctorList.size();
    }

    public void updateList(List<DoctorModel> newList) {
        this.doctorList = newList;
        notifyDataSetChanged();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtSpecialty, txtLocation, txtRating, txtReviewCount, txtPrice;
        View badgeAvailable, dotOnline;
        ImageButton btnBookmark;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgDoctorAvatar);
            txtName = itemView.findViewById(R.id.txtDoctorName);
            txtSpecialty = itemView.findViewById(R.id.txtDoctorSpecialty);
            txtLocation = itemView.findViewById(R.id.txtDoctorLocation);
            txtRating = itemView.findViewById(R.id.txtDoctorRating);
            txtReviewCount = itemView.findViewById(R.id.txtDoctorReviewCount);
            txtPrice = itemView.findViewById(R.id.txtDoctorPrice);
            badgeAvailable = itemView.findViewById(R.id.badgeDoctorAvailable);
            dotOnline = itemView.findViewById(R.id.dotDoctorAvailable);
            btnBookmark = itemView.findViewById(R.id.btnDoctorBookmark);
        }
    }
}
