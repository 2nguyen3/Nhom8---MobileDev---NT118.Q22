package com.example.heami.ui.community;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class PostCommentActivity extends AppCompatActivity {

    private View layoutPostCommentSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_comment);

        bindViews();
        prepareSheetIntro();
        setupActions();
        startSheetIntroAnimation();
    }

    private void bindViews() {
        layoutPostCommentSheet = findViewById(R.id.layoutPostCommentSheet);
    }

    private void prepareSheetIntro() {
        if (layoutPostCommentSheet != null) {
            layoutPostCommentSheet.setTranslationY(1400f);
            layoutPostCommentSheet.setAlpha(1f);
        }
    }

    private void startSheetIntroAnimation() {
        if (layoutPostCommentSheet != null) {
            layoutPostCommentSheet.animate()
                    .translationY(0f)
                    .setDuration(420)
                    .start();
        }
    }

    private void setupActions() {
        View root = findViewById(R.id.postCommentRoot);
        if (root != null) {
            root.setOnClickListener(v -> finish());
        }

        if (layoutPostCommentSheet != null) {
            layoutPostCommentSheet.setOnClickListener(v -> {
                // chặn click xuyên xuống overlay
            });
        }

        View btnClose = findViewById(R.id.btnClosePostComment);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }
}