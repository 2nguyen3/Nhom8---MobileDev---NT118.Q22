package com.example.heami.ui.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.ui.auth.LoginActivity;
import com.example.heami.utils.ExitDialogHelper;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;

public class DoctorHomeActivity extends AppCompatActivity {

    private TextView txtDoctorGreetingTime;
    private TextView txtDoctorGreetingTitle;
    private ImageView imgDoctorAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home);

        initViews();
        setupActions();
        DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_OVERVIEW);
        ExitDialogHelper.registerExitHandler(this);
    }

    private void initViews() {
        txtDoctorGreetingTime = findViewById(R.id.txtDoctorGreetingTime);
        txtDoctorGreetingTitle = findViewById(R.id.txtDoctorGreetingTitle);
        imgDoctorAvatar = findViewById(R.id.imgDoctorAvatar);
    }

    private void setupActions() {
        updateTimeGreeting();

        String uid = "doc_001";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        FirebaseFirestore.getInstance().collection("doctors").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("full_name");
                        String avatarUrl = documentSnapshot.getString("avatar_url");

                        if (fullName != null && !fullName.isEmpty()) {
                            txtDoctorGreetingTitle.setText(fullName);
                        } else {
                            txtDoctorGreetingTitle.setText("Bác sĩ Minh Anh");
                        }

                        if (avatarUrl != null && !avatarUrl.isEmpty() && imgDoctorAvatar != null) {
                            Glide.with(DoctorHomeActivity.this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.img_doctor_1)
                                    .into(imgDoctorAvatar);
                        }
                    } else {
                        txtDoctorGreetingTitle.setText("Bác sĩ Minh Anh");
                    }
                })
                .addOnFailureListener(e -> {
                    txtDoctorGreetingTitle.setText("Bác sĩ Minh Anh");
                });
    }

    private void updateTimeGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 4 && hour < 10) {
            greeting = "Chào buổi sáng";
        } else if (hour >= 10 && hour < 13) {
            greeting = "Chào buổi trưa";
        } else if (hour >= 13 && hour < 18) {
            greeting = "Chào buổi chiều";
        } else {
            greeting = "Chào buổi tối";
        }
        if (txtDoctorGreetingTime != null) {
            txtDoctorGreetingTime.setText(greeting);
        }
    }
}
