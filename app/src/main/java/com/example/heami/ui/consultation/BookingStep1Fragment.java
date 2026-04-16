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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class BookingStep1Fragment extends Fragment {

    private ImageView imgDoctor;
    private TextView txtDoctorName, txtDoctorDegree, txtDoctorSpecialty;
    private TextView txtUserName, txtUserNickname, txtUserPhone, txtUserEmail;
    private TextView txtBookingDateTime, txtBookingPackage, txtBookingFormat;
    private TextView txtBookingPrice, txtBookingTotal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_step1, container, false);
        initViews(view);
        displayBookingData();
        displayUserData();
        return view;
    }

    private void initViews(View v) {
        imgDoctor = v.findViewById(R.id.imgDoctorStep1);
        txtDoctorName = v.findViewById(R.id.txtDoctorNameStep1);
        txtDoctorDegree = v.findViewById(R.id.txtDoctorDegreeStep1);
        txtDoctorSpecialty = v.findViewById(R.id.txtDoctorSpecialtyStep1);

        txtUserName = v.findViewById(R.id.txtUserNameStep1);
        txtUserNickname = v.findViewById(R.id.txtUserNicknameStep1);
        txtUserPhone = v.findViewById(R.id.txtUserPhoneStep1);
        txtUserEmail = v.findViewById(R.id.txtUserEmailStep1);

        txtBookingDateTime = v.findViewById(R.id.txtBookingDateTimeStep1);
        txtBookingPackage = v.findViewById(R.id.txtBookingPackageStep1);
        txtBookingFormat = v.findViewById(R.id.txtBookingFormatStep1);

        txtBookingPrice = v.findViewById(R.id.txtBookingPriceStep1);
        txtBookingTotal = v.findViewById(R.id.txtBookingTotalStep1);
    }

    private void displayBookingData() {
        if (getActivity() instanceof BookingFlowActivity) {
            BookingModel booking = ((BookingFlowActivity) getActivity()).getBookingModel();
            if (booking != null) {
                txtDoctorName.setText(booking.getDoctorName());
                txtDoctorDegree.setText(booking.getDoctorSpecialty());
                txtDoctorSpecialty.setText(booking.getDoctorSpecialty());
                
                if (getContext() != null) {
                    Glide.with(getContext())
                        .load(booking.getDoctorAvatar())
                        .placeholder(R.drawable.img_doctor_1)
                        .into(imgDoctor);
                }

                String dateTime = booking.getDate() + " · " + booking.getTime();
                txtBookingDateTime.setText(dateTime);
                txtBookingPackage.setText(booking.getPackageType());
                txtBookingFormat.setText(booking.getFormatType());

                txtBookingPrice.setText(booking.getPrice());
                txtBookingTotal.setText(booking.getPrice());
            }
        }
    }

    private void displayUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Hiển thị tên (Họ và tên) lấy từ Auth DisplayName
            String displayName = user.getDisplayName();
            txtUserName.setText(displayName != null && !displayName.isEmpty() ? displayName : "Người dùng Heami");
            
            txtUserEmail.setText(user.getEmail());
            txtUserPhone.setText(user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() 
                ? user.getPhoneNumber() : "Chưa cập nhật");

            // Lấy Nickname từ bảng "users" trên Firestore
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nickname = documentSnapshot.getString("nickname");
                        if (nickname != null && !nickname.isEmpty()) {
                            // Hiển thị nickname có dấu @ phía trước cho đúng phong cách
                            txtUserNickname.setText("@" + nickname);
                        } else {
                            txtUserNickname.setText("Người dùng Heami");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    txtUserNickname.setText("Người dùng Heami");
                });
        }
    }
}
