package com.example.heami.ui.consultation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.data.models.BookingModel;

public class BookingStep2Fragment extends Fragment {

    private ImageView imgDoctor;
    private TextView tvDoctorName, tvSessionType, tvAmount, tvBookingTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_step2, container, false);
        initViews(view);
        displayBookingData();
        return view;
    }

    private void initViews(View v) {
        imgDoctor = v.findViewById(R.id.imgDoctor);
        tvDoctorName = v.findViewById(R.id.tvDoctorName);
        tvSessionType = v.findViewById(R.id.tvSessionType);
        tvAmount = v.findViewById(R.id.tvAmount);
        tvBookingTime = v.findViewById(R.id.tvBookingTime);
    }

    private void displayBookingData() {
        if (getActivity() instanceof BookingFlowActivity) {
            BookingModel booking = ((BookingFlowActivity) getActivity()).getBookingModel();
            if (booking != null) {
                tvDoctorName.setText(booking.getDoctorName());
                
                String sessionInfo = booking.getPackageType() + " • " + 
                                    (booking.getFormatType().equals("Chat") ? "Phiên Chat" : "Phiên Video");
                tvSessionType.setText(sessionInfo);

                // Hiển thị giá (xử lý bỏ ký tự 'đ' và dấu '.' nếu cần để hiển thị số to)
                String priceStr = booking.getPrice().replace("đ", "").trim();
                tvAmount.setText(priceStr);

                String dateTime = booking.getDate() + " • " + booking.getTime();
                tvBookingTime.setText(dateTime);

                if (getContext() != null) {
                    Glide.with(getContext())
                        .load(booking.getDoctorAvatar())
                        .placeholder(R.drawable.img_doctor_1)
                        .into(imgDoctor);
                }
            }
        }
    }
}
