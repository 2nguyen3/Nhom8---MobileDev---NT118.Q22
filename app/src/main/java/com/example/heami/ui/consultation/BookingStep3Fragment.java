package com.example.heami.ui.consultation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.heami.R;
import com.example.heami.data.models.BookingModel;

public class BookingStep3Fragment extends Fragment {

    private TextView txtTransactionId, txtFinalDateTime, txtFinalPackage, txtFinalFormat, txtFinalAmount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_step3, container, false);
        initViews(view);
        displayBookingData();
        return view;
    }

    private void initViews(View v) {
        txtTransactionId = v.findViewById(R.id.txtTransactionId);
        txtFinalDateTime = v.findViewById(R.id.txtFinalDateTime);
        txtFinalPackage = v.findViewById(R.id.txtFinalPackage);
        txtFinalFormat = v.findViewById(R.id.txtFinalFormat);
        txtFinalAmount = v.findViewById(R.id.txtFinalAmount);
    }

    private void displayBookingData() {
        if (getActivity() instanceof BookingFlowActivity) {
            BookingFlowActivity activity = (BookingFlowActivity) getActivity();
            BookingModel booking = activity.getBookingModel();
            
            if (booking != null) {
                // Lấy sessionId thực tế được trả về từ Firestore sau khi lưu thành công
                String sessionId = activity.getLastSessionId();
                if (sessionId != null && !sessionId.isEmpty()) {
                    // Lấy 8 ký tự cuối của Document ID và viết hoa để tạo mã giao dịch đẹp
                    String displayId = sessionId.length() > 8 
                        ? sessionId.substring(sessionId.length() - 8).toUpperCase() 
                        : sessionId.toUpperCase();
                    txtTransactionId.setText("#HEAMI-" + displayId);
                } else {
                    txtTransactionId.setText("#HEAMI-SUCCESS");
                }

                String dateTime = booking.getDate() + " · " + booking.getTime();
                txtFinalDateTime.setText(dateTime);
                
                txtFinalPackage.setText(booking.getPackageType());
                txtFinalFormat.setText(booking.getFormatType());
                txtFinalAmount.setText(booking.getPrice());
            }
        }
    }
}
