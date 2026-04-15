package com.example.heami.ui.community;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class CommunityChatListActivity extends AppCompatActivity {

    private ImageButton btnBackCommunityChatList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat_list);

        bindViews();
        setupActions();
    }

    private void bindViews() {
        btnBackCommunityChatList = findViewById(R.id.btnBackCommunityChatList);
    }

    private void setupActions() {
        if (btnBackCommunityChatList != null) {
            btnBackCommunityChatList.setOnClickListener(v -> finish());
        }
    }
}