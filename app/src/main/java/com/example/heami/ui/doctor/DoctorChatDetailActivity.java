package com.example.heami.ui.doctor;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class DoctorChatDetailActivity extends AppCompatActivity {

    private ImageButton btnChatDetailBack;
    private ImageButton btnChatDetailCall;
    private ImageButton btnChatDetailSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_chat_detail);

        initViews();
        setupActions();
    }

    private void initViews() {
        btnChatDetailBack = findViewById(R.id.btnChatDetailBack);
        btnChatDetailCall = findViewById(R.id.btnChatDetailCall);
        btnChatDetailSend = findViewById(R.id.btnChatDetailSend);
    }

    private void setupActions() {
        btnChatDetailBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnChatDetailCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DoctorChatDetailActivity.this, "Đang kết nối cuộc gọi tư vấn...", Toast.LENGTH_SHORT).show();
            }
        });

        btnChatDetailSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DoctorChatDetailActivity.this, "Tin nhắn đã gửi!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
